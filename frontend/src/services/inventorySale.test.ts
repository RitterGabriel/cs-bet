import { beforeEach, afterEach, describe, expect, it, vi } from 'vitest';
import { sellInventoryItem, sellAllInventory } from './inventory';
import { memoryOpeningStorage, openingResponse, openingSkin, openingUser } from '../test/openingFixture';
import type { InventoryItem } from '../types/inventory';
const fetchMock = vi.fn();
const item: InventoryItem = { id: 22, skinId: 1, externalId: openingSkin.id, name: openingSkin.name, imageUrl: openingSkin.image,
  rarity: openingSkin.rarityData!, obtainedAt: '2026-11-22T12:00:00Z', status: 'AVAILABLE', marketPrice: openingSkin.marketPrices![0] };
const receipt = { saleId: 7, inventoryId: 22, amount: '10.00', balance: '510.00' };
function session() {
  fetchMock.mockResolvedValueOnce(Response.json({ accessToken: 'token', tokenType: 'Bearer', expiresIn: 3600 }));
  fetchMock.mockResolvedValueOnce(Response.json(openingUser));
}
beforeEach(() => { vi.stubGlobal('fetch', fetchMock); vi.stubGlobal('window', { setTimeout, clearTimeout }); vi.stubGlobal('localStorage', memoryOpeningStorage()); });
afterEach(() => { vi.resetAllMocks(); vi.unstubAllGlobals(); });
describe('venda fictícia de uma unidade', () => {
  it('envia apenas UUID e preço esperado ao endpoint da unidade autenticada', async () => {
    session(); fetchMock.mockResolvedValueOnce(Response.json(receipt));
    expect(await sellInventoryItem(item,1)).toEqual(receipt);
    const [url, options] = fetchMock.mock.calls[2];
    expect(url).toBe('/api/inventory/22/sell'); expect(options.headers.Authorization).toBe('Bearer token');
    expect(JSON.parse(options.body)).toEqual({ requestId: expect.any(String), expectedPrice: '10.00' });
  });
  it('repete uma venda incerta com mesmo UUID e preço mesmo após mudar a cotação', async () => {
    session(); fetchMock.mockRejectedValueOnce(new Error('network'));
    await expect(sellInventoryItem(item,1)).rejects.toMatchObject({ status: 0 });
    session(); fetchMock.mockResolvedValueOnce(Response.json(receipt));
    await sellInventoryItem({ ...item, marketPrice: { ...item.marketPrice!, currentPrice: '15.00' } },1);
    expect(fetchMock.mock.calls[5][1].body).toBe(fetchMock.mock.calls[2][1].body);
  });
  it('não troca o UUID se a carteira ainda estiver ocupada', async () => {
    session(); fetchMock.mockResolvedValueOnce(Response.json({ message: 'Ocupada', code: 'SALE_BUSY' },{ status: 409 }));
    await expect(sellInventoryItem(item,1)).rejects.toMatchObject({ status: 409 });
    session(); fetchMock.mockResolvedValueOnce(Response.json(receipt));
    await sellInventoryItem(item,1); expect(fetchMock.mock.calls[5][1].body).toBe(fetchMock.mock.calls[2][1].body);
  });
  it('não vende com identidade de outra conta ou cotação ausente', async () => {
    session(); await expect(sellInventoryItem(item,2)).rejects.toMatchObject({ status: 401 });
    session(); await expect(sellInventoryItem({ ...item, marketPrice: undefined },1)).rejects.toMatchObject({ status: 409 });
    expect(fetchMock.mock.calls.filter(c => c[1].method === 'POST')).toHaveLength(0);
  });
  it('não aceita resposta de outro item como confirmação e mantém tentativa para recuperar', async () => {
    session(); fetchMock.mockResolvedValueOnce(Response.json({ ...receipt, inventoryId: 23 }));
    await expect(sellInventoryItem(item,1)).rejects.toThrow('confirmar');
    session(); fetchMock.mockResolvedValueOnce(Response.json(receipt)); await sellInventoryItem(item,1);
    expect(fetchMock.mock.calls[5][1].body).toBe(fetchMock.mock.calls[2][1].body);
    expect(openingResponse().inventoryId).toBe(item.id);
  });
  it('vender tudo consulta todo o inventário cotado e preserva unidades sem preço', async () => {
    session(); fetchMock.mockResolvedValueOnce(Response.json({ items: [{ inventoryId: 22, expectedPrice: '10.00' }, { inventoryId: 23, expectedPrice: '12.50' }], skippedItems: 3, total: '22.50' }));
    fetchMock.mockResolvedValueOnce(Response.json({ batchId: 8, soldItems: 2, amount: '22.50', balance: '522.50' }));
    const result = await sellAllInventory(1);
    expect(result.soldItems).toBe(2); expect(result.skippedItems).toBe(3);
    expect(fetchMock.mock.calls[2][0]).toBe('/api/inventory/sell-all/quote');
    expect(fetchMock.mock.calls[3][0]).toBe('/api/inventory/sell-all');
    expect(JSON.parse(fetchMock.mock.calls[3][1].body).items).toHaveLength(2);
  });
  it('recupera vender tudo após timeout sem refazer a cotação nem vender itens novos', async () => {
    session(); fetchMock.mockResolvedValueOnce(Response.json({ items: [{ inventoryId: 22, expectedPrice: '10.00' }], skippedItems: 0, total: '10.00' }));
    fetchMock.mockRejectedValueOnce(new Error('network'));
    await expect(sellAllInventory(1)).rejects.toMatchObject({ status: 0 });
    const body = fetchMock.mock.calls[3][1].body;
    session(); fetchMock.mockResolvedValueOnce(Response.json({ batchId: 8, soldItems: 1, amount: '10.00', balance: '510.00' }));
    await sellAllInventory(1);
    expect(fetchMock.mock.calls[6][1].body).toBe(body);
    expect(fetchMock.mock.calls.filter(c => c[0] === '/api/inventory/sell-all/quote')).toHaveLength(1);
  });
  it('não vende nada quando não há variantes cotadas', async () => {
    session(); fetchMock.mockResolvedValueOnce(Response.json({ items: [], skippedItems: 3, total: '0.00' }));
    await expect(sellAllInventory(1)).rejects.toThrow('Nenhuma skin');
    expect(fetchMock.mock.calls.filter(c => c[1].method === 'POST')).toHaveLength(0);
  });

});
