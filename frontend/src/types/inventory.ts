export type InventoryStatus = 'AVAILABLE' | 'SOLD';

export interface InventoryItem {
  id: number;
  skinId: number;
  externalId: string;
  name: string;
  imageUrl: string | null;
  rarity: { id: string; name: string; color: string };
  obtainedAt: string;
  status: InventoryStatus;
}

export interface InventoryPageResponse {
  content: InventoryItem[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
}
