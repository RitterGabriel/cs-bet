import { useMemo, useState } from 'react';
import { Link } from 'react-router';
import {
  Box,
  Check,
  Crosshair,
  Layers3,
  Search,
  ShieldCheck,
  SlidersHorizontal,
  Sparkles,
} from 'lucide-react';
import { CaseCard } from '../components/CaseCard';
import { cases } from '../data/cases';
import type { CaseCategory } from '../types/catalog';

export function CatalogPage({
  category,
  home = false,
}: {
  category: CaseCategory;
  home?: boolean;
}) {
  const [query, setQuery] = useState('');
  const [sort, setSort] = useState('featured');
  const visibleCases = useMemo(() => {
    const result = cases.filter(
      (item) =>
        item.category === category &&
        item.name
          .toLocaleLowerCase('pt-BR')
          .includes(query.toLocaleLowerCase('pt-BR').trim()),
    );
    return sort === 'name'
      ? result.sort((a, b) => a.name.localeCompare(b.name, 'pt-BR'))
      : result;
  }, [category, query, sort]);
  const custom = category === 'custom';
  return (
    <>
      <section className={`hero ${custom ? 'hero-custom' : ''}`}>
        <div className="container hero-inner">
          <div className="hero-copy">
            <span className="pill">
              <span className="pill-marker" />{' '}
              {custom
                ? 'COLEÇÕES COM IDENTIDADE'
                : 'BEM-VINDO AO UNIVERSO CS BET'}
            </span>
            <h1>
              {custom ? (
                <>
                  Novas ideias.
                  <br />
                  <em>Outras possibilidades.</em>
                </>
              ) : (
                <>
                  Cada caixa,
                  <br />
                  um universo <em>para explorar.</em>
                </>
              )}
            </h1>
            <p>
              {custom
                ? 'Explore coleções temáticas criadas para o CS Bet. Uma curadoria fictícia, com espaço para a criatividade.'
                : 'Descubra caixas icônicas e coleções únicas do CS2. Explore o catálogo e conheça os conceitos por trás de cada item.'}
            </p>
            <div className="hero-actions">
              <a className="button primary" href="#catalog">
                <Box size={18} /> Explorar {custom ? 'coleções' : 'catálogo'}
              </a>
              <Link className="text-link" to="/statistics">
                <BarChartIcon /> Entenda as probabilidades
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
          <div className="hero-visual" aria-hidden="true">
            <div className="orbit orbit-one" />
            <div className="orbit orbit-two" />
            <span className="visual-cross cross-one">+</span>
            <span className="visual-cross cross-two">+</span>
            <div className="hero-visual-label">
              <Crosshair size={15} />{' '}
              {custom ? 'CURADORIA CS BET' : 'EXPLORE O UNIVERSO CS2'}
            </div>
            <img
              src={custom ? '/images/neon.svg' : '/images/revolution.svg'}
              alt=""
              width="640"
              height="420"
            />
            <div className="hero-image-caption">
              <span>{custom ? 'Coleção Neon' : 'Revolution Case'}</span>
              <small>ILUSTRAÇÃO DEMONSTRATIVA</small>
            </div>
          </div>
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
                ? 'CRIADAS PARA EXPLORAR'
                : 'SEU PRÓXIMO PONTO DE PARTIDA'}
            </span>
            <h2 id="catalog-title">
              {custom ? 'Coleções personalizadas' : 'Caixas oficiais'}{' '}
              <span className="count-tag">
                {cases.filter((item) => item.category === category).length}
              </span>
            </h2>
            <p>
              {custom
                ? 'Criações fictícias do CS Bet, inspiradas em temas e estilos.'
                : 'Conheça algumas das caixas que fazem parte do Counter-Strike 2.'}
            </p>
          </div>
          <span className="section-label">
            <span className="tiny-diamond" /> CATÁLOGO DEMONSTRATIVO
          </span>
        </div>
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
                onChange={(event) => setQuery(event.target.value)}
                type="search"
                placeholder="Buscar no catálogo..."
                aria-label="Buscar caixas pelo nome"
              />
            </label>
            <label className="sort-field">
              <SlidersHorizontal size={16} />
              <select
                value={sort}
                onChange={(event) => setSort(event.target.value)}
                aria-label="Ordenar catálogo"
              >
                <option value="featured">Em destaque</option>
                <option value="name">Nome: A–Z</option>
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
        {visibleCases.length ? (
          <div className="case-grid">
            {visibleCases.map((item) => (
              <CaseCard key={item.id} item={item} />
            ))}
          </div>
        ) : (
          <div className="empty-state">
            <Search size={32} />
            <h3>Nenhuma caixa encontrada</h3>
            <p>Tente buscar outro nome.</p>
            <button className="button secondary" onClick={() => setQuery('')}>
              Limpar busca
            </button>
          </div>
        )}
        <div className="catalog-footnote">
          <span>
            <ShieldCheck size={16} />{' '}
            {custom
              ? 'Coleções fictícias. Não representam caixas oficiais do jogo.'
              : 'Imagens ilustrativas. Itens e probabilidades oficiais ainda não cadastrados.'}
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
    </>
  );
}
function BarChartIcon() {
  return <SlidersHorizontal size={17} aria-hidden="true" />;
}
