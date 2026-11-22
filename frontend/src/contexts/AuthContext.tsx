import {
  createContext,
  useCallback,
  useContext,
  useEffect,
  useRef,
  useState,
} from 'react';
import type { ReactNode } from 'react';
import { api, ApiError } from '../services/api';
import { fetchInventory } from '../services/inventory';
import type {
  InventoryPageResponse,
  InventoryStatus,
} from '../types/inventory';
import type { LoginRequest, UserResponse } from '../types/api';

interface Session {
  token: string;
  user: UserResponse;
  expiresAt: number;
}
interface AuthValue {
  user: UserResponse | null;
  isLoading: boolean;
  notice: string;
  login: (data: LoginRequest) => Promise<void>;
  logout: () => void;
  refreshUser: () => Promise<void>;
  dismissNotice: () => void;
  getInventory: (
    page: number,
    status?: InventoryStatus,
  ) => Promise<InventoryPageResponse>;
}
const AuthContext = createContext<AuthValue | undefined>(undefined);

export function AuthProvider({ children }: { children: ReactNode }) {
  // A sessão existe somente na memória deste provider. Recarregar exige novo login.
  const [session, setSession] = useState<Session | null>(null);
  const [isLoading, setIsLoading] = useState(false);
  const [notice, setNotice] = useState('');
  const revision = useRef(0);

  const logout = useCallback(() => {
    revision.current++;
    setSession(null);
    setIsLoading(false);
    setNotice('');
  }, []);

  const expire = useCallback(() => {
    revision.current++;
    setSession(null);
    setIsLoading(false);
    setNotice('Sua sessão expirou. Entre novamente para acessar seus dados.');
  }, []);

  useEffect(() => {
    if (!session) return;
    const check = () => {
      if (Date.now() >= session.expiresAt) expire();
    };
    const timer = window.setTimeout(
      expire,
      Math.max(0, session.expiresAt - Date.now()),
    );
    document.addEventListener('visibilitychange', check);
    window.addEventListener('focus', check);
    return () => {
      window.clearTimeout(timer);
      document.removeEventListener('visibilitychange', check);
      window.removeEventListener('focus', check);
    };
  }, [session, expire]);

  const getInventory = useCallback(
    async (page: number, status?: InventoryStatus) => {
      if (!session || Date.now() >= session.expiresAt) {
        expire();
        throw new ApiError(
          401,
          'Entre novamente para consultar seu inventário.',
        );
      }
      const attempt = revision.current;
      try {
        const data = await fetchInventory(session.token, page, status);
        if (attempt !== revision.current)
          throw new ApiError(401, 'A sessão foi encerrada.');
        return data;
      } catch (error) {
        if (
          attempt === revision.current &&
          error instanceof ApiError &&
          error.status === 401
        )
          expire();
        throw error;
      }
    },
    [session, expire],
  );

  async function login(data: LoginRequest) {
    const attempt = ++revision.current;
    setIsLoading(true);
    setNotice('');
    try {
      const result = await api.login(data);
      const expiresAt = Date.now() + result.expiresIn * 1000;
      const user = await api.me(result.accessToken);
      if (attempt !== revision.current)
        throw new Error('Login cancelado. Tente novamente.');
      if (Date.now() >= expiresAt)
        throw new Error('A sessão expirou. Tente novamente.');
      setSession({ token: result.accessToken, user, expiresAt });
    } finally {
      if (attempt === revision.current) setIsLoading(false);
    }
  }

  async function refreshUser() {
    if (!session) return;
    if (Date.now() >= session.expiresAt) {
      expire();
      return;
    }
    const attempt = revision.current;
    try {
      const user = await api.me(session.token);
      if (attempt === revision.current)
        setSession((current) => (current ? { ...current, user } : null));
    } catch (error) {
      if (
        attempt === revision.current &&
        error instanceof ApiError &&
        error.status === 401
      )
        expire();
      throw error;
    }
  }

  return (
    <AuthContext.Provider
      value={{
        user: session?.user ?? null,
        isLoading,
        notice,
        login,
        logout,
        refreshUser,
        getInventory,
        dismissNotice: () => setNotice(''),
      }}
    >
      {children}
    </AuthContext.Provider>
  );
}

export function useAuth() {
  const context = useContext(AuthContext);
  if (!context) throw new Error('AuthProvider não encontrado.');
  return context;
}
