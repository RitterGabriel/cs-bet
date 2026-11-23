import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { requestDemoOpening, requestOpeningBatch } from './demoOpening';
import { getOfficialCase } from './catalog';
import { openingResponse, openingBatchResponse, memoryOpeningStorage, openingBox, openingSkin, openingUser } from '../test/openingFixture';
vi.mock('./catalog', () => ({ getOfficialCase: vi.fn() }));
const fetchMock = vi.fn();
beforeEach(() => {
  vi.stubGlobal('fetch', fetchMock); vi.stubGlobal('window', { setTimeout, clearTimeout });
  vi.stubGlobal('localStorage', memoryOpeningStorage());
});
afterEach(() => { vi.resetAllMocks(); vi.unstubAllGlobals(); });
function session() {
  fetchMock.mockResolvedValueOnce(Response.json({ accessToken: 'token', tokenType: 'Bearer', expiresIn: 3600 }));
  fetchMock.mockResolvedValueOnce(Response.json(openingUser));
}
describe('abertura persistida com variante definida pelo servidor', () => {
  it('envia caixa, preço esperado e UUID sem userId, e retorna a única variante recebida', async () => {
    session(); fetchMock.mockResolvedValueOnce(Response.json(openingResponse()));
    const outcome = await requestDemoOpening(openingBox, 1);
    if (outcome.status !== 'AVAILABLE') throw new Error('Resultado ausente');
    expect(outcome.result.item.id).toBe(openingSkin.id); expect(outcome.result.item.image).toBe(openingSkin.image);
    expect(outcome.result.referencePrice).toBe('10.00'); expect(outcome.result.variant).toEqual(openingResponse().variant);
    expect(outcome.result.inventoryId).toBe(22);
    const [url, options] = fetchMock.mock.calls[2];
    expect(url).toBe('/api/openings'); expect(options.headers.Authorization).toBe('Bearer token');
    expect(JSON.parse(options.body)).toEqual({ caseId: openingBox.id, expectedPrice: '2.71', requestId: expect.any(String) });
    expect(fetchMock.mock.calls.filter(call => call[1].method === 'POST')).toHaveLength(1);
  });
  it('mantém UUID e preço ao repetir timeout, sem transformar retry em nova compra', async () => {
    session(); fetchMock.mockRejectedValueOnce(new Error('network'));
    await expect(requestDemoOpening(openingBox, 1)).rejects.toMatchObject({ status: 0 });
    const first = fetchMock.mock.calls[2][1].body;
    session(); fetchMock.mockResolvedValueOnce(Response.json(openingResponse()));
    await requestDemoOpening({ ...openingBox, marketPrice: { ...openingBox.marketPrice!, currentPrice: '3.00' } }, 1);
    expect(fetchMock.mock.calls[5][1].body).toBe(first);
    session(); fetchMock.mockResolvedValueOnce(Response.json(openingResponse()));
    await requestDemoOpening(openingBox, 1);
    expect(JSON.parse(fetchMock.mock.calls[8][1].body).requestId).not.toBe(JSON.parse(first).requestId);
  });
  it('mantém UUID quando outra tentativa pode ainda estar em execução', async () => {
    session(); fetchMock.mockResolvedValueOnce(Response.json({ message: 'Carteira ocupada.', code: 'OPENING_BUSY' }, { status: 409 }));
    await expect(requestDemoOpening(openingBox, 1)).rejects.toMatchObject({ status: 409 });
    const first = fetchMock.mock.calls[2][1].body;
    session(); fetchMock.mockResolvedValueOnce(Response.json(openingResponse()));
    await requestDemoOpening(openingBox, 1);
    expect(fetchMock.mock.calls[5][1].body).toBe(first);
  });
  it('preserva tentativa após resposta inválida e não sorteia uma alternativa', async () => {
    session(); fetchMock.mockResolvedValueOnce(Response.json({ ...openingResponse(), variant: { wear: 'MADE_UP' } }));
    await expect(requestDemoOpening(openingBox, 1)).rejects.toThrow('validar');
    session(); fetchMock.mockResolvedValueOnce(Response.json(openingResponse()));
    await requestDemoOpening(openingBox, 1);
    expect(fetchMock.mock.calls[5][1].body).toBe(fetchMock.mock.calls[2][1].body);
  });
  it('recupera catálogo desatualizado sem uma segunda abertura', async () => {
    session(); fetchMock.mockResolvedValueOnce(Response.json(openingResponse(true)));
    vi.mocked(getOfficialCase).mockResolvedValueOnce(openingBox);
    const outcome = await requestDemoOpening({ ...openingBox, specialItems: [] }, 1);
    expect(outcome.status).toBe('AVAILABLE'); expect(getOfficialCase).toHaveBeenCalledTimes(1);
    expect(fetchMock.mock.calls.filter(call => call[1].method === 'POST')).toHaveLength(1);
  });
  it('mantém falta de preço da variante sem usar faixa de outras variantes', async () => {
    session(); fetchMock.mockResolvedValueOnce(Response.json(openingResponse(true)));
    const outcome = await requestDemoOpening(openingBox, 1);
    if (outcome.status !== 'AVAILABLE') throw new Error('Resultado ausente');
    expect(outcome.result.referencePrice).toBeNull(); expect(outcome.result.variant.phase).toBeNull();
  });
  it('não envia abertura quando a sessão pertence a outra conta', async () => {
    session(); await expect(requestDemoOpening(openingBox, 2)).rejects.toMatchObject({ status: 401 });
    expect(fetchMock.mock.calls.filter(call => call[1].method === 'POST')).toHaveLength(0);
  });
  it('exibe motivo real de recusa do backend', async () => {
    session(); fetchMock.mockResolvedValueOnce(Response.json({ message: 'Saldo fictício insuficiente.' }, { status: 409 }));
    await expect(requestDemoOpening(openingBox, 1)).rejects.toThrow('Saldo fictício insuficiente.');
  });
  it('envia lote como uma operação e valida quantidade, cobrança e resultados distintos', async () => {
    session(); fetchMock.mockResolvedValueOnce(Response.json(openingBatchResponse(5)));
    const outcome = await requestOpeningBatch(openingBox, 1, 5);
    if (outcome.status !== 'AVAILABLE') throw new Error('Resultado ausente');
    expect(outcome.results).toHaveLength(5);
    expect(fetchMock.mock.calls[2][0]).toBe('/api/openings/batch');
    expect(JSON.parse(fetchMock.mock.calls[2][1].body)).toMatchObject({ quantity: 5, expectedPrice: '2.71' });
  });
  it('recupera lote incerto com mesmo UUID e impede trocar quantidade antes da recuperação', async () => {
    session(); fetchMock.mockRejectedValueOnce(new Error('network'));
    await expect(requestOpeningBatch(openingBox, 1, 2)).rejects.toMatchObject({ status: 0 });
    const body = fetchMock.mock.calls[2][1].body;
    await expect(requestOpeningBatch(openingBox, 1, 5)).rejects.toThrow('Recupere primeiro');
    expect(fetchMock).toHaveBeenCalledTimes(3);
    session(); fetchMock.mockResolvedValueOnce(Response.json(openingBatchResponse(2)));
    await requestOpeningBatch(openingBox, 1, 2);
    expect(fetchMock.mock.calls[5][1].body).toBe(body);
  });
  it('rejeita lote incompleto ou com uma mesma unidade entregue duas vezes', async () => {
    const batch = openingBatchResponse(2); batch.results[1] = batch.results[0];
    session(); fetchMock.mockResolvedValueOnce(Response.json(batch));
    await expect(requestOpeningBatch(openingBox, 1, 2)).rejects.toThrow('validar');
    expect(fetchMock.mock.calls.filter(c => c[1].method === 'POST')).toHaveLength(1);
  });

});
