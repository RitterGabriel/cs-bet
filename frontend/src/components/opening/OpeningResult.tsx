import type { CSSProperties } from 'react';
import { Check, Sparkles } from 'lucide-react';
import { Link } from 'react-router';
import { CatalogImage } from '../CatalogImage';
import { formatBRL, wearLabels } from '../../services/market';
import type { DemoOpeningResult } from '../../types/opening';

export function OpeningResult({ result, failedImages }: { result: DemoOpeningResult; failedImages: Set<string> }) {
  return <div className={`opening-result${result.special ? ' is-special' : ''}`} role="region" aria-label="Skin recebida"
    style={{ '--result-rarity': result.item.rarityData?.color ?? '#4b69ff' } as CSSProperties}>
    <div className="opening-result-art"><CatalogImage src={failedImages.has(result.item.image) ? '' : result.item.image} alt={result.item.name} remote item loading="eager" /></div>
    <div className="opening-result-copy">
      <span className="opening-obtained"><Check size={14} /> Adicionada ao seu inventário</span>
      <h3>{result.item.name}</h3>
      <p className="opening-result-rarity">{result.item.rarityData?.name ?? result.item.rarity}
        {result.special && <span className="opening-special-badge"><Sparkles size={13} /> Item especial</span>}</p>
      {'rule' in result.variant && <p className="opening-variant-note">{wearLabels[result.variant.wear]} · {result.variant.statTrak ? 'Com StatTrak™' : 'Sem StatTrak™'}{result.variant.phase ? ` · ${result.variant.phase}` : ''}</p>}
    </div>
    <div className="opening-result-value"><span>Preço de referência da variante</span><strong>{result.referencePrice ? formatBRL(result.referencePrice) : 'Indisponível'}</strong><Link to="/inventory" className="text-link">Ver no inventário →</Link></div>
  </div>;
}
