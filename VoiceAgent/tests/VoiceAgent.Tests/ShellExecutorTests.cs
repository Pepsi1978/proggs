using System.Threading.Tasks;
using VoiceAgent.Services;

namespace VoiceAgent.Tests
{
    public class ShellExecutorTests
    {
        [Fact]
        public async Task RunAsync_Echo_ReturnsOutput()
        {
            var exec = new ShellExecutor();
            var r = await exec.RunAsync("Write-Output 'hallo123'");
            Assert.True(r.Ok);
            Assert.Equal(0, r.ExitCode);
            Assert.Contains("hallo123", r.Output);
        }

        [Fact]
        public async Task RunAsync_Empty_Fails()
        {
            var exec = new ShellExecutor();
            var r = await exec.RunAsync("");
            Assert.False(r.Ok);
        }

        [Fact]
        public async Task RunAsync_NonZeroExit_Reported()
        {
            var exec = new ShellExecutor();
            var r = await exec.RunAsync("exit 3");
            Assert.False(r.Ok);
            Assert.Equal(3, r.ExitCode);
        }
    }
}
