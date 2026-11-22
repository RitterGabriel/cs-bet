import { Hand, Layers3, Sword, Users, Zap } from 'lucide-react';
import type { MarketCategoryOption } from '../services/marketCatalog';

function CategoryIcon({ category }: { category: string }) {
  if (category === 'agents') return <Users size={18} aria-hidden="true" />;
  if (category === 'knives') return <Sword size={18} aria-hidden="true" />;
  if (category === 'gloves') return <Hand size={18} aria-hidden="true" />;
  if (category === 'equipment') return <Zap size={18} aria-hidden="true" />;
  const paths: Record<string, string> = {
    rifles: 'M2 9h5l2-2h5v2h8v2h-8l-2 5h-3l1-5H7l-3 3H2V9ZM10 7V5h5v2',
    pistols: 'M4 6h15v5h-6l-2 8H6l2-8H4V6ZM14 11v4h-3',
    smgs: 'M2 8h15v2h5v2h-8v6h-3v-6H7l-3 4H2V8ZM8 8V5h5v3',
    heavy: 'M2 8h14v2h6v2h-9l-1 7H8l1-7H5l-3 3V8ZM6 8V5h3v3M16 12v5',
  };
  const path = paths[category];
  return path ? (
    <svg
      width="18"
      height="18"
      viewBox="0 0 24 24"
      fill="none"
      stroke="currentColor"
      strokeWidth="1.7"
      strokeLinecap="round"
      strokeLinejoin="round"
      aria-hidden="true"
    >
      <path d={path} />
    </svg>
  ) : (
    <Layers3 size={18} aria-hidden="true" />
  );
}

export function MarketCategoryFilters({
  categories,
  selected,
  onChange,
}: {
  categories: MarketCategoryOption[];
  selected: string;
  onChange: (category: string) => void;
}) {
  return (
    <fieldset className="market-categories">
      <legend>Categoria</legend>
      <div className="market-category-chips">
        {[{ id: '', name: 'Todas', weapons: [] }, ...categories].map(
          (category) => (
            <button
              key={category.id}
              type="button"
              className="market-category-chip"
              aria-pressed={selected === category.id}
              onClick={() => {
                if (selected !== category.id) onChange(category.id);
              }}
            >
              <CategoryIcon category={category.id} />
              {category.name}
            </button>
          ),
        )}
      </div>
    </fieldset>
  );
}
