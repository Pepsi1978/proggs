using System.Threading;
using System.Threading.Tasks;
using PromptBoard.Core.Models;

namespace PromptBoard.Core.Repositories;

public interface IAppSettingsRepository
{
    /// <summary>
    /// Returns the singleton settings row, creating it with defaults if missing.
    /// </summary>
    Task<AppSettings> GetAsync(CancellationToken ct = default);

    Task UpdateAsync(AppSettings settings, CancellationToken ct = default);
}
