import { act, create } from 'react-test-renderer';
import type { ReactTestRenderer } from 'react-test-renderer';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { MemoryRouter, Route, Routes } from 'react-router';
import { AuthProvider, useAuth } from './AuthContext';
import { ProtectedRoute } from '../components/ProtectedRoute';
import { fetchInventory } from '../services/inventory';
import { api, ApiError } from '../services/api';

vi.mock('../services/api', async original => ({
  ...await original<typeof import('../services/api')>(),
  api: { login: vi.fn(), me: vi.fn(), restoreSession: vi.fn(), logout: vi.fn() },
}));
vi.mock('../services/inventory', () => ({ fetchInventory: vi.fn() }));
const inventory = { content: [], page: 0, size: 12, totalElements: 0, totalPages: 0, estimatedAvailableTotal: '123.45', availableItems: 0, unpricedAvailableItems: 0, valuationCurrency: 'BRL' as const };
const user = { id: 1, username: 'player', email: 'player@example.com', balance: 50, createdAt: '2026-11-22T12:00:00Z' };
const session = { accessToken: 'test-token', tokenType: 'Bearer', expiresIn: 3600 };
let auth: ReturnType<typeof useAuth>;
let renderer: ReactTestRenderer | undefined;
function Probe() { auth = useAuth(); return null; }
async function mount() {
  await act(async () => {
    renderer = create(<MemoryRouter initialEntries={['/profile']}><AuthProvider><Probe />
      <Routes>
        <Route element={<ProtectedRoute />}><Route path="profile" element={<p>Perfil privado</p>} /></Route>
        <Route path="login" element={<p>Login</p>} />
      </Routes>
    </AuthProvider></MemoryRouter>);
  });
}
beforeEach(() => {
  vi.mocked(fetchInventory).mockResolvedValue(inventory);
  vi.useFakeTimers();
  vi.stubGlobal('IS_REACT_ACT_ENVIRONMENT', true);
  vi.stubGlobal('window', { setTimeout, clearTimeout, addEventListener: vi.fn(), removeEventListener: vi.fn() });
  vi.stubGlobal('document', { addEventListener: vi.fn(), removeEventListener: vi.fn() });
  vi.mocked(api.login).mockResolvedValue(session);
  vi.mocked(api.restoreSession).mockResolvedValue(session);
  vi.mocked(api.me).mockResolvedValue(user);
  vi.mocked(api.logout).mockResolvedValue({ message: 'Sessão encerrada.' });
});
afterEach(async () => {
  await act(async () => renderer?.unmount());
  renderer = undefined;
  vi.resetAllMocks(); vi.unstubAllGlobals(); vi.useRealTimers();
});
describe('persistência da sessão', () => {
  it('restaura usuário consultado no backend após remontar o aplicativo', async () => {
    vi.mocked(api.restoreSession).mockRejectedValueOnce(new ApiError(401, 'Sem sessão'));
    await mount();
    await act(async () => auth.login({ email: user.email, password: 'password' }));
    await act(async () => renderer!.unmount());
    await mount();
    expect(auth.user).toEqual(user);
    expect(api.restoreSession).toHaveBeenCalledTimes(2);
    expect(api.me).toHaveBeenLastCalledWith('test-token');
    expect(JSON.stringify(renderer!.toJSON())).toContain('Perfil privado');
  });
  it('aguarda a restauração antes de redirecionar uma rota privada', async () => {
    let resolve!: (value: typeof session) => void;
    vi.mocked(api.restoreSession).mockReturnValueOnce(new Promise(done => { resolve = done; }));
    await mount();
    expect(auth.isLoading).toBe(true);
    expect(JSON.stringify(renderer!.toJSON())).toContain('Verificando sessão');
    expect(JSON.stringify(renderer!.toJSON())).not.toContain('Login');
    await act(async () => resolve(session));
    expect(JSON.stringify(renderer!.toJSON())).toContain('Perfil privado');
  });
  it('redireciona para login quando o cookie está ausente ou expirado', async () => {
    vi.mocked(api.restoreSession).mockRejectedValueOnce(new ApiError(401, 'Sessão expirada'));
    await mount();
    expect(auth.user).toBeNull();
    expect(auth.isLoading).toBe(false);
    expect(api.me).not.toHaveBeenCalled();
    expect(JSON.stringify(renderer!.toJSON())).toContain('Login');
  });
  it('sair limpa a sessão no servidor e ela não reaparece ao recarregar', async () => {
    await mount();
    await act(async () => auth.logout());
    expect(api.logout).toHaveBeenCalledTimes(1);
    expect(auth.user).toBeNull();
    await act(async () => renderer!.unmount());
    vi.mocked(api.restoreSession).mockRejectedValueOnce(new ApiError(401, 'Sem sessão'));
    await mount();
    expect(auth.user).toBeNull();
  });
  it('não restaura uma resposta atrasada depois de sair', async () => {
    let resolve!: (value: typeof session) => void;
    vi.mocked(api.restoreSession).mockReturnValueOnce(new Promise(done => { resolve = done; }));
    await mount();
    await act(async () => auth.logout());
    await act(async () => resolve(session));
    expect(auth.user).toBeNull();
  });
  it('expira a sessão restaurada sem ampliar seu tempo restante', async () => {
    vi.mocked(api.restoreSession).mockResolvedValueOnce({ ...session, expiresIn: 1 });
    await mount();
    expect(auth.user).toEqual(user);
    await act(async () => { vi.advanceTimersByTime(1001); });
    expect(auth.user).toBeNull();
    expect(auth.notice).toContain('Sua sessão expirou');
  });
  it('mantém a sessão e informa quando não é possível concluir o logout', async () => {
    await mount();
    vi.mocked(api.logout).mockRejectedValueOnce(new ApiError(0, 'offline'));
    await act(async () => auth.logout());
    expect(auth.user).toEqual(user);
    expect(auth.notice).toContain('Tente sair novamente');
  });
});

