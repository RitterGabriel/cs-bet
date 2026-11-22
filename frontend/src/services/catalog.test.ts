import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { getOfficialCase, listOfficialCases, mapOfficialCase } from './catalog';
import { permittedImage } from '../components/CatalogImage';

const fetchMock = vi.fn();
const box = {
  id: 'crate-4880',
  externalId: 'crate-4880',
  name: 'Revolution Case',
  category: 'official' as const,
  type: 'Case',
  description: null,
  imageUrl: 'https://community.akamai.steamstatic.com/economy/image/reference',
  language: 'en',
};
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

describe('catálogo oficial', () => {
  it('preserva identificador externo, imagem e idioma sem inventar conteúdo', () => {
    const mapped = mapOfficialCase(box);
    expect(mapped.id).toBe(box.id);
    expect(mapped.image).toBe(box.imageUrl);
    expect(mapped.items).toEqual([]);
    expect(mapped.specialItems).toEqual([]);
    expect(mapped.isReal).toBe(true);
    expect(mapped.language).toBe('en');
  });
  it('envia busca, paginação e categoria sem autenticação', async () => {
    fetchMock.mockResolvedValueOnce(
      Response.json({
        content: [box],
        page: 1,
        size: 12,
        totalElements: 25,
        totalPages: 3,
      }),
    );
    const response = await listOfficialCases(1, ' Revolution ', 'name');
    const [url, options] = fetchMock.mock.calls[0];
    const query = new URL(String(url), 'https://example.test').searchParams;
    expect(query.get('page')).toBe('1');
    expect(query.get('search')).toBe('Revolution');
    expect(query.get('category')).toBe('official');
    expect(query.get('sort')).toBe('name');
    expect(options.headers.Authorization).toBeUndefined();
    expect(response.totalPages).toBe(3);
  });
  it('mantém raridade original e grupo especial vinculado à caixa', async () => {
    const special = {
      id: 'fixture-glove',
      name: 'Fixture',
      weapon: 'Fixture gloves',
      weaponType: 'gloves',
      specialCategory: 'glove',
      imageUrl: null,
      rarity: { id: 'rarity_ancient', name: 'Extraordinary', color: '#eb4b4b' },
      special: true,
    };
    fetchMock.mockResolvedValueOnce(
      Response.json({
        box,
        items: [],
        specialItems: [special],
        specialNotice: 'Variantes não exaustivas',
        source: 'ByMykel',
        sourceRevision: 'abc',
        imageNotice: 'Valve',
      }),
    );
    const response = await getOfficialCase(box.id);
    expect(response.specialItems[0].specialCategory).toBe('glove');
    expect(response.specialItems[0].rarityData?.name).toBe('Extraordinary');
    expect(response.specialNotice).toContain('não exaustivas');
    expect(response.items).toEqual([]);
  });
  it('trata detalhes inexistentes e respostas incompletas', async () => {
    fetchMock.mockResolvedValueOnce(new Response(null, { status: 404 }));
    await expect(getOfficialCase('missing')).rejects.toMatchObject({
      status: 404,
    });
    fetchMock.mockResolvedValueOnce(Response.json({ content: null }));
    await expect(listOfficialCases(0, '', 'name')).rejects.toThrow(
      'incompleta',
    );
    fetchMock.mockResolvedValueOnce(
      Response.json({ box, items: null, specialItems: [] }),
    );
    await expect(getOfficialCase(box.id)).rejects.toThrow('incompletos');
  });
  it('habilita imagens da fonte e rejeita URLs inseguras ou hosts desconhecidos', () => {
    expect(permittedImage(box.imageUrl!, true)).toBe(true);
    expect(permittedImage('http://community.akamai.steamstatic.com/image', true)).toBe(false);
    expect(permittedImage('https://example.com/image.png', true)).toBe(false);
    expect(permittedImage('https://user:pass@cdn.steamstatic.com/image.png', true)).toBe(false);
    expect(permittedImage('javascript:alert(1)', false)).toBe(false);
    expect(permittedImage('/images/neon.svg', false)).toBe(true);
  });
});
