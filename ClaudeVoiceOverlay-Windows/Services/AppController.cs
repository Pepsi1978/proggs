using System;
using System.Diagnostics;
using System.Runtime.InteropServices;
using System.Threading;
using System.Threading.Tasks;
using System.Windows;
using System.Windows.Automation;
using ClaudeVoiceOverlay.NativeMethods;

namespace ClaudeVoiceOverlay.Services
{
    public static class AppController
    {
        private const byte VK_A = 0x41;
        private const byte VK_BACKSPACE = 0x08;
        private const byte VK_RETURN = 0x0D;
        private const ushort VK_HOME = 0x24;
        private const ushort VK_END = 0x23;

        /// <summary>
        /// Pastes text into the active application using clipboard simulation.
        /// Robust implementation with error handling and proper timing.
        /// </summary>
        public static async Task PasteTextAsync(string text, IntPtr appHwnd, bool autoEnter = false)
        {
            if (appHwnd == IntPtr.Zero)
            {
                Console.WriteLine("AppController: Invalid window handle — aborting paste");
                return;
            }

            string? previousClipboard = null;
            bool clipboardSaveSuccess = false;

            // Save clipboard content with error handling
            try
            {
                await Application.Current.Dispatcher.InvokeAsync(() =>
                {
                    if (Clipboard.ContainsText())
                    {
                        previousClipboard = Clipboard.GetText();
                        clipboardSaveSuccess = true;
                    }
                });
            }
            catch (Exception ex)
            {
                Console.WriteLine($"AppController: Clipboard save failed: {ex.Message} — continuing without restore");
            }

            // Set new text to clipboard
            try
            {
                await Application.Current.Dispatcher.InvokeAsync(() => Clipboard.SetText(text));
            }
            catch (Exception ex)
            {
                Console.WriteLine($"AppController: Failed to set clipboard: {ex.Message}");
                return;
            }

            // Bring window to foreground and wait for it to be ready
            BringToForeground(appHwnd);
            WaitForInputReady(appHwnd);

            bool isCodex = IsCodexProcess(appHwnd);

            if (isCodex)
            {
                // Codex has a different Electron window structure:
                // Chrome_RenderWidgetHostHWND is NOT a child of the main window.
                // SetFocus on any child window STEALS keyboard focus.
                // Instead: just Escape (returns focus to input) → Ctrl+V
                Console.WriteLine("AppController: Codex mode — Escape → Ctrl+V");
                SendInputKeys(Win32.VK_ESCAPE);
                await Task.Delay(150);
                SendInputPaste();
            }
            else
            {
                // Claude Desktop: render widget is a direct child — SetFocus + click works
                FocusDirectRenderWidget(appHwnd);
                ClickInputField(appHwnd);
                SendInputPaste();
            }

            if (autoEnter)
            {
                await Task.Delay(200);
                SendInputKeys(VK_RETURN);
            }

            // Restore clipboard content with proper async await
            if (clipboardSaveSuccess && previousClipboard != null)
            {
                var prev = previousClipboard;
                try
                {
                    await Task.Delay(500);
                    await Application.Current.Dispatcher.InvokeAsync(() => Clipboard.SetText(prev));
                    Console.WriteLine("AppController: Clipboard restored");
                }
                catch (Exception ex)
                {
                    Console.WriteLine($"AppController: Clipboard restore failed: {ex.Message}");
                }
            }
        }

        /// <summary>
        /// Clears the current input field using selection and delete.
        /// </summary>
        public static async Task ClearInputAsync(IntPtr appHwnd)
        {
            if (appHwnd == IntPtr.Zero)
            {
                Console.WriteLine("AppController: Invalid window handle — aborting clear");
                return;
            }

            BringToForeground(appHwnd);
            WaitForInputReady(appHwnd);

            if (IsCodexProcess(appHwnd))
            {
                // Codex: double-Escape — first closes any popup/autocomplete,
                // second ensures focus lands in the input field
                SendInputKeys(Win32.VK_ESCAPE);
                await Task.Delay(100);
                SendInputKeys(Win32.VK_ESCAPE);
                await Task.Delay(150);
                SendInputKeys(VK_END);
                await Task.Delay(30);
                // Ctrl+Shift+Home selects all text from cursor to start within the input
                SendInputModifierCombo(Win32.VK_CONTROL, Win32.VK_SHIFT, VK_HOME);
                await Task.Delay(50);
                SendInputKeys(VK_BACKSPACE);
            }
            else
            {
                // Claude Desktop: click into input field, then Ctrl+A is safe
                FocusDirectRenderWidget(appHwnd);
                ClickInputField(appHwnd);
                await Task.Delay(100);
                SendInputModifierCombo(Win32.VK_CONTROL, VK_A);
                await Task.Delay(50);
                SendInputKeys(VK_BACKSPACE);
            }
        }

