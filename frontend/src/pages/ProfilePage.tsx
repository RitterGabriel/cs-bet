import { useState } from 'react';
import { Link } from 'react-router';
import {
  BarChart3,
  CalendarDays,
  Coins,
  History,
  LoaderCircle,
  Mail,
  Package,
  RefreshCw,
  UserRound,
} from 'lucide-react';
import { useAuth } from '../contexts/AuthContext';
import { formatVirtualBalance } from '../services/market';
import { ApiError } from '../services/api';

export function ProfilePage() {
  const { user, refreshUser } = useAuth();
  const [refreshing, setRefreshing] = useState(false);
  const [message, setMessage] = useState('');
  if (!user) return null;
  async function refresh() {
    setRefreshing(true);
    setMessage('');
    try {
      await refreshUser();
      setMessage('Dados atualizados.');
    } catch (error) {
      setMessage(
        error instanceof ApiError && error.status !== 401
          ? error.message
          : 'Entre novamente para consultar seu perfil.',
      );
    } finally {
      setRefreshing(false);
    }
  }
  return (
    <div className="container page-content">
      <div className="page-heading">
        <span className="eyebrow">SEU ESPAÇO</span>
        <h1>
          Meu <em>perfil.</em>
        </h1>
        <p>Suas informações, em um só lugar.</p>
      </div>
      <section className="panel profile-panel">
        <div className="profile-heading">
          <span className="large-avatar">
            <UserRound size={35} />
          </span>
          <div>
            <h2>{user.username}</h2>
            <p className="muted">Explorador do CS Bet</p>
          </div>
          <button
            className="button secondary"
            disabled={refreshing}
            onClick={refresh}
          >
            {refreshing ? (
              <LoaderCircle className="spin" size={17} />
            ) : (
              <RefreshCw size={17} />
            )}{' '}
            Atualizar dados
          </button>
        </div>
        <p className="status-message" role="status">
          {message}
        </p>
        <dl className="profile-details">
          <div>
            <dt>
              <Mail size={17} /> E-mail
            </dt>
            <dd>{user.email}</dd>
          </div>
          <div>
            <dt>
              <CalendarDays size={17} /> Conta criada em
            </dt>
            <dd>
              {new Intl.DateTimeFormat('pt-BR', { dateStyle: 'long' }).format(
                new Date(user.createdAt),
              )}
            </dd>
          </div>
          <div>
            <dt>
              <Coins size={17} /> Saldo virtual
            </dt>
            <dd className="cyan-text">{formatVirtualBalance(user.balance)}</dd>
          </div>
        </dl>
        <p className="muted">
          Saldo exclusivamente virtual para simulação, sem valor monetário.
        </p>
      </section>
      <div className="education-grid">
        {[
          {
            icon: Package,
            title: 'Inventário',
            description:
              'Consulte suas unidades individuais de skins virtuais.',
          },
          {
            icon: BarChart3,
            title: 'Estatísticas pessoais',
            description: 'Um olhar sobre a sua jornada na simulação.',
          },
          {
            icon: History,
            title: 'Histórico de simulações',
            description: 'Um espaço para suas futuras experiências.',
          },
        ].map(({ icon: Icon, title, description }) => (
          <section className="panel" key={title}>
            <span className="feature-icon">
              <Icon />
            </span>
            <h2>{title}</h2>
            <p className="muted">{description}</p>
            {title === 'Inventário' ? (
              <Link className="button secondary" to="/inventory">
                Meu inventário
              </Link>
            ) : (
              <span className="coming-soon">Em breve</span>
            )}
          </section>
        ))}
      </div>
    </div>
  );
}
