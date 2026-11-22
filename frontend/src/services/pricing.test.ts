import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import {
  fetchPriceHistory,
  formatSimulatedCredits,
  validatePrice,
} from './pricing';
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

describe('preços exclusivamente simulados', () => {
  it('formata créditos sem perda de centavos nem símbolo de moeda', () => {
    expect(formatSimulatedCredits('9999999999999999.99')).toBe(
      '9.999.999.999.999.999,99',
    );
    expect(formatSimulatedCredits('0.00')).toBe('0,00');
    expect(formatSimulatedCredits('100.25')).toBe('100,25');
    expect(formatSimulatedCredits('invalid')).toBe('Indisponível');
  });
  it('aceita preço não inicializado, mas rejeita contrato de cotação real', () => {
    expect(validatePrice(null)).toBeNull();
    expect(() =>
      validatePrice({
        basePrice: '100.00',
        currentPrice: '101.00',
        priceDate: '2026-11-22',
        updatedAt: '2026-11-22T12:00:00Z',
        ruleVersion: 'EDUCATIONAL_V1',
        unit: 'USD',
      } as never),
    ).toThrow('inválido');
  });
  it('consulta histórico público sem enviar preço ou token', async () => {
    fetchMock.mockResolvedValueOnce(
      Response.json({
        content: [
          { date: '2026-11-21', amount: '100.00', eventType: 'INITIAL' },
        ],
        page: 0,
        size: 30,
        totalElements: 1,
        totalPages: 1,
        unit: 'SIMULATED_CREDITS',
      }),
    );
    const result = await fetchPriceHistory('skin-fixture');
    expect(result.content[0].amount).toBe('100.00');
    const [url, options] = fetchMock.mock.calls[0];
    expect(url).toBe('/api/skins/skin-fixture/price/history?page=0&size=30');
    expect(options.headers.Authorization).toBeUndefined();
    expect(options.method).toBeUndefined();
  });
  it('trata erro e histórico inválido', async () => {
    fetchMock.mockResolvedValueOnce(new Response(null, { status: 503 }));
    await expect(fetchPriceHistory('skin-fixture')).rejects.toMatchObject({
      status: 503,
    });
    fetchMock.mockResolvedValueOnce(
      Response.json({
        content: [{ date: 'wrong', amount: 100 }],
        unit: 'SIMULATED_CREDITS',
        totalElements: 1,
      }),
    );
    await expect(fetchPriceHistory('skin-fixture')).rejects.toThrow(
      'incompleto',
    );
  });
});
