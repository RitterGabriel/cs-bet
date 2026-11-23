import { validateMarketQuote } from './market';
import { api, ApiError, request } from './api';
import { isCreditAmount, validatePrice } from './pricing';
import type {
  InventoryPageResponse,
  InventoryStatus,
  InventorySort,
} from '../types/inventory';

export async function fetchInventory(
  token: string,
  page: number,
  status?: InventoryStatus,
  sort?: InventorySort,
): Promise<InventoryPageResponse> {
  const query = new URLSearchParams({ page: String(page), size: '12' });
  if (status) query.set('status', status);
  if (sort && sort !== 'RECENT') query.set('sort', sort);
  const result = await request<InventoryPageResponse>(
    `/inventory/me?${query}`,
    {
      headers: { Authorization: `Bearer ${token}` },
    },
  );
  if (
    !result ||
    !Array.isArray(result.content) ||
    result.valuationCurrency !== 'BRL' ||
    (result.estimatedAvailableTotal !== null &&
      !isCreditAmount(result.estimatedAvailableTotal)) ||
    !Number.isSafeInteger(result.availableItems) ||
    result.availableItems < 0 ||
    !Number.isSafeInteger(result.unpricedAvailableItems) ||
    result.unpricedAvailableItems < 0 ||
    !Number.isSafeInteger(result.totalElements) ||
    result.totalElements < 0 ||
    !Number.isInteger(result.totalPages) ||
    result.totalPages < 0 ||
    !Number.isInteger(result.page) ||
    result.page < 0 ||
    !Number.isInteger(result.size) ||
    result.size < 1 ||
    result.content.some(
      (item) =>
        !item ||
        !Number.isSafeInteger(item.id) ||
        !Number.isSafeInteger(item.skinId) ||
        typeof item.externalId !== 'string' ||
        typeof item.name !== 'string' ||
        (item.imageUrl !== null && typeof item.imageUrl !== 'string') ||
        !item.rarity ||
        typeof item.rarity.id !== 'string' ||
        typeof item.rarity.name !== 'string' ||
        !/^#[a-f\d]{6}$/i.test(item.rarity.color) ||
        typeof item.obtainedAt !== 'string' ||
        Number.isNaN(Date.parse(item.obtainedAt)) ||
        !['AVAILABLE', 'SOLD'].includes(item.status),
    )
  ) {
    throw new ApiError(
      0,
      'Os dados do inventário estão incompletos. Tente novamente.',
    );
  }
  result.content.forEach((item) => {
    validatePrice(item.simulatedPrice);
    item.marketPrice = validateMarketQuote(item.marketPrice);
  });
  return result;
}

export interface InventorySaleReceipt { saleId: number; inventoryId: number; amount: string; balance: string }
export async function sellInventoryItem(item: import('../types/inventory').InventoryItem, expectedUserId: number): Promise<InventorySaleReceipt> {
  const session = await api.restoreSession();
  const owner = await api.me(session.accessToken);
  if (owner.id !== expectedUserId) throw new ApiError(401, 'A sessão mudou. Atualize a página.');
  const key = `csbet:pending-sale:${expectedUserId}:${item.id}`;
  const stored = localStorage.getItem(key);
  const pending: { requestId: string; expectedPrice: string } = stored ? JSON.parse(stored) : {
    requestId: crypto.randomUUID(), expectedPrice: item.marketPrice?.currentPrice ?? '',
  };
  if (!isCreditAmount(pending.expectedPrice)) throw new ApiError(409, 'Esta variante está sem cotação para venda.');
  localStorage.setItem(key, JSON.stringify(pending));
  let receipt: InventorySaleReceipt;
  try {
    receipt = await request(`/inventory/${item.id}/sell`, { method: 'POST',
      headers: { Authorization: `Bearer ${session.accessToken}`, 'Content-Type': 'application/json' },
      body: JSON.stringify(pending),
    }, true);
  } catch (error) {
    if (error instanceof ApiError && ([400, 404].includes(error.status) || error.code === 'SALE_REJECTED')) localStorage.removeItem(key);
    throw error;
  }
  if (!receipt || !Number.isSafeInteger(receipt.saleId) || receipt.saleId <= 0 || receipt.inventoryId !== item.id ||
      !isCreditAmount(receipt.amount) || !isCreditAmount(receipt.balance) ||
      BigInt(receipt.amount.replace('.', '')) !== BigInt(pending.expectedPrice.replace('.', '')))
    throw new ApiError(0, 'Não foi possível confirmar a venda. Repita a mesma tentativa para recuperar o resultado.');
  localStorage.removeItem(key);
  return receipt;
}

interface BulkSaleLine { inventoryId: number; expectedPrice: string }
interface BulkSaleQuote { items: BulkSaleLine[]; skippedItems: number; total: string }
export interface BulkSaleReceipt { batchId: number; soldItems: number; amount: string; balance: string; skippedItems: number }
export async function sellAllInventory(expectedUserId: number): Promise<BulkSaleReceipt> {
  const session = await api.restoreSession();
  const owner = await api.me(session.accessToken);
  if (owner.id !== expectedUserId) throw new ApiError(401, 'A sessão mudou. Atualize a página.');
  const headers = { Authorization: `Bearer ${session.accessToken}`, 'Content-Type': 'application/json' };
  const key = `csbet:pending-sell-all:${expectedUserId}`;
  const stored = localStorage.getItem(key);
  let pending: { requestId: string; items: BulkSaleLine[]; skippedItems: number };
  const cents = (value: string) => BigInt(value.replace('.', ''));
  if (stored) pending = JSON.parse(stored);
  else {
    const quote = await request<BulkSaleQuote>('/inventory/sell-all/quote', { headers }, true);
    if (!quote || !Array.isArray(quote.items) || !Number.isSafeInteger(quote.skippedItems) || quote.skippedItems < 0 ||
        !isCreditAmount(quote.total) || quote.items.some(line => !line || !Number.isSafeInteger(line.inventoryId) || line.inventoryId <= 0 ||
          !isCreditAmount(line.expectedPrice) || cents(line.expectedPrice) <= 0n) || new Set(quote.items.map(line => line.inventoryId)).size !== quote.items.length ||
        quote.items.reduce((sum, line) => sum + cents(line.expectedPrice), 0n) !== cents(quote.total))
      throw new ApiError(0, 'Não foi possível validar os itens disponíveis para venda.');
    if (!quote.items.length) throw new ApiError(409, 'Nenhuma skin disponível com cotação vigente para vender.');
    pending = { requestId: crypto.randomUUID(), items: quote.items, skippedItems: quote.skippedItems };
    localStorage.setItem(key, JSON.stringify(pending));
  }
  let result: BulkSaleReceipt;
  try {
    result = await request('/inventory/sell-all', { method: 'POST', headers,
      body: JSON.stringify({ requestId: pending.requestId, items: pending.items }),
    }, true);
  } catch (error) {
    if (error instanceof ApiError && ([400, 404].includes(error.status) || error.code === 'SALE_REJECTED')) localStorage.removeItem(key);
    throw error;
  }
  if (!result || !Number.isSafeInteger(result.batchId) || result.batchId <= 0 || result.soldItems !== pending.items.length ||
      !isCreditAmount(result.amount) || !isCreditAmount(result.balance) ||
      pending.items.reduce((sum, line) => sum + cents(line.expectedPrice), 0n) !== cents(result.amount))
    throw new ApiError(0, 'Não foi possível confirmar a venda. Repita a mesma tentativa para recuperar o resultado.');
  localStorage.removeItem(key);
  return { ...result, skippedItems: pending.skippedItems };
}