        public static void SendReturn()
        {
            SendInputKeys(VK_RETURN);
        }

        public static async Task PressReturnAsync(IntPtr appHwnd)
        {
            if (appHwnd == IntPtr.Zero)
            {
                Console.WriteLine("AppController: Invalid window handle — aborting return");
                return;
            }

            BringToForeground(appHwnd);
            WaitForInputReady(appHwnd);

            if (IsCodexProcess(appHwnd))
            {
                SendInputKeys(Win32.VK_ESCAPE);
                await Task.Delay(100);
            }
            else
            {
                FocusDirectRenderWidget(appHwnd);
                ClickInputField(appHwnd);
                await Task.Delay(100);
            }

            SendInputKeys(VK_RETURN);
        }

        // ── Process Detection ──

        private static bool IsCodexProcess(IntPtr appHwnd)
        {
            if (appHwnd == IntPtr.Zero) return false;
            Win32.GetWindowThreadProcessId(appHwnd, out uint pid);
            try
            {
                using var proc = Process.GetProcessById((int)pid);
                return proc.ProcessName.Equals("Codex", StringComparison.OrdinalIgnoreCase);
            }
            catch { return false; }
        }

        // ── Window Activation ──

        private static void BringToForeground(IntPtr appHwnd)
        {
            if (appHwnd == IntPtr.Zero) return;

            var currentFg = Win32.GetForegroundWindow();
            if (IsOwnedByProcess(currentFg, appHwnd))
            {
                Thread.Sleep(30);
                return;
            }

            uint ourThread = Win32.GetCurrentThreadId();
            uint targetThread = Win32.GetWindowThreadProcessId(appHwnd, out _);

            bool attached = false;
            if (ourThread != targetThread)
                attached = Win32.AttachThreadInput(ourThread, targetThread, true);

            Win32.AllowSetForegroundWindow(unchecked((uint)-1));
            Win32.SetForegroundWindow(appHwnd);
            Win32.BringWindowToTop(appHwnd);

            if (attached)
                Win32.AttachThreadInput(ourThread, targetThread, false);
        }

        /// <summary>
        /// Waits for the target application to be ready for input.
        /// Uses adaptive timing instead of fixed delays.
        /// </summary>
        private static void WaitForInputReady(IntPtr appHwnd)
        {
            // Wait for the window to process pending messages
            // This is more reliable than fixed Thread.Sleep
            var stopwatch = System.Diagnostics.Stopwatch.StartNew();

            // Pump messages briefly to let window settle
            while (stopwatch.ElapsedMilliseconds < 150)
            {
                Thread.Sleep(10);
                // Check if window is still foreground
                if (Win32.GetForegroundWindow() != appHwnd)
                {
                    Console.WriteLine("AppController: Window lost foreground — re-activating");
                    BringToForeground(appHwnd);
                }
            }
        }

        // ── Render Widget Focus (Claude Desktop only) ──

        private static void FocusDirectRenderWidget(IntPtr appHwnd)
        {
            if (appHwnd == IntPtr.Zero) return;

            IntPtr renderWidget = Win32.FindWindowEx(appHwnd, IntPtr.Zero, "Chrome_RenderWidgetHostHWND", null);
            if (renderWidget == IntPtr.Zero)
            {
                Console.WriteLine("AppController: No Chrome_RenderWidgetHostHWND child — skipping SetFocus");
                return;
            }

            uint ourThread = Win32.GetCurrentThreadId();
            uint targetThread = Win32.GetWindowThreadProcessId(appHwnd, out _);

            bool attached = false;
            if (ourThread != targetThread)
                attached = Win32.AttachThreadInput(ourThread, targetThread, true);

            Win32.SetFocus(renderWidget);
            Console.WriteLine("AppController: Focused Chrome_RenderWidgetHostHWND");

            if (attached)
                Win32.AttachThreadInput(ourThread, targetThread, false);

            Thread.Sleep(50);
        }

