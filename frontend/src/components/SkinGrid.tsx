import { useState } from 'react';
import type { CSSProperties } from 'react';
import type { CaseItem } from '../types/catalog';
import { CatalogImage } from './CatalogImage';
import { SkinDetailsModal } from './SkinDetailsModal';
import { formatBRL, variantPriceRange } from '../services/market';

export function SkinCard({
  item,
  onOpen,
}: {
  item: CaseItem;
  onOpen: () => void;
}) {
  const range = variantPriceRange(item.marketPrices ?? []);
  return (
    <article
      className={`skin-market-card rarity-${item.rarity}`}
      style={
        {
          '--item-rarity': item.rarityData?.color ?? '#8aabc8',
        } as CSSProperties
      }
    >
      <button
        className="skin-card-button"
        onClick={onOpen}
        aria-label={`Ver detalhes de ${item.name}`}
      >
        <CatalogImage src={item.image} alt={item.name} remote item />
        <div className="skin-card-copy">
          <span className="skin-rarity">
            {item.rarityData?.name ?? item.rarity}
          </span>
          <h3>{item.name}</h3>
          <span className="skin-price-range">
            {range
              ? `${formatBRL(range.minimum)}${range.minimum !== range.maximum ? ` – ${formatBRL(range.maximum)}` : ''}`
              : 'Preço indisponível'}
          </span>
          {range && <small>Referências entre variantes</small>}
        </div>
      </button>
    </article>
  );
}

export function SkinGrid({ items }: { items: CaseItem[] }) {
  const [selected, setSelected] = useState<CaseItem | null>(null);
  // Uma skin pode pertencer a mais de uma caixa; cada ID aparece uma única vez.
  const unique = [...new Map(items.map((item) => [item.id, item])).values()];
  return (
    <>
      <div className="skin-grid">
        {unique.map((item) => (
          <SkinCard
            key={item.id}
            item={item}
            onOpen={() => setSelected(item)}
          />
        ))}
      </div>
      {selected && (
        <SkinDetailsModal item={selected} onClose={() => setSelected(null)} />
      )}
    </>
  );
}
