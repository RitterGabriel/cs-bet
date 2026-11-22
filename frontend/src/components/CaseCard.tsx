import { Link } from 'react-router';
import { Box, Layers3 } from 'lucide-react';
import type { CSSProperties } from 'react';
import type { CatalogCase } from '../types/catalog';
import { CatalogImage } from './CatalogImage';
import { categoryLabels } from '../data/cases';

export function CaseCard({ item }: { item: CatalogCase }) {
  return (
    <article
      className="case-card"
      style={{ '--case-accent': item.accent } as CSSProperties}
    >
      <Link
        to={`/cases/${item.id}`}
        className="case-link"
        aria-label={`Ver detalhes de ${item.name}`}
      >
        <div className="case-art">
          <span className="art-label">
            {item.category === 'official' ? (
              <Box size={13} />
            ) : (
              <Layers3 size={13} />
            )}
            {item.category === 'official' ? 'CS2' : 'CS BET'}
          </span>
          <CatalogImage src={item.image} alt={item.name} remote={item.isReal} />
          <span className="art-watermark">
            {item.isReal
              ? 'REFERÊNCIA VISUAL'
              : 'ILUSTRAÇÃO DEMONSTRATIVA'}
          </span>
        </div>
        <div className="case-info">
          <span
            className={`eyebrow ${item.category === 'custom' ? 'purple-text' : ''}`}
          >
            {categoryLabels[item.category]}
          </span>
          <h3>{item.name}</h3>
          <p>
            {item.category === 'official'
              ? item.isReal
                ? 'Conteúdo real da fonte pública ByMykel.'
                : 'Prévia do catálogo. Conteúdo em preparação.'
              : 'Coleção temática fictícia criada pelo CS Bet.'}
          </p>
          <span className="case-cta">
            Ver detalhes <Box size={15} aria-hidden="true" />
          </span>
        </div>
      </Link>
    </article>
  );
}