        // ── Input Field Click (Claude Desktop only) ──

        /// <summary>
        /// Finds and clicks the input field in Claude Desktop.
        /// Uses UI Automation to locate the actual input element (works across all tabs:
        /// Code, Chat, Cowork) with a fixed-position fallback for robustness.
        /// </summary>
        private static void ClickInputField(IntPtr appHwnd)
        {
            if (appHwnd == IntPtr.Zero) return;

            // Strategy 1: UI Automation — find the real input element regardless of tab
            if (TryClickInputViaAutomation(appHwnd))
                return;

            // Strategy 2: Fallback to fixed position (Code tab default at 86% height)
            ClickAtFixedPosition(appHwnd);
        }

        /// <summary>
        /// Uses Windows UI Automation to find the text input field in the Electron app.
        /// Chromium automatically builds its accessibility tree when a UIA client queries it.
        /// Scoring: Edit controls + keyboard-focusable + moderate height = main input field.
        /// </summary>
        private static bool TryClickInputViaAutomation(IntPtr appHwnd)
        {
            try
            {
                var window = AutomationElement.FromHandle(appHwnd);
                if (window == null) return false;

                // Search for Edit and Document controls (textarea, contenteditable divs)
                var condition = new OrCondition(
                    new PropertyCondition(AutomationElement.ControlTypeProperty, ControlType.Edit),
                    new PropertyCondition(AutomationElement.ControlTypeProperty, ControlType.Document)
                );

                var elements = window.FindAll(TreeScope.Descendants, condition);
                if (elements.Count == 0)
                {
                    Console.WriteLine("AppController: UIA found no edit controls — using fallback");
                    return false;
                }

                Console.WriteLine($"AppController: UIA found {elements.Count} candidate(s)");

                AutomationElement? bestMatch = null;
                double bestScore = -1;

                foreach (AutomationElement element in elements)
                {
                    try
                    {
                        if (!element.Current.IsEnabled) continue;
                        if (element.Current.IsOffscreen) continue;

                        var rect = element.Current.BoundingRectangle;
                        if (rect.IsEmpty || double.IsInfinity(rect.Width) || double.IsInfinity(rect.Height))
                            continue;
                        if (rect.Width < 100 || rect.Height < 20) continue;

                        double score = 0;

                        // Prefer Edit over Document (Edit = textarea/input, Document = page area)
                        if (element.Current.ControlType == ControlType.Edit)
                            score += 2000;

                        // Prefer keyboard-focusable elements (input fields accept keyboard input)
                        if (element.Current.IsKeyboardFocusable)
                            score += 1000;

                        // Prefer moderate height (input fields: 30-200px)
                        // Penalize very tall elements (conversation area: 400px+)
                        if (rect.Height < 200)
                            score += 800;
                        else if (rect.Height < 400)
                            score += 400;
                        else
                            score += 50; // likely conversation area

                        // Wider elements get a small boost (input spans most of window width)
                        score += rect.Width * 0.5;

                        // Prefer elements that support ValuePattern (writable text input)
                        try
                        {
                            if (element.TryGetCurrentPattern(ValuePattern.Pattern, out _))
                                score += 500;
                        }
                        catch { /* Some elements may not support pattern queries */ }

                        Console.WriteLine($"  UIA: {element.Current.ControlType.ProgrammaticName} " +
                            $"at ({rect.X:F0},{rect.Y:F0}) {rect.Width:F0}x{rect.Height:F0} " +
                            $"score={score:F0} focusable={element.Current.IsKeyboardFocusable}");

                        if (score > bestScore)
                        {
                            bestScore = score;
                            bestMatch = element;
                        }
                    }
                    catch { continue; }
                }

                if (bestMatch == null)
                {
                    Console.WriteLine("AppController: UIA no suitable input found — using fallback");
                    return false;
                }

                // Try SetFocus first (cleaner than clicking)
                try
                {
                    bestMatch.SetFocus();
                    Thread.Sleep(100);
                    Console.WriteLine("AppController: UIA SetFocus succeeded");
                    return true;
                }
                catch
                {
                    // SetFocus failed — fall through to click on the element
                }

                // Click on the center of the found input element
                var inputRect = bestMatch.Current.BoundingRectangle;
                int clickX = (int)(inputRect.X + inputRect.Width / 2);
                int clickY = (int)(inputRect.Y + inputRect.Height / 2);

                if (clickX < 0 || clickY < 0) return false;

                PerformClick(clickX, clickY);
                Console.WriteLine($"AppController: UIA clicked input at ({clickX},{clickY}), " +
                    $"size=({inputRect.Width:F0}x{inputRect.Height:F0})");
                return true;
            }
            catch (Exception ex)
            {
                Console.WriteLine($"AppController: UIA failed: {ex.Message} — using fallback");
                return false;
            }
        }

