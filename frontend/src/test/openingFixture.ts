import type { CatalogCase, CaseItem } from '../types/catalog';
import type { DemoOpeningResult, OpeningResponse } from '../types/opening';
import { OFFICIAL_ODDS_SOURCE } from '../services/officialOdds';
import { unavailableQuote } from '../services/market';

export const openingSkin: CaseItem = {
  id: 'skin-normal', name: 'AK-47 | Fixture Demo', rarity: 'restricted',
  image: 'https://community.akamai.steamstatic.com/economy/image/normal',
  rarityData: { id: 'rarity_mythical_weapon', name: 'Restricted', color: '#8847ff' },
  dropChance: { percent: '1.06564365', basis: 'OFFICIAL_RULES_CALCULATED', scope: 'SKIN_ALL_VARIANTS', sourceUrl: OFFICIAL_ODDS_SOURCE },
  marketPrices: [10, 12.5].map((amount, index) => ({ ...unavailableQuote, variantId: index + 1, status: 'AVAILABLE', currentPrice: amount.toFixed(2),
    source: 'MARKET_CSGO', provider: 'TEST', quotedAt: new Date().toISOString(), validUntil: new Date(Date.now() + 3600000).toISOString(), wear: index === 0 ? 'FT' : 'FN' })),
};
export const openingSpecial: CaseItem = { ...openingSkin, id: 'skin-special', name: 'Knife | Fixture Special', rarity: 'covert', phase: 'Phase 2',
  image: 'https://community.akamai.steamstatic.com/economy/image/special', dropChance: null, marketPrices: [], specialCategory: 'knife',
  rarityData: { id: 'rarity_ancient_weapon', name: 'Covert', color: '#eb4b4b' } };
export const openingBox: CatalogCase = {
  id: 'crate-fixture', name: 'Fixture Case', category: 'official', isReal: true, theme: '', accent: '#00D4FF', image: '', description: '',
  items: [openingSkin], specialItems: [openingSpecial],
  marketPrice: { ...openingSkin.marketPrices![0], currentPrice: '2.71', wear: 'NONE' },
  specialReward: { configured: true, selectionRule: 'CSBET_UNIFORM_CATALOG_ITEM',
    dropChance: { percent: '0.25575448', basis: 'OFFICIAL_RULES_CALCULATED', scope: 'SPECIAL_GROUP', sourceUrl: OFFICIAL_ODDS_SOURCE } },
};
export const openingResult: DemoOpeningResult = { skinId: 1, item: openingSkin, special: false, variant: { wear: 'FT', statTrak: false, phase: null, rule: 'CSBET_UNIFORM_WEAR_THEN_STATTRAK_V1' } };
export const openingUser = { id: 1, username: 'player', email: 'p@example.invalid', balance: 500, createdAt: '2026-11-22T12:00:00Z' };

export function openingResponse(special = false): OpeningResponse {
  const skin = special ? openingSpecial : openingSkin;
  return { openingId: 11, inventoryId: 22, caseId: openingBox.id, charged: '2.71', balance: '497.29',
    referencePrice: special ? null : '10.00',
    item: { skinId: special ? 88 : 1, externalId: skin.id, name: `${skin.name} (FT)`, imageUrl: skin.image, rarity: skin.rarityData!, special },
    variant: { wear: 'FT', statTrak: false, phase: null, rule: 'CSBET_UNIFORM_WEAR_THEN_STATTRAK_V1' } };
}
export function memoryOpeningStorage() {
  const entries = new Map<string, string>();
  return { getItem: (key: string) => entries.get(key) ?? null,
    setItem: (key: string, value: string) => { entries.set(key, value); },
    removeItem: (key: string) => { entries.delete(key); } };
}

export function openingBatchResponse(quantity: number): import('../types/opening').OpeningBatchResponse {
  return { batchId: 31, caseId: openingBox.id, quantity, charged: (quantity * 2.71).toFixed(2),
    balance: (500 - quantity * 2.71).toFixed(2), results: Array.from({ length: quantity }, (_, index) => ({
      ...openingResponse(index % 2 === 1), openingId: index + 1, inventoryId: index + 1, balance: (500 - (index + 1) * 2.71).toFixed(2),
    })) };
}
