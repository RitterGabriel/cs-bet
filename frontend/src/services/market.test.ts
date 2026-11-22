import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import {
  fetchMarketHistory,
  formatBRL,
  variantPriceRange,
  formatVirtualBalance,
  quoteAvailable,
  unavailableQuote,
  validateMarketQuote,
} from './market';
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

describe('referências BRL', () => {
  it('formata BRL sem perder centavos nem converter o saldo virtual', () => {
    expect(formatBRL('9999999999999999.99')).toBe(
      'R$\u00a09.999.999.999.999.999,99',
    );
    expect(formatBRL('0.00')).toBe('R$\u00a00,00');
    expect(formatVirtualBalance(0)).toBe('R$\u00a00,00');
  });
  it('resume somente preços de variantes válidas, comparando centavos sem arredondar', () => {
    const base = {
      ...unavailableQuote,
      variantId: 1,
      status: 'AVAILABLE' as const,
      source: 'MARKET_CSGO',
      quotedAt: new Date().toISOString(),
      validUntil: new Date(Date.now() + 3600000).toISOString(),
    };
    const result = variantPriceRange([
      { ...base, currentPrice: '1000000000000000.01' },
      { ...base, variantId: 2, currentPrice: '9.99' },
      {
        ...base,
        variantId: 3,
        currentPrice: '1.00',
        validUntil: '2020-01-01T00:00:00Z',
      },
      unavailableQuote,
    ]);
    expect(result?.minimum).toBe('9.99');
    expect(result?.maximum).toBe('1000000000000000.01');
    expect(result?.quotes).toHaveLength(2);
    expect(variantPriceRange([])).toBeNull();
  });
  it('não trata simulação antiga ou outra moeda como cotação BRL', () => {
    expect(validateMarketQuote(undefined)).toEqual(unavailableQuote);
    expect(() =>
      validateMarketQuote({
        status: 'AVAILABLE',
        currentPrice: '100.00',
        currency: 'USD',
      } as never),
    ).toThrow();
    expect(() =>
      validateMarketQuote({
        unit: 'SIMULATED_CREDITS',
        currentPrice: '100.00',
      } as never),
    ).toThrow();
    expect(quoteAvailable(unavailableQuote)).toBe(false);
  });
  it('consulta histórico de origem verificada, com fonte e data', async () => {
    fetchMock.mockResolvedValueOnce(
      Response.json({
        origin: 'MARKET_REFERENCE',
        content: [
          {
            amount: '12.50',
            currency: 'BRL',
            source: 'STEAM_COMMUNITY_MARKET',
            provider: 'TEST_PROVIDER',
            quotedAt: '2026-11-21T12:00:00Z',
          },
        ],
      }),
    );
    const points = await fetchMarketHistory(1);
    expect(points[0].source).toBe('STEAM_COMMUNITY_MARKET');
    expect(fetchMock.mock.calls[0][0]).toBe(
      '/api/market/variants/1/history?page=0&size=30',
    );
    expect(fetchMock.mock.calls[0][1].headers.Authorization).toBeUndefined();
  });
  it('aceita referências convertidas de Market.CSGO e preserva o histórico da fonte', async () => {
    const quote = {
      ...unavailableQuote,
      variantId: 42,
      status: 'AVAILABLE' as const,
      currentPrice: '2.82',
      source: 'MARKET_CSGO',
      provider: 'MARKET_CSGO_LOWEST_ASK_BCB_PTAX',
      quotedAt: new Date().toISOString(),
      validUntil: new Date(Date.now() + 3600000).toISOString(),
      originalCurrency: 'USD',
      originalAmount: '0.479000',
      exchangeRate: '5.88890000',
      exchangeRateAt: '2026-11-19T16:04:18Z',
    };
    expect(validateMarketQuote(quote)).toEqual(quote);
    expect(quoteAvailable(quote)).toBe(true);
    fetchMock.mockResolvedValueOnce(
      Response.json({
        origin: 'MARKET_REFERENCE',
        content: [
          {
            amount: '2.82',
            currency: 'BRL',
            source: 'MARKET_CSGO',
            provider: quote.provider,
            quotedAt: quote.quotedAt,
          },
        ],
      }),
    );
    expect((await fetchMarketHistory(42))[0].source).toBe('MARKET_CSGO');
    expect(() =>
      validateMarketQuote({ ...quote, source: 'UNKNOWN' }),
    ).toThrow();
  });
  it('rejeita dados simulados no histórico de mercado e trata falhas externas', async () => {
    fetchMock.mockResolvedValueOnce(
      Response.json({ origin: 'SIMULATED_CREDITS', content: [] }),
    );
    await expect(fetchMarketHistory(1)).rejects.toThrow('inválido');
    fetchMock.mockRejectedValueOnce(new TypeError('offline'));
    await expect(fetchMarketHistory(1)).rejects.toMatchObject({ status: 0 });
  });
});