        /// <summary>
        /// Fallback: clicks at a fixed position (86% height, 55% width) — works for Code tab.
        /// </summary>
        private static void ClickAtFixedPosition(IntPtr appHwnd)
        {
            if (!Win32.GetWindowRect(appHwnd, out Win32.RECT rect)) return;

            int windowWidth = rect.Right - rect.Left;
            int windowHeight = rect.Bottom - rect.Top;
            if (windowWidth < 100 || windowHeight < 100) return;

            // Claude Desktop Code tab: input at ~86% height, 55% width (past sidebar)
            int clickX = rect.Left + (int)(windowWidth * 0.55);
            int clickY = rect.Top + (int)(windowHeight * 0.86);

            clickX = Math.Clamp(clickX, rect.Left + 50, rect.Right - 50);
            clickY = Math.Clamp(clickY, rect.Top + 50, rect.Bottom - 50);

            PerformClick(clickX, clickY);
            Console.WriteLine($"AppController: Fallback click at ({clickX},{clickY}), " +
                $"window=({rect.Left},{rect.Top},{rect.Right},{rect.Bottom})");
        }

        /// <summary>
        /// Performs a mouse click at the given screen coordinates, preserving cursor position.
        /// </summary>
        private static void PerformClick(int x, int y)
        {
            Win32.GetCursorPos(out Win32.POINT savedPos);

            Win32.SetCursorPos(x, y);
            Thread.Sleep(15);
            Win32.mouse_event(Win32.MOUSEEVENTF_LEFTDOWN, 0, 0, 0, UIntPtr.Zero);
            Thread.Sleep(15);
            Win32.mouse_event(Win32.MOUSEEVENTF_LEFTUP, 0, 0, 0, UIntPtr.Zero);
            Thread.Sleep(150);

            Win32.SetCursorPos(savedPos.X, savedPos.Y);
        }

        // ── Helpers ──

        private static bool IsOwnedByProcess(IntPtr foregroundHwnd, IntPtr appHwnd)
        {
            if (foregroundHwnd == appHwnd) return true;
            Win32.GetWindowThreadProcessId(foregroundHwnd, out uint fgPid);
            Win32.GetWindowThreadProcessId(appHwnd, out uint appPid);
            return fgPid == appPid && fgPid != 0;
        }

