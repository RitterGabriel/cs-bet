import { Link, useParams } from 'react-router';
import { Box, ChevronRight, Info, Layers3, Sparkles } from 'lucide-react';
import type { CSSProperties } from 'react';
import { cases, categoryLabels } from '../data/cases';
import { NotFoundPage } from './NotFoundPage';

const rarityLabels = {
  industrial: 'Industrial',
  'mil-spec': 'Mil-Spec',
  restricted: 'Restrito',
  classified: 'Confidencial',
  covert: 'Oculto',
  special: 'Especial',
};

export function CaseDetailsPage() {
  const { id } = useParams();
  const item = cases.find((entry) => entry.id === id);
  if (!item) return <NotFoundPage />;
  return (
    <div className="container page-content">
      <nav className="breadcrumbs" aria-label="Localização">
        <Link to={item.category === 'official' ? '/official' : '/custom'}>
          Catálogo
        </Link>
        <ChevronRight size={14} />
        <span>{item.name}</span>
      </nav>
      <section
        className="case-detail-hero"
        style={{ '--case-accent': item.accent } as CSSProperties}
      >
        <div className="detail-art">
          <img
            src={item.image}
            alt={`Ilustração demonstrativa de ${item.name}`}
            width="640"
            height="420"
          />
          <span>ILUSTRAÇÃO DEMONSTRATIVA</span>
        </div>
        <div className="detail-copy">
          <span className="pill">
            {item.category === 'official' ? (
              <Box size={15} />
            ) : (
              <Layers3 size={15} />
            )}
            {categoryLabels[item.category]}
          </span>
          <h1>{item.name}</h1>
          <p>{item.description}</p>
          <div className="info-box">
            <Info size={20} />
            <p>
              Esta é uma prévia do catálogo. A abertura de caixas não está
              disponível nesta etapa.
            </p>
          </div>
        </div>
      </section>
      <section className="panel" aria-labelledby="contents-title">
        <div className="panel-title">
          <Box size={21} />
          <h2 id="contents-title">Conteúdo da caixa</h2>
          <span className="mini-tag">Em preparação</span>
        </div>
        {item.items.length ? (
          <div className="item-grid">
            {item.items.map((entry) => (
              <article
                className={`item-card rarity-${entry.rarity}`}
                key={entry.id}
              >
                <img src={entry.image} alt={entry.name} />
                <h3>{entry.name}</h3>
                <span>{rarityLabels[entry.rarity]}</span>
              </article>
            ))}
          </div>
        ) : (
          <div className="empty-state compact">
            <Box size={35} />
            <h3>Os detalhes estão a caminho</h3>
            <p>
              {item.category === 'official'
                ? 'O conjunto real de itens desta caixa ainda não foi cadastrado. Não exibimos itens ou probabilidades sem dados verificados.'
                : 'Os itens desta coleção fictícia ainda estão em preparação.'}
            </p>
          </div>
        )}
      </section>
      <div className="two-column">
        <section className="panel">
          <div className="panel-title">
            <Layers3 size={21} />
            <h2>Como funcionam as raridades?</h2>
          </div>
          <p className="muted">
            Raridades são categorias de itens. As cores ajudam a diferenciá-las
            visualmente, mas não informam, por si só, a probabilidade de obter
            um item.
          </p>
          <div className="rarity-scale">
            <span className="rarity-mil-spec">Mil-Spec</span>
            <span className="rarity-restricted">Restrito</span>
            <span className="rarity-classified">Confidencial</span>
            <span className="rarity-covert">Oculto</span>
          </div>
          <Link className="text-link" to="/statistics">
            Conheça os conceitos estatísticos
          </Link>
        </section>
        <section className="panel">
          <div className="panel-title">
            <Sparkles size={21} />
            <h2>Itens especiais</h2>
          </div>
          {item.specialItems.length ? (
            <>
              <p>
                {item.specialKind === 'knives'
                  ? 'Facas'
                  : item.specialKind === 'gloves'
                    ? 'Luvas'
                    : 'Itens especiais'}{' '}
                desta caixa
              </p>
              <ul>
                {item.specialItems.map((entry) => (
                  <li key={entry.id}>{entry.name}</li>
                ))}
              </ul>
            </>
          ) : (
            <p className="muted">
              Não há dados de itens especiais cadastrados para esta caixa. Cada
              caixa terá seu próprio grupo de itens; facas e luvas não são
              compartilhadas automaticamente entre elas.
            </p>
          )}
        </section>
      </div>
    </div>
  );
}
