import { act, create } from 'react-test-renderer';
import type { ReactTestRenderer } from 'react-test-renderer';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { MemoryRouter, Route, Routes } from 'react-router';
import { AuthProvider, useAuth } from './AuthContext';
import { ProtectedRoute } from '../components/ProtectedRoute';
import { api, ApiError } from '../services/api';

vi.mock('../services/api', async original => ({
  ...await original<typeof import('../services/api')>(),
  api: { login: vi.fn(), me: vi.fn(), restoreSession: vi.fn(), logout: vi.fn() },
}));
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
