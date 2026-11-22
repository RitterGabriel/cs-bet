import { useState } from 'react';
import type { CaseItem } from '../types/catalog';
import { SpecialItemsModal } from './SpecialItemsModal';
import { Star } from 'lucide-react';
import type { SpecialReward } from '../services/officialOdds';
import {
  formatDropChance,
  verifiedSpecialReward,
} from '../services/officialOdds';

export function GoldSpecialCard({
  reward,
  items = [],
  caseName = 'Caixa',
}: {
  reward?: SpecialReward | null;
  items?: CaseItem[];
  caseName?: string;
}) {
  const [open, setOpen] = useState(false);
  const verified = verifiedSpecialReward(reward);
  return (
    <>
      <article
        className="skin-market-card gold-special-card"
        aria-label="Item especial raro"
      >
        <button
          className="skin-card-button"
          aria-label={`Ver itens especiais de ${caseName}`}
          onClick={() => setOpen(true)}
        >
          <span
            className="skin-drop-chance"
            title="Chance de obter o grupo especial; não é a chance de uma faca ou luva específica."
          >
            {verified?.dropChance
              ? formatDropChance(verified.dropChance)
              : 'Chance n/d'}
          </span>
          <div className="gold-special-symbol" aria-hidden="true">
            <Star size={66} strokeWidth={1.5} />
          </div>
          <div className="skin-card-copy">
            <span className="skin-rarity">ESPECIAL</span>
            <h3>Item especial raro</h3>
            <p>Ver itens especiais desta caixa</p>
          </div>
        </button>
      </article>
      {open && (
        <SpecialItemsModal
          items={items}
          caseName={caseName}
          onClose={() => setOpen(false)}
        />
      )}
    </>
  );
}
