import { describe, it, expect } from 'vitest';
import {
  formatDropChance,
  verifiedDropChance,
  OFFICIAL_ODDS_SOURCE,
} from './officialOdds';
import type { OfficialDropChance } from './officialOdds';
const chance: OfficialDropChance = {
  percent: '1.06564365',
  basis: 'OFFICIAL_RULES_CALCULATED',
  scope: 'SKIN_ALL_VARIANTS',
  sourceUrl: OFFICIAL_ODDS_SOURCE,
};
describe('chances verificadas', () => {
  it('formata uma chance derivada das regras oficiais como aproximação', () => {
    expect(verifiedDropChance(chance)).toEqual(chance);
    expect(formatDropChance(chance)).toBe('≈ 1,0656%');
  });
  it('não transforma probabilidade de grupo ou dados sem fonte em chance individual', () => {
    expect(verifiedDropChance(null)).toBeNull();
    expect(
      verifiedDropChance({ ...chance, scope: 'RARITY_GROUP' } as never),
    ).toBeNull();
    expect(
      verifiedDropChance({ ...chance, sourceUrl: 'https://example.com' }),
    ).toBeNull();
    for (const percent of ['0', '-1', '101', 'NaN', '1e2'])
      expect(verifiedDropChance({ ...chance, percent })).toBeNull();
  });
});
