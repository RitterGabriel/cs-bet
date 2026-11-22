import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { fetchInventory } from './inventory';

const fetchMock = vi.fn();
const page = {
  content: [],
  page: 0,
  size: 12,
  totalElements: 0,
  totalPages: 0,
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

describe('API de inventário', () => {
  it('consulta /me com JWT, paginação e status sem enviar userId', async () => {
    fetchMock.mockResolvedValueOnce(Response.json(page));
    await expect(fetchInventory('test-token', 0, 'AVAILABLE')).resolves.toEqual(
      page,
    );
    const [url, options] = fetchMock.mock.calls[0];
    expect(url).toBe('/api/inventory/me?page=0&size=12&status=AVAILABLE');
    expect(options.headers.Authorization).toBe('Bearer test-token');
    expect(url).not.toContain('userId');
  });
  it('trata erro de comunicação e sessão expirada', async () => {
    fetchMock.mockRejectedValueOnce(new TypeError('offline'));
    await expect(fetchInventory('test-token', 0)).rejects.toMatchObject({
      status: 0,
    });
    fetchMock.mockResolvedValueOnce(new Response(null, { status: 401 }));
    await expect(fetchInventory('test-token', 0)).rejects.toMatchObject({
      status: 401,
    });
  });
  it('não apresenta dados incompletos como itens reais', async () => {
    fetchMock.mockResolvedValueOnce(
      Response.json({ ...page, content: [{ id: 1 }] }),
    );
    await expect(fetchInventory('test-token', 0)).rejects.toThrow(
      'incompletos',
    );
  });
});
