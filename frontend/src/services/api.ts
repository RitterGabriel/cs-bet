import type {
  LoginRequest,
  LoginResponse,
  RegisterRequest,
  UserResponse,
} from '../types/api';

const API_URL = (import.meta.env.VITE_API_URL || '/api').replace(/\/$/, '');

export class ApiError extends Error {
  constructor(
    public readonly status: number,
    message: string,
  ) {
    super(message);
  }
}

async function request<T>(path: string, options: RequestInit = {}): Promise<T> {
  const controller = new AbortController();
  const timeout = window.setTimeout(() => controller.abort(), 12000);
  try {
    const response = await fetch(`${API_URL}${path}`, {
      ...options,
      signal: controller.signal,
      credentials: 'omit',
      cache: 'no-store',
      headers: { Accept: 'application/json', ...options.headers },
    });
    if (!response.ok) {
      const messages: Record<number, string> = {
        400: 'Confira os dados informados e tente novamente.',
        401: 'E-mail ou senha inválidos.',
        403: 'Você não tem permissão para acessar este recurso.',
        409: 'Este nome de usuário ou e-mail já está cadastrado.',
        429: 'Muitas tentativas. Aguarde um momento antes de tentar novamente.',
      };
      throw new ApiError(
        response.status,
        messages[response.status] ??
          'O serviço está indisponível. Tente novamente mais tarde.',
      );
    }
    return (await response.json()) as T;
  } catch (error) {
    if (error instanceof ApiError) throw error;
    throw new ApiError(
      0,
      controller.signal.aborted
        ? 'O servidor demorou para responder. Tente novamente.'
        : 'Não foi possível conectar ao servidor. Verifique sua conexão e tente novamente.',
    );
  } finally {
    window.clearTimeout(timeout);
  }
}

export const api = {
  async login(data: LoginRequest): Promise<LoginResponse> {
    const result = await request<LoginResponse>('/auth/login', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(data),
    });
    if (
      typeof result.accessToken !== 'string' ||
      !result.accessToken ||
      result.tokenType !== 'Bearer' ||
      !Number.isFinite(result.expiresIn) ||
      result.expiresIn <= 0
    ) {
      throw new ApiError(
        0,
        'Não foi possível iniciar a sessão. Tente novamente.',
      );
    }
    return result;
  },
  register(data: RegisterRequest) {
    return request<UserResponse>('/users', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(data),
    });
  },
  async me(token: string): Promise<UserResponse> {
    const user = await request<UserResponse>('/users/me', {
      headers: { Authorization: `Bearer ${token}` },
    });
    if (
      !Number.isSafeInteger(user.id) ||
      typeof user.username !== 'string' ||
      typeof user.email !== 'string' ||
      typeof user.balance !== 'number' ||
      !Number.isFinite(user.balance) ||
      typeof user.createdAt !== 'string' ||
      Number.isNaN(Date.parse(user.createdAt))
    ) {
      throw new ApiError(
        0,
        'Não foi possível carregar os dados do perfil. Tente novamente.',
      );
    }
    return user;
  },
};
