import { api, ApiError, request } from './api';

export type ActivityType = 'ALL' | 'CREDIT_ADDITION' | 'ITEM_OBTAINED' | 'ITEM_SOLD';
export interface PersonalSummary {
  balance: string;
  creditsAdded: string;
  walletMovements: number;
  availableItems: number;
  inventoryEstimatedValue: string | null;
  unpricedAvailableItems: number;
  soldItems: number;
  salesTotal: string | null;
  unvaluedSales: number;
  openingHistoryAvailable: boolean;
  openedCases: number | null;
  openingSpend: string | null;
}
export interface PersonalActivity {
  id: string;
  type: Exclude<ActivityType, 'ALL'>;
  occurredAt: string | null;
  itemName: string | null;
  amount: string | null;
  resultingBalance: string | null;
}
export interface PersonalStatistics {
  summary: PersonalSummary;
  activity: { content: PersonalActivity[]; page: number; size: number; totalElements: number; totalPages: number };
}
export const activityLabels = {
  CREDIT_ADDITION: 'Créditos adicionados', ITEM_OBTAINED: 'Item recebido', ITEM_SOLD: 'Venda fictícia',
};
const money = (value: unknown): value is string => typeof value === 'string' && /^\d{1,38}(\.\d{1,2})?$/.test(value);
const count = (value: unknown): value is number => Number.isSafeInteger(value) && Number(value) >= 0;

export function validatePersonalStatistics(data: PersonalStatistics): PersonalStatistics {
  const s = data?.summary;
  const a = data?.activity;
  if (!s || !a || !money(s.balance) || !money(s.creditsAdded) || !count(s.walletMovements) ||
      !count(s.availableItems) || !count(s.unpricedAvailableItems) || s.unpricedAvailableItems > s.availableItems ||
      !count(s.soldItems) || !count(s.unvaluedSales) || s.unvaluedSales > s.soldItems ||
      (s.inventoryEstimatedValue !== null && !money(s.inventoryEstimatedValue)) ||
      (s.salesTotal !== null && !money(s.salesTotal)) || typeof s.openingHistoryAvailable !== 'boolean' ||
      (s.openingHistoryAvailable ? !count(s.openedCases) || !money(s.openingSpend) : s.openedCases !== null || s.openingSpend !== null) ||
      !Array.isArray(a.content) || !count(a.page) || !count(a.size) || a.size < 1 || a.size > 60 ||
      !count(a.totalElements) || !count(a.totalPages) || a.totalPages !== Math.ceil(a.totalElements / a.size) || a.content.length > a.size ||
      a.content.some(row => !row || typeof row.id !== 'string' || !Object.hasOwn(activityLabels, row.type) ||
        (row.occurredAt !== null && (typeof row.occurredAt !== 'string' || Number.isNaN(Date.parse(row.occurredAt)))) ||
        (row.itemName !== null && typeof row.itemName !== 'string') ||
        (row.amount !== null && !money(row.amount)) || (row.resultingBalance !== null && !money(row.resultingBalance))))
    throw new ApiError(0, 'Os dados das estatísticas estão incompletos. Tente novamente.');
  return data;
}

export async function getPersonalStatistics(expectedUserId: number, page: number, type: ActivityType): Promise<PersonalStatistics> {
  const session = await api.restoreSession();
  const owner = await api.me(session.accessToken);
  if (owner.id !== expectedUserId) throw new ApiError(401, 'A sessão mudou. Atualize a página.');
  const query = new URLSearchParams({ page: String(page), size: '20', type });
  const data = await request<PersonalStatistics>(`/statistics/me?${query}`, {
    headers: { Authorization: `Bearer ${session.accessToken}` },
  });
  const validated = validatePersonalStatistics(data);
  if (validated.activity.page !== page) throw new ApiError(0, 'A página do histórico não corresponde à solicitação.');
  return validated;
}

export function formatPersonalAmount(value: string | null): string {
  if (value === null) return 'Não disponível';
  const [integer, fraction = ''] = value.split('.');
  return `R$\u00a0${BigInt(integer).toLocaleString('pt-BR')},${fraction.padEnd(2, '0')}`;
}
