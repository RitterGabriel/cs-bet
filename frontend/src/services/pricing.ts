import { ApiError, request } from './api';

export interface SimulatedPrice {
  basePrice: string;
  currentPrice: string;
  priceDate: string;
  updatedAt: string;
  ruleVersion: string;
  unit: 'SIMULATED_CREDITS';
}
export interface PriceRange {
  minimum: string | null;
  maximum: string | null;
  unpricedItems: number;
}
export interface HistoryPoint {
  date: string;
  amount: string;
  eventType: 'INITIAL' | 'DAILY' | 'MARKET';
  source?: string;
  provider?: string;
}
export interface HistoryPage {
  content: HistoryPoint[];
  totalElements: number;
  totalPages: number;
  page: number;
  size: number;
  unit: 'SIMULATED_CREDITS';
}

export const isCreditAmount = (value: unknown): value is string =>
  typeof value === 'string' && /^\d{1,38}\.\d{2}$/.test(value);
// Formata centavos sem converter os preços para ponto flutuante nem somá-los no cliente.
export function formatSimulatedCredits(value: string): string {
  if (!isCreditAmount(value)) return 'Indisponível';
  const [integer, cents] = value.split('.');
  return `${new Intl.NumberFormat('pt-BR').format(BigInt(integer))},${cents}`;
}
export function validatePrice(
  price: SimulatedPrice | null | undefined,
): SimulatedPrice | null {
  if (price == null) return null;
  if (
    !isCreditAmount(price.basePrice) ||
    !isCreditAmount(price.currentPrice) ||
    price.unit !== 'SIMULATED_CREDITS' ||
    typeof price.priceDate !== 'string' ||
    Number.isNaN(Date.parse(price.updatedAt))
  ) {
    throw new ApiError(
      0,
      'Preço simulado inválido na resposta. Tente novamente.',
    );
  }
  return price;
}
export async function fetchPriceHistory(id: string): Promise<HistoryPage> {
  const result = await request<HistoryPage>(
    `/skins/${encodeURIComponent(id)}/price/history?page=0&size=30`,
  );
  if (
    !result ||
    !Array.isArray(result.content) ||
    result.unit !== 'SIMULATED_CREDITS' ||
    !Number.isSafeInteger(result.totalElements) ||
    result.content.some(
      (point) =>
        !isCreditAmount(point.amount) ||
        typeof point.date !== 'string' ||
        !/^\d{4}-\d{2}-\d{2}$/.test(point.date) ||
        Number.isNaN(Date.parse(point.date)),
    )
  ) {
    throw new ApiError(0, 'O histórico de preços está incompleto.');
  }
  return result;
}
