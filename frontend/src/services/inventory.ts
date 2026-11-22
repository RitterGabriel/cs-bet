import { ApiError, request } from './api';
import type {
  InventoryPageResponse,
  InventoryStatus,
} from '../types/inventory';

export async function fetchInventory(
  token: string,
  page: number,
  status?: InventoryStatus,
): Promise<InventoryPageResponse> {
  const query = new URLSearchParams({ page: String(page), size: '12' });
  if (status) query.set('status', status);
  const result = await request<InventoryPageResponse>(
    `/inventory/me?${query}`,
    {
      headers: { Authorization: `Bearer ${token}` },
    },
  );
  if (
    !result ||
    !Array.isArray(result.content) ||
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
  return result;
}
