import { describe, it, expect } from 'vitest';
import { renderToStaticMarkup } from 'react-dom/server';
import { GoldSpecialCard } from './GoldSpecialCard';
import {
  verifiedDropChance,
  verifiedSpecialReward,
  OFFICIAL_ODDS_SOURCE,
} from '../services/officialOdds';
import type { SpecialReward } from '../services/officialOdds';

const reward: SpecialReward = {
  configured: true,
  selectionRule: 'CSBET_UNIFORM_CATALOG_ITEM',
  dropChance: {
    percent: '0.25575448',
    basis: 'OFFICIAL_RULES_CALCULATED',
    scope: 'SPECIAL_GROUP',
    sourceUrl: OFFICIAL_ODDS_SOURCE,
  },
};
describe('grupo dourado', () => {
  it('mostra o símbolo dourado e a chance do grupo sem apresentar skins individuais', () => {
    const html = renderToStaticMarkup(<GoldSpecialCard reward={reward} />);
    expect(html).toContain('Item especial raro');
    expect(html).toContain('Ver itens especiais de Caixa');
    expect(html).toContain('<button');
    expect(html).toContain('<svg');
    expect(html).toContain('≈ 0,2558%');
    expect(html).toContain('Chance de obter o grupo especial');
    expect(html).not.toContain('<img');
    expect(html).not.toContain('<select');
  });
  it('não confunde a chance do grupo com a de uma skin e não inventa chance sem dados', () => {
    expect(verifiedDropChance(reward.dropChance as never)).toBeNull();
    expect(
      verifiedSpecialReward({
        ...reward,
        selectionRule: 'OFFICIAL_UNIFORM',
      } as never),
    ).toBeNull();
    expect(verifiedSpecialReward({ ...reward, configured: false })).toBeNull();
    expect(renderToStaticMarkup(<GoldSpecialCard />)).toContain('Chance n/d');
  });
});
