using Microsoft.EntityFrameworkCore;

namespace PromptBoard.Data;

/// <summary>
/// Root database context for PromptBoard.
/// Phase 0: empty skeleton. Phase 1 will add DbSets for
/// Category, Prompt, AiImprovementPrompt and AppSettings.
/// </summary>
public class PromptBoardDbContext : DbContext
{
    public PromptBoardDbContext(DbContextOptions<PromptBoardDbContext> options)
        : base(options)
    {
    }
}
