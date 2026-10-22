using System;
using System.Collections.Concurrent;
using System.IO;
using System.Security.AccessControl;
using System.Security.Principal;
using System.Text;
using System.Threading;

namespace TerminalVoiceOverlay.Services
{
    /// <summary>
    /// Crash-safe file writes for every file the sync services touch (vocabulary,
    /// quick prompts, Gemini prompt files, sync markers). Writes go to a temp file in
    /// the SAME folder, are flushed to disk, then swapped in with File.Replace /
    /// File.Move — a crash leaves either the old or the new content, never a
    /// truncated file (a truncated vocabulary file used to look like "user deleted
    /// every term" to the three-way merge).
    /// </summary>
    internal static class AtomicFile
    {
        private static readonly UTF8Encoding Utf8NoBom = new(encoderShouldEmitUTF8Identifier: false);

        public static void WriteAllText(string path, string? text) =>
            WriteAllBytes(path, Utf8NoBom.GetBytes(text ?? string.Empty));

        public static void WriteAllBytes(string path, byte[] bytes)
        {
            string full = Path.GetFullPath(path);
            string dir = Path.GetDirectoryName(full)!;
            Directory.CreateDirectory(dir);
            string tmp = Path.Combine(dir, $".{Path.GetFileName(full)}.{Guid.NewGuid():N}.tmp");
            try
            {
                using (var fs = new FileStream(tmp, FileMode.CreateNew, FileAccess.Write, FileShare.None))
                {
                    fs.Write(bytes, 0, bytes.Length);
                    fs.Flush(flushToDisk: true);
                }
                ReplaceWithRetry(tmp, full);
            }
            finally
            {
                try { if (File.Exists(tmp)) File.Delete(tmp); } catch { /* best effort */ }
            }
        }

        /// <summary>
        /// Moves <paramref name="source"/> over <paramref name="target"/>. Retries briefly
        /// when another process (TVO/CVO, an editor, the virus scanner) holds the target.
        /// </summary>
        public static void ReplaceWithRetry(string source, string target)
        {
            for (int attempt = 0; ; attempt++)
            {
                try
                {
                    if (File.Exists(target)) File.Replace(source, target, destinationBackupFileName: null);
                    else File.Move(source, target);
                    return;
                }
                catch (Exception ex) when ((ex is IOException or UnauthorizedAccessException) && attempt < 5)
                {
                    Thread.Sleep(30 * (attempt + 1));
                }
            }
        }

        /// <summary>
        /// Reads a file while tolerating concurrent writers/replacers (FileShare.ReadWrite |
        /// Delete) with a short retry. Returns null when the file does not exist.
        /// </summary>
        public static byte[]? ReadAllBytesShared(string path)
        {
            for (int attempt = 0; ; attempt++)
            {
                try
                {
                    if (!File.Exists(path)) return null;
                    using var fs = new FileStream(path, FileMode.Open, FileAccess.Read,
                        FileShare.ReadWrite | FileShare.Delete);
                    using var ms = new MemoryStream();
                    fs.CopyTo(ms);
                    return ms.ToArray();
                }
                catch (FileNotFoundException) { return null; }
                catch (DirectoryNotFoundException) { return null; }
                catch (IOException) when (attempt < 5)
                {
                    Thread.Sleep(30 * (attempt + 1));
                }
            }
        }

        public static string? ReadAllTextShared(string path)
        {
            var bytes = ReadAllBytesShared(path);
            if (bytes is null) return null;
            // Strip a UTF-8 BOM the same way File.ReadAllText does.
            int offset = bytes.Length >= 3 && bytes[0] == 0xEF && bytes[1] == 0xBB && bytes[2] == 0xBF ? 3 : 0;
            return Encoding.UTF8.GetString(bytes, offset, bytes.Length - offset);
        }
    }

