import { useRef, useState } from 'react';
import type { FormEvent } from 'react';
import { Link, Navigate, useLocation, useNavigate } from 'react-router';
import {
  Box,
  CheckCircle2,
  Eye,
  EyeOff,
  LoaderCircle,
  LockKeyhole,
  ShieldCheck,
} from 'lucide-react';
import { useAuth } from '../contexts/AuthContext';
import { api, ApiError } from '../services/api';
import { safeReturnPath, validateCredentials } from '../services/validation';

export function AuthPage({ register = false }: { register?: boolean }) {
  const { user, login, isLoading } = useAuth();
  const location = useLocation();
  const navigate = useNavigate();
  const [error, setError] = useState('');
  const [submitting, setSubmitting] = useState(false);
  const [visible, setVisible] = useState(false);
  const pending = useRef(false);
  const busy = submitting || isLoading;
  const from = safeReturnPath(location.state?.from);
  const success = !register && location.state?.registered === true;
  if (user) return <Navigate to={from} replace />;

  async function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (pending.current) return;
    const form = event.currentTarget;
    const fields = new FormData(form);
    const email = String(fields.get('email') ?? '')
      .trim()
      .toLowerCase();
    const password = String(fields.get('password') ?? '');
    const username = String(fields.get('username') ?? '').trim();
    const confirm = String(fields.get('confirm') ?? '');
    const invalid = validateCredentials(email, password, register);
    if (invalid) {
      setError(invalid);
      return;
    }
    if (register && (!username || Array.from(username).length > 50)) {
      setError('Informe um nome de usuário de até 50 caracteres.');
      return;
    }
    if (register && password !== confirm) {
      setError('As senhas não coincidem. Confira a confirmação.');
      return;
    }
    pending.current = true;
    setSubmitting(true);
    setError('');
    try {
      if (register) {
        await api.register({ username, email, password });
        form.reset();
        navigate('/login', {
          replace: true,
          state: { registered: true, from },
        });
      } else {
        await login({ email, password });
        form.reset();
        navigate(from, { replace: true });
      }
    } catch (failure) {
      setError(
        failure instanceof ApiError
          ? failure.message
          : 'Não foi possível concluir. Tente novamente.',
      );
    } finally {
      // Não mantém senha em estado React, storage ou histórico de navegação.
      for (const name of ['password', 'confirm']) {
        const input = form.elements.namedItem(name);
        if (input instanceof HTMLInputElement) input.value = '';
      }
      pending.current = false;
      setSubmitting(false);
    }
  }

  return (
    <div className="container auth-layout">
      <aside className="auth-intro">
        <span className="pill">
          <Box size={15} /> SEU ESPAÇO NO CS BET
        </span>
        <h1>
          {register ? (
            <>
              Uma nova forma
              <br />
              de <em>explorar.</em>
            </>
          ) : (
            <>
              Bom ter você
              <br />
              <em>por aqui.</em>
            </>
          )}
        </h1>
        <p>
          Conheça o universo dos itens cosméticos do CS2 em uma experiência
          feita para aprender.
        </p>
        <img
          src="/images/neon.svg"
          alt="Ilustração de uma caixa fictícia da Coleção Neon"
          width="640"
          height="420"
        />
        <div className="auth-promise">
          <ShieldCheck size={22} />
          <span>
            100% educacional
            <small>Créditos fictícios. Nenhum valor monetário.</small>
          </span>
        </div>
      </aside>
      <section className="auth-panel" aria-labelledby="auth-title">
        <span className="feature-icon">
          <LockKeyhole size={23} />
        </span>
        <h2 id="auth-title">
          {register ? 'Crie sua conta' : 'Entre na sua conta'}
        </h2>
        <p className="muted">
          {register
            ? 'Seu perfil é o próximo passo dessa jornada.'
            : 'Acesse seu perfil e consulte seus créditos fictícios.'}
        </p>
        {success && (
          <div className="success-message" role="status">
            <CheckCircle2 size={20} /> Conta criada com sucesso! Entre para
            continuar.
          </div>
        )}
        {error && (
          <div className="error-message" id="auth-error" role="alert">
            {error}
          </div>
        )}
        <form
          onSubmit={submit}
          aria-describedby={error ? 'auth-error' : undefined}
          aria-busy={busy}
        >
          <fieldset disabled={busy}>
            {register && (
              <label htmlFor="username">
                Nome de usuário
                <input
                  id="username"
                  name="username"
                  autoComplete="username"
                  placeholder="Como podemos chamar você?"
                  required
                  maxLength={50}
                />
              </label>
            )}
            <label htmlFor="email">
              E-mail
              <input
                id="email"
                name="email"
                type="email"
                autoComplete={register ? 'email' : 'username'}
                placeholder="voce@exemplo.com"
                required
                maxLength={150}
              />
            </label>
            <label htmlFor="password">
              Senha
              <div className="password-input">
                <input
                  id="password"
                  name="password"
                  type={visible ? 'text' : 'password'}
                  autoComplete={register ? 'new-password' : 'current-password'}
                  placeholder={
                    register ? 'Pelo menos 8 caracteres' : 'Sua senha'
                  }
                  required
                  minLength={register ? 8 : undefined}
                  maxLength={72}
                  aria-describedby={register ? 'password-help' : undefined}
                />
                <button
                  type="button"
                  className="icon-button"
                  aria-label={visible ? 'Ocultar senha' : 'Mostrar senha'}
                  aria-pressed={visible}
                  onClick={() => setVisible((value) => !value)}
                >
                  {visible ? <EyeOff size={18} /> : <Eye size={18} />}
                </button>
              </div>
            </label>
            {register && (
              <>
                <small id="password-help" className="field-help">
                  De 8 a 72 caracteres, respeitando o limite de 72 bytes em
                  UTF-8.
                </small>
                <label htmlFor="confirm">
                  Confirmar senha
                  <input
                    id="confirm"
                    name="confirm"
                    type={visible ? 'text' : 'password'}
                    autoComplete="new-password"
                    placeholder="Digite sua senha novamente"
                    required
                    maxLength={72}
                  />
                </label>
              </>
            )}
            <button type="submit" className="button primary full-width">
              {busy ? (
                <>
                  <LoaderCircle className="spin" size={18} /> Aguarde...
                </>
              ) : register ? (
                'Criar conta gratuita'
              ) : (
                'Entrar'
              )}
            </button>
          </fieldset>
        </form>
        {!register && (
          <p className="session-hint">
            <LockKeyhole size={13} /> Por segurança, recarregar a página encerra
            sua sessão.
          </p>
        )}
        <p className="auth-switch">
          {register ? 'Já tem uma conta?' : 'Ainda não tem conta?'}{' '}
          <Link to={register ? '/login' : '/register'} state={{ from }}>
            {register ? 'Entrar' : 'Criar conta'}
          </Link>
        </p>
        <Link to="/" className="back-catalog">
          Continuar explorando o catálogo
        </Link>
      </section>
    </div>
  );
}