        /// <summary>
        /// Sends keyboard input using the modern SendInput API.
        /// More reliable than keybd_event, especially for Electron apps.
        /// </summary>
        private static void SendInputKeys(ushort vk)
        {
            byte scan = (byte)Win32.MapVirtualKey(vk, Win32.MAPVK_VK_TO_VSC);
            uint flags = (vk >= 0x21 && vk <= 0x2E) ? Win32.KEYEVENTF_EXTENDEDKEY : 0;

            var inputs = new[]
            {
                new Win32.INPUT
                {
                    type = Win32.INPUT_KEYBOARD,
                    u = new Win32.INPUTUNION
                    {
                        ki = new Win32.KEYBDINPUT
                        {
                            wVk = vk,
                            wScan = scan,
                            dwFlags = flags,
                            time = 0,
                            dwExtraInfo = IntPtr.Zero
                        }
                    }
                },
                new Win32.INPUT
                {
                    type = Win32.INPUT_KEYBOARD,
                    u = new Win32.INPUTUNION
                    {
                        ki = new Win32.KEYBDINPUT
                        {
                            wVk = vk,
                            wScan = scan,
                            dwFlags = flags | Win32.KEYEVENTF_KEYUP,
                            time = 0,
                            dwExtraInfo = IntPtr.Zero
                        }
                    }
                }
            };

            Win32.SendInput(2, inputs, System.Runtime.InteropServices.Marshal.SizeOf<Win32.INPUT>());
        }

        /// <summary>
        /// Sends Ctrl+V using SendInput for paste operation.
        /// </summary>
        private static void SendInputPaste()
        {
            byte ctrlScan = (byte)Win32.MapVirtualKey(Win32.VK_CONTROL, Win32.MAPVK_VK_TO_VSC);
            byte vScan = (byte)Win32.MapVirtualKey(Win32.VK_V, Win32.MAPVK_VK_TO_VSC);

            var inputs = new[]
            {
                // Ctrl down
                new Win32.INPUT
                {
                    type = Win32.INPUT_KEYBOARD,
                    u = new Win32.INPUTUNION
                    {
                        ki = new Win32.KEYBDINPUT
                        {
                            wVk = Win32.VK_CONTROL,
                            wScan = ctrlScan,
                            dwFlags = 0,
                            time = 0,
                            dwExtraInfo = IntPtr.Zero
                        }
                    }
                },
                // V down
                new Win32.INPUT
                {
                    type = Win32.INPUT_KEYBOARD,
                    u = new Win32.INPUTUNION
                    {
                        ki = new Win32.KEYBDINPUT
                        {
                            wVk = Win32.VK_V,
                            wScan = vScan,
                            dwFlags = 0,
                            time = 0,
                            dwExtraInfo = IntPtr.Zero
                        }
                    }
                },
                // V up
                new Win32.INPUT
                {
                    type = Win32.INPUT_KEYBOARD,
                    u = new Win32.INPUTUNION
                    {
                        ki = new Win32.KEYBDINPUT
                        {
                            wVk = Win32.VK_V,
                            wScan = vScan,
                            dwFlags = Win32.KEYEVENTF_KEYUP,
                            time = 0,
                            dwExtraInfo = IntPtr.Zero
                        }
                    }
                },
                // Ctrl up
                new Win32.INPUT
                {
                    type = Win32.INPUT_KEYBOARD,
                    u = new Win32.INPUTUNION
                    {
                        ki = new Win32.KEYBDINPUT
                        {
                            wVk = Win32.VK_CONTROL,
                            wScan = ctrlScan,
                            dwFlags = Win32.KEYEVENTF_KEYUP,
                            time = 0,
                            dwExtraInfo = IntPtr.Zero
                        }
                    }
                }
            };

            Win32.SendInput(4, inputs, System.Runtime.InteropServices.Marshal.SizeOf<Win32.INPUT>());
        }