    /// <summary>
    /// Cross-process lock for files that TVO and CVO share on one machine (SK folder,
    /// %LOCALAPPDATA%\PromptBoard). Backed by a named mutex whose DACL grants every
    /// authenticated user Synchronize+Modify, so an elevated creator does not lock out a
    /// non-elevated sibling. Opening never throws: when the mutex cannot be created or
    /// opened, callers get a no-op lock and continue unprotected instead of crashing.
    ///
    /// IMPORTANT: a mutex has thread affinity. Acquire and Dispose on the same thread,
    /// never hold a <see cref="CrossProcessLock"/> across an await.
    /// </summary>
    internal sealed class CrossProcessLock : IDisposable
    {
        private static readonly ConcurrentDictionary<string, Mutex?> Cache = new(StringComparer.Ordinal);

        private readonly Mutex? _mutex;
        private bool _owned;

        private CrossProcessLock(Mutex? mutex, bool owned)
        {
            _mutex = mutex;
            _owned = owned;
        }

        /// <summary>True when the lock is really held (false = fallback without lock).</summary>
        public bool IsHeld => _owned;

        /// <summary>
        /// Creates or opens the named mutex with a permissive DACL. Returns null (and logs)
        /// when that is impossible — callers must treat null as "run without the lock".
        /// </summary>
        public static Mutex? CreateSharedMutex(string name)
        {
            const MutexRights rights = MutexRights.Synchronize | MutexRights.Modify;
            try
            {
                var security = new MutexSecurity();
                security.AddAccessRule(new MutexAccessRule(
                    new SecurityIdentifier(WellKnownSidType.AuthenticatedUserSid, null),
                    rights, AccessControlType.Allow));
                security.AddAccessRule(new MutexAccessRule(
                    WindowsIdentity.GetCurrent().User!,
                    MutexRights.FullControl, AccessControlType.Allow));
                return MutexAcl.Create(initiallyOwned: false, name, out _, security);
            }
            catch (Exception createEx)
            {
                // Typical: the mutex already exists with a restrictive DACL (created by an
                // older, elevated build). Try to open it with the minimal rights we need.
                try
                {
                    if (MutexAcl.TryOpenExisting(name, rights, out var existing)) return existing;
                }
                catch (Exception openEx)
                {
                    TryLog(name, openEx);
                }
                TryLog(name, createEx);
                return null;
            }
        }

        /// <summary>
        /// Waits for the named lock. On timeout or when the mutex is unavailable a
        /// non-holding instance is returned when <paramref name="throwOnTimeout"/> is false.
        /// </summary>
        public static CrossProcessLock Acquire(string name, TimeSpan timeout, bool throwOnTimeout = true)
        {
            var mutex = Cache.GetOrAdd(name, CreateSharedMutex);
            if (mutex is null) return new CrossProcessLock(null, false);
            bool owned;
            try
            {
                owned = mutex.WaitOne(timeout);
            }
            catch (AbandonedMutexException)
            {
                // Previous owner died while holding it — we own it now.
                owned = true;
            }
            if (!owned && throwOnTimeout)
                throw new TimeoutException($"Cross-process lock '{name}' not acquired within {timeout.TotalSeconds:0} s.");
            return new CrossProcessLock(mutex, owned);
        }

        public void Dispose()
        {
            if (!_owned || _mutex is null) return;
            _owned = false;
            try { _mutex.ReleaseMutex(); }
            catch (ApplicationException) { /* not owned by this thread — nothing to release */ }
        }

        private static void TryLog(string name, Exception ex)
        {
            try { DiagLog.Write("CrossProcessLock", "mutex unavailable, continuing without lock", ("name", name), ("err", ex.Message)); }
            catch { /* logging must never throw */ }
        }
    }

    /// <summary>Names of the cross-process locks shared by TVO and CVO.</summary>
    internal static class SharedLockNames
    {
        /// <summary>personal-vocabulary.txt + its three-way base.</summary>
        public const string Vocabulary = @"Local\VoiceOverlays.PersonalVocabulary";

        /// <summary>Gemini prompt files, quick prompts and their sync markers in the SK folder.</summary>
        public const string GeminiPromptFiles = @"Local\VoiceOverlays.GeminiPromptFiles";

        /// <summary>The shared PromptBoard DB merge (launch restore / merge-before-upload).</summary>
        public const string PromptBoardMerge = @"Local\PromptBoard.DriveMerge";
    }
}