describe('valor do inventário no cabeçalho', () => {
  it('carrega ao restaurar a sessão sem acessar o inventário', async () => {
    await mount();
    expect(fetchInventory).toHaveBeenCalledWith(session.accessToken, 0);
    expect(auth.inventoryEstimate).toBe('123.45');
    expect(auth.inventoryLoading).toBe(false);
    await act(async () => auth.refreshUser());
    expect(fetchInventory).toHaveBeenCalledTimes(1);
  });
  it('carrega após login e não consulta para visitantes', async () => {
    vi.mocked(api.restoreSession).mockRejectedValueOnce(new ApiError(401, 'Sem sessão'));
    await mount();
    expect(fetchInventory).not.toHaveBeenCalled();
    await act(async () => auth.login({ email: user.email, password: 'password' }));
    expect(auth.inventoryEstimate).toBe('123.45');
  });
  it('não bloqueia a sessão enquanto carrega e ignora respostas após logout', async () => {
    let resolve!: (value: typeof inventory) => void;
    vi.mocked(fetchInventory).mockReturnValueOnce(new Promise(done => { resolve = done; }));
    await mount();
    expect(auth.user).toEqual(user);
    expect(auth.isLoading).toBe(false);
    expect(auth.inventoryLoading).toBe(true);
    await act(async () => auth.logout());
    await act(async () => resolve(inventory));
    expect(auth.inventoryEstimate).toBeUndefined();
  });
  it('preserva a atualização recente quando a consulta inicial chega atrasada', async () => {
    let resolve!: (value: typeof inventory) => void;
    vi.mocked(fetchInventory).mockReturnValueOnce(new Promise(done => { resolve = done; }));
    await mount();
    vi.mocked(fetchInventory).mockResolvedValueOnce({ ...inventory, estimatedAvailableTotal: '200.00' });
    await act(async () => { await auth.getInventory(0); });
    await act(async () => resolve(inventory));
    expect(auth.inventoryEstimate).toBe('200.00');
  });
  it('mostra falha sem inventar valor nem encerrar a sessão', async () => {
    vi.mocked(fetchInventory).mockRejectedValueOnce(new ApiError(0, 'offline'));
    await mount();
    expect(auth.inventoryError).toBe(true);
    expect(auth.inventoryLoading).toBe(false);
    expect(auth.inventoryEstimate).toBeUndefined();
    expect(auth.user).toEqual(user);
  });
  it.each(['0.00', null])('preserva o total retornado pelo backend: %s', async total => {
    vi.mocked(fetchInventory).mockResolvedValueOnce({ ...inventory, estimatedAvailableTotal: total });
    await mount();
    expect(auth.inventoryEstimate).toBe(total);
    expect(auth.inventoryError).toBe(false);
  });
});
