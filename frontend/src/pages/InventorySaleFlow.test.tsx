import { act, create } from 'react-test-renderer';
import type { ReactTestRenderer } from 'react-test-renderer';
import { MemoryRouter } from 'react-router';
import { beforeEach, afterEach, describe, it, expect, vi } from 'vitest';
import { InventoryPage } from './InventoryPage';
import { sellInventoryItem, sellAllInventory } from '../services/inventory';
import { openingSkin } from '../test/openingFixture';
import { ApiError } from '../services/api';
const auth = vi.hoisted(() => ({ user: { id: 1 }, getInventory: vi.fn(), refreshUser: vi.fn() }));
vi.mock('../contexts/AuthContext', () => ({ useAuth: () => auth }));
vi.mock('../services/inventory', () => ({ sellInventoryItem: vi.fn(), sellAllInventory: vi.fn() }));
let renderer: ReactTestRenderer | undefined;
const item = { id: 22, skinId: 1, externalId: openingSkin.id, name: openingSkin.name, imageUrl: null,
  rarity: openingSkin.rarityData!, obtainedAt: '2026-11-22T12:00:00Z', status: 'AVAILABLE', marketPrice: openingSkin.marketPrices![0] };
const page = { content: [item], page: 0, size: 12, totalElements: 1, totalPages: 1,
  estimatedAvailableTotal: '10.00', availableItems: 1, unpricedAvailableItems: 0, valuationCurrency: 'BRL' };
const text = () => JSON.stringify(renderer!.toJSON());
const flush = () => new Promise(resolve => setTimeout(resolve, 0));
const sellButton = () => renderer!.root.findAllByType('button').find(button => button.children.join('').includes('Vender por'))!;
beforeEach(() => {
  vi.stubGlobal('IS_REACT_ACT_ENVIRONMENT',true);
  auth.getInventory.mockResolvedValue(page); auth.refreshUser.mockResolvedValue(undefined);
});
afterEach(async () => { await act(async () => renderer?.unmount()); renderer = undefined; vi.resetAllMocks(); vi.unstubAllGlobals(); });
async function mount() { await act(async () => { renderer = create(<MemoryRouter><InventoryPage /></MemoryRouter>); await flush(); }); }
describe('venda pelo inventário', () => {
  it('um clique duplo vende uma vez, atualiza o inventário e solicita atualização do saldo', async () => {
    let resolve!: (value: { saleId: number; inventoryId: number; amount: string; balance: string }) => void;
    vi.mocked(sellInventoryItem).mockReturnValue(new Promise(done => { resolve = done; }));
    await mount(); const click = sellButton().props.onClick;
    await act(async () => { click(); click(); });
    expect(sellInventoryItem).toHaveBeenCalledTimes(1); expect(text()).toContain('Vendendo...');
    auth.getInventory.mockResolvedValue({ ...page, content: [{ ...item, status: 'SOLD', salePrice: '10.00' }], availableItems: 0, estimatedAvailableTotal: '0.00' });
    await act(async () => { resolve({ saleId: 7, inventoryId: 22, amount: '10.00', balance: '510.00' }); await flush(); });
    expect(auth.refreshUser).toHaveBeenCalledTimes(1); expect(auth.getInventory).toHaveBeenCalledTimes(2);
    expect(text()).toContain('Skin vendida por'); expect(text()).toContain('Vendido por'); expect(text()).not.toContain('Vender por');
  });
  it('recusa de cotação mantém o item e mostra mensagem do backend', async () => {
    vi.mocked(sellInventoryItem).mockRejectedValue(new ApiError(409,'O preço mudou. Atualize o inventário.','SALE_REJECTED'));
    await mount(); await act(async () => { sellButton().props.onClick(); await flush(); });
    expect(text()).toContain('O preço mudou'); expect(text()).toContain('Vender por'); expect(auth.refreshUser).not.toHaveBeenCalled();
  });
  it('não atualiza a sessão depois de desmontar com uma venda pendente', async () => {
    let resolve!: (value: { saleId: number; inventoryId: number; amount: string; balance: string }) => void;
    vi.mocked(sellInventoryItem).mockReturnValue(new Promise(done => { resolve = done; }));
    await mount(); await act(async () => sellButton().props.onClick());
    await act(async () => renderer!.unmount()); renderer = undefined;
    await act(async () => resolve({ saleId: 7, inventoryId: 22, amount: '10.00', balance: '510.00' }));
    expect(auth.refreshUser).not.toHaveBeenCalled();
  });
  it('Vender tudo usa uma única operação mesmo com clique duplo e atualiza saldo e inventário', async () => {
    let resolve!: (value: { batchId: number; soldItems: number; amount: string; balance: string; skippedItems: number }) => void;
    vi.mocked(sellAllInventory).mockReturnValue(new Promise(done => { resolve = done; }));
    await mount(); const click = renderer!.root.findAllByType('button').find(b => b.children.includes('Vender tudo'))!.props.onClick;
    await act(async () => { click(); click(); });
    expect(sellAllInventory).toHaveBeenCalledTimes(1); expect(text()).toContain('Vendendo tudo...');
    auth.getInventory.mockResolvedValue({ ...page, content: [], availableItems: 0, totalElements: 0, totalPages: 0, estimatedAvailableTotal: '0.00' });
    await act(async () => { resolve({ batchId: 4, soldItems: 2, amount: '20.00', balance: '520.00', skippedItems: 1 }); await flush(); });
    expect(auth.refreshUser).toHaveBeenCalledTimes(1); expect(text()).toContain('2 skins vendidas');
    expect(text()).toContain('1 item sem cotação'); expect(sellInventoryItem).not.toHaveBeenCalled();
  });

  it('oferece maior e menor valor e reinicia a paginação ao mudar a ordem', async () => {
    auth.getInventory.mockResolvedValue({ ...page, page: 0, totalElements: 25, totalPages: 3 });
    await mount();
    await act(async () => renderer!.root.findAllByType('button').find(b => b.children.includes('Próxima'))!.props.onClick());
    expect(auth.getInventory).toHaveBeenLastCalledWith(1, undefined, 'RECENT');
    const sort = renderer!.root.findByProps({ id: 'inventory-sort' });
    expect(sort.findAllByType('option').map(o => o.children.join(''))).toEqual(['Mais recentes','Maior valor','Menor valor']);
    await act(async () => sort.props.onChange({ target: { value: 'PRICE_DESC' } }));
    expect(auth.getInventory).toHaveBeenLastCalledWith(0, undefined, 'PRICE_DESC');
    await act(async () => sort.props.onChange({ target: { value: 'PRICE_ASC' } }));
    expect(auth.getInventory).toHaveBeenLastCalledWith(0, undefined, 'PRICE_ASC');
  });

});
