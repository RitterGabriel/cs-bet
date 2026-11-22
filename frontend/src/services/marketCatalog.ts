import { request, ApiError } from './api';
import { mapItem } from './catalog';
import type { OfficialItem } from './catalog';
import type { CaseItem } from '../types/catalog';

export interface MarketFilters {
  search: string;
  weapon: string;
  rarity: string;
  wear: string;
  sort: string;
}
export interface MarketSkinPage {
  content: CaseItem[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
  weapons: string[];
  rarities: { id: string; name: string; color: string }[];
}
export async function listMarketSkins(
  page: number,
  filters: MarketFilters,
): Promise<MarketSkinPage> {
  const query = new URLSearchParams({
    page: String(page),
    size: '24',
    ...filters,
  });
  const data = await request<
    Omit<MarketSkinPage, 'content'> & { content: OfficialItem[] }
  >(`/market/skins?${query}`);
  if (
    !data ||
    !Array.isArray(data.content) ||
    !Number.isInteger(data.totalPages) ||
    data.totalPages < 0 ||
    !Number.isSafeInteger(data.totalElements) ||
    data.totalElements < 0 ||
    !Array.isArray(data.weapons) ||
    !Array.isArray(data.rarities)
  )
    throw new ApiError(0, 'A resposta do mercado está incompleta.');
  return { ...data, content: data.content.map(mapItem) };
}
