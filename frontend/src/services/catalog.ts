import { validateMarketQuote } from './market';
import type { MarketQuote } from './market';
import { ApiError, request } from './api';
import { validatePrice } from './pricing';
import type { SimulatedPrice, PriceRange } from './pricing';
import type { CaseItem, CatalogCase, Rarity } from '../types/catalog';

export interface OfficialCaseSummary {
  id: string;
  externalId: string;
  name: string;
  category: 'official';
  type: string;
  description: string | null;
  imageUrl: string | null;
  language: string;
  simulatedItemPrices?: PriceRange | null;
  marketPrice?: MarketQuote;
}
export interface OfficialItem {
  id: string;
  name: string;
  weapon: string;
  weaponType: string;
  specialCategory: 'none' | 'knife' | 'glove';
  imageUrl: string | null;
  rarity: { id: string; name: string; color: string };
  special: boolean;
  simulatedPrice?: SimulatedPrice | null;
  marketPrices?: MarketQuote[];
}
interface OfficialCaseDetail {
  box: OfficialCaseSummary;
  items: OfficialItem[];
  specialItems: OfficialItem[];
  source: string;
  sourceRevision: string;
  specialNotice: string;
  imageNotice: string;
}
export interface CatalogPageResponse {
  content: CatalogCase[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
}
const rarityKeys: Record<string, Rarity> = {
  rarity_rare_weapon: 'mil-spec',
  rarity_mythical_weapon: 'restricted',
  rarity_legendary_weapon: 'classified',
  rarity_ancient_weapon: 'covert',
  rarity_ancient: 'special',
  rarity_uncommon_weapon: 'industrial',
};
export function mapOfficialCase(box: OfficialCaseSummary): CatalogCase {
  if (
    !box ||
    typeof box.id !== 'string' ||
    !box.id.startsWith('crate-') ||
    typeof box.name !== 'string' ||
    box.category !== 'official'
  ) {
    throw new ApiError(
      0,
      'A resposta do catálogo está incompleta. Tente novamente.',
    );
  }
  return {
    id: box.id,
    name: box.name,
    category: 'official',
    theme: box.name.toUpperCase(),
    accent: '#00D4FF',
    image: box.imageUrl ?? '',
    description:
      box.description ||
      'Caixa oficial de armas do CS2. Consulte abaixo os itens associados na fonte pública.',
    items: [],
    specialItems: [],
    isReal: true,
    descriptionFromSource: Boolean(box.description),
    language: box.language,
    simulatedItemPrices: box.simulatedItemPrices,
    marketPrice: validateMarketQuote(box.marketPrice),
  };
}
export function mapItem(item: OfficialItem): CaseItem {
  if (
    !item ||
    typeof item.id !== 'string' ||
    typeof item.name !== 'string' ||
    !item.rarity ||
    typeof item.rarity.id !== 'string' ||
    typeof item.rarity.name !== 'string' ||
    !/^#[a-f\d]{6}$/i.test(item.rarity.color)
  ) {
    throw new ApiError(
      0,
      'Os detalhes dos itens estão incompletos. Tente novamente.',
    );
  }
  return {
    id: item.id,
    name: item.name,
    image: item.imageUrl ?? '',
    rarity: rarityKeys[item.rarity.id] ?? 'industrial',
    rarityData: item.rarity,
    weapon: item.weapon,
    weaponType: item.weaponType,
    specialCategory: item.specialCategory,
    simulatedPrice: validatePrice(item.simulatedPrice),
    marketPrices: (item.marketPrices ?? []).map(validateMarketQuote),
  };
}
export async function listOfficialCases(
  page: number,
  search: string,
  sort: string,
): Promise<CatalogPageResponse> {
  const query = new URLSearchParams({
    page: String(page),
    size: '12',
    search: search.trim(),
    category: 'official',
    sort,
  });
  const result = await request<
    Omit<CatalogPageResponse, 'content'> & { content: OfficialCaseSummary[] }
  >(`/cases?${query}`);
  if (
    !result ||
    !Array.isArray(result.content) ||
    !Number.isInteger(result.totalPages) ||
    !Number.isSafeInteger(result.totalElements) ||
    result.totalElements < 0 ||
    result.totalPages < 0
  ) {
    throw new ApiError(
      0,
      'A resposta do catálogo está incompleta. Tente novamente.',
    );
  }
  return { ...result, content: result.content.map(mapOfficialCase) };
}
export async function getOfficialCase(id: string): Promise<CatalogCase> {
  const result = await request<OfficialCaseDetail>(
    `/cases/${encodeURIComponent(id)}`,
  );
  if (
    !result ||
    !Array.isArray(result.items) ||
    !Array.isArray(result.specialItems)
  ) {
    throw new ApiError(
      0,
      'Os detalhes da caixa estão incompletos. Tente novamente.',
    );
  }
  return {
    ...mapOfficialCase(result.box),
    items: result.items.map(mapItem),
    specialItems: result.specialItems.map(mapItem),
    source: result.source,
    sourceRevision: result.sourceRevision,
    specialNotice: result.specialNotice,
    imageNotice: result.imageNotice,
  };
}
