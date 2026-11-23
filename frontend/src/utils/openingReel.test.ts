import { afterEach, describe, expect, it, vi } from 'vitest';
import { buildOpeningReel, openingPreviewItems, visualSpecialCard, openingOffset, prepareOpeningImages } from './openingReel';
import { openingSkin, openingSpecial } from '../test/openingFixture';
afterEach(() => { vi.unstubAllGlobals(); vi.useRealTimers(); });
describe('trilho ilustrativo e preparo de imagens', () => {
  it('insere exatamente o vencedor recebido sem qualquer sorteio de raridade', () => {
    for (const winner of [openingSkin, openingSpecial]) {
      const plan = buildOpeningReel([openingSkin], winner);
      expect(plan.cards[plan.winningIndex]).toBe(winner);
      expect(plan.cards.filter((_, index) => index !== plan.winningIndex)).toEqual(Array(51).fill(openingSkin));
    }
    expect(() => buildOpeningReel([], openingSkin)).toThrow();
  });
  it('o dourado é opcional, muda de posição e nunca substitui o resultado', () => {
    const shown = buildOpeningReel([openingSkin], openingSkin, true, () => 0);
    const absent = buildOpeningReel([openingSkin], openingSkin, true, () => .99);
    expect(shown.cards.filter(card => card.decorativeSpecial)).toHaveLength(1);
    expect(absent.cards.some(card => card.decorativeSpecial)).toBe(false);
    expect(shown.cards[shown.winningIndex]).toBe(openingSkin);
    expect(shown.cards[shown.winningIndex - 1]).toBe(openingSkin);
    expect(shown.cards[shown.winningIndex + 1]).toBe(openingSkin);
    expect(openingPreviewItems([openingSkin], true, () => 0)).toContain(visualSpecialCard);
    expect(openingPreviewItems([openingSkin], true, () => .99)).not.toContain(visualSpecialCard);
    expect(buildOpeningReel([openingSkin], openingSpecial, true, () => 0).cards[45]).toBe(openingSpecial);
    const later = buildOpeningReel([openingSkin], openingSkin, true, () => .2);
    expect(later.cards.indexOf(visualSpecialCard)).not.toBe(shown.cards.indexOf(visualSpecialCard));
  });
  it('cada trilho tem sequência própria sem repetir a ordem do catálogo ou vizinhos idênticos', () => {
    const items = [openingSkin, { ...openingSkin, id: 'second' }, { ...openingSkin, id: 'third' }, { ...openingSkin, id: 'fourth' }];
    const seeded = (seed: number) => () => { seed = (Math.imul(seed, 1664525) + 1013904223) >>> 0; return seed / 4294967296; };
    const first = buildOpeningReel(items, openingSkin, true, seeded(1));
    const second = buildOpeningReel(items, openingSkin, true, seeded(42));
    expect(first.cards.map(card => card.id)).not.toEqual(second.cards.map(card => card.id));
    for (const plan of [first, second]) {
      expect(plan.cards[plan.winningIndex]).toBe(openingSkin);
      expect(plan.cards.every((card, index) => index === 0 || card.id !== plan.cards[index - 1].id)).toBe(true);
      expect(plan.cards.every(card => card.decorativeSpecial || items.some(item => item.id === card.id))).toBe(true);
    }
    expect(openingPreviewItems(items, true, seeded(1)).map(card => card.id))
      .not.toEqual(openingPreviewItems(items, true, seeded(42)).map(card => card.id));
  });
  it('centraliza o card no desktop e celular preservando medidas fracionárias', () => {
    for (const [width, cardWidth, gap] of [[1000, 160, 12], [390, 128, 8], [319.5, 128, 8]]) {
      const left = 45 * (cardWidth + gap);
      const offset = openingOffset(width, left, cardWidth);
      expect(left + cardWidth / 2 + offset).toBe(width / 2);
    }
  });
  it('prepara URLs únicas e usa falha explícita quando imagem não carrega', async () => {
    const images: Array<{ onload: (() => void) | null; onerror: (() => void) | null; src: string }> = [];
    class FakeImage {
      onload = null; onerror = null; src = ''; referrerPolicy = '';
      constructor() { images.push(this); }
      decode() { return Promise.resolve(); }
    }
    vi.stubGlobal('Image', FakeImage);
    const task = prepareOpeningImages([openingSkin, openingSkin, openingSpecial]);
    expect(images).toHaveLength(2);
    images[0].onload!(); images[1].onerror!();
    expect(await task).toEqual(new Set([openingSpecial.image]));
    expect(images[0].onload).toBeNull();
  });
  it('limita a espera por uma imagem que nunca responde', async () => {
    vi.useFakeTimers();
    vi.stubGlobal('Image', class { src = ''; onload = null; onerror = null; });
    const task = prepareOpeningImages([openingSkin], 1000);
    await vi.advanceTimersByTimeAsync(1001);
    expect(await task).toEqual(new Set([openingSkin.image]));
    expect(vi.getTimerCount()).toBe(0);
  });
});
