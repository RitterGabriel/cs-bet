import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { api, ApiError } from './api';

const fetchMock = vi.fn();
const user = {
  id: 1,
  username: 'player',
  email: 'p@example.com',
  balance: 0,
  createdAt: '2026-11-21T10:00:00',
};
beforeEach(() => {
  vi.stubGlobal('fetch', fetchMock);
  vi.stubGlobal('window', {
    setTimeout: globalThis.setTimeout,
    clearTimeout: globalThis.clearTimeout,
  });
});
afterEach(() => {
  vi.unstubAllGlobals();
  vi.clearAllMocks();
});

describe('cliente HTTP', () => {
  it('envia login para o endpoint real e não usa cookies', async () => {
    fetchMock.mockResolvedValueOnce(
      Response.json({
        accessToken: 'test-token',
        tokenType: 'Bearer',
        expiresIn: 3600,
      }),
    );
    await expect(
      api.login({ email: 'p@example.com', password: 'password' }),
    ).resolves.toHaveProperty('expiresIn', 3600);
    expect(fetchMock).toHaveBeenCalledWith(
      '/api/auth/login',
      expect.objectContaining({
        method: 'POST',
        credentials: 'omit',
        cache: 'no-store',
        body: JSON.stringify({ email: 'p@example.com', password: 'password' }),
      }),
    );
  });
  it('envia token somente no cabeçalho Authorization', async () => {
    fetchMock.mockResolvedValueOnce(Response.json(user));
    await expect(api.me('test-token')).resolves.toEqual(user);
    expect(fetchMock).toHaveBeenCalledWith(
      '/api/users/me',
      expect.objectContaining({
        headers: expect.objectContaining({
          Authorization: 'Bearer test-token',
        }),
      }),
    );
  });
  it('não encaminha detalhes internos em 401 e 500', async () => {
    for (const status of [401, 500]) {
      fetchMock.mockResolvedValueOnce(
        Response.json({ message: 'internal passwordHash secret' }, { status }),
      );
      try {
        await api.login({ email: 'p@example.com', password: 'password' });
        throw new Error('Expected rejection');
      } catch (error) {
        expect(error).toBeInstanceOf(ApiError);
        expect((error as Error).message).not.toContain('secret');
      }
    }
  });
  it('preserva status 409 para conflito de cadastro', async () => {
    fetchMock.mockResolvedValueOnce(new Response(null, { status: 409 }));
    await expect(
      api.register({
        username: 'player',
        email: 'p@example.com',
        password: 'password',
      }),
    ).rejects.toMatchObject({ status: 409 });
  });
  it('trata falha de comunicação', async () => {
    fetchMock.mockRejectedValueOnce(new TypeError('Network failed'));
    await expect(api.me('test-token')).rejects.toMatchObject({
      status: 0,
      message: expect.stringContaining('conectar'),
    });
  });
  it('rejeita contratos inválidos sem criar sessão', async () => {
    fetchMock.mockResolvedValueOnce(
      Response.json({
        accessToken: 'x',
        tokenType: 'invalid',
        expiresIn: 3600,
      }),
    );
    await expect(
      api.login({ email: 'p@example.com', password: 'password' }),
    ).rejects.toBeInstanceOf(ApiError);
    fetchMock.mockResolvedValueOnce(
      Response.json({ ...user, balance: 'invented' }),
    );
    await expect(api.me('test-token')).rejects.toBeInstanceOf(ApiError);
  });
});
