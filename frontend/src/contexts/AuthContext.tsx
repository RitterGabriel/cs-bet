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
import { walletApi } from '../services/wallet';
import type { WalletBalance, WalletHistory } from '../services/wallet';
import { fetchInventory } from '../services/inventory';
import type {
  InventoryPageResponse,
  InventoryStatus,
  InventorySort,
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
  inventoryEstimate: string | null | undefined;
  login: (data: LoginRequest) => Promise<void>;
  logout: () => Promise<void>;
  refreshUser: () => Promise<void>;
  dismissNotice: () => void;
  getWalletBalance: () => Promise<WalletBalance>;
  getWalletHistory: (page: number) => Promise<WalletHistory>;
  addCredits: (amount: string, requestId: string) => Promise<WalletBalance>;
  getInventory: (
    page: number,
    status?: InventoryStatus,
    sort?: InventorySort,
  ) => Promise<InventoryPageResponse>;
}
const AuthContext = createContext<AuthValue | undefined>(undefined);

export function AuthProvider({ children }: { children: ReactNode }) {
  // O JWT fica em memória; o cookie HttpOnly permite restaurá-lo após recarregar.
  const [session, setSession] = useState<Session | null>(null);
  const [isLoading, setIsLoading] = useState(true);
  const [notice, setNotice] = useState('');
  const [inventoryEstimate, setInventoryEstimate] = useState<
    string | null | undefined
  >(undefined);
  const revision = useRef(0);

  const logout = useCallback(async () => {
    const attempt = ++revision.current;
    setIsLoading(true);
    try {
      await api.logout();
      if (attempt !== revision.current) return;
      setSession(null);
      setInventoryEstimate(undefined);
      setNotice('');
    } catch {
      if (attempt === revision.current)
        setNotice('Não foi possível encerrar a sessão no servidor. Tente sair novamente.');
    } finally {
      if (attempt === revision.current) setIsLoading(false);
    }
  }, []);

  useEffect(() => {
    let active = true;
    const attempt = revision.current;
    async function restore() {
      try {
        const result = await api.restoreSession();
        const expiresAt = Date.now() + result.expiresIn * 1000;
        const user = await api.me(result.accessToken);
        if (active && attempt === revision.current && Date.now() < expiresAt)
          setSession({ token: result.accessToken, user, expiresAt });
      } catch (error) {
        if (active && attempt === revision.current && !(error instanceof ApiError && error.status === 401))
          setNotice('Não foi possível restaurar a sessão. Atualize a página para tentar novamente.');
      } finally {
        if (active && attempt === revision.current) setIsLoading(false);
      }
    }
    void restore();
    return () => { active = false; };
  }, []);

  const expire = useCallback(() => {
    revision.current++;
    setSession(null);
    setInventoryEstimate(undefined);
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
    async (page: number, status?: InventoryStatus, sort?: InventorySort) => {
      if (!session || Date.now() >= session.expiresAt) {
        expire();
        throw new ApiError(
          401,
          'Entre novamente para consultar seu inventário.',
        );
      }
      const attempt = revision.current;
      try {
        const data = await fetchInventory(session.token, page, status, sort);
        if (attempt !== revision.current)
          throw new ApiError(401, 'A sessão foi encerrada.');
        setInventoryEstimate(data.estimatedAvailableTotal);
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

  const token = session?.token;
  const expiresAt = session?.expiresAt;
  const walletCall = useCallback(async <T,>(action: (token: string) => Promise<T>): Promise<T> => {
    if (!token || !expiresAt || Date.now() >= expiresAt) {
      expire();
      throw new ApiError(401, 'Entre novamente para acessar a carteira.');
    }
    const attempt = revision.current;
    try {
      const result = await action(token);
      if (attempt !== revision.current) throw new ApiError(401, 'A sessão foi encerrada.');
      return result;
    } catch (error) {
      if (attempt === revision.current && error instanceof ApiError && error.status === 401) expire();
      throw error;
    }
  }, [token, expiresAt, expire]);
  const getWalletBalance = useCallback(async () => {
    const data = await walletCall(walletApi.balance);
    setSession(current => current && current.token === token ? { ...current, user: { ...current.user, balance: Number(data.balance) } } : current);
    return data;
  }, [walletCall, token]);
  const getWalletHistory = useCallback((page: number) => walletCall(token => walletApi.history(token, page)), [walletCall]);
  const addCredits = useCallback(async (amount: string, requestId: string) => {
    const data = await walletCall(token => walletApi.add(token, amount, requestId));
    setSession(current => current && current.token === token ? { ...current, user: { ...current.user, balance: Number(data.balance) } } : current);
    return data;
  }, [walletCall, token]);

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
        inventoryEstimate,
        login,
        logout,
        refreshUser,
        getInventory,
        getWalletBalance,
        getWalletHistory,
        addCredits,
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
