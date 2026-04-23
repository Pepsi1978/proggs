using System;
using System.IO;
using System.Text.Json;
using System.Threading;
using System.Threading.Tasks;
using Microsoft.Extensions.Logging;
using PromptBoard.Core.Models;
using PromptBoard.Core.Services;

namespace PromptBoard.Services;

/// <summary>
/// Default <see cref="IBackupFileService"/>: writes a UTF-8, indented
/// JSON file to disk and reads it back. Uses System.Text.Json which is
/// already in the BCL, no extra dep.
/// </summary>
public sealed class BackupFileService : IBackupFileService
{
    private static readonly JsonSerializerOptions s_jsonOptions = new()
    {
        WriteIndented = true,
        PropertyNamingPolicy = JsonNamingPolicy.CamelCase,
        DefaultIgnoreCondition = System.Text.Json.Serialization.JsonIgnoreCondition.WhenWritingNull,
    };

    private readonly IBackupService _backupService;
    private readonly ILogger<BackupFileService> _logger;

    public BackupFileService(IBackupService backupService, ILogger<BackupFileService> logger)
    {
        _backupService = backupService;
        _logger = logger;
    }

    public async Task ExportAsync(string path, CancellationToken ct = default)
    {
        if (string.IsNullOrWhiteSpace(path))
        {
            throw new ArgumentException("Pfad darf nicht leer sein.", nameof(path));
        }

        BackupDocument document = await _backupService.CreateAsync(ct);
        string json = JsonSerializer.Serialize(document, s_jsonOptions);

        string? dir = Path.GetDirectoryName(path);
        if (!string.IsNullOrEmpty(dir))
        {
            Directory.CreateDirectory(dir);
        }

        await File.WriteAllTextAsync(path, json, System.Text.Encoding.UTF8, ct);
        _logger.LogInformation("Backup written: {Path} ({Bytes} bytes).", path, json.Length);
    }

    public async Task<BackupDocument> ReadAsync(string path, CancellationToken ct = default)
    {
        if (!File.Exists(path))
        {
            throw new FileNotFoundException($"Backup-Datei nicht gefunden: {path}", path);
        }

        string json = await File.ReadAllTextAsync(path, System.Text.Encoding.UTF8, ct);
        BackupDocument? document = JsonSerializer.Deserialize<BackupDocument>(json, s_jsonOptions);

        if (document is null)
        {
            throw new InvalidDataException("Backup-Datei enthielt kein gueltiges JSON-Dokument.");
        }

        _logger.LogInformation(
            "Backup read: {Path}, schema {Schema}, {Cat} categories, {Prm} prompts.",
            path, document.SchemaVersion, document.Categories.Count, document.Prompts.Count);
        return document;
    }

    public async Task<string> SerializeAsync(CancellationToken ct = default)
    {
        BackupDocument document = await _backupService.CreateAsync(ct);
        return JsonSerializer.Serialize(document, s_jsonOptions);
    }

    public BackupDocument Deserialize(string json)
    {
        if (string.IsNullOrWhiteSpace(json))
        {
            throw new InvalidDataException("Backup-Inhalt ist leer.");
        }
        BackupDocument? document = JsonSerializer.Deserialize<BackupDocument>(json, s_jsonOptions);
        if (document is null)
        {
            throw new InvalidDataException("Backup-Inhalt enthielt kein gueltiges JSON-Dokument.");
        }
        _logger.LogInformation(
            "Backup deserialized (in-memory): schema {Schema}, {Cat} categories, {Prm} prompts.",
            document.SchemaVersion, document.Categories.Count, document.Prompts.Count);
        return document;
    }
}
