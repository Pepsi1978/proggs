using System;
using System.Collections.Generic;
using System.Threading;
using System.Threading.Tasks;
using PromptBoard.Core.Models;

namespace PromptBoard.Core.Repositories;

public interface ICategoryRepository
{
    Task<IReadOnlyList<Category>> GetAllAsync(CancellationToken ct = default);

    Task<Category?> GetByIdAsync(Guid id, CancellationToken ct = default);

    Task AddAsync(Category category, CancellationToken ct = default);

    Task UpdateAsync(Category category, CancellationToken ct = default);

    Task DeleteAsync(Guid id, CancellationToken ct = default);
}
