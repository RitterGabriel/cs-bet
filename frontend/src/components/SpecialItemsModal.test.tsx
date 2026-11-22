import { describe, it, expect } from 'vitest';
import { renderToStaticMarkup } from 'react-dom/server';
import { SpecialItemsModal } from './SpecialItemsModal';
import { MarketCategoryFilters } from './MarketCategoryFilters';
import {
  updateMarketFilter,
  resetMarketFilters,
  INITIAL_MARKET_FILTERS,
} from '../services/marketCatalog';
import type { CaseItem } from '../types/catalog';

describe('itens especiais e agentes no mercado', () => {
  it('o modal mostra somente os especiais recebidos daquela caixa e diferencia fases', () => {
    const item: CaseItem = {
      id: 'rare-ruby',
      name: 'Faca | Doppler',
      image: '',
      rarity: 'covert',
      phase: 'Ruby',
    };
    const html = renderToStaticMarkup(
      <SpecialItemsModal
        items={[item]}
        caseName="Fever Case"
        onClose={() => {}}
      />,
    );
    expect(html).toContain('Itens especiais — Fever Case');
    expect(html).toContain('Faca | Doppler · Ruby');
    expect(html).not.toContain('Outra caixa');
    expect(html.match(/class="skin-market-card/g)).toHaveLength(1);
  });
  it('agentes aparecem como categoria ao lado de equipamentos', () => {
    const html = renderToStaticMarkup(
      <MarketCategoryFilters
        categories={[
          { id: 'equipment', name: 'Equipamentos', weapons: ['Zeus x27'] },
          { id: 'agents', name: 'Agentes', weapons: ['Terrorist'] },
        ]}
        selected="agents"
        onChange={() => {}}
      />,
    );
    expect(html).toContain('Equipamentos');
    expect(html).toContain('Agentes');
    expect(html).not.toContain('role="tablist"');
    expect(html.match(/aria-pressed="true"/g)).toHaveLength(1);
  });
  it('trocar para agentes limpa arma, raridade e desgaste, preservando busca e ordenação', () => {
    const filters = {
      ...INITIAL_MARKET_FILTERS,
      search: 'Teste',
      sort: 'price_desc',
      category: 'rifles',
      weapon: 'AK-47',
      rarity: 'rare',
      wear: 'FT',
    };
    const agents = updateMarketFilter(filters, 'category', 'agents');
    expect(agents).toEqual({
      ...INITIAL_MARKET_FILTERS,
      category: 'agents',
      search: 'Teste',
      sort: 'price_desc',
    });
    expect(
      updateMarketFilter(
        { ...agents, weapon: 'Terrorist', rarity: 'agent-rarity' },
        'category',
        'rifles',
      ),
    ).toEqual({
      ...INITIAL_MARKET_FILTERS,
      category: 'rifles',
      search: 'Teste',
      sort: 'price_desc',
    });
    expect(resetMarketFilters('all')).toEqual(INITIAL_MARKET_FILTERS);
  });
});
