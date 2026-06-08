using VoiceAgent.Core;

namespace VoiceAgent.Tests
{
    public class CommandGuardTests
    {
        // ---- Hardline: NIE erlaubt, in JEDER Stufe (auch Vollzugriff) ----

        [Theory]
        [InlineData("format C:")]
        [InlineData("format /fs:ntfs D:")]
        [InlineData("shutdown /s /t 0")]
        [InlineData("Stop-Computer")]
        [InlineData("Restart-Computer -Force")]
        [InlineData("diskpart")]
        [InlineData("cipher /w:C")]
        [InlineData("rmdir /s C:\\Windows\\System32")]
        [InlineData("Remove-Item -Recurse C:\\Windows")]
        [InlineData("del /s /q C:\\")]
        [InlineData("reg delete HKLM")]
        [InlineData("bcdedit /delete {current}")]
        public void Hardline_BlockedEvenInFullMode(string cmd)
        {
            Assert.Equal(CommandVerdict.BlockedHardline, CommandGuard.Evaluate(cmd, ComputerUseMode.Full));
            Assert.Equal(CommandVerdict.BlockedHardline, CommandGuard.Evaluate(cmd, ComputerUseMode.Safe));
            Assert.Equal(CommandVerdict.BlockedHardline, CommandGuard.Evaluate(cmd, ComputerUseMode.Off));
            Assert.True(CommandGuard.IsHardline(cmd));
        }

        [Fact]
        public void Hardline_DetectedThroughObfuscation()
        {
            // cmd-Caret / PowerShell-Backtick muessen die Normalisierung NICHT ueberleben.
            Assert.Equal(CommandVerdict.BlockedHardline, CommandGuard.Evaluate("sh^utdown /s", ComputerUseMode.Full));
            Assert.Equal(CommandVerdict.BlockedHardline, CommandGuard.Evaluate("for`mat C:", ComputerUseMode.Full));
        }

        // ---- Dangerous: in "Sicher" bestaetigungspflichtig, bei "Vollzugriff" direkt ----

        [Theory]
        [InlineData("del /s build")]
        [InlineData("Remove-Item -Recurse .\\build")]
        [InlineData("reg delete HKCU\\Software\\Test")]
        [InlineData("git push --force origin main")]
        [InlineData("git reset --hard HEAD~3")]
        [InlineData("taskkill /im notepad.exe")]
        [InlineData("Set-ExecutionPolicy Bypass")]
        [InlineData("net user gast /add")]
        public void Dangerous_NeedsApproval_InSafe_ButRunsInFull(string cmd)
        {
            Assert.Equal(CommandVerdict.NeedsApproval, CommandGuard.Evaluate(cmd, ComputerUseMode.Safe));
            Assert.Equal(CommandVerdict.Approved, CommandGuard.Evaluate(cmd, ComputerUseMode.Full));
        }

        // ---- Harmlos: direkt in Safe + Full ----

        [Theory]
        [InlineData("dir")]
        [InlineData("echo hallo welt")]
        [InlineData("git status")]
        [InlineData("start notepad")]
        [InlineData("Get-Date")]
        [InlineData("ipconfig")]
        public void Harmless_Approved(string cmd)
        {
            Assert.Equal(CommandVerdict.Approved, CommandGuard.Evaluate(cmd, ComputerUseMode.Safe));
            Assert.Equal(CommandVerdict.Approved, CommandGuard.Evaluate(cmd, ComputerUseMode.Full));
        }

        // ---- Aus: nichts laeuft (ausser Hardline bleibt Hardline) ----

        [Fact]
        public void Off_BlocksEverything()
        {
            Assert.Equal(CommandVerdict.BlockedDisabled, CommandGuard.Evaluate("dir", ComputerUseMode.Off));
            Assert.Equal(CommandVerdict.BlockedDisabled, CommandGuard.Evaluate("git status", ComputerUseMode.Off));
            // Hardline bleibt auch im Aus-Modus Hardline (zuerst geprueft).
            Assert.Equal(CommandVerdict.BlockedHardline, CommandGuard.Evaluate("shutdown", ComputerUseMode.Off));
        }

        [Fact]
        public void Empty_IsDisabled()
        {
            Assert.Equal(CommandVerdict.BlockedDisabled, CommandGuard.Evaluate("", ComputerUseMode.Full));
            Assert.Equal(CommandVerdict.BlockedDisabled, CommandGuard.Evaluate("   ", ComputerUseMode.Safe));
            Assert.Equal(CommandVerdict.BlockedDisabled, CommandGuard.Evaluate(null, ComputerUseMode.Full));
        }

        [Fact]
        public void Normalize_StripsObfuscation()
        {
            Assert.Equal("shutdown /s", CommandGuard.Normalize("SH^UT`DOWN   /s"));
            Assert.Equal("del x", CommandGuard.Normalize("d\"\"el x"));
        }
    }
}
