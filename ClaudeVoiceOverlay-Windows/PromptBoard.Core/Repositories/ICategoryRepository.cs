using System;
using System.Collections.Generic;
using System.Threading;
using System.Threading.Tasks;
using PromptBoard.Core.Models;

namespace PromptBoard.Core.Repositories;

public interface ICategoryRepository
{
    Task<IReadOnlyList<Category>> GetAllAsync(CancellationToken ct = default);

    /// <summary>
    /// Wie <see cref="GetAllAsync"/>, ladet aber NICHT die navigierte Prompts-
    /// Sammlung pro Kategorie. Aufrufer die nur die Tab-Anzeige (Name, Sortier-
    /// Reihenfolge, Farbe) brauchen, sparen pro Aufruf einen unnoetigen
    /// SQL-JOIN ueber alle Prompts. Default-Implementation faellt auf
    /// GetAllAsync zurueck — bestehende Implementer brauchen keine Aenderung.
    /// </summary>
    Task<IReadOnlyList<Category>> GetAllSummaryAsync(CancellationToken ct = default) =>
        GetAllAsync(ct);

    Task<Category?> GetByIdAsync(Guid id, CancellationToken ct = default);

    Task AddAsync(Category category, CancellationToken ct = default);

    Task UpdateAsync(Category category, CancellationToken ct = default);

    Task DeleteAsync(Guid id, CancellationToken ct = default);
}