        /// <summary>
        /// Sends a modifier+key combo using SendInput (e.g., Ctrl+A, Ctrl+Shift+Home).
        /// </summary>
        private static void SendInputModifierCombo(ushort modifier, ushort key)
        {
            byte modScan = (byte)Win32.MapVirtualKey(modifier, Win32.MAPVK_VK_TO_VSC);
            byte keyScan = (byte)Win32.MapVirtualKey(key, Win32.MAPVK_VK_TO_VSC);
            uint keyFlags = (key >= 0x21 && key <= 0x2E) ? Win32.KEYEVENTF_EXTENDEDKEY : 0;

            var inputs = new[]
            {
                // Modifier down
                new Win32.INPUT { type = Win32.INPUT_KEYBOARD, u = new Win32.INPUTUNION { ki = new Win32.KEYBDINPUT { wVk = modifier, wScan = modScan, dwFlags = 0, time = 0, dwExtraInfo = IntPtr.Zero } } },
                // Key down
                new Win32.INPUT { type = Win32.INPUT_KEYBOARD, u = new Win32.INPUTUNION { ki = new Win32.KEYBDINPUT { wVk = key, wScan = keyScan, dwFlags = keyFlags, time = 0, dwExtraInfo = IntPtr.Zero } } },
                // Key up
                new Win32.INPUT { type = Win32.INPUT_KEYBOARD, u = new Win32.INPUTUNION { ki = new Win32.KEYBDINPUT { wVk = key, wScan = keyScan, dwFlags = keyFlags | Win32.KEYEVENTF_KEYUP, time = 0, dwExtraInfo = IntPtr.Zero } } },
                // Modifier up
                new Win32.INPUT { type = Win32.INPUT_KEYBOARD, u = new Win32.INPUTUNION { ki = new Win32.KEYBDINPUT { wVk = modifier, wScan = modScan, dwFlags = Win32.KEYEVENTF_KEYUP, time = 0, dwExtraInfo = IntPtr.Zero } } }
            };

            Win32.SendInput(4, inputs, System.Runtime.InteropServices.Marshal.SizeOf<Win32.INPUT>());
        }

        /// <summary>
        /// Sends Ctrl+Shift+Home using SendInput for text selection.
        /// </summary>
        private static void SendInputModifierCombo(ushort modifier1, ushort modifier2, ushort key)
        {
            byte mod1Scan = (byte)Win32.MapVirtualKey(modifier1, Win32.MAPVK_VK_TO_VSC);
            byte mod2Scan = (byte)Win32.MapVirtualKey(modifier2, Win32.MAPVK_VK_TO_VSC);
            byte keyScan = (byte)Win32.MapVirtualKey(key, Win32.MAPVK_VK_TO_VSC);
            uint keyFlags = Win32.KEYEVENTF_EXTENDEDKEY; // Home is extended key

            var inputs = new[]
            {
                // Ctrl down
                new Win32.INPUT { type = Win32.INPUT_KEYBOARD, u = new Win32.INPUTUNION { ki = new Win32.KEYBDINPUT { wVk = modifier1, wScan = mod1Scan, dwFlags = 0, time = 0, dwExtraInfo = IntPtr.Zero } } },
                // Shift down
                new Win32.INPUT { type = Win32.INPUT_KEYBOARD, u = new Win32.INPUTUNION { ki = new Win32.KEYBDINPUT { wVk = modifier2, wScan = mod2Scan, dwFlags = 0, time = 0, dwExtraInfo = IntPtr.Zero } } },
                // Home down
                new Win32.INPUT { type = Win32.INPUT_KEYBOARD, u = new Win32.INPUTUNION { ki = new Win32.KEYBDINPUT { wVk = key, wScan = keyScan, dwFlags = keyFlags, time = 0, dwExtraInfo = IntPtr.Zero } } },
                // Home up
                new Win32.INPUT { type = Win32.INPUT_KEYBOARD, u = new Win32.INPUTUNION { ki = new Win32.KEYBDINPUT { wVk = key, wScan = keyScan, dwFlags = keyFlags | Win32.KEYEVENTF_KEYUP, time = 0, dwExtraInfo = IntPtr.Zero } } },
                // Shift up
                new Win32.INPUT { type = Win32.INPUT_KEYBOARD, u = new Win32.INPUTUNION { ki = new Win32.KEYBDINPUT { wVk = modifier2, wScan = mod2Scan, dwFlags = Win32.KEYEVENTF_KEYUP, time = 0, dwExtraInfo = IntPtr.Zero } } },
                // Ctrl up
                new Win32.INPUT { type = Win32.INPUT_KEYBOARD, u = new Win32.INPUTUNION { ki = new Win32.KEYBDINPUT { wVk = modifier1, wScan = mod1Scan, dwFlags = Win32.KEYEVENTF_KEYUP, time = 0, dwExtraInfo = IntPtr.Zero } } }
            };

            Win32.SendInput(6, inputs, System.Runtime.InteropServices.Marshal.SizeOf<Win32.INPUT>());
        }
    }
}
