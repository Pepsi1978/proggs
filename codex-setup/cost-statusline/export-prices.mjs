import { writeFile } from 'node:fs/promises';
import { withOpenAIPriorityPricing, withOpenAICacheReadMarkup } from '../../opencode-setup/plugins/token-cost-sidebar/dist/pricing.ts';

const response = await fetch('https://models.dev/api.json');
if (!response.ok) throw new Error(`Preiskatalog: HTTP ${response.status}`);
const catalog = (await response.json()).openai.models;
const models = {};
for (const id of new Set([...Object.keys(catalog), 'gpt-6-astra'])) {
  models[id] = {};
  for (const tier of ['default', 'priority', 'fast', 'flex', 'batch']) {
    const priced = withOpenAICacheReadMarkup(
      withOpenAIPriorityPricing(catalog[id] ?? {}, 'openai', id, tier === 'fast' ? 'priority' : tier), 'openai');
    // Non-Astra flex/batch and undocumented priority prices must not silently use standard.
    if (tier !== 'default' && id !== 'gpt-6-astra' && !priced.pricingServiceTier) continue;
    if (priced.cost) models[id][tier] = {
      ...priced.cost,
      unsupportedAbove: priced.pricingUnsupportedAbove,
    };
  }
}
await writeFile(new URL('prices.json', import.meta.url), JSON.stringify({
  generatedAt: new Date().toISOString(),
  source: 'OpenCode token-cost-sidebar; models.dev; https://developers.openai.com/api/docs/pricing',
  cacheReadMarkup: 1.2,
  models,
}, null, 2) + '\n');
