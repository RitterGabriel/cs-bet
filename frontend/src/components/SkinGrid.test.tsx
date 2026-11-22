import { describe, expect, it } from 'vitest';
import { renderToStaticMarkup } from 'react-dom/server';
import { SkinCard, SkinGrid } from './SkinGrid';
import { SkinDetailsModal } from './SkinDetailsModal';
import type { CaseItem } from '../types/catalog';
import { unavailableQuote } from '../services/market';

const item: CaseItem = {
  id: 'skin-fixture',
  name: 'AK-47 | Fixture',
  image: '',
  rarity: 'restricted',
  rarityData: { id: 'rare', name: 'Restricted', color: '#8847ff' },
  marketPrices: [
    {
      ...unavailableQuote,
      variantId: 1,
      status: 'AVAILABLE',
      currentPrice: '12.50',
      source: 'MARKET_CSGO',
      provider: 'TEST',
      quotedAt: new Date().toISOString(),
      validUntil: new Date(Date.now() + 3600000).toISOString(),
      wear: 'FT',
    },
  ],
};
describe('cards compactos e detalhes de skins', () => {
  it('preserva a cor da raridade e a faixa sem controles ou histórico no card', () => {
    const html = renderToStaticMarkup(
      <SkinCard item={item} onOpen={() => {}} />,
    );
    expect(html).toContain('--item-rarity:#8847ff');
    expect(html).toContain('Restricted');
    expect(html).toContain('12,50');
    expect(html).toContain('Ver detalhes de AK-47 | Fixture');
    expect(html).not.toContain('<select');
    expect(html).not.toContain('Histórico');
  });
  it('unifica raridades e remove duplicações pelo ID', () => {
    const html = renderToStaticMarkup(
      <SkinGrid
        items={[
          item,
          {
            ...item,
            id: 'skin-other',
            rarity: 'covert',
            rarityData: { id: 'covert', name: 'Covert', color: '#eb4b4b' },
          },
          item,
        ]}
      />,
    );
    expect(html.match(/class="skin-market-card/g)).toHaveLength(2);
    expect(html.match(/class="skin-grid"/g)).toHaveLength(1);
    expect(html).not.toContain('rarity-group');
    expect(html).toContain('--item-rarity:#eb4b4b');
  });
  it('modal contém variantes e fonte, sem acesso ao histórico antigo', () => {
    const html = renderToStaticMarkup(
      <SkinDetailsModal item={item} onClose={() => {}} />,
    );
    expect(html).toContain('<dialog');
    expect(html).toContain('Selecione o desgaste');
    expect(html).toContain('Market.CSGO');
    expect(html).not.toContain('Histórico simulado anterior');
  });
  it('uma skin sem cotação continua visível no card e no modal', () => {
    const empty = { ...item, marketPrices: [] };
    expect(
      renderToStaticMarkup(<SkinCard item={empty} onOpen={() => {}} />),
    ).toContain('Preço indisponível');
    expect(
      renderToStaticMarkup(
        <SkinDetailsModal item={empty} onClose={() => {}} />,
      ),
    ).toContain('Preço indisponível');
  });
});
