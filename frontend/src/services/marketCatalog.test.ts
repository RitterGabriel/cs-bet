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
      search: 'Redline',
      weapon: 'AK-47',
      rarity: 'rare',
      wear: 'FT',
      sort: 'price_desc',
    });
    expect(data.totalElements).toBe(90);
  });
  it('trata resposta incompleta e indisponibilidade da API', async () => {
    const filters = {
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
