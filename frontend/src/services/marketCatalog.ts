import { request, ApiError } from './api';
import { mapItem } from './catalog';
import type { OfficialItem } from './catalog';
import type { CaseItem } from '../types/catalog';

export interface MarketFilters {
  kind: 'all' | 'skins' | 'agents';
  category: string;
  search: string;
  weapon: string;
  rarity: string;
  wear: string;
  sort: string;
}
export interface MarketCategoryOption {
  id: string;
  name: string;
  weapons: string[];
}

export const INITIAL_MARKET_FILTERS: MarketFilters = {
  kind: 'all',
  category: '',
  search: '',
  weapon: '',
  rarity: '',
  wear: '',
  sort: 'name',
};

export function updateMarketFilter(
  current: MarketFilters,
  key: keyof MarketFilters,
  value: string,
): MarketFilters {
  return {
    ...current,
    [key]: value,
    ...(key === 'category' && value !== current.category
      ? {
          weapon: '',
          ...(value === 'agents' || current.category === 'agents'
            ? { rarity: '', wear: '' }
            : {}),
        }
      : {}),
  };
}

export function resetMarketFilters(kind: MarketFilters['kind']): MarketFilters {
  return { ...INITIAL_MARKET_FILTERS, kind };
}
export function marketTeamLabel(value: string) {
  return value === 'Counter-Terrorist'
    ? 'Contraterroristas'
    : value === 'Terrorist'
      ? 'Terroristas'
      : value;
}

export function categoryWeapons(
  data: Pick<MarketSkinPage, 'categories' | 'weapons'>,
  category: string,
): string[] {
  return category
    ? (data.categories.find((option) => option.id === category)?.weapons ?? [])
    : data.weapons;
}

export interface MarketSkinPage {
  content: CaseItem[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
  categories: MarketCategoryOption[];
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
  const categories = data?.categories ?? [];
  if (
    !data ||
    !Array.isArray(data.content) ||
    !Number.isInteger(data.totalPages) ||
    data.totalPages < 0 ||
    !Number.isSafeInteger(data.totalElements) ||
    data.totalElements < 0 ||
    !Array.isArray(data.weapons) ||
    !Array.isArray(data.rarities) ||
    !Array.isArray(categories) ||
    categories.some(
      (category) =>
        category == null ||
        typeof category.id !== 'string' ||
        typeof category.name !== 'string' ||
        !Array.isArray(category.weapons) ||
        category.weapons.some((weapon) => typeof weapon !== 'string'),
    )
  )
    throw new ApiError(0, 'A resposta do mercado está incompleta.');
  return { ...data, categories, content: data.content.map(mapItem) };
}
