import { useEffect, useState } from 'react';
import { Crosshair } from 'lucide-react';
import type { CatalogCase } from '../types/catalog';
import { CatalogImage } from './CatalogImage';

export function CatalogHeroShowcase({ items, custom, loading = false }: {
  items: CatalogCase[];
  custom: boolean;
  loading?: boolean;
}) {
  const [index, setIndex] = useState(0);
  const currentIndex = items.length ? index % items.length : 0;
  const featured = items[currentIndex];
  const previewItems = items.length > 2
    ? [-1, 0, 1].map(offset => items[(currentIndex + offset + items.length) % items.length])
    : items;

  useEffect(() => { setIndex(0); }, [items]);
  useEffect(() => {
    if (items.length < 2) return;
    const timer = window.setInterval(() => setIndex(current => (current + 1) % items.length), 5000);
    return () => window.clearInterval(timer);
  }, [items]);

  return (
    <div className={`hero-visual hero-showcase${custom ? '' : ' hero-official-art'}`} role="region" aria-roledescription="carrossel" aria-label="Caixas em destaque">
      <div className="showcase-heading">
        <span><Crosshair size={14} /> {custom ? 'CRIAÇÕES CS BET' : 'VITRINE OFICIAL'}</span>
        <span>{featured ? `${String(currentIndex + 1).padStart(2, '0')} / ${String(items.length).padStart(2, '0')}` : 'CS2'}</span>
      </div>
      {featured ? <>
        <div className="showcase-stage">
          <div className="showcase-halo" aria-hidden="true" />
          <span className="showcase-corner corner-start" aria-hidden="true" />
          <span className="showcase-corner corner-end" aria-hidden="true" />
          <CatalogImage key={featured.id} src={featured.image} alt={featured.name} remote={!custom} />
        </div>
        <div className="hero-image-caption"><span>{featured.name}</span><small>{custom ? 'COLEÇÃO DEMONSTRATIVA' : 'COUNTER-STRIKE 2'}</small></div>
        {previewItems.length > 1 && <div className="showcase-preview-strip" style={{ gridTemplateColumns: `repeat(${previewItems.length}, minmax(0, 1fr))` }} aria-hidden="true">
          {previewItems.map(item => <div key={item.id} className={`showcase-preview${item.id === featured.id ? ' is-current' : ''}`}>
            <CatalogImage src={item.image} alt="" remote={!custom} /><span>{item.name}</span>
          </div>)}
        </div>}
        {items.length > 1 && <div key={featured.id} className="showcase-progress" aria-hidden="true"><span /></div>}
      </> : <div className="hero-showcase-empty" role="status"><Crosshair size={32} /><p>{loading ? 'Preparando a vitrine...' : 'Nenhuma caixa disponível nesta seleção.'}</p></div>}
    </div>
  );
}
