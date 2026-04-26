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

    Task AddAsync(Prompt prompt, CancellationToken ct = default);

    Task UpdateAsync(Prompt prompt, CancellationToken ct = default);

    Task DeleteAsync(Guid id, CancellationToken ct = default);
}
