import { permittedImage } from '../components/CatalogImage';
import type { CaseItem } from '../types/catalog';
import type { OpeningReelPlan } from '../types/opening';

export const visualSpecialCard: CaseItem = {
  id: 'csbet:visual-special-group', name: 'Item especial raro', image: '', rarity: 'special', decorativeSpecial: true,
  rarityData: { id: 'visual-special-group', name: 'Especial', color: '#e4b34a' },
};
type VisualRandom = () => number;

function visualItem(items: CaseItem[], previousId: string | undefined, random: VisualRandom): CaseItem {
  const alternatives = items.filter(item => item.id !== previousId);
  const pool = alternatives.length ? alternatives : items;
  return pool[Math.floor(random() * pool.length)];
}

export function openingPreviewItems(items: CaseItem[], hasSpecialGroup: boolean, random: VisualRandom = Math.random): CaseItem[] {
  if (!items.length) return [];
  const cards: CaseItem[] = [];
  for (let index = 0; index < 10; index++) cards.push(visualItem(items, cards.at(-1)?.id, random));
  // Presença meramente decorativa e opcional; não representa a chance oficial de especiais.
  if (hasSpecialGroup && random() < .25) cards[Math.floor(random() * cards.length)] = visualSpecialCard;
  return cards;
}

/** Aleatoriedade somente visual: o resultado e as probabilidades pertencem ao backend. */
export function buildOpeningReel(regularItems: CaseItem[], winner: CaseItem, hasSpecialGroup = false,
                                 random: VisualRandom = Math.random): OpeningReelPlan {
  if (!regularItems.length) throw new Error('Conteúdo da caixa indisponível.');
  const cards = new Array<CaseItem>(52);
  const winningIndex = 45;
  cards[winningIndex] = winner;
  // Evita vizinhos idênticos quando o catálogo oferece outras skins, inclusive junto ao vencedor.
  for (let index = winningIndex - 1; index >= 0; index--) cards[index] = visualItem(regularItems, cards[index + 1].id, random);
  for (let index = winningIndex + 1; index < cards.length; index++) cards[index] = visualItem(regularItems, cards[index - 1].id, random);
  // Pode haver uma passagem dourada ou nenhuma. Nunca posiciona um quase-acerto junto ao resultado.
  if (hasSpecialGroup && random() < .25) cards[6 + Math.floor(random() * 31)] = visualSpecialCard;
  return { cards, winningIndex };
}
export function openingOffset(viewportWidth: number, cardLeft: number, cardWidth: number): number {
  return viewportWidth / 2 - cardLeft - cardWidth / 2;
}

/** Prepara cada URL uma vez, com espera limitada e reserva ilustrativa para falhas. */
export async function prepareOpeningImages(items: CaseItem[], timeoutMs = 2500): Promise<Set<string>> {
  const urls = [...new Set(items.map(item => item.image).filter(src => permittedImage(src, true)))];
  const failed = new Set<string>();
  if (typeof Image === 'undefined') return failed;
  await Promise.all(urls.map(src => new Promise<void>(resolve => {
    const image = new Image();
    let settled = false;
    const finish = (success: boolean) => {
      if (settled) return;
      settled = true;
      clearTimeout(timer);
      image.onload = null;
      image.onerror = null;
      if (!success) failed.add(src);
      resolve();
    };
    const timer = setTimeout(() => finish(false), timeoutMs);
    image.referrerPolicy = 'no-referrer';
    image.onload = () => {
      if (typeof image.decode === 'function') image.decode().then(() => finish(true), () => finish(false));
      else finish(true);
    };
    image.onerror = () => finish(false);
    image.src = src;
  })));
  return failed;
}
