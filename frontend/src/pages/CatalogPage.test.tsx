import { act, create } from 'react-test-renderer';
import type { ReactTestRenderer } from 'react-test-renderer';
import { MemoryRouter } from 'react-router';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { CatalogPage } from './CatalogPage';
import { listOfficialCases, listCustomCases } from '../services/catalog';
import { cases } from '../data/cases';

vi.mock('../services/catalog', () => ({ listOfficialCases: vi.fn(), listCustomCases: vi.fn() }));
let renderer: ReactTestRenderer | undefined;
const official = cases.filter(item => item.category === 'official').map((item, index) => ({ ...item, id: `crate-${index}`, isReal: true }));
async function mount(category: 'official' | 'custom' = 'official') {
  await act(async () => { renderer = create(<MemoryRouter><CatalogPage category={category} home /></MemoryRouter>); });
  await act(async () => { vi.advanceTimersByTime(250); });
}
beforeEach(() => {
  vi.useFakeTimers();
  vi.stubGlobal('IS_REACT_ACT_ENVIRONMENT', true);
  vi.stubGlobal('window', { setTimeout, clearTimeout, setInterval, clearInterval });
  vi.mocked(listCustomCases).mockResolvedValue({ content: [{ ...cases.find(item => item.category === 'custom')!, id: 'custom-all-win', name: 'All Win', customPrice: '3.00', customReady: true }], page: 0, size: 12, totalElements: 14, totalPages: 2 });
  vi.mocked(listOfficialCases).mockImplementation(async (page, search) => ({ content: search ? [official[0]] : official,
    page, size: 12, totalElements: search ? 1 : 42, totalPages: search ? 1 : 4 }));
});
afterEach(async () => { await act(async () => renderer?.unmount()); renderer = undefined; vi.resetAllMocks(); vi.useRealTimers(); vi.unstubAllGlobals(); });

describe('catálogo redesenhado', () => {
  it('preserva busca e ordenação da API e reinicia a paginação ao pesquisar', async () => {
    await mount();
    await act(async () => renderer!.root.findByProps({ 'aria-label': 'Buscar caixas pelo nome' }).props.onChange({ target: { value: 'Revolution' } }));
    await act(async () => { vi.advanceTimersByTime(250); });
    expect(listOfficialCases).toHaveBeenLastCalledWith(0, 'Revolution', 'featured');
    await act(async () => renderer!.root.findByProps({ 'aria-label': 'Ordenar catálogo' }).props.onChange({ target: { value: 'price_asc' } }));
    await act(async () => { vi.advanceTimersByTime(250); });
    expect(listOfficialCases).toHaveBeenLastCalledWith(0, 'Revolution', 'price_asc');
  });
  it('preserva paginação e oferece os avisos de preços sem controles de abertura', async () => {
    await mount();
    expect(renderer!.root.findAllByType('details')).toHaveLength(1);
    expect(JSON.stringify(renderer!.toJSON())).toContain('Os preços são referências de mercado');
    await act(async () => renderer!.root.findAllByType('button').find(button => button.children.includes('Próxima'))!.props.onClick());
    await act(async () => { vi.advanceTimersByTime(250); });
    expect(listOfficialCases).toHaveBeenLastCalledWith(1, '', 'featured');
    expect(JSON.stringify(renderer!.toJSON())).not.toContain('Abrir caixa');
  });
  it('consulta personalizadas reais com preço fictício e preserva o histórico', async () => {
    await mount('custom');
    expect(listOfficialCases).not.toHaveBeenCalled();
    const html = JSON.stringify(renderer!.toJSON());
    expect(listCustomCases).toHaveBeenCalledWith(0, '', 'featured');
    expect(html).toContain('All Win'); expect(html).toContain('3,00');
    expect(html).toContain('Meu histórico');
    expect(html).toContain('/statistics');
  });
});
