import { act, create } from 'react-test-renderer';
import type { ReactTestRenderer } from 'react-test-renderer';
import { MemoryRouter } from 'react-router';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { DemoOpeningPanel } from './DemoOpeningPanel';
import { prepareOpeningImages } from '../../utils/openingReel';
import { openingResponse, openingBatchResponse, memoryOpeningStorage, openingBox, openingSkin, openingSpecial, openingUser } from '../../test/openingFixture';
import type { CatalogCase } from '../../types/catalog';

const auth = vi.hoisted(() => ({ user: { id: 1, balance: 500 } as { id: number; balance: number } | null, isLoading: false, refreshUser: async () => {}, getInventory: async () => {} }));
vi.mock('../../contexts/AuthContext', () => ({ useAuth: () => auth }));
vi.mock('../../utils/openingReel', async original => ({ ...await original<typeof import('../../utils/openingReel')>(), prepareOpeningImages: vi.fn() }));
const fetchMock = vi.fn();
let renderer: ReactTestRenderer | undefined;
let postResponse: () => Promise<Response>;
let reduced = false;
let width = 800;
const showModal = vi.fn();
const close = vi.fn();
const disconnect = vi.fn();
let animations: Array<{ finish: () => void; cancel: ReturnType<typeof vi.fn>; frames: Keyframe[] }>;
let resize: (() => void)[];
let rail: { style: { transform: string }; children: unknown[]; getBoundingClientRect: () => { left: number }; animate: ReturnType<typeof vi.fn> };
const text = () => JSON.stringify(renderer!.toJSON());
const flush = () => new Promise(resolve => setTimeout(resolve, 0));
const postCount = () => fetchMock.mock.calls.filter(call => ['/api/openings', '/api/openings/batch'].includes(call[0])).length;
function button(label: string) {
  return renderer!.root.findAllByType('button').find(node => node.children.filter(child => typeof child === 'string').join('').includes(label))!;
}
function tree(box = openingBox) { return <MemoryRouter initialEntries={['/cases/crate-fixture']}><DemoOpeningPanel box={box} /></MemoryRouter>; }
async function mount(box: CatalogCase = openingBox) {
  await act(async () => { renderer = create(tree(box), { createNodeMock: element => {
    const props = element.props as Record<string, unknown>;
    if (String(props.className).startsWith('opening-viewport')) return { clientLeft: 0, getBoundingClientRect: () => ({ width, left: 0 }) };
    if (props.className === 'opening-track') return rail;
    if (props['data-opening-winner']) return { getBoundingClientRect: () => ({ left: 45 * 172, width: 160 }) };
    if (element.type === 'dialog') return { showModal, close };
    return {};
  } }); });
}
async function click(label: string) { await act(async () => { button(label).props.onClick(); await flush(); }); }

beforeEach(() => {
  vi.stubGlobal('IS_REACT_ACT_ENVIRONMENT', true);
  auth.user = { id: 1, balance: 500 }; auth.isLoading = false; reduced = false; width = 800; animations = []; resize = [];
  vi.stubGlobal('window', { setTimeout, clearTimeout, matchMedia: () => ({ matches: reduced, addEventListener: vi.fn(), removeEventListener: vi.fn() }) });
  vi.stubGlobal('document', { activeElement: { isConnected: true, focus: vi.fn() } });
  vi.stubGlobal('ResizeObserver', class { constructor(callback: () => void) { resize.push(callback); } observe() {} disconnect = disconnect; });
  vi.stubGlobal('fetch', fetchMock);
  vi.stubGlobal('localStorage', memoryOpeningStorage());
  postResponse = async () => Response.json(openingResponse());
  fetchMock.mockImplementation(async (url: string, options: RequestInit) => {
    if (url === '/api/auth/session') return Response.json({ accessToken: 'token', tokenType: 'Bearer', expiresIn: 3600 });
    if (url === '/api/users/me') return Response.json(openingUser);
    if (url === '/api/openings') return postResponse();
    if (url === '/api/openings/batch') return Response.json(openingBatchResponse(JSON.parse(options.body as string).quantity));
    throw new Error(`Endpoint inesperado: ${url}`);
  });
  vi.mocked(prepareOpeningImages).mockResolvedValue(new Set());
  rail = { style: { transform: '' }, children: Array.from({ length: 52 }, (_, index) => ({ getBoundingClientRect: () => ({ left: index * 172, width: 160 }) })),
    getBoundingClientRect: () => ({ left: 0 }), animate: vi.fn((frames: Keyframe[]) => {
      let resolve!: () => void; let reject!: (error: Error) => void;
      const finished = new Promise<void>((done, fail) => { resolve = done; reject = fail; });
      const cancel = vi.fn(() => reject(new Error('Cancelled')));
      animations.push({ finish: resolve, cancel, frames });
      return { finished, cancel };
    }) };
});
afterEach(async () => {
  await act(async () => renderer?.unmount()); renderer = undefined;
  vi.resetAllMocks(); vi.restoreAllMocks(); vi.unstubAllGlobals();
});

