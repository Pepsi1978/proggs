using System;
using System.Collections.Generic;
using System.Net;
using System.Net.Http;
using System.Threading;
using System.Threading.Tasks;
using Microsoft.Extensions.Logging.Abstractions;
using Moq;
using PromptBoard.Core.Models;
using PromptBoard.Core.Repositories;
using PromptBoard.Core.Services;
using PromptBoard.Services;

namespace PromptBoard.Tests;

public class GroqTranscriptionServiceTests
{
    [Fact]
    public async Task TranscribeAsync_WithoutApiKey_ThrowsGroqApiKeyMissing()
    {
        var handler = new StubHandler();
        var settingsMock = new Mock<IAppSettingsRepository>();
        settingsMock
            .Setup(s => s.GetAsync(It.IsAny<CancellationToken>()))
            .ReturnsAsync(new AppSettings { GroqApiKey = null });

        var sut = MakeService(handler, settingsMock.Object);

        await Assert.ThrowsAsync<GroqApiKeyMissingException>(
            () => sut.TranscribeAsync(SampleWav(), CancellationToken.None));
        Assert.Equal(0, handler.CallCount);
    }

    [Fact]
    public async Task TranscribeAsync_WithEmptyAudio_ThrowsArgument()
    {
        var sut = MakeService(new StubHandler(), KeyedSettings("key"));
        await Assert.ThrowsAsync<ArgumentException>(
            () => sut.TranscribeAsync([], CancellationToken.None));
    }

    [Fact]
    public async Task TranscribeAsync_Returns200Text()
    {
        var handler = new StubHandler();
        handler.Responses.Enqueue(JsonResponse(HttpStatusCode.OK,
            "{\"text\":\"Hallo Welt\"}"));

        var sut = MakeService(handler, KeyedSettings("key"));
        string transcript = await sut.TranscribeAsync(SampleWav());

        Assert.Equal("Hallo Welt", transcript);
        Assert.Equal(1, handler.CallCount);
        Assert.NotNull(handler.LastRequest);
        Assert.Equal("Bearer", handler.LastRequest!.Headers.Authorization?.Scheme);
        Assert.Equal("key", handler.LastRequest.Headers.Authorization?.Parameter);
    }

    [Fact]
    public async Task TranscribeAsync_On401_ThrowsSpecificExceptionNoRetry()
    {
        var handler = new StubHandler();
        handler.Responses.Enqueue(JsonResponse(HttpStatusCode.Unauthorized,
            "{\"error\":\"invalid key\"}"));

        var sut = MakeService(handler, KeyedSettings("bad"));
        var ex = await Assert.ThrowsAsync<GroqTranscriptionException>(
            () => sut.TranscribeAsync(SampleWav()));

        Assert.Equal((int)HttpStatusCode.Unauthorized, ex.StatusCode);
        Assert.Contains("401", ex.Message);
        Assert.Equal(1, handler.CallCount); // 401 is not retried
    }

    [Fact]
    public async Task TranscribeAsync_On429_RetriesUpToThreeTimes()
    {
        var handler = new StubHandler();
        handler.Responses.Enqueue(JsonResponse(HttpStatusCode.TooManyRequests, "{\"error\":\"rate\"}"));
        handler.Responses.Enqueue(JsonResponse(HttpStatusCode.TooManyRequests, "{\"error\":\"rate\"}"));
        handler.Responses.Enqueue(JsonResponse(HttpStatusCode.OK, "{\"text\":\"OK\"}"));

        var sut = MakeService(handler, KeyedSettings("key"));
        string transcript = await sut.TranscribeAsync(SampleWav());

        Assert.Equal("OK", transcript);
        Assert.Equal(3, handler.CallCount);
    }

    [Fact]
    public async Task TranscribeAsync_On500_RetriesAndEventuallyFails()
    {
        var handler = new StubHandler();
        handler.Responses.Enqueue(JsonResponse(HttpStatusCode.InternalServerError, "boom"));
        handler.Responses.Enqueue(JsonResponse(HttpStatusCode.InternalServerError, "boom"));
        handler.Responses.Enqueue(JsonResponse(HttpStatusCode.InternalServerError, "boom"));

        var sut = MakeService(handler, KeyedSettings("key"));
        await Assert.ThrowsAsync<GroqTranscriptionException>(
            () => sut.TranscribeAsync(SampleWav()));

        Assert.Equal(3, handler.CallCount);
    }

    [Fact]
    public async Task TranscribeAsync_ResponseWithoutTextField_ThrowsGroqException()
    {
        var handler = new StubHandler();
        handler.Responses.Enqueue(JsonResponse(HttpStatusCode.OK, "{\"foo\":\"bar\"}"));

        var sut = MakeService(handler, KeyedSettings("key"));
        await Assert.ThrowsAsync<GroqTranscriptionException>(
            () => sut.TranscribeAsync(SampleWav()));
    }

    [Fact]
    public async Task TranscribeAsync_MalformedJson_ThrowsGroqException()
    {
        var handler = new StubHandler();
        handler.Responses.Enqueue(JsonResponse(HttpStatusCode.OK, "not-json"));

        var sut = MakeService(handler, KeyedSettings("key"));
        await Assert.ThrowsAsync<GroqTranscriptionException>(
            () => sut.TranscribeAsync(SampleWav()));
    }

    // -------- helpers --------

    private static GroqTranscriptionService MakeService(
        StubHandler handler,
        IAppSettingsRepository settings)
    {
        var client = new HttpClient(handler);
        return new GroqTranscriptionService(client, settings, NullLogger<GroqTranscriptionService>.Instance);
    }

    private static IAppSettingsRepository KeyedSettings(string key, string? model = null)
    {
        var mock = new Mock<IAppSettingsRepository>();
        mock
            .Setup(s => s.GetAsync(It.IsAny<CancellationToken>()))
            .ReturnsAsync(new AppSettings { GroqApiKey = key, GroqModel = model ?? "whisper-large-v3-turbo" });
        return mock.Object;
    }

    private static HttpResponseMessage JsonResponse(HttpStatusCode status, string body)
    {
        var msg = new HttpResponseMessage(status);
        msg.Content = new StringContent(body, System.Text.Encoding.UTF8, "application/json");
        return msg;
    }

    private static byte[] SampleWav() => [0x52, 0x49, 0x46, 0x46, 0x24, 0x00, 0x00, 0x00]; // not real WAV, enough for tests

    private sealed class StubHandler : HttpMessageHandler
    {
        public Queue<HttpResponseMessage> Responses { get; } = new();
        public int CallCount { get; private set; }
        public HttpRequestMessage? LastRequest { get; private set; }

        protected override Task<HttpResponseMessage> SendAsync(HttpRequestMessage request, CancellationToken ct)
        {
            CallCount++;
            LastRequest = request;
            HttpResponseMessage response = Responses.Count > 0
                ? Responses.Dequeue()
                : new HttpResponseMessage(HttpStatusCode.InternalServerError) { Content = new StringContent("no response queued") };
            return Task.FromResult(response);
        }
    }
}
