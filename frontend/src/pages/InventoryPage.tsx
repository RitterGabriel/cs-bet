import { useEffect, useState } from 'react';
import type { CSSProperties } from 'react';
import { Link } from 'react-router';
import { CalendarDays, LoaderCircle, Package } from 'lucide-react';
import { useAuth } from '../contexts/AuthContext';
import { formatBRL } from '../services/market';
import { MarketPricePanel } from '../components/MarketPricePanel';
import { ApiError } from '../services/api';
import { CatalogImage } from '../components/CatalogImage';
import type {
  InventoryPageResponse,
  InventoryStatus,
} from '../types/inventory';

export function InventoryPage() {
  const { getInventory } = useAuth();
  const [page, setPage] = useState(0);
  const [status, setStatus] = useState<InventoryStatus | ''>('');
  const [data, setData] = useState<InventoryPageResponse | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [retry, setRetry] = useState(0);

  useEffect(() => {
    let active = true;
    setLoading(true);
    setError('');
    getInventory(page, status || undefined)
      .then((result) => {
        if (active) setData(result);
      })
      .catch((failure) => {
        if (active)
          setError(
            failure instanceof ApiError
              ? failure.message
              : 'Não foi possível carregar seu inventário.',
          );
      })
      .finally(() => {
        if (active) setLoading(false);
      });
    return () => {
      active = false;
    };
  }, [getInventory, page, status, retry]);

  return (
    <div className="container page-content">
      <div className="page-heading">
        <span className="eyebrow">SUAS SKINS VIRTUAIS</span>
        <h1>
          Meu <em>inventário</em>
        </h1>
        <p>Cada item representa uma unidade individual da sua coleção.</p>
      </div>
      {!loading && !error && data && (
        <section
          className="panel inventory-valuation"
          aria-label="Valor estimado do inventário"
        >
          <h2>Valor estimado dos itens disponíveis</h2>
          <p className="simulated-price">
            {data.estimatedAvailableTotal === null
              ? 'Preço indisponível'
              : formatBRL(data.estimatedAvailableTotal)}
          </p>
          <p className="muted">
            {data.availableItems} unidades disponíveis no inventário completo,
            independentemente da página ou filtro. Sem valor monetário.
          </p>
          {data.unpricedAvailableItems > 0 && (
            <p>
              {data.unpricedAvailableItems} itens sem cotação confiável ou sem
              variante identificada; o total será exibido quando houver
              referências para todos.
            </p>
          )}
        </section>
      )}
      <div className="catalog-toolbar inventory-toolbar">
        <span className="toolbar-label">
          <Package size={18} />{' '}
          {loading || error
            ? 'Consultando inventário'
            : `${data?.totalElements ?? 0} ${(data?.totalElements ?? 0) === 1 ? 'item' : 'itens'}${status ? ' neste filtro' : ''}`}
        </span>
        <div className="inventory-status-filter">
          <label htmlFor="inventory-status">Status</label>
          <select
            id="inventory-status"
            value={status}
            onChange={(event) => {
              setStatus(event.target.value as InventoryStatus | '');
              setPage(0);
            }}
          >
            <option value="">Todos</option>
            <option value="AVAILABLE">Disponíveis</option>
            <option value="SOLD">Vendidos</option>
          </select>
        </div>
      </div>
      <InventoryContent
        data={data}
        loading={loading}
        error={error}
        filtered={Boolean(status)}
        onRetry={() => setRetry((value) => value + 1)}
        onPageChange={setPage}
      />
    </div>
  );
}

export function InventoryContent({
  data,
  loading,
  error,
  filtered = false,
  onRetry,
  onPageChange,
}: {
  data: InventoryPageResponse | null;
  loading: boolean;
  error: string;
  filtered?: boolean;
  onRetry: () => void;
  onPageChange: (page: number) => void;
}) {
  if (loading)
    return (
      <div className="empty-state" role="status">
        <LoaderCircle size={32} className="spin" />
        <h2>Carregando inventário...</h2>
        <p>Consultando suas skins virtuais.</p>
      </div>
    );
  if (error)
    return (
      <div className="empty-state" role="alert">
        <h2>Inventário indisponível</h2>
        <p>{error}</p>
        <button className="button secondary" onClick={onRetry}>
          Tentar novamente
        </button>
      </div>
    );
  if (!data?.content.length)
    return (
      <div className="empty-state">
        <Package size={38} />
        <h2>
          {filtered
            ? 'Nenhum item neste filtro'
            : 'Seu inventário ainda está vazio'}
        </h2>
        <p>
          {filtered
            ? 'Não há itens com o status selecionado.'
            : 'Você ainda não possui skins virtuais. Explore o catálogo enquanto preparamos as próximas etapas da simulação.'}
        </p>
        <Link className="button secondary" to="/official">
          Explorar catálogo
        </Link>
      </div>
    );
  return (
    <>
      <div className="inventory-grid">
        {data.content.map((item) => (
          <article
            className="item-card inventory-card"
            key={item.id}
            style={{ '--item-rarity': item.rarity.color } as CSSProperties}
            aria-label={`${item.name}, unidade ${item.id}`}
          >
            <CatalogImage
              src={item.imageUrl ?? ''}
              alt={item.name}
              remote
              item
            />
            <div className="item-card-copy">
              <span className="item-weapon">Unidade #{item.id}</span>
              <h2>{item.name}</h2>
              <span className="item-rarity">{item.rarity.name}</span>
              <MarketPricePanel
                quotes={item.marketPrice ? [item.marketPrice] : []}
                name={item.name}
                fixedVariant
              />
              <p className="inventory-date">
                <CalendarDays size={15} />
                <span>
                  Obtido em{' '}
                  <time dateTime={item.obtainedAt}>
                    {new Intl.DateTimeFormat('pt-BR', {
                      dateStyle: 'medium',
                      timeStyle: 'short',
                    }).format(new Date(item.obtainedAt))}
                  </time>
                </span>
              </p>
              <span className="mini-tag">
                {item.status === 'AVAILABLE' ? 'Disponível' : 'Vendido'}
              </span>
            </div>
          </article>
        ))}
      </div>
      {data.totalPages > 1 && (
        <nav className="pagination" aria-label="Paginação do inventário">
          <button
            className="button secondary"
            disabled={data.page === 0}
            onClick={() => onPageChange(data.page - 1)}
          >
            Anterior
          </button>
          <span role="status">
            Página {data.page + 1} de {data.totalPages}
          </span>
          <button
            className="button secondary"
            disabled={data.page + 1 >= data.totalPages}
            onClick={() => onPageChange(data.page + 1)}
          >
            Próxima
          </button>
        </nav>
      )}
    </>
  );
}
