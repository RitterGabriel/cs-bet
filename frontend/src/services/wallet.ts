import { request } from './api';

export interface WalletBalance { balance: string }
export interface WalletMovement {
  id: number;
  type: 'CREDIT_ADDITION';
  amount: string;
  resultingBalance: string;
  createdAt: string;
}
export interface WalletHistory {
  content: WalletMovement[];
  page: number;
  totalPages: number;
  totalElements: number;
}
// O Oracle pode retornar decimais sem zeros finais. Formata sem perder centavos.
export function formatWalletAmount(amount: string): string {
  const parts = /^(\d{1,16})(?:\.(\d{1,2}))?$/.exec(amount);
  if (!parts) return 'Valor indisponível';
  const integer = BigInt(parts[1]).toLocaleString('pt-BR');
  const cents = (parts[2] ?? '').padEnd(2, '0');
  return `R$\u00a0${integer},${cents}`;
}

export function validCreditAmount(value: string) {
  return /^\d{1,5}([.,]\d{1,2})?$/.test(value) && Number(value.replace(',', '.')) > 0 && Number(value.replace(',', '.')) <= 10000;
}
export const walletApi = {
  balance: (token: string) => request<WalletBalance>('/wallet', { headers: { Authorization: `Bearer ${token}` } }),
  history: (token: string, page: number) => request<WalletHistory>(`/wallet/movements?page=${page}`, { headers: { Authorization: `Bearer ${token}` } }),
  add: (token: string, amount: string, requestId: string) => request<WalletBalance>('/wallet/credits', {
    method: 'POST', headers: { Authorization: `Bearer ${token}`, 'Content-Type': 'application/json' },
    body: JSON.stringify({ amount: amount.replace(',', '.'), requestId }),
  }),
};
