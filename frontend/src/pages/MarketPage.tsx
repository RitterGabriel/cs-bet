import { useEffect, useState } from 'react';
import { Search } from 'lucide-react';
import { listMarketSkins } from '../services/marketCatalog';
import type { MarketFilters, MarketSkinPage } from '../services/marketCatalog';
import { SkinGrid } from '../components/SkinGrid';
import { MarketPriceNotice } from '../components/MarketPriceNotice';
import { wearLabels } from '../services/market';
import { ApiError } from '../services/api';

const initial: MarketFilters = {
  search: '',
  weapon: '',
  rarity: '',
  wear: '',
  sort: 'name',
};
export function MarketPage() {
  const [filters, setFilters] = useState(initial);
  const [page, setPage] = useState(0);
  const [retry, setRetry] = useState(0);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [data, setData] = useState<MarketSkinPage>({
    content: [],
    page: 0,
    size: 24,
    totalPages: 0,
    totalElements: 0,
    weapons: [],
    rarities: [],
  });
  const change = (key: keyof MarketFilters, value: string) => {
    setFilters((current) => ({ ...current, [key]: value }));
    setPage(0);
  };
  useEffect(() => {
    let active = true;
    setLoading(true);
    setError('');
    const timer = window.setTimeout(() => {
      listMarketSkins(page, filters)
        .then((result) => {
          if (active) setData(result);
        })
        .catch((failure) => {
          if (active)
            setError(
              failure instanceof ApiError
                ? failure.message
                : 'Não foi possível carregar o mercado.',
            );
        })
        .finally(() => {
          if (active) setLoading(false);
        });
    }, 250);
    return () => {
      active = false;
      window.clearTimeout(timer);
    };
  }, [page, filters, retry]);
  return (
    <div className="container page-content">
      <div className="section-heading">
        <div>
          <span className="eyebrow">CATÁLOGO PÚBLICO</span>
          <h1>Mercado</h1>
          <p className="muted">
            Compare referências de skins. Saldos e itens são virtuais; nenhuma
            transação está disponível.
          </p>
        </div>
      </div>
      <MarketPriceNotice
        quotes={
          loading ? [] : data.content.flatMap((item) => item.marketPrices ?? [])
        }
      />
      <div className="market-filters">
        <label>
          Buscar por nome
          <input
            type="search"
            aria-label="Buscar skins pelo nome"
            value={filters.search}
            maxLength={150}
            onChange={(e) => change('search', e.target.value)}
            placeholder="Nome da skin..."
          />
        </label>
        <label>
          Arma
          <select
            aria-label="Filtrar por arma"
            value={filters.weapon}
            onChange={(e) => change('weapon', e.target.value)}
          >
            <option value="">Todas as armas</option>
            {data.weapons.map((weapon) => (
              <option key={weapon} value={weapon}>
                {weapon}
              </option>
            ))}
          </select>
        </label>
        <label>
          Raridade
          <select
            aria-label="Filtrar por raridade"
            value={filters.rarity}
            onChange={(e) => change('rarity', e.target.value)}
          >
            <option value="">Todas as raridades</option>
            {data.rarities.map((rarity) => (
              <option key={rarity.id} value={rarity.id}>
                {rarity.name}
              </option>
            ))}
          </select>
        </label>
        <label>
          Desgaste
          <select
            aria-label="Filtrar por desgaste"
            value={filters.wear}
            onChange={(e) => change('wear', e.target.value)}
          >
            <option value="">Todos os desgastes</option>
            {Object.entries(wearLabels).map(([key, label]) => (
              <option key={key} value={key}>
                {label}
              </option>
            ))}
          </select>
        </label>
        <label>
          Ordenação
          <select
            aria-label="Ordenar skins"
            value={filters.sort}
            onChange={(e) => change('sort', e.target.value)}
          >
            <option value="name">Nome A–Z</option>
            <option value="name_desc">Nome Z–A</option>
            <option value="price_asc">Menor preço</option>
            <option value="price_desc">Maior preço</option>
          </select>
        </label>
      </div>
      <p className="market-sort-criterion">
        Ordenação por preço: menor cotação válida entre as variantes compatíveis
        com os filtros. Skins sem cotação ficam no final. O filtro de desgaste
        considera apenas variantes identificadas.
      </p>
      {loading ? (
        <p role="status" className="empty-state">
          Carregando mercado...
        </p>
      ) : error ? (
        <div role="alert" className="empty-state">
          <p>{error}</p>
          <button
            className="button secondary"
            onClick={() => setRetry((v) => v + 1)}
          >
            Tentar novamente
          </button>
        </div>
      ) : data.content.length ? (
        <SkinGrid
          key={`${page}:${JSON.stringify(filters)}`}
          items={data.content}
        />
      ) : (
        <div className="empty-state">
          <Search />
          <h2>Nenhuma skin encontrada</h2>
          <button
            className="button secondary"
            onClick={() => {
              setFilters(initial);
              setPage(0);
            }}
          >
            Limpar filtros
          </button>
        </div>
      )}
      {!loading && !error && data.totalPages > 0 && (
        <nav className="pagination" aria-label="Paginação do mercado">
          <button
            className="button secondary"
            disabled={page === 0}
            onClick={() => setPage((v) => v - 1)}
          >
            Anterior
          </button>
          <span role="status">
            Página {page + 1} de {data.totalPages} · {data.totalElements} skins
          </span>
          <button
            className="button secondary"
            disabled={page + 1 >= data.totalPages}
            onClick={() => setPage((v) => v + 1)}
          >
            Próxima
          </button>
        </nav>
      )}
    </div>
  );
}
