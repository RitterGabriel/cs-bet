import { api, ApiError, request } from './api';
import { getOfficialCase } from './catalog';
import { verifiedDropChance, verifiedSpecialReward } from './officialOdds';
import type { CatalogCase } from '../types/catalog';
import { isCreditAmount } from './pricing';
import { wearLabels } from './market';
import type { OpeningResponse, DemoOpeningOutcome } from '../types/opening';

export const DEMO_OPENING_NOTICE = 'Simulação com créditos fictícios. A abertura desconta o valor da caixa e adiciona uma skin virtual ao inventário. Sem dinheiro real.';
const invalidResult = () => new ApiError(0, 'Não foi possível validar o resultado demonstrativo. Tente novamente.');

export function demoOpeningAvailable(box: CatalogCase): boolean {
  return box.isReal === true && box.category === 'official' && box.items.length > 0 &&
    verifiedSpecialReward(box.specialReward) !== null && box.items.every(item => verifiedDropChance(item.dropChance) !== null);
}

// Mantém o mesmo UUID após timeout ou resposta inválida, inclusive após F5.
// Repetir uma tentativa incerta recupera o resultado já persistido; nunca cobra de novo.
export async function requestDemoOpening(box: CatalogCase, expectedUserId: number): Promise<DemoOpeningOutcome> {
  const session = await api.restoreSession();
  const user = await api.me(session.accessToken);
  if (user.id !== expectedUserId) throw new ApiError(401, 'A sessão mudou. Atualize a página.');
  const key = `csbet:pending-opening:${expectedUserId}:${box.id}`;
  const stored = localStorage.getItem(key);
  const pending: { requestId: string; expectedPrice: string } = stored ? JSON.parse(stored) : {
    requestId: crypto.randomUUID(), expectedPrice: box.marketPrice?.currentPrice ?? '',
  };
  if (!isCreditAmount(pending.expectedPrice) || !pending.requestId) throw new ApiError(409, 'Preço da caixa indisponível. Atualize a página.');
  localStorage.setItem(key, JSON.stringify(pending));
  let data: OpeningResponse;
  try {
    data = await request<OpeningResponse>('/openings', {
      method: 'POST', headers: { Authorization: `Bearer ${session.accessToken}`, 'Content-Type': 'application/json' },
      body: JSON.stringify({ caseId: box.id, ...pending }),
    }, true);
  } catch (error) {
    // Erros de validação/conflito recusam a transação; falhas 5xx e de rede são ambíguas.
    if (error instanceof ApiError && ([400, 404].includes(error.status) || error.code === 'OPENING_REJECTED')) localStorage.removeItem(key);
    throw error;
  }
  const result = await normalizeOpeningResponse(data, box, pending.expectedPrice);
  localStorage.removeItem(key);
  return { status: 'AVAILABLE', result };
}

async function normalizeOpeningResponse(data: OpeningResponse, box: CatalogCase, expectedPrice: string) {
  if (!data || data.caseId !== box.id || !Number.isSafeInteger(data.openingId) || data.openingId <= 0 ||
      !Number.isSafeInteger(data.inventoryId) || data.inventoryId <= 0 || !isCreditAmount(data.charged) ||
      !isCreditAmount(data.balance) || BigInt(data.charged.replace('.', '')) !== BigInt(expectedPrice.replace('.', '')) ||
      (data.referencePrice !== null && !isCreditAmount(data.referencePrice)) || !data.item ||
      !Number.isSafeInteger(data.item.skinId) || typeof data.item.name !== 'string' || !data.item.name ||
      typeof data.item.special !== 'boolean' || !data.item.rarity || !/^#[a-f\d]{6}$/i.test(data.item.rarity.color) ||
      !data.variant || !Object.hasOwn(wearLabels, data.variant.wear) || typeof data.variant.statTrak !== 'boolean' ||
      (data.variant.phase !== null && typeof data.variant.phase !== 'string') || data.variant.rule !== 'CSBET_UNIFORM_WEAR_THEN_STATTRAK_V1') throw invalidResult();
  const winner = data.item;
  const match = (catalog: CatalogCase) => (winner.special ? catalog.specialItems : catalog.items)
    .find(item => item.id === winner.externalId && (!item.rarityData || item.rarityData.id === winner.rarity.id));
  const catalogItem = match(box) ?? match(await getOfficialCase(box.id));
  if (!catalogItem) throw invalidResult();
  return {
    skinId: winner.skinId, special: winner.special, variant: data.variant,
    inventoryId: data.inventoryId, charged: data.charged, balance: data.balance, referencePrice: data.referencePrice,
    item: { ...catalogItem, name: winner.name, rarityData: winner.rarity, phase: data.variant.phase },
  };
}

