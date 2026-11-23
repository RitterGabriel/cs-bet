import { Link } from 'react-router';
import { ArrowUpRight, Box, Layers3 } from 'lucide-react';
import type { CSSProperties } from 'react';
import type { CatalogCase } from '../types/catalog';
import { CatalogImage } from './CatalogImage';
import { MarketPriceLabel } from './MarketPricePanel';
import { quoteAvailable, unavailableQuote, formatBRL } from '../services/market';
import { categoryLabels } from '../data/cases';

export function CaseCard({ item }: { item: CatalogCase }) {
  const priced = quoteAvailable(item.marketPrice ?? unavailableQuote);
  return (
    <article className="case-card" style={{ '--case-accent': item.accent } as CSSProperties}>
      <Link to={`/cases/${item.id}`} className="case-link" aria-label={`Ver detalhes de ${item.name}`}>
        <div className="case-art">
          <span className="art-label">
            {item.category === 'official' ? <Box size={12} /> : <Layers3 size={12} />}
            {item.category === 'official' ? 'CS2' : 'CS BET'}
          </span>
          <span className="case-art-marker" aria-hidden="true">+</span>
          <CatalogImage src={item.image} alt={item.name} remote={item.isReal} />
        </div>
        <div className="case-info">
          <span className="case-category-label">{categoryLabels[item.category]}</span>
          <h3>{item.name}</h3>
          <div className="case-card-bottom">
            {item.isReal ? <div className={`case-reference-price${priced ? '' : ' price-unavailable'}`}>
              <small>Referência de mercado</small><MarketPriceLabel quote={item.marketPrice} />
            </div> : item.customPrice ? <div className="case-reference-price custom-case-price"><small>Créditos fictícios por caixa</small><strong>{formatBRL(item.customPrice)}</strong>{item.customReady === false && <small>Indisponível</small>}</div> : <span className="case-concept-label">Coleção demonstrativa</span>}
            <span className="case-cta" aria-hidden="true"><ArrowUpRight size={18} /></span>
          </div>
        </div>
      </Link>
    </article>
  );
}
