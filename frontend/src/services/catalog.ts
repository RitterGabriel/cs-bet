import { verifiedDropChance, verifiedSpecialReward } from './officialOdds';
import type { OfficialDropChance, SpecialReward } from './officialOdds';
import { validateMarketQuote } from './market';
import type { MarketQuote } from './market';
import { ApiError, request } from './api';
import { validatePrice } from './pricing';
import type { SimulatedPrice, PriceRange } from './pricing';
import type { CaseItem, CatalogCase, Rarity } from '../types/catalog';
import { customCaseArt } from '../data/customCaseArt';

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
  dropChance?: OfficialDropChance | null;
  phase?: string | null;
}
interface OfficialCaseDetail {
  box: OfficialCaseSummary;
  items: OfficialItem[];
  specialItems: OfficialItem[];
  specialReward?: SpecialReward | null;
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
  rarity_rare_character: 'mil-spec',
  rarity_mythical_character: 'restricted',
  rarity_legendary_character: 'classified',
  rarity_ancient_character: 'covert',
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
    dropChance: verifiedDropChance(item.dropChance),
    phase: typeof item.phase === 'string' ? item.phase : null,
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
    specialReward: verifiedSpecialReward(result.specialReward),
    source: result.source,
    sourceRevision: result.sourceRevision,
    specialNotice: result.specialNotice,
    imageNotice: result.imageNotice,
  };
}

interface CustomSummary {
  id: string; name: string; description: string; imageUrl: string; accent: string;
  openingPrice: string; available: boolean; itemCount: number; ruleVersion: string;
}
interface CustomItem extends Omit<OfficialItem, 'dropChance'> { customChance: string }
export function mapCustomCase(box: CustomSummary): CatalogCase {
  if (!box || !box.id?.startsWith('custom-') || !box.name || !/^\d{1,16}\.\d{2}$/.test(box.openingPrice) ||
      BigInt(box.openingPrice.replace('.', '')) <= 0n || !/^#[a-f\d]{6}$/i.test(box.accent) ||
      typeof box.available !== 'boolean' || !Number.isSafeInteger(box.itemCount) || box.itemCount < 0 ||
      typeof box.ruleVersion !== 'string' || !box.imageUrl?.startsWith('/images/'))
    throw new ApiError(0, 'Caixa personalizada inválida na resposta.');
  return { id: box.id, name: box.name, description: box.description, image: customCaseArt[box.id] ?? box.imageUrl, accent: box.accent,
    category: 'custom', theme: box.name.toUpperCase(), items: [], specialItems: [], isReal: false,
    customPrice: box.openingPrice, customReady: box.available, source: 'CSBET', sourceRevision: box.ruleVersion };
}
export async function listCustomCases(page: number, search: string, sort: string): Promise<CatalogPageResponse> {
  const query = new URLSearchParams({ page: String(page), search: search.trim(), sort });
  const result = await request<Omit<CatalogPageResponse, 'content'> & { content: CustomSummary[] }>(`/custom-cases?${query}`);
  if (!result || !Array.isArray(result.content) || !Number.isSafeInteger(result.totalElements) || result.totalElements < 0 ||
      !Number.isInteger(result.totalPages) || result.totalPages < 0 || result.page !== page || result.size !== 12)
    throw new ApiError(0, 'Catálogo personalizado inválido na resposta.');
  return { ...result, content: result.content.map(mapCustomCase) };
}
export async function getCustomCase(id: string): Promise<CatalogCase> {
  const result = await request<{ box: CustomSummary; items: CustomItem[]; notice: string }>(`/custom-cases/${encodeURIComponent(id)}`);
  const box = mapCustomCase(result?.box);
  if (box.id !== id || !Array.isArray(result.items) || result.items.length !== result.box.itemCount ||
      new Set(result.items.map(item => item.id)).size !== result.items.length)
    throw new ApiError(0, 'Conteúdo personalizado inválido na resposta.');
  const items = result.items.map(item => {
    if (typeof item.customChance !== 'string' || !/^\d{1,3}\.\d{2}$/.test(item.customChance) ||
        Number(item.customChance) <= 0 || Number(item.customChance) > 100 || typeof item.special !== 'boolean')
      throw new ApiError(0, 'Chance personalizada inválida.');
    return { ...mapItem(item), customChance: item.customChance };
  });
  if (box.customReady && items.reduce((sum, item) => sum + BigInt(item.customChance.replace('.', '')), 0n) !== 10000n)
    throw new ApiError(0, 'As chances personalizadas estão incompletas.');
  return { ...box, items: items.filter(item => item.specialCategory === 'none'),
    specialItems: items.filter(item => item.specialCategory !== 'none'), specialNotice: result.notice };
}
export const getCatalogCase = (id: string) => id.startsWith('custom-') ? getCustomCase(id) : getOfficialCase(id);
