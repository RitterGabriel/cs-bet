import { act, create } from 'react-test-renderer';
import type { ReactTestRenderer } from 'react-test-renderer';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { CatalogHeroShowcase } from './CatalogHeroShowcase';
import { cases } from '../data/cases';

const items = cases.filter(item => item.category === 'custom');
let renderer: ReactTestRenderer | undefined;
async function mount(selected = items) {
  await act(async () => { renderer = create(<CatalogHeroShowcase items={selected} custom />); });
}
const image = () => renderer!.root.findAllByType('img')[0].props.src;
const caption = () => renderer!.root.findByProps({ className: 'hero-image-caption' }).findByType('span').children.join('');
beforeEach(() => {
  vi.useFakeTimers();
  vi.stubGlobal('IS_REACT_ACT_ENVIRONMENT', true);
  vi.stubGlobal('window', { setInterval, clearInterval });
});
afterEach(async () => {
  await act(async () => renderer?.unmount());
  renderer = undefined;
  vi.useRealTimers(); vi.unstubAllGlobals();
});

describe('destaque de caixas do catálogo', () => {
  it('troca imagem e nome juntos a cada cinco segundos e reinicia o ciclo', async () => {
    await mount();
    for (const item of items) {
      expect(image()).toBe(item.image);
      expect(caption()).toBe(item.name);
      await act(async () => { vi.advanceTimersByTime(5000); });
    }
    expect(image()).toBe(items[0].image);
    expect(caption()).toBe(items[0].name);
  });
  it('alterna automaticamente sem controles de navegação ou pausa', async () => {
    await mount();
    expect(renderer!.root.findAllByType('button')).toHaveLength(0);
    await act(async () => { vi.advanceTimersByTime(5000); });
    expect(image()).toBe(items[1].image);
    expect(caption()).toBe(items[1].name);
  });
  it('acompanha a nova seleção de caixas após trocar filtro ou página', async () => {
    await mount();
    await act(async () => { vi.advanceTimersByTime(5000); });
    await act(async () => renderer!.update(<CatalogHeroShowcase items={[items[2]]} custom />));
    expect(image()).toBe(items[2].image);
    expect(caption()).toBe(items[2].name);
    expect(vi.getTimerCount()).toBe(0);
  });
  it('limpa o temporizador ao desmontar', async () => {
    await mount();
    expect(vi.getTimerCount()).toBe(1);
    await act(async () => renderer!.unmount());
    expect(vi.getTimerCount()).toBe(0);
  });
  it('não inventa uma caixa quando a seleção está vazia', async () => {
    await mount([]);
    expect(renderer!.root.findAllByType('img')).toHaveLength(0);
    expect(JSON.stringify(renderer!.toJSON())).toContain('Nenhuma caixa disponível');
    expect(vi.getTimerCount()).toBe(0);
  });
  it('mantém uma caixa única sem criar temporizador', async () => {
    await mount([items[0]]);
    expect(vi.getTimerCount()).toBe(0);
    expect(caption()).toBe(items[0].name);
  });
});
