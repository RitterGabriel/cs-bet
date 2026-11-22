import type { MarketQuote } from '../services/market';
import type { SimulatedPrice, PriceRange } from '../services/pricing';

export type CaseCategory = 'official' | 'custom';
export type Rarity =
  | 'industrial'
  | 'mil-spec'
  | 'restricted'
  | 'classified'
  | 'covert'
  | 'special';

export interface CaseItem {
  id: string;
  name: string;
  image: string;
  rarity: Rarity;
  simulatedPrice?: SimulatedPrice | null;
  marketPrices?: MarketQuote[];
  rarityData?: { id: string; name: string; color: string };
  weapon?: string;
  weaponType?: string;
  specialCategory?: 'none' | 'knife' | 'glove';
}

export interface CatalogCase {
  id: string;
  name: string;
  category: CaseCategory;
  theme: string;
  accent: string;
  image: string;
  description: string;
  // Cada caixa possui seus próprios itens e seu próprio grupo especial.
  items: CaseItem[];
  specialItems: CaseItem[];
  specialKind?: 'knives' | 'gloves';
  isReal?: boolean;
  simulatedItemPrices?: PriceRange | null;
  marketPrice?: MarketQuote;
  descriptionFromSource?: boolean;
  language?: string;
  source?: string;
  sourceRevision?: string;
  specialNotice?: string;
  imageNotice?: string;
}
