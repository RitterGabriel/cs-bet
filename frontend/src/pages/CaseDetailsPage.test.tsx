import { describe, it, expect } from 'vitest';
import { renderToStaticMarkup } from 'react-dom/server';
import { AuthProvider } from '../contexts/AuthContext';
import { MemoryRouter } from 'react-router';
import { CaseDetailsContent } from './CaseDetailsPage';
import type { CatalogCase, CaseItem } from '../types/catalog';
import { OFFICIAL_ODDS_SOURCE } from '../services/officialOdds';
const normal: CaseItem = {
  id: 'skin-normal',
  name: 'Skin comum',
  image: '',
  rarity: 'restricted',
  marketPrices: [],
};
const box: CatalogCase = {
  id: 'crate-fixture',
  name: 'Fixture Case',
  category: 'official',
  theme: '',
  accent: '#00D4FF',
  image: '',
  description: '',
  isReal: true,
  items: [normal],
  specialItems: Array.from({ length: 52 }, (_, i) => ({
    ...normal,
    id: `rare-${i}`,
    name: `Faca interna ${i}`,
    specialCategory: 'knife' as const,
  })),
  specialReward: {
    configured: true,
    selectionRule: 'CSBET_UNIFORM_CATALOG_ITEM',
    dropChance: {
      percent: '0.25575448',
      basis: 'OFFICIAL_RULES_CALCULATED',
      scope: 'SPECIAL_GROUP',
      sourceUrl: OFFICIAL_ODDS_SOURCE,
    },
  },
};
describe('conteúdo da caixa com grupo especial', () => {
  it('mantém skins comuns e substitui os 52 especiais por um único card dourado', () => {
    const html = renderToStaticMarkup(
      <MemoryRouter>
        <AuthProvider><CaseDetailsContent item={box} /></AuthProvider>
      </MemoryRouter>,
    );
    expect(html.match(/class="skin-market-card/g)).toHaveLength(2);
    expect(html.match(/gold-special-card/g)).toHaveLength(1);
    expect(html).toContain('Skin comum');
    expect(html).toContain('≈ 0,2558%');
    expect(html).not.toContain('Faca interna');
    expect(html).toContain('regra da simulação CS Bet');
    expect(html).not.toContain('seleção uniforme oficial');
  });
  it('uma caixa sem itens especiais não recebe dourado nem chance inventada', () => {
    const html = renderToStaticMarkup(
      <MemoryRouter>
        <AuthProvider><CaseDetailsContent
          item={{ ...box, specialItems: [], specialReward: null }}
        /></AuthProvider>
      </MemoryRouter>,
    );
    expect(html).not.toContain('gold-special-card');
    expect(html.match(/class="skin-market-card/g)).toHaveLength(1);
  });
  it('personalizada exibe cada resultado e suas chances próprias, inclusive a faca', () => {
    const custom = { ...box, id: 'custom-all-win', name: 'All Win', category: 'custom' as const, isReal: false,
      customPrice: '3.00', customReady: true, specialReward: null,
      items: [{ ...normal, customChance: '99.00' }], specialItems: [{ ...box.specialItems[0], customChance: '1.00' }] };
    const html = renderToStaticMarkup(<MemoryRouter><AuthProvider><CaseDetailsContent item={custom} /></AuthProvider></MemoryRouter>);
    expect(html.match(/class="skin-market-card/g)).toHaveLength(2);
    expect(html).not.toContain('gold-special-card');expect(html).toContain('Faca interna 0');
    expect(html).toContain('99% · CS BET');expect(html).toContain('1% · CS BET');
    expect(html).toContain('3,00');expect(html).not.toContain('regras oficiais da Perfect World');
  });

});