export const openingQuantities = [1, 2, 5, 10] as const;
export type OpeningQuantity = typeof openingQuantities[number];
export function pendingOpeningQuantity(boxId: string, userId: number | undefined): OpeningQuantity | null {
  if (!userId) return null;
  for (const kind of ['pending-batch', 'pending-opening']) {
    try {
      const stored = localStorage.getItem(`csbet:${kind}:${userId}:${boxId}`);
      if (!stored) continue;
      const pending = JSON.parse(stored) as { quantity?: number };
      const quantity = pending.quantity ?? 1;
      if (openingQuantities.includes(quantity as OpeningQuantity)) return quantity as OpeningQuantity;
    } catch { /* Armazenamento indisponível não cria uma abertura. */ }
  }
  return null;
}
export async function requestOpeningBatch(box: CatalogCase, expectedUserId: number, quantity: OpeningQuantity) {
  const pendingQuantity = pendingOpeningQuantity(box.id, expectedUserId);
  if (pendingQuantity !== null && pendingQuantity !== quantity)
    throw new ApiError(409, `Recupere primeiro a tentativa de ${pendingQuantity} caixa(s), sem uma nova cobrança.`);
  if (quantity === 1) {
    const outcome = await requestDemoOpening(box, expectedUserId);
    return outcome.status === 'AVAILABLE' ? { status: 'AVAILABLE' as const, results: [outcome.result] } : outcome;
  }
  const session = await api.restoreSession();
  const user = await api.me(session.accessToken);
  if (user.id !== expectedUserId) throw new ApiError(401, 'A sessão mudou. Atualize a página.');
  const key = `csbet:pending-batch:${expectedUserId}:${box.id}`;
  const stored = localStorage.getItem(key);
  const pending: { requestId: string; expectedPrice: string; quantity: number } = stored ? JSON.parse(stored) : {
    requestId: crypto.randomUUID(), expectedPrice: box.marketPrice?.currentPrice ?? '', quantity,
  };
  if (pending.quantity !== quantity || !isCreditAmount(pending.expectedPrice)) throw invalidResult();
  localStorage.setItem(key, JSON.stringify(pending));
  let batch: import('../types/opening').OpeningBatchResponse;
  try {
    batch = await request('/openings/batch', { method: 'POST',
      headers: { Authorization: `Bearer ${session.accessToken}`, 'Content-Type': 'application/json' },
      body: JSON.stringify({ caseId: box.id, ...pending }),
    }, true);
  } catch (error) {
    if (error instanceof ApiError && ([400, 404].includes(error.status) || error.code === 'OPENING_REJECTED')) localStorage.removeItem(key);
    throw error;
  }
  const cents = (value: string) => BigInt(value.replace('.', ''));
  if (!batch || !Number.isSafeInteger(batch.batchId) || batch.batchId <= 0 || batch.caseId !== box.id || batch.quantity !== quantity ||
      !isCreditAmount(batch.charged) || !isCreditAmount(batch.balance) || cents(batch.charged) !== cents(pending.expectedPrice) * BigInt(quantity) ||
      !Array.isArray(batch.results) || batch.results.length !== quantity ||
      new Set(batch.results.map(r => r?.inventoryId)).size !== quantity || new Set(batch.results.map(r => r?.openingId)).size !== quantity) throw invalidResult();
  const results = await Promise.all(batch.results.map(result => normalizeOpeningResponse(result, box, pending.expectedPrice)));
  if (batch.results.at(-1)!.balance !== batch.balance || batch.results.some((r, i) => i > 0 &&
      cents(batch.results[i - 1].balance) - cents(r.balance) !== cents(pending.expectedPrice))) throw invalidResult();
  localStorage.removeItem(key);
  return { status: 'AVAILABLE' as const, results };
}
