import { useEffect, useId, useRef } from 'react';
import { X } from 'lucide-react';
import type { CaseItem } from '../types/catalog';
import { CatalogImage } from './CatalogImage';
import { marketTeamLabel } from '../services/marketCatalog';
import { MarketPricePanel } from './MarketPricePanel';

export function SkinDetailsModal({
  item,
  onClose,
}: {
  item: CaseItem;
  onClose: () => void;
}) {
  const dialog = useRef<HTMLDialogElement>(null);
  const title = useId();
  useEffect(() => {
    const previous = document.activeElement as HTMLElement | null;
    const element = dialog.current;
    element?.showModal();
    return () => {
      element?.close();
      previous?.focus();
    };
  }, []);
  return (
    <dialog
      ref={dialog}
      className="credits-modal skin-details-modal"
      aria-labelledby={title}
      onCancel={(event) => {
        if (event.target === event.currentTarget) {
          event.preventDefault();
          onClose();
        }
      }}
      onClick={(event) => {
        if (event.target === event.currentTarget) onClose();
      }}
    >
      <div className="modal-content">
        <button
          className="icon-button close-button"
          onClick={onClose}
          aria-label={
            item.weaponType === 'agent'
              ? 'Fechar detalhes do agente'
              : 'Fechar detalhes da skin'
          }
          autoFocus
        >
          <X />
        </button>
        <CatalogImage src={item.image} alt={item.name} remote item />
        <span className="eyebrow">REFERÊNCIAS DE MERCADO</span>
        <h2 id={title}>{item.name}</h2>
        <p className="skin-rarity" style={{ color: item.rarityData?.color }}>
          {item.rarityData?.name ?? item.rarity}
        </p>
        {item.weaponType === 'agent' && (
          <p className="muted">
            Equipe: {marketTeamLabel(item.weapon ?? '')} · Sem desgaste
          </p>
        )}
        <MarketPricePanel quotes={item.marketPrices ?? []} name={item.name} />
      </div>
    </dialog>
  );
}
