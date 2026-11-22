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
  const featured = items.length ? items[index % items.length] : undefined;

  useEffect(() => { setIndex(0); }, [items]);
  useEffect(() => {
    if (items.length < 2) return;
    const timer = window.setInterval(() => setIndex(current => (current + 1) % items.length), 5000);
    return () => window.clearInterval(timer);
  }, [items]);

  return (
    <div
      className={`hero-visual hero-showcase${custom ? '' : ' hero-official-art'}`}
      role="region"
      aria-roledescription="carrossel"
      aria-label="Caixas em destaque"

    >
      <div className="orbit orbit-one" aria-hidden="true" />
      <div className="orbit orbit-two" aria-hidden="true" />
      <span className="visual-cross cross-one" aria-hidden="true">+</span>
      <span className="visual-cross cross-two" aria-hidden="true">+</span>
      <div className="hero-visual-label" aria-hidden="true">
        <Crosshair size={15} /> {custom ? 'CURADORIA CS BET' : 'EXPLORE O UNIVERSO CS2'}
      </div>
      {featured ? (
        <>
          <CatalogImage key={featured.id} src={featured.image} alt={featured.name} remote={!custom} />
          <div className="hero-image-caption">
            <span>{featured.name}</span>
            <small>{custom ? 'ILUSTRAÇÃO DEMONSTRATIVA' : 'COUNTER-STRIKE 2'}</small>
          </div>
        </>
      ) : (
        <p className="hero-showcase-empty" role="status">
          {loading ? 'Carregando caixas...' : 'Nenhuma caixa disponível nesta seleção.'}
        </p>
      )}

    </div>
  );
}
