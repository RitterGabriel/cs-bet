import { useEffect, useRef } from 'react';
import { Link, NavLink, Outlet, useLocation } from 'react-router';
import {
  Activity,
  BarChart3,
  Box,
  Coins,
  FlaskConical,
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
              <small>EXPLORE. DESCUBRA. APRENDA.</small>
            </span>
          </Link>
          <div className="recent-activity">
            <span className="activity-icon">
              <Activity size={19} />
            </span>
            <div>
              <span>Atividades recentes</span>
              <small>Novas histórias em breve</small>
            </div>
            <span className="mini-tag">Em desenvolvimento</span>
          </div>
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
        <div className="nav-row">
          <div className="container nav-inner">
            <nav aria-label="Navegação principal">
              <NavLink
                to="/official"
                className={({ isActive }) =>
                  isActive || location.pathname === '/' ? 'active' : ''
                }
              >
                <Box size={18} /> Oficiais
              </NavLink>
              <NavLink to="/custom">
                <Layers3 size={18} /> Personalizadas
              </NavLink>
              <NavLink to="/market">
                <Package size={18} /> Mercado
              </NavLink>
              <NavLink to="/statistics">
                <BarChart3 size={18} /> Estatísticas
              </NavLink>
            </nav>
            <span className="educational-badge">
              <FlaskConical size={15} /> Simulação <span>•</span> Sem dinheiro
              real
            </span>
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
