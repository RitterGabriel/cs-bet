import { useEffect, useMemo, useState } from 'react';
import { Link } from 'react-router';
import {
  ArrowUpRight,
  Box,
  ChevronDown,
  Info,
  Check,
  Crosshair,
  Layers3,
  Search,
  ShieldCheck,
  SlidersHorizontal,
  Sparkles,
} from 'lucide-react';
import { listOfficialCases } from '../services/catalog';
import type { CatalogPageResponse } from '../services/catalog';
import { ApiError } from '../services/api';
import { MarketPriceNotice } from '../components/MarketPriceNotice';
import { CaseCard } from '../components/CaseCard';
import { CatalogHeroShowcase } from '../components/CatalogHeroShowcase';
import { cases } from '../data/cases';
import type { CaseCategory } from '../types/catalog';

export function CatalogPage({
  category,
  home = false,
}: {
  category: CaseCategory;
  home?: boolean;
}) {
  const custom = category === 'custom';
  const [page, setPage] = useState(0);
  const [retry, setRetry] = useState(0);
  const [loading, setLoading] = useState(!custom);
  const [error, setError] = useState('');
  const [remote, setRemote] = useState<CatalogPageResponse>({
    content: [],
    page: 0,
    size: 12,
    totalElements: 0,
    totalPages: 0,
  });
  const [query, setQuery] = useState('');
  const [sort, setSort] = useState('featured');
  useEffect(() => {
    if (custom) return;
    let active = true;
    setLoading(true);
    setError('');
    const timer = window.setTimeout(() => {
      listOfficialCases(page, query, sort)
        .then((result) => {
          if (active) setRemote(result);
        })
        .catch((failure) => {
          if (active)
            setError(
              failure instanceof ApiError
                ? failure.message
                : 'Não foi possível carregar o catálogo.',
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
  }, [custom, page, query, sort, retry]);
  const visibleCases = useMemo(() => {
    if (!custom) return remote.content;
    const result = cases.filter(
      (item) =>
        item.category === category &&
        item.name
          .toLocaleLowerCase('pt-BR')
          .includes(query.toLocaleLowerCase('pt-BR').trim()),
    );
    return sort === 'name'
      ? result.sort((a, b) => a.name.localeCompare(b.name, 'pt-BR'))
      : sort === 'name_desc'
        ? result.sort((a, b) => b.name.localeCompare(a.name, 'pt-BR'))
        : result;
  }, [category, custom, query, sort, remote]);
  return (
    <div className="catalog-experience">
      <section className={`hero ${custom ? 'hero-custom' : ''}`}>
        <div className="container hero-inner">
          <div className="hero-copy">
            <span className="pill">
              <span className="pill-marker" />{' '}
              {custom
                ? 'CRIATIVIDADE EM CADA DETALHE'
                : 'O SEU UNIVERSO CS2'}
            </span>
            <h1>
              {custom ? (
                <>
                  SEU ESTILO.
                  <br />
                  <em>OUTRAS POSSIBILIDADES.</em>
                </>
              ) : (
                <>
                  EXPLORE O
                  <br />
                  <em>UNIVERSO CS2.</em>
                </>
              )}
            </h1>
            <p>
              {custom
                ? 'Coleções temáticas com a identidade CS Bet. Novas combinações para conhecer e imaginar.'
                : 'Caixas icônicas e skins marcantes. Descubra os detalhes que tornam cada coleção única.'}
            </p>
            <div className="hero-actions">
              <a className="button primary" href="#catalog">
                Explorar {custom ? 'coleções' : 'caixas'} <ArrowUpRight size={18} />
              </a>
              <Link className="text-link" to="/statistics">
                <BarChartIcon /> Meu histórico
              </Link>
            </div>
            <div className="hero-notes">
              <span>
                <Check size={14} /> Catálogo público
              </span>
              <span>
                <ShieldCheck size={14} /> 100% educacional
              </span>
              <span>
                <Check size={14} /> Sem dinheiro real
              </span>
            </div>
          </div>
          <CatalogHeroShowcase
            items={!custom && (loading || error) ? [] : visibleCases}
            custom={custom}
            loading={!custom && loading}
          />
        </div>
      </section>
      <section
        className="catalog-section container"
        id="catalog"
        aria-labelledby="catalog-title"
      >
        <div className="section-heading">
          <div>
            <span className="eyebrow">
              {custom
                ? 'FEITAS DE CRIATIVIDADE'
                : 'ESCOLHA. CONHEÇA. EXPLORE.'}
            </span>
            <h2 id="catalog-title">
              {custom ? 'Coleções personalizadas' : 'Caixas oficiais'}{' '}
              <span className="count-tag">
                {custom
                  ? cases.filter((item) => item.category === 'custom').length
                  : loading ? '—' : remote.totalElements}
              </span>
            </h2>
            <p>
              {custom
                ? 'Criações fictícias do CS Bet, inspiradas em temas e estilos.'
                : 'Encontre suas favoritas e conheça os itens de cada coleção.'}
            </p>
          </div>
          <span className="section-label">
            <span className="tiny-diamond" />{' '}
            {custom ? 'CATÁLOGO DEMONSTRATIVO' : 'CATÁLOGO COUNTER-STRIKE 2'}
          </span>
        </div>
        {!custom && <details className="catalog-pricing-note">
          <summary><span><Info size={15} /> Preços de referência <span className="pricing-note-description">· Podem variar com as ofertas e o dólar</span></span><ChevronDown size={15} /></summary>
          <MarketPriceNotice quotes={remote.content.flatMap(item => item.marketPrice ? [item.marketPrice] : [])} />
        </details>}
        <div className="catalog-toolbar">
          <div className="toolbar-label">
            {custom ? <Layers3 size={18} /> : <Box size={18} />}{' '}
            {custom ? 'Todas as coleções' : 'Todas as caixas'}
            <span>{visibleCases.length}</span>
          </div>
          <div className="catalog-controls">
            <label className="search-field">
              <Search size={17} />
              <input
                value={query}
                onChange={(event) => {
                  setQuery(event.target.value);
                  setPage(0);
                }}
                maxLength={150}
                type="search"
                placeholder="Buscar no catálogo..."
                aria-label="Buscar caixas pelo nome"
              />
            </label>
            <label className="sort-field">
              <SlidersHorizontal size={16} />
              <select
                value={sort}
                onChange={(event) => {
                  setSort(event.target.value);
                  setPage(0);
                }}
                aria-label="Ordenar catálogo"
              >
                <option value="featured">Em destaque</option>
                <option value="name">Nome A–Z</option>
                <option value="name_desc">Nome Z–A</option>
                {!custom && (
                  <>
                    <option value="price_desc">Maior preço</option>
                    <option value="price_asc">Menor preço</option>
                  </>
                )}
              </select>
            </label>
          </div>
        </div>
        <p className="sr-only" role="status">
          {visibleCases.length}{' '}
          {visibleCases.length === 1
            ? 'resultado encontrado'
            : 'resultados encontrados'}
        </p>
        {!custom && loading ? (
          <div className="catalog-loading" role="status">
            <span className="sr-only">Carregando catálogo...</span>
            <div className="case-grid" aria-hidden="true">{Array.from({ length: 8 }, (_, index) => <div className="catalog-card-skeleton" key={index}><div /><span /><span /></div>)}</div>
          </div>
        ) : !custom && error ? (
          <div className="empty-state" role="alert">
            <h3>Não foi possível carregar o catálogo</h3>
            <p>{error}</p>
            <button
              className="button secondary"
              onClick={() => setRetry((value) => value + 1)}
            >
              Tentar novamente
            </button>
          </div>
        ) : visibleCases.length ? (
          <div className="case-grid">
            {visibleCases.map((item) => (
              <CaseCard key={item.id} item={item} />
            ))}
          </div>
        ) : (
          <div className="empty-state">
            <Search size={32} />
            <h3>Nenhuma caixa encontrada</h3>
            <p>
              {query
                ? 'Tente buscar outro nome.'
                : 'Nenhuma caixa cadastrada na API. O catálogo fica disponível após a importação.'}
            </p>
            <button className="button secondary" onClick={() => setQuery('')}>
              Limpar busca
            </button>
          </div>
        )}
        {!custom && !loading && !error && remote.totalPages > 1 && (
          <nav className="pagination" aria-label="Paginação do catálogo">
            <button
              className="button secondary"
              disabled={page === 0}
              onClick={() => setPage((value) => value - 1)}
            >
              Anterior
            </button>
            <span role="status">
              Página {page + 1} de {remote.totalPages} · {remote.totalElements}{' '}
              caixas
            </span>
            <button
              className="button secondary"
              disabled={page + 1 >= remote.totalPages}
              onClick={() => setPage((value) => value + 1)}
            >
              Próxima
            </button>
          </nav>
        )}
        <div className="catalog-footnote">
          <span>
            <ShieldCheck size={16} />{' '}
            {custom
              ? 'Coleções fictícias. Não representam caixas oficiais do jogo.'
              : 'Fonte: ByMykel/CSGO-API (MIT). Nomes originais preservados. Imagens sujeitas aos direitos da Valve.'}
          </span>
          <span>Explore no seu ritmo.</span>
        </div>
        {home && (
          <div className="collection-banner">
            <span className="banner-icon">
              <Sparkles size={30} />
            </span>
            <div>
              <span className="eyebrow purple-text">ALÉM DO CONVENCIONAL</span>
              <h3>Um novo olhar para os seus estilos favoritos.</h3>
              <p>Conheça as coleções temáticas criadas para o CS Bet.</p>
            </div>
            <Link className="button secondary" to="/custom">
              <Layers3 size={17} /> Ver personalizadas
            </Link>
          </div>
        )}
      </section>
      <div className="container principles">
        <div>
          <ShieldCheck />
          <span>
            Sem valor monetário<small>Uma experiência de aprendizado</small>
          </span>
        </div>
        <div>
          <Box />
          <span>
            Explore sem compromisso<small>Catálogo aberto para todos</small>
          </span>
        </div>
        <div>
          <Crosshair />
          <span>
            Transparência em primeiro lugar
            <small>Conheça os conceitos por trás dos itens</small>
          </span>
        </div>
      </div>
    </div>
  );
}
function BarChartIcon() {
  return <SlidersHorizontal size={17} aria-hidden="true" />;
}
