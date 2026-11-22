import { describe, it, expect } from 'vitest';
import { renderToStaticMarkup } from 'react-dom/server';
import { MarketCategoryFilters } from './MarketCategoryFilters';
import {
  INITIAL_MARKET_FILTERS,
  categoryWeapons,
  updateMarketFilter,
} from '../services/marketCatalog';

const categories = [
  { id: 'rifles', name: 'Rifles', weapons: ['AK-47', 'AWP'] },
  { id: 'pistols', name: 'Pistolas', weapons: ['Glock-18'] },
  { id: 'knives', name: 'Facas', weapons: ['Karambit'] },
  { id: 'gloves', name: 'Luvas', weapons: ['Hand Wraps'] },
];
describe('categoria → arma', () => {
  it('mostra chips com ícones SVG e seleção acessível, incluindo Todas', () => {
    const html = renderToStaticMarkup(
      <MarketCategoryFilters
        categories={categories}
        selected="rifles"
        onChange={() => {}}
      />,
    );
    expect(html).toContain('<legend>Categoria</legend>');
    expect(html).toContain('Todas');
    expect(html).toContain('Facas');
    expect(html).toContain('Luvas');
    expect(html.match(/aria-pressed="true"/g)).toHaveLength(1);
    expect(html).toContain('<svg');
  });
  it('limpa apenas a arma na troca de categoria e preserva os filtros independentes', () => {
    const current = {
      ...INITIAL_MARKET_FILTERS,
      category: 'rifles',
      weapon: 'AK-47',
      search: 'Fixture',
      rarity: 'rare',
      wear: 'FT',
      sort: 'price_desc',
    };
    expect(updateMarketFilter(current, 'category', 'pistols')).toEqual({
      ...current,
      category: 'pistols',
      weapon: '',
    });
    expect(updateMarketFilter(current, 'category', '')).toEqual({
      ...current,
      category: '',
      weapon: '',
    });
    expect(updateMarketFilter(current, 'category', 'rifles')).toEqual(current);
    expect(updateMarketFilter(current, 'weapon', 'AWP')).toEqual({
      ...current,
      weapon: 'AWP',
    });
  });
  it('lista apenas as armas da categoria e permite voltar a Todas', () => {
    const data = {
      categories,
      weapons: categories.flatMap((category) => category.weapons),
    };
    expect(categoryWeapons(data, 'rifles')).toEqual(['AK-47', 'AWP']);
    expect(categoryWeapons(data, 'gloves')).toEqual(['Hand Wraps']);
    expect(categoryWeapons(data, 'knives')).toEqual(['Karambit']);
    expect(categoryWeapons(data, '')).toEqual(data.weapons);
    expect(categoryWeapons(data, 'unknown')).toEqual([]);
  });
});
