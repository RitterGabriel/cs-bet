import { useEffect, useRef, useState } from 'react';
import { Link, useLocation } from 'react-router';
import {
  BarChart3,
  ChevronDown,
  LogIn,
  LogOut,
  Package,
  UserRound,
  UserRoundPlus,
} from 'lucide-react';
import { useAuth } from '../contexts/AuthContext';

export function ProfileMenu() {
  const { user, logout } = useAuth();
  const [open, setOpen] = useState(false);
  const root = useRef<HTMLDivElement>(null);
  const trigger = useRef<HTMLButtonElement>(null);
  const location = useLocation();
  useEffect(() => {
    setOpen(false);
  }, [location.pathname]);
  useEffect(() => {
    if (!open) return;
    const closeOutside = (event: PointerEvent) => {
      if (!root.current?.contains(event.target as Node)) setOpen(false);
    };
    document.addEventListener('pointerdown', closeOutside);
    return () => document.removeEventListener('pointerdown', closeOutside);
  }, [open]);

  function focusItem(direction: number) {
    const items = [
      ...(root.current?.querySelectorAll<HTMLElement>(
        '[role="menuitem"]:not(:disabled)',
      ) ?? []),
    ];
    const index = items.indexOf(document.activeElement as HTMLElement);
    const next =
      index < 0
        ? direction > 0
          ? 0
          : items.length - 1
        : (index + direction + items.length) % items.length;
    items[next]?.focus();
  }

  return (
    <div
      className="profile-menu"
      ref={root}
      onBlur={(event) => {
        if (!event.currentTarget.contains(event.relatedTarget as Node | null))
          setOpen(false);
      }}
      onKeyDown={(event) => {
        if (event.key === 'Escape') {
          setOpen(false);
          trigger.current?.focus();
        }
        if (open && (event.key === 'ArrowDown' || event.key === 'ArrowUp')) {
          event.preventDefault();
          focusItem(event.key === 'ArrowDown' ? 1 : -1);
        }
        if (open && (event.key === 'Home' || event.key === 'End')) {
          event.preventDefault();
          const items = root.current?.querySelectorAll<HTMLElement>(
            '[role="menuitem"]:not(:disabled)',
          );
          items?.[event.key === 'Home' ? 0 : items.length - 1]?.focus();
        }
      }}
    >
      <button
        ref={trigger}
        className="profile-trigger"
        aria-label={user ? `Menu de ${user.username}` : 'Entrar ou criar conta'}
        aria-expanded={open}
        aria-haspopup="menu"
        aria-controls="profile-dropdown"
        onClick={() => setOpen((value) => !value)}
        onKeyDown={(event) => {
          if (!open && (event.key === 'ArrowDown' || event.key === 'ArrowUp')) {
            event.preventDefault();
            setOpen(true);
            window.requestAnimationFrame(() =>
              focusItem(event.key === 'ArrowDown' ? 1 : -1),
            );
          }
        }}
      >
        <span className="avatar">
          <UserRound size={20} />
        </span>
        <span className="profile-label">
          {user?.username ?? 'Minha conta'}
          <small>
            {user ? 'Perfil do explorador' : 'Entrar / Criar conta'}
          </small>
        </span>
        <ChevronDown size={15} className={open ? 'rotated' : ''} />
      </button>
      {open && (
        <div
          className="dropdown"
          id="profile-dropdown"
          role="menu"
          aria-label="Minha conta"
        >
          {user ? (
            <>
              <div className="dropdown-heading">Olá, {user.username}</div>
              <Link to="/profile" role="menuitem">
                <UserRound size={17} /> Meu perfil
              </Link>
              <Link to="/inventory" role="menuitem">
                <Package size={17} /> Meu inventário
              </Link>
              <button role="menuitem" disabled>
                <BarChart3 size={17} /> Minhas estatísticas{' '}
                <small>Em breve</small>
              </button>
              <div className="menu-divider" />
              <button
                role="menuitem"
                onClick={() => {
                  logout();
                  setOpen(false);
                }}
              >
                <LogOut size={17} /> Sair
              </button>
            </>
          ) : (
            <>
              <Link
                to="/login"
                state={{ from: location.pathname + location.search }}
                role="menuitem"
              >
                <LogIn size={17} /> Entrar
              </Link>
              <Link to="/register" role="menuitem">
                <UserRoundPlus size={17} /> Criar conta
              </Link>
            </>
          )}
        </div>
      )}
    </div>
  );
}
