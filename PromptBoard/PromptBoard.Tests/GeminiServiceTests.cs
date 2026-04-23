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

public class GeminiServiceTests
{
    [Fact]
    public async Task ImproveAsync_WithoutKey_ThrowsGeminiApiKeyMissing()
    {
        var handler = new StubHandler();
        var sut = MakeService(handler, WithSettings(null));
        await Assert.ThrowsAsync<GeminiApiKeyMissingException>(
            () => sut.ImproveAsync("user", "meta", "gemini-2.5-flash"));
        Assert.Equal(0, handler.CallCount);
    }

    [Fact]
    public async Task ImproveAsync_WithEmptyUser_ThrowsArgument()
    {
        var sut = MakeService(new StubHandler(), WithSettings("key"));
        await Assert.ThrowsAsync<ArgumentException>(
            () => sut.ImproveAsync("", "meta", "gemini-2.5-flash"));
    }

    [Fact]
    public async Task ImproveAsync_WithEmptyMeta_ThrowsArgument()
    {
        var sut = MakeService(new StubHandler(), WithSettings("key"));
        await Assert.ThrowsAsync<ArgumentException>(
            () => sut.ImproveAsync("user", "", "gemini-2.5-flash"));
    }

    [Fact]
    public async Task ImproveAsync_OnSuccess_ReturnsJoinedParts()
    {
        string body = "{\"candidates\":[{\"content\":{\"parts\":[{\"text\":\"Hallo \"},{\"text\":\"Welt\"}]}}]}";
        var handler = new StubHandler();
        handler.Responses.Enqueue(JsonResponse(HttpStatusCode.OK, body));

        var sut = MakeService(handler, WithSettings("key"));
        string result = await sut.ImproveAsync("user", "meta", "gemini-2.5-flash");

        Assert.Equal("Hallo Welt", result);
        Assert.Equal(1, handler.CallCount);
        Assert.NotNull(handler.LastRequest);
        Assert.Contains("generativelanguage.googleapis.com", handler.LastRequest!.RequestUri!.ToString());
        Assert.Contains("gemini-2.5-flash:generateContent", handler.LastRequest.RequestUri!.ToString());
    }

    [Fact]
    public async Task ImproveAsync_On401_ThrowsSpecificNoRetry()
    {
        var handler = new StubHandler();
        handler.Responses.Enqueue(JsonResponse(HttpStatusCode.Unauthorized, "{\"error\":\"bad key\"}"));

        var sut = MakeService(handler, WithSettings("bad"));
        var ex = await Assert.ThrowsAsync<GeminiServiceException>(
            () => sut.ImproveAsync("user", "meta", "gemini-2.5-flash"));
        Assert.Equal((int)HttpStatusCode.Unauthorized, ex.StatusCode);
        Assert.Equal(1, handler.CallCount);
    }

    [Fact]
    public async Task ImproveAsync_On429_RetriesUpToThreeTimes()
    {
        var handler = new StubHandler();
        handler.Responses.Enqueue(JsonResponse(HttpStatusCode.TooManyRequests, "{\"error\":\"rate\"}"));
        handler.Responses.Enqueue(JsonResponse(HttpStatusCode.TooManyRequests, "{\"error\":\"rate\"}"));
        handler.Responses.Enqueue(JsonResponse(HttpStatusCode.OK,
            "{\"candidates\":[{\"content\":{\"parts\":[{\"text\":\"OK\"}]}}]}"));

        var sut = MakeService(handler, WithSettings("key"));
        string result = await sut.ImproveAsync("user", "meta", "gemini-2.5-flash");
        Assert.Equal("OK", result);
        Assert.Equal(3, handler.CallCount);
    }

    [Fact]
    public async Task ImproveAsync_NoCandidates_ThrowsGeminiException()
    {
        var handler = new StubHandler();
        handler.Responses.Enqueue(JsonResponse(HttpStatusCode.OK, "{\"candidates\":[]}"));

        var sut = MakeService(handler, WithSettings("key"));
        await Assert.ThrowsAsync<GeminiServiceException>(
            () => sut.ImproveAsync("user", "meta", "gemini-2.5-flash"));
    }

    [Fact]
    public async Task ImproveAsync_MalformedJson_ThrowsGeminiException()
    {
        var handler = new StubHandler();
        handler.Responses.Enqueue(JsonResponse(HttpStatusCode.OK, "not-json"));

        var sut = MakeService(handler, WithSettings("key"));
        await Assert.ThrowsAsync<GeminiServiceException>(
            () => sut.ImproveAsync("user", "meta", "gemini-2.5-flash"));
    }

    // -------- helpers --------

    private static GeminiService MakeService(StubHandler handler, IAppSettingsRepository settings)
    {
        var client = new HttpClient(handler);
        return new GeminiService(client, settings, NullLogger<GeminiService>.Instance);
    }

    private static IAppSettingsRepository WithSettings(string? key)
    {
        var mock = new Mock<IAppSettingsRepository>();
        mock
            .Setup(s => s.GetAsync(It.IsAny<CancellationToken>()))
            .ReturnsAsync(new AppSettings { GeminiApiKey = key });
        return mock.Object;
    }

    private static HttpResponseMessage JsonResponse(HttpStatusCode status, string body)
    {
        return new HttpResponseMessage(status)
        {
            Content = new StringContent(body, System.Text.Encoding.UTF8, "application/json"),
        };
    }

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
