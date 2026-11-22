import { useEffect, useRef } from 'react';
import { Link, NavLink, Outlet, useLocation } from 'react-router';
import {
  BarChart3,
  Box,
  Coins,
  Layers3,
  Package,
  ShieldCheck,
  X,
} from 'lucide-react';
import { useAuth } from '../contexts/AuthContext';
import { formatBRL, formatVirtualBalance } from '../services/market';
import { ProfileMenu } from './ProfileMenu';

export function Layout() {
  const { user, notice, dismissNotice, inventoryEstimate } = useAuth();
  const location = useLocation();
  const main = useRef<HTMLElement>(null);
  const previousPath = useRef(location.pathname);
  useEffect(() => {
    window.scrollTo(0, 0);
    if (previousPath.current !== location.pathname)
      main.current?.focus({ preventScroll: true });
    previousPath.current = location.pathname;
  }, [location.pathname]);

  return (
    <>
      <a className="skip-link" href="#main-content">
        Pular para o conteúdo
      </a>
      <header className="site-header">
        <div className="header-main container">
          <Link to="/" className="brand" aria-label="CS Bet, página inicial">
            <span className="brand-symbol">
              <Box size={28} />
            </span>
            <span>
              CS<span className="cyan-text">BET</span>
              <small>SIMULAÇÃO EDUCACIONAL · CS2</small>
            </span>
          </Link>
          <nav className="header-navigation" aria-label="Navegação principal">
            <NavLink to="/official" className={({ isActive }) => isActive || location.pathname === '/' ? 'active' : ''}>
              <Box size={17} /> Oficiais
            </NavLink>
            <NavLink to="/custom"><Layers3 size={17} /> Personalizadas</NavLink>
            <NavLink to="/market"><Package size={17} /> Mercado</NavLink>
            <NavLink to="/statistics"><BarChart3 size={17} /> Estatísticas</NavLink>
          </nav>
          <div className="header-account">
            {user ? (
              <Link
                to="/wallet"
                className="account-stat balance-button"
              >
                <Coins size={20} />
                <span>
                  Saldo virtual
                  <strong>{formatVirtualBalance(user.balance)}</strong>
                </span>
              </Link>
            ) : (
              <Link
                to="/login"
                state={{ from: '/wallet' }}
                className="account-stat"
              >
                <Coins size={20} />
                <span>
                  Saldo virtual<strong>Entre para consultar</strong>
                </span>
              </Link>
            )}
            <Link
              to={user ? '/inventory' : '/login'}
              state={user ? undefined : { from: '/inventory' }}
              className="account-stat inventory-stat"
            >
              <Package size={20} />
              <span>
                Valor do inventário
                <strong>
                  {inventoryEstimate === undefined
                    ? 'Consultar'
                    : inventoryEstimate === null
                      ? 'Preço indisponível'
                      : formatBRL(inventoryEstimate)}
                </strong>
              </span>
            </Link>
            <ProfileMenu />
          </div>
        </div>

      </header>
      {notice && (
        <div className="session-notice container" role="status">
          <span>
            {notice}{' '}
            <Link to="/login" state={{ from: location.pathname }}>
              Entrar
            </Link>
          </span>
          <button
            className="icon-button"
            onClick={dismissNotice}
            aria-label="Dispensar aviso"
          >
            <X size={18} />
          </button>
        </div>
      )}
      <main id="main-content" ref={main} tabIndex={-1}>
        <Outlet />
      </main>
      <footer className="site-footer">
        <div className="container footer-top">
          <div>
            <Link to="/" className="brand footer-brand">
              <Box size={25} />
              <span>
                CS<span className="cyan-text">BET</span>
              </span>
            </Link>
            <p>O universo CS2, com uma nova perspectiva.</p>
          </div>
          <div className="footer-disclaimer">
            <ShieldCheck size={22} />
            <p>
              Uma experiência exclusivamente educacional.
              <br />
              <span>
                Saldos e itens virtuais sem valor monetário. Sem pagamentos,
                saques ou prêmios.
              </span>
            </p>
          </div>
        </div>
        <div className="container footer-bottom">
          <span>
            © {new Date().getFullYear()} CS Bet · Feito para aprender.
          </span>
          <span>
            Projeto independente, sem afiliação à Valve ou ao Counter-Strike.
          </span>
        </div>
      </footer>
    </>
  );
}
