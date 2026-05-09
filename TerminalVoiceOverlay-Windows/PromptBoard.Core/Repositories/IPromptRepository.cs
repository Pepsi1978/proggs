using System;
using System.Collections.Generic;
using System.Threading;
using System.Threading.Tasks;
using PromptBoard.Core.Models;

namespace PromptBoard.Core.Repositories;

public interface IPromptRepository
{
    Task<IReadOnlyList<Prompt>> GetByCategoryAsync(Guid categoryId, CancellationToken ct = default);

    Task<IReadOnlyList<Prompt>> GetAllAlwaysOnAsync(CancellationToken ct = default);

    /// <summary>
    /// Liefert alle Prompts aller Kategorien in einem Roundtrip. Wird vom
    /// PromptBoardPanel-Render genutzt um die N+1-Schleife
    /// (GetByCategoryAsync pro Kategorie) durch eine einzige Query zu
    /// ersetzen — bei 8 Kategorien sparte das vorher bis zu 24 Roundtrips
    /// pro Tab-Klick. Sortierung wird vom Aufrufer in-memory gemacht.
    /// </summary>
    Task<IReadOnlyList<Prompt>> GetAllAsync(CancellationToken ct = default);

    Task AddAsync(Prompt prompt, CancellationToken ct = default);

    Task UpdateAsync(Prompt prompt, CancellationToken ct = default);

    Task DeleteAsync(Guid id, CancellationToken ct = default);
}
