import { ApiError, request } from './api';
import { isCreditAmount, formatSimulatedCredits } from './pricing';

export const marketSourceLabel = (source: string | null | undefined) =>
  source === 'MARKET_CSGO'
    ? 'Market.CSGO · PTAX/BCB'
    : source === 'STEAM_COMMUNITY_MARKET'
      ? 'Mercado Steam'
      : 'Fonte indisponível';
const supportedSource = (source: unknown) =>
  source === 'MARKET_CSGO' || source === 'STEAM_COMMUNITY_MARKET';

export type Wear = 'NONE' | 'FN' | 'MW' | 'FT' | 'WW' | 'BS';
export interface MarketQuote {
  variantId: number | null;
  status: 'AVAILABLE' | 'UNAVAILABLE' | 'STALE';
  currentPrice: string | null;
  currency: 'BRL';
  source: string | null;
  provider: string | null;
  quotedAt: string | null;
  validUntil: string | null;
  wear: Wear | null;
  statTrak: boolean;
  souvenir: boolean;
  marketHashName: string | null;
  originalAmount?: string | null;
  originalCurrency?: string | null;
  exchangeRate?: string | null;
  exchangeRateAt?: string | null;
}
export const unavailableQuote: MarketQuote = {
  variantId: null,
  status: 'UNAVAILABLE',
  currentPrice: null,
  currency: 'BRL',
  source: null,
  provider: null,
  quotedAt: null,
  validUntil: null,
  wear: null,
  statTrak: false,
  souvenir: false,
  marketHashName: null,
};
export const wearLabels: Record<Wear, string> = {
  NONE: 'Sem desgaste aplicável',
  FN: 'Nova de fábrica',
  MW: 'Pouco usada',
  FT: 'Testada em campo',
  WW: 'Bem desgastada',
  BS: 'Veterana de guerra',
};
export const formatBRL = (amount: string) =>
  isCreditAmount(amount)
    ? `R$\u00a0${formatSimulatedCredits(amount)}`
    : 'Preço indisponível';
export const formatVirtualBalance = (amount: number) =>
  new Intl.NumberFormat('pt-BR', { style: 'currency', currency: 'BRL' }).format(
    amount,
  );

export function validateMarketQuote(
  value: MarketQuote | undefined | null,
): MarketQuote {
  if (value == null) return unavailableQuote;
  if (
    !['AVAILABLE', 'UNAVAILABLE', 'STALE'].includes(value.status) ||
    value.currency !== 'BRL' ||
    (value.status === 'AVAILABLE' &&
      (!isCreditAmount(value.currentPrice) ||
        BigInt(value.currentPrice.replace('.', '')) <= 0n ||
        !supportedSource(value.source) ||
        !value.provider ||
        !value.quotedAt ||
        !value.validUntil ||
        Number.isNaN(Date.parse(value.quotedAt)) ||
        Number.isNaN(Date.parse(value.validUntil))))
  ) {
    throw new ApiError(0, 'Cotação de referência inválida na resposta.');
  }
  return value;
}
export function quoteAvailable(quote: MarketQuote): boolean {
  return (
    quote.status === 'AVAILABLE' &&
    quote.currentPrice !== null &&
    quote.currency === 'BRL' &&
    supportedSource(quote.source) &&
    quote.validUntil !== null &&
    Date.parse(quote.validUntil) > Date.now()
  );
}
// Resumo de cotações existentes, sem criar um preço para uma variante não selecionada.
export function variantPriceRange(quotes: MarketQuote[]) {
  const available = quotes.filter(
    (quote) =>
      quote.variantId !== null &&
      quoteAvailable(quote) &&
      isCreditAmount(quote.currentPrice),
  );
  if (!available.length) return null;
  let minimum = available[0].currentPrice!;
  let maximum = minimum;
  for (const quote of available) {
    const amount = quote.currentPrice!;
    if (BigInt(amount.replace('.', '')) < BigInt(minimum.replace('.', '')))
      minimum = amount;
    if (BigInt(amount.replace('.', '')) > BigInt(maximum.replace('.', '')))
      maximum = amount;
  }
  return { minimum, maximum, quotes: available };
}

export const formatQuoteDate = (value: string) =>
  new Intl.DateTimeFormat('pt-BR', {
    dateStyle: 'short',
    timeStyle: 'short',
  }).format(new Date(value));

export function quoteProvenance(quotes: MarketQuote[]) {
  const dated = quotes.filter(
    (quote) => quote.quotedAt && !Number.isNaN(Date.parse(quote.quotedAt)),
  );
  if (!dated.length) return null;
  return {
    sources: [
      ...new Set(dated.map((quote) => marketSourceLabel(quote.source))),
    ].join(' / '),
    latest: dated.reduce(
      (latest, quote) =>
        Date.parse(quote.quotedAt!) > Date.parse(latest)
          ? quote.quotedAt!
          : latest,
      dated[0].quotedAt!,
    ),
  };
}

export interface MarketHistoryPoint {
  amount: string;
  currency: 'BRL';
  source: string;
  provider: string;
  quotedAt: string;
}
export async function fetchMarketHistory(
  id: number,
): Promise<MarketHistoryPoint[]> {
  const result = await request<{
    content: MarketHistoryPoint[];
    origin: string;
  }>(`/market/variants/${id}/history?page=0&size=30`);
  if (
    !result ||
    result.origin !== 'MARKET_REFERENCE' ||
    !Array.isArray(result.content) ||
    result.content.some(
      (point) =>
        !isCreditAmount(point.amount) ||
        point.currency !== 'BRL' ||
        !supportedSource(point.source) ||
        !point.provider ||
        Number.isNaN(Date.parse(point.quotedAt)),
    )
  ) {
    throw new ApiError(0, 'Histórico de referência inválido.');
  }
  return result.content;
}
