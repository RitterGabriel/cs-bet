import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { formatWalletAmount, walletApi } from './wallet';
const fetchMock = vi.fn();
beforeEach(() => {
  vi.stubGlobal('fetch', fetchMock);
  vi.stubGlobal('window', { setTimeout, clearTimeout });
});
afterEach(() => { vi.unstubAllGlobals(); vi.clearAllMocks(); });
describe('API da carteira', () => {
  it('consulta saldo e histórico com JWT e sem identidade enviada pelo cliente', async () => {
    fetchMock.mockImplementation(() => Promise.resolve(Response.json({ balance: '50.00' })));
    await walletApi.balance('token');
    await walletApi.history('token', 2);
    expect(fetchMock.mock.calls.map(call => call[0])).toEqual(['/api/wallet', '/api/wallet/movements?page=2']);
    for (const [, options] of fetchMock.mock.calls) expect(options.headers.Authorization).toBe('Bearer token');
  });
  it('envia decimal exato e identificador de operação sem userId', async () => {
    fetchMock.mockResolvedValue(Response.json({ balance: '75.50' }));
    await walletApi.add('token', '25,50', 'operation-id');
    const [url, options] = fetchMock.mock.calls[0];
    expect(url).toBe('/api/wallet/credits');
    expect(options.method).toBe('POST');
    expect(JSON.parse(options.body)).toEqual({ amount: '25.50', requestId: 'operation-id' });
  });
  it('propaga falhas e autenticação expirada', async () => {
    fetchMock.mockResolvedValueOnce(new Response(null, { status: 401 }));
    await expect(walletApi.balance('token')).rejects.toMatchObject({ status: 401 });
    fetchMock.mockRejectedValueOnce(new Error('offline'));
    await expect(walletApi.add('token', '10', 'id')).rejects.toMatchObject({ status: 0 });
  });
});

describe('formatação de créditos fictícios', () => {
  it('aceita saldos do Oracle com escalas diferentes', () => {
    expect(formatWalletAmount('999')).toBe('R$\u00a0999,00');
    expect(formatWalletAmount('999.00')).toBe('R$\u00a0999,00');
    expect(formatWalletAmount('0')).toBe('R$\u00a00,00');
    expect(formatWalletAmount('10.5')).toBe('R$\u00a010,50');
    expect(formatWalletAmount('10.25')).toBe('R$\u00a010,25');
  });
  it('preserva centavos no limite de precisão sem usar ponto flutuante', () => {
    expect(formatWalletAmount('9999999999999999.99')).toBe('R$\u00a09.999.999.999.999.999,99');
    expect(formatWalletAmount('1000')).toBe('R$\u00a01.000,00');
  });
  it('não apresenta valores inválidos como saldo zero', () => {
    for (const value of ['', 'abc', '-1', '1.001', '1e3']) {
      expect(formatWalletAmount(value)).toBe('Valor indisponível');
    }
  });
});
