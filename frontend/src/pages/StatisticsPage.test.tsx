import { act, create } from 'react-test-renderer';
import type { ReactTestRenderer } from 'react-test-renderer';
import { MemoryRouter } from 'react-router';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { StatisticsPage } from './StatisticsPage';
import { getPersonalStatistics } from '../services/statistics';
import { ApiError } from '../services/api';
import { statisticsFixture } from '../test/statisticsFixture';
const auth = vi.hoisted(() => ({ user: { id: 1 } as { id: number } | null, isLoading: false }));
vi.mock('../contexts/AuthContext', () => ({ useAuth: () => auth }));
vi.mock('../services/statistics', async original => ({ ...await original<typeof import('../services/statistics')>(), getPersonalStatistics: vi.fn() }));
let renderer: ReactTestRenderer | undefined;
const text = () => JSON.stringify(renderer!.toJSON());
async function mount() {
  await act(async () => { renderer = create(<MemoryRouter><StatisticsPage /></MemoryRouter>); });
}
beforeEach(() => {
  vi.stubGlobal('IS_REACT_ACT_ENVIRONMENT', true);
  auth.user = { id: 1 }; auth.isLoading = false;
  vi.mocked(getPersonalStatistics).mockResolvedValue(statisticsFixture);
});
afterEach(async () => {
  await act(async () => renderer?.unmount());
  renderer = undefined; vi.resetAllMocks(); vi.unstubAllGlobals();
});
describe('painel pessoal de estatísticas', () => {
  it('mostra saldo, inventário e histórico sem controles de simulação ou abertura', async () => {
    await mount();
    expect(text()).toContain('Minhas ');
    expect(text()).toContain('900,00'); expect(text()).toContain('500,00'); expect(text()).toContain('40,00');
    expect(text()).toContain('Histórico da conta'); expect(text()).toContain('Fixture skin');
    expect(text()).toContain('Venda fictícia'); expect(text()).toContain('Item recebido');
    expect(text()).toContain('Não disponível'); expect(text()).toContain('não são contados como gastos ou aberturas');
    expect(renderer!.root.findAllByType('form')).toHaveLength(0);
    expect(text()).not.toContain('Executar simulação'); expect(text()).not.toContain('Quantidade de amostras');
    expect(getPersonalStatistics).toHaveBeenCalledWith(1, 0, 'ALL');
  });
  it('protege os dados de visitantes e preserva o destino após login', async () => {
    auth.user = null; await mount();
    expect(text()).toContain('Acessar minha conta');
    expect(getPersonalStatistics).not.toHaveBeenCalled();
    expect(text()).not.toContain('900,00');
  });
  it('exibe loading sem apresentar números antigos ou inventados', async () => {
    vi.mocked(getPersonalStatistics).mockReturnValueOnce(new Promise(() => {}));
    await mount();
    expect(text()).toContain('Carregando suas estatísticas');
    expect(text()).not.toContain('Saldo virtual atual');
  });
  it.each([0, 401, 500])('mostra falha %s e permite tentar novamente', async status => {
    vi.mocked(getPersonalStatistics).mockRejectedValueOnce(new ApiError(status, 'failure'));
    await mount();
    expect(renderer!.root.findAllByProps({ role: 'alert' })).toHaveLength(1);
    expect(text()).toContain('Tentar novamente'); expect(text()).not.toContain('900,00');
  });
  it('filtra movimentações e pagina o histórico', async () => {
    vi.mocked(getPersonalStatistics).mockResolvedValue({ ...statisticsFixture, activity: { ...statisticsFixture.activity, totalElements: 42, totalPages: 3 } });
    await mount();
    await act(async () => renderer!.root.findAllByType('button').find(button => button.children.includes('Próxima'))!.props.onClick());
    expect(getPersonalStatistics).toHaveBeenLastCalledWith(1, 1, 'ALL');
    await act(async () => renderer!.root.findByType('select').props.onChange({ target: { value: 'ITEM_SOLD' } }));
    expect(getPersonalStatistics).toHaveBeenLastCalledWith(1, 0, 'ITEM_SOLD');
  });
  it('mostra histórico vazio e valores ausentes sem supor preço zero para venda', async () => {
    vi.mocked(getPersonalStatistics).mockResolvedValueOnce({ ...statisticsFixture,
      summary: { ...statisticsFixture.summary, inventoryEstimatedValue: null, unpricedAvailableItems: 1, salesTotal: null, unvaluedSales: 1 },
      activity: { ...statisticsFixture.activity, content: [{ ...statisticsFixture.activity.content[0], amount: null, occurredAt: null }] } });
    await mount();
    expect(text()).toContain('1 sem cotação'); expect(text()).toContain('1 venda sem valor registrado');
    expect(text()).toContain('Valor não registrado'); expect(text()).toContain('Data não registrada');
  });
  it('mostra estado vazio para conta sem movimentações', async () => {
    vi.mocked(getPersonalStatistics).mockResolvedValueOnce({ ...statisticsFixture, activity: { page: 0, size: 20, content: [], totalPages: 0, totalElements: 0 } });
    await mount(); expect(text()).toContain('Nenhuma movimentação encontrada');
  });
  it('remove imediatamente os dados da conta anterior ao trocar de usuário', async () => {
    await mount();
    vi.mocked(getPersonalStatistics).mockReturnValueOnce(new Promise(() => {}));
    auth.user = { id: 2 };
    await act(async () => renderer!.update(<MemoryRouter><StatisticsPage /></MemoryRouter>));
    expect(text()).not.toContain('900,00');
    expect(getPersonalStatistics).toHaveBeenLastCalledWith(2, 0, 'ALL');
  });
});
