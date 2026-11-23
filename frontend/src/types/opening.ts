import type { CaseItem } from './catalog';

export interface DemoOpeningResult {
  skinId: number;
  item: CaseItem;
  special: boolean;
  variant: { wear: import('../services/market').Wear; statTrak: boolean; phase: string | null; rule: string };
  inventoryId?: number;
  charged?: string;
  balance?: string;
  referencePrice?: string | null;
}
export type DemoOpeningOutcome =
  | { status: 'AVAILABLE'; result: DemoOpeningResult }
  | { status: 'UNAVAILABLE'; reason: string };
export interface OpeningReelPlan {
  cards: CaseItem[];
  winningIndex: number;
}
export type OpeningPhase = 'idle' | 'requesting' | 'preparing' | 'animating' | 'revealed' | 'complete' | 'unavailable' | 'error';
export interface DemoOpeningState {
  phase: OpeningPhase;
  runId: number;
  result: DemoOpeningResult | null;
  reel: OpeningReelPlan | null;
  results: DemoOpeningResult[];
  reels: OpeningReelPlan[];
  failedImages: Set<string>;
  error: string;
  authError: boolean;
}

export interface OpeningResponse {
  openingId: number; inventoryId: number; caseId: string;
  charged: string; balance: string; referencePrice: string | null;
  item: { skinId: number; externalId: string; name: string; imageUrl: string | null;
    rarity: { id: string; name: string; color: string }; special: boolean };
  variant: { wear: import('../services/market').Wear; statTrak: boolean; phase: string | null; rule: string };
}

export interface OpeningBatchResponse {
  batchId: number; caseId: string; quantity: number; charged: string; balance: string; results: OpeningResponse[];
}