describe('fluxo completo da abertura com créditos fictícios', () => {
  it('não sorteia ao abrir a página e exige login para experimentar', async () => {
    auth.user = null; await mount();
    expect(fetchMock).not.toHaveBeenCalled();
    expect(text()).toContain('Entrar para abrir');
    expect(text()).toContain('créditos fictícios');
  });
  it('faz um único POST apesar de cliques duplicados e revela exatamente a skin retornada', async () => {
    await mount();
    const start = button('Abrir caixa').props.onClick;
    await act(async () => { start(); start(); await flush(); });
    expect(postCount()).toBe(1);
    expect(rail.animate).toHaveBeenCalledTimes(1);
    expect(renderer!.root.findAllByType('dialog')).toHaveLength(0);
    await act(async () => animations[0].finish());
    expect(renderer!.root.findByProps({ 'aria-label': 'Skin recebida' }).findByType('h3').children).toEqual([openingResponse().item.name]);
    expect(rail.style.transform).toBe('translateX(-7420px)');
    expect(showModal).not.toHaveBeenCalled();
    expect(text()).toContain('Preço de referência da variante');
    expect(text()).toContain('10,00'); expect(text()).not.toContain('12,50');
    expect(text()).not.toContain('100%');
    expect(fetchMock.mock.calls.map(call => call[0])).toEqual(['/api/auth/session', '/api/users/me', '/api/openings']);
  });
  it('centraliza o especial concreto no trilho e revela o mesmo nome do backend', async () => {
    postResponse = async () => Response.json(openingResponse(true));
    await mount(); await click('Abrir caixa');
    const winner = renderer!.root.findByProps({ 'data-opening-winner': 'true' });
    expect(winner.props['data-opening-item']).toBe(openingSpecial.id);
    expect(winner.findByType('strong').children).toEqual([openingResponse(true).item.name]);
    await act(async () => animations[0].finish());
    expect(renderer!.root.findByProps({ 'aria-label': 'Skin recebida' }).findByType('h3').children).toEqual([openingResponse(true).item.name]);
    expect(postCount()).toBe(1);
  });
  it('pula a animação e ignora seu término posterior sem nova requisição', async () => {
    await mount(); await click('Abrir caixa'); await click('Pular animação');
    await act(async () => animations[0].finish());
    expect(postCount()).toBe(1); expect(showModal).not.toHaveBeenCalled();
    expect(renderer!.root.findByProps({ 'aria-label': 'Skin recebida' }).findByType('h3').children).toEqual([openingResponse().item.name]);
    expect(animations[0].cancel).toHaveBeenCalled();
  });
  it('pular durante o preparo revela o mesmo resultado e não inicia animação atrasada', async () => {
    let resolve!: (value: Set<string>) => void;
    vi.mocked(prepareOpeningImages).mockReturnValueOnce(new Promise(done => { resolve = done; }));
    await mount(); await click('Abrir caixa');
    expect(text()).toContain('Preparando imagens');
    await click('Pular animação');
    await act(async () => resolve(new Set([openingSkin.image])));
    expect(rail.animate).not.toHaveBeenCalled(); expect(postCount()).toBe(1);
    const dialog = renderer!.root.findByProps({ 'aria-label': 'Skin recebida' });
    expect(dialog.findByType('h3').children).toEqual([openingResponse().item.name]);
    expect(dialog.findByType('img').props.src).toBe('/images/catalog-item.svg');
  });
  it('revela diretamente com movimento reduzido e identifica especiais sem inventar fase ou preço', async () => {
    reduced = true; postResponse = async () => Response.json(openingResponse(true));
    await mount(); await click('Abrir caixa');
    expect(rail.animate).not.toHaveBeenCalled();
    const dialog = renderer!.root.findByProps({ 'aria-label': 'Skin recebida' });
    expect(dialog.findByType('h3').children).toEqual([openingResponse(true).item.name]);
    expect(text()).toContain('Item especial'); expect(text()).toContain('Indisponível');
    expect(text()).toContain('Testada em campo'); expect(text()).toContain('Sem StatTrak');
    expect(text()).not.toContain('Phase 2'); expect(postCount()).toBe(1);
  });
  it('usa revelação direta se o navegador não suporta a API de animação', async () => {
    Object.assign(rail, { animate: undefined });
    await mount(); await click('Abrir caixa');
    expect(renderer!.root.findAllByProps({ 'aria-label': 'Skin recebida' })).toHaveLength(1); expect(postCount()).toBe(1);
  });
  it('trata UNAVAILABLE sem animação nem modal de uma skin falsa', async () => {
    postResponse = async () => Response.json({ message: 'Esta caixa não tem dados suficientes para abertura.' }, { status: 409 });
    await mount(); await click('Abrir caixa');
    expect(text()).toContain('não tem dados suficientes');
    expect(renderer!.root.findAllByType('dialog')).toHaveLength(0); expect(rail.animate).not.toHaveBeenCalled();
  });
  it.each([401, 403, 500])('exibe falha HTTP %s sem inventar resultado', async status => {
    postResponse = async () => new Response(null, { status });
    await mount(); await click('Abrir caixa');
    expect(renderer!.root.findAllByProps({ role: 'alert' })).toHaveLength(1);
    expect(renderer!.root.findAllByType('dialog')).toHaveLength(0); expect(rail.animate).not.toHaveBeenCalled();
  });
  it('descarta requisição antiga ao trocar de caixa', async () => {
    let resolve!: (value: Response) => void;
    postResponse = () => new Promise(done => { resolve = done; });
    await mount(); await click('Abrir caixa');
    expect(text()).toContain('Confirmando abertura');
    await act(async () => renderer!.update(tree({ ...openingBox, id: 'crate-other' })));
    await act(async () => { resolve(Response.json(openingResponse())); await flush(); });
    expect(renderer!.root.findAllByType('dialog')).toHaveLength(0); expect(rail.animate).not.toHaveBeenCalled();
  });
  it('descarta resultado quando o usuário sai durante a requisição', async () => {
    let resolve!: (value: Response) => void;
    postResponse = () => new Promise(done => { resolve = done; });
    await mount(); await click('Abrir caixa');
    auth.user = null;
    await act(async () => renderer!.update(tree()));
    await act(async () => { resolve(Response.json(openingResponse())); await flush(); });
    expect(renderer!.root.findAllByType('dialog')).toHaveLength(0); expect(text()).toContain('Entrar para abrir');
  });
  it('finaliza mantendo o vencedor caso o viewport mude durante a animação', async () => {
    await mount(); await click('Abrir caixa'); width = 390;
    await act(async () => resize[0]());
    expect(rail.style.transform).toBe('translateX(-7625px)');
    expect(renderer!.root.findByProps({ 'aria-label': 'Skin recebida' }).findByType('h3').children).toEqual([openingResponse().item.name]);
    expect(postCount()).toBe(1);
  });
  it('cancela animação e observador ao desmontar, sem revelação tardia', async () => {
    await mount(); await click('Abrir caixa');
    await act(async () => renderer!.unmount()); renderer = undefined;
    await act(async () => animations[0].finish());
    expect(animations[0].cancel).toHaveBeenCalled(); expect(disconnect).toHaveBeenCalled(); expect(showModal).not.toHaveBeenCalled();
  });
  it('repete somente por ação explícita e não abre popup', async () => {
    reduced = true; await mount(); await click('Abrir caixa');
    expect(postCount()).toBe(1); expect(renderer!.root.findAllByType('dialog')).toHaveLength(0);
    await click('Abrir caixa'); expect(postCount()).toBe(2);
  });
  it('bloqueia abertura sem saldo ou cotação vigente', async () => {
    auth.user = { id: 1, balance: 0 }; await mount();
    expect(button('Abrir caixa').props.disabled).toBe(true); expect(text()).toContain('Saldo fictício insuficiente');
    expect(postCount()).toBe(0);
    auth.user = { id: 1, balance: 500 };
    await act(async () => renderer!.update(tree({ ...openingBox, marketPrice: undefined })));
    expect(button('Abrir caixa').props.disabled).toBe(true); expect(text()).toContain('sem cotação vigente');
  });
  it.each([1, 2, 5, 10])('selecionar %s caixas mostra uma roleta por unidade e revela todos os resultados', async quantity => {
    reduced = true; await mount();
    await act(async () => renderer!.root.findAllByType('button').find(node => node.children.join('') === String(quantity))!.props.onClick());
    expect(renderer!.root.findAll(node => typeof node.props.className === 'string' && node.props.className.startsWith('opening-viewport'))).toHaveLength(quantity);
    await click(quantity === 1 ? 'Abrir caixa' : `Abrir ${quantity} caixas`);
    expect(postCount()).toBe(1);
    const results = renderer!.root.findAllByProps({ 'aria-label': 'Skin recebida' });
    expect(results).toHaveLength(quantity);
    const returned = quantity === 1 ? [openingResponse()] : openingBatchResponse(quantity).results;
    expect(results.map(region => region.findByType('h3').children.join(''))).toEqual(returned.map(r => r.item.name));
    expect(renderer!.root.findAllByProps({ 'data-opening-winner': 'true' }).map(card => card.props['data-opening-item'])).toEqual(returned.map(r => r.item.externalId));
    expect(renderer!.root.findAllByType('dialog')).toHaveLength(0);
  });
  it('aguarda ambas as roletas e pular não solicita outro lote', async () => {
    await mount(); await act(async () => button('2').props.onClick()); await click('Abrir 2 caixas');
    expect(animations).toHaveLength(2);
    await act(async () => animations[0].finish());
    expect(renderer!.root.findAllByProps({ 'aria-label': 'Skin recebida' })).toHaveLength(0);
    await click('Pular animação');
    expect(renderer!.root.findAllByProps({ 'aria-label': 'Skin recebida' })).toHaveLength(2);
    expect(postCount()).toBe(1);
  });
  it('exibe custo total e bloqueia quando o saldo não cobre o lote', async () => {
    auth.user = { id: 1, balance: 5 }; await mount(); await act(async () => button('2').props.onClick());
    expect(button('Abrir 2 caixas').props.disabled).toBe(true); expect(text()).toContain('5,42'); expect(postCount()).toBe(0);
  });

  it('permite o card dourado ilustrativo antes de abrir e usa layout de dez roletas', async () => {
    vi.spyOn(Math, 'random').mockReturnValue(0);
    await mount();
    expect(renderer!.root.findAllByProps({ 'data-opening-item': 'csbet:visual-special-group' }).length).toBeGreaterThan(0);
    expect(fetchMock).not.toHaveBeenCalled();
    await act(async () => button('10').props.onClick());
    expect(renderer!.root.findAllByProps({ className: 'opening-roulettes is-multiple is-ten' })).toHaveLength(1);
    expect(renderer!.root.findAllByProps({ className: 'opening-lane' })).toHaveLength(10);
  });

  it('cada roleta recebe cards aleatórios próprios preservando os vencedores do backend', async () => {
    let seed = 42;
    vi.spyOn(Math, 'random').mockImplementation(() => { seed = (Math.imul(seed, 1664525) + 1013904223) >>> 0; return seed / 4294967296; });
    const box = { ...openingBox, items: [openingSkin, ...['second', 'third', 'fourth'].map(id => ({ ...openingSkin, id }))] };
    reduced = true; await mount(box); await act(async () => button('2').props.onClick()); await click('Abrir 2 caixas');
    const lanes = renderer!.root.findAllByProps({ className: 'opening-lane' });
    const cards = lanes.map(lane => lane.findAll(node => typeof node.props['data-opening-item'] === 'string'));
    expect(cards[0].filter(card => !card.props['data-opening-winner']).map(card => card.props['data-opening-item']))
      .not.toEqual(cards[1].filter(card => !card.props['data-opening-winner']).map(card => card.props['data-opening-item']));
    expect(cards.map(lane => lane.find(card => card.props['data-opening-winner'])!.props['data-opening-item']))
      .toEqual(openingBatchResponse(2).results.map(result => result.item.externalId));
    expect(postCount()).toBe(1);
  });
  it('atualizar o saldo não reembaralha o trilho em repouso', async () => {
    let seed = 7;
    vi.spyOn(Math, 'random').mockImplementation(() => { seed = (Math.imul(seed, 1664525) + 1013904223) >>> 0; return seed / 4294967296; });
    const box = { ...openingBox, items: [openingSkin, ...['second', 'third', 'fourth'].map(id => ({ ...openingSkin, id }))] };
    await mount(box);
    const ids = () => renderer!.root.findAll(node => typeof node.props['data-opening-item'] === 'string').map(card => card.props['data-opening-item']);
    const before = ids(); auth.user = { id: 1, balance: 600 };
    await act(async () => renderer!.update(tree(box)));
    expect(ids()).toEqual(before); expect(postCount()).toBe(0);
  });

});
