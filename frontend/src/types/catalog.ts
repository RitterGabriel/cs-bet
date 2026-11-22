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
}
