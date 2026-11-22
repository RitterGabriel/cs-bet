import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { act, create } from 'react-test-renderer';
import type { ReactTestRenderer } from 'react-test-renderer';
import { MemoryRouter } from 'react-router';
import { AuthProvider, useAuth } from '../contexts/AuthContext';
import { App } from '../App';
import { api, ApiError } from '../services/api';
import { walletApi, validCreditAmount } from '../services/wallet';

vi.mock('../services/api', async importOriginal => {
  const original = await importOriginal<typeof import('../services/api')>();
  return { ...original, api: { login: vi.fn(), me: vi.fn(), restoreSession: vi.fn(), logout: vi.fn() } };
});
vi.mock('../services/wallet', async importOriginal => {
  const original = await importOriginal<typeof import('../services/wallet')>();
  return { ...original, walletApi: { balance: vi.fn(), history: vi.fn(), add: vi.fn() } };
});
let auth: ReturnType<typeof useAuth>;
function Probe() { auth = useAuth(); return null; }
let renderer: ReactTestRenderer | undefined;
const empty = { content: [], page: 0, totalPages: 0, totalElements: 0 };
const user = { id: 1, username: 'owner', email: 'owner@example.com', balance: 50, createdAt: '2026-11-22T12:00:00Z' };
const text = () => JSON.stringify(renderer!.toJSON());
async function openWallet() {
  await act(async () => { renderer = create(<MemoryRouter initialEntries={['/wallet']}><AuthProvider><Probe /><App /></AuthProvider></MemoryRouter>); });
  await act(async () => { await auth.login({ email: user.email, password: 'test-password' }); });
}
async function submit(value: string) {
  await act(async () => { renderer!.root.findByProps({ id: 'credits-amount' }).props.onChange({ target: { value } }); });
  await act(async () => { await renderer!.root.findByType('form').props.onSubmit({ preventDefault() {} }); });
}
beforeEach(() => {
  vi.stubGlobal('IS_REACT_ACT_ENVIRONMENT', true);
  vi.stubGlobal('window', { setTimeout, clearTimeout, scrollTo: vi.fn(), addEventListener: vi.fn(), removeEventListener: vi.fn() });
  vi.stubGlobal('document', { addEventListener: vi.fn(), removeEventListener: vi.fn() });
  vi.mocked(api.login).mockResolvedValue({ accessToken: 'token', tokenType: 'Bearer', expiresIn: 3600 });
  vi.mocked(api.me).mockResolvedValue(user);
  vi.mocked(api.restoreSession).mockRejectedValue(new ApiError(401, 'Sem sessão'));
  vi.mocked(api.logout).mockResolvedValue({ message: 'Sessão encerrada.' });
  vi.mocked(walletApi.balance).mockResolvedValue({ balance: '50.00' });
  vi.mocked(walletApi.history).mockResolvedValue(empty);
  vi.mocked(walletApi.add).mockResolvedValue({ balance: '75.50' });
});
afterEach(async () => {
  await act(async () => { renderer?.unmount(); });
  renderer = undefined;
  vi.unstubAllGlobals(); vi.clearAllMocks();
});
describe('carteira virtual', () => {
  it('valida valores positivos com até duas casas e limite por operação', () => {
    for (const value of ['0', '-1', '10000.01', '1.001', '', 'abc', '1e3']) expect(validCreditAmount(value)).toBe(false);
    for (const value of ['0.01', '10000.00', '25,50', '100']) expect(validCreditAmount(value)).toBe(true);
  });
  it('protege a rota de visitantes', async () => {
    await act(async () => { renderer = create(<MemoryRouter initialEntries={['/wallet']}><AuthProvider><App /></AuthProvider></MemoryRouter>); });
    expect(text()).not.toContain('credits-amount');
    expect(walletApi.balance).not.toHaveBeenCalled();
  });
  it('adiciona créditos e atualiza saldo do cabeçalho e histórico', async () => {
    await openWallet();
    expect(text()).toContain('Não representam dinheiro real');
    vi.mocked(walletApi.balance).mockResolvedValue({ balance: '75.50' });
    vi.mocked(walletApi.history).mockResolvedValue({ ...empty, totalPages: 1, totalElements: 1, content: [{ id: 1, type: 'CREDIT_ADDITION', amount: '25.50', resultingBalance: '75.50', createdAt: '2026-11-22T12:00:00Z' }] });
    await submit('25,50');
    expect(walletApi.add).toHaveBeenCalledWith('token', '25.50', expect.any(String));
    expect(auth.user?.balance).toBe(75.5);
    const header = renderer!.root.findByType('header');
    expect(JSON.stringify(header.findAllByType('strong').map(item => item.children))).toContain('75,50');
    expect(text()).toContain('Créditos fictícios adicionados.');
    expect(text()).toContain('Adição de créditos');
    expect(walletApi.history).toHaveBeenLastCalledWith('token', 0);
  });
  it('seleciona valores rápidos sem adicionar antes da confirmação', async () => {
    await openWallet();
    for (const value of [10, 20, 50, 100]) {
      await act(async () => {
        renderer!.root.findByProps({ 'aria-label': `Selecionar R$ ${value} fictícios` }).props.onClick();
      });
      expect(renderer!.root.findByProps({ id: 'credits-amount' }).props.value).toBe(String(value));
      expect(renderer!.root.findByProps({ 'aria-label': `Selecionar R$ ${value} fictícios` }).props['aria-pressed']).toBe(true);
    }
    expect(walletApi.add).not.toHaveBeenCalled();
    expect(text()).toContain('o valor que você colocaria em um site de verdade');
    await act(async () => {
      await renderer!.root.findByType('form').props.onSubmit({ preventDefault() {} });
    });
    expect(walletApi.add).toHaveBeenCalledWith('token', '100', expect.any(String));
  });
  it('exibe saldo e histórico com escala variável sem mensagem de preço indisponível', async () => {
    vi.mocked(walletApi.balance).mockResolvedValue({ balance: '999' });
    vi.mocked(walletApi.history).mockResolvedValue({
      ...empty, totalPages: 1, totalElements: 1,
      content: [{ id: 1, type: 'CREDIT_ADDITION', amount: '10', resultingBalance: '999', createdAt: '2026-11-22T12:00:00Z' }],
    });
    await openWallet();
    expect(text()).toContain('R$\u00a0999,00');
    expect(text()).toContain('R$\u00a010,00');
    expect(text()).not.toContain('Preço indisponível');
  });
  it('não envia valor inválido e preserva identificador após falha de rede', async () => {
    await openWallet();
    await submit('10000.01');
    expect(walletApi.add).not.toHaveBeenCalled();
    vi.mocked(walletApi.add).mockRejectedValueOnce(new ApiError(0, 'Falha de rede'));
    await submit('10');
    const id = vi.mocked(walletApi.add).mock.calls[0][2];
    expect(text()).toContain('Não foi possível confirmar a adição');
    await submit('10');
    expect(vi.mocked(walletApi.add).mock.calls[1][2]).toBe(id);
  });
  it('mostra carregamento enquanto aguarda o histórico', async () => {
    vi.mocked(walletApi.history).mockReturnValueOnce(new Promise(() => {}));
    await openWallet();
    expect(text()).toContain('Carregando histórico...');
    expect(renderer!.root.findAllByType('button').find(button => button.children.includes('Adicionar créditos fictícios'))?.props.disabled).toBe(true);
  });
  it('mostra erro da consulta', async () => {
    vi.mocked(walletApi.history).mockRejectedValue(new ApiError(0, 'Falha de rede'));
    await openWallet();
    expect(text()).toContain('Não foi possível carregar a carteira');
    expect(text()).toContain('Tentar novamente');
  });
});
