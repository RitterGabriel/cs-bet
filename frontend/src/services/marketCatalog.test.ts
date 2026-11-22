import { beforeEach, afterEach, describe, it, expect, vi } from 'vitest';
import { listMarketSkins } from './marketCatalog';
const fetchMock = vi.fn();
beforeEach(() => {
  vi.stubGlobal('fetch', fetchMock);
  vi.stubGlobal('window', {
    setTimeout: globalThis.setTimeout,
    clearTimeout: globalThis.clearTimeout,
  });
});
afterEach(() => {
  vi.unstubAllGlobals();
  vi.clearAllMocks();
});
describe('consulta paginada de mercado', () => {
  it('envia filtros combinados e ordenação ao backend, sem carregar o catálogo completo', async () => {
    fetchMock.mockResolvedValueOnce(
      Response.json({
        content: [],
        page: 2,
        size: 24,
        totalPages: 4,
        totalElements: 90,
        weapons: ['AK-47'],
        rarities: [],
      }),
    );
    const data = await listMarketSkins(2, {
      kind: 'all',
      category: 'rifles',
      search: 'Redline',
      weapon: 'AK-47',
      rarity: 'rare',
      wear: 'FT',
      sort: 'price_desc',
    });
    const url = new URL(fetchMock.mock.calls[0][0], 'http://localhost');
    expect(url.pathname).toBe('/api/market/skins');
    expect(Object.fromEntries(url.searchParams)).toEqual({
      page: '2',
      size: '24',
      kind: 'all',
      category: 'rifles',
      search: 'Redline',
      weapon: 'AK-47',
      rarity: 'rare',
      wear: 'FT',
      sort: 'price_desc',
    });
    expect(data.totalElements).toBe(90);
  });
  it('preserva metadados estruturados das categorias vindos do backend', async () => {
    const categories = [{ id: 'rifles', name: 'Rifles', weapons: ['AK-47'] }];
    fetchMock.mockResolvedValueOnce(
      Response.json({
        content: [],
        page: 0,
        size: 24,
        totalPages: 0,
        totalElements: 0,
        weapons: ['AK-47'],
        rarities: [],
        categories,
      }),
    );
    const result = await listMarketSkins(0, {
      kind: 'all',
      category: 'rifles',
      search: '',
      weapon: '',
      rarity: '',
      wear: '',
      sort: 'name',
    });
    expect(result.categories).toEqual(categories);
    expect(result.weapons).toEqual(['AK-47']);
  });
  it('solicita agentes com paginação e preserva a raridade de personagem', async () => {
    fetchMock.mockResolvedValueOnce(
      Response.json({
        content: [
          {
            id: 'agent-4613',
            name: 'Agent Fixture',
            weapon: 'Terrorist',
            weaponType: 'agent',
            specialCategory: 'none',
            imageUrl: null,
            rarity: {
              id: 'rarity_legendary_character',
              name: 'Superior',
              color: '#d32ce6',
            },
            special: false,
            marketPrices: [],
          },
        ],
        page: 0,
        size: 24,
        totalPages: 3,
        totalElements: 63,
        weapons: ['Terrorist'],
        rarities: [],
        categories: [],
      }),
    );
    const result = await listMarketSkins(0, {
      kind: 'all',
      category: 'agents',
      search: '',
      weapon: 'Terrorist',
      rarity: '',
      wear: '',
      sort: 'price_asc',
    });
    const url = new URL(fetchMock.mock.calls[0][0], 'http://localhost');
    expect(url.searchParams.get('kind')).toBe('all');
    expect(url.searchParams.get('category')).toBe('agents');
    expect(url.searchParams.get('wear')).toBe('');
    expect(result.content[0].rarity).toBe('classified');
    expect(result.content[0].rarityData?.name).toBe('Superior');
  });
  it('trata resposta incompleta e indisponibilidade da API', async () => {
    const filters = {
      kind: 'all' as const,
      category: '',
      search: '',
      weapon: '',
      rarity: '',
      wear: '',
      sort: 'name',
    };
    fetchMock.mockResolvedValueOnce(Response.json({ content: [] }));
    await expect(listMarketSkins(0, filters)).rejects.toThrow('incompleta');
    fetchMock.mockRejectedValueOnce(new Error('offline'));
    await expect(listMarketSkins(0, filters)).rejects.toMatchObject({
      status: 0,
    });
  });
});
