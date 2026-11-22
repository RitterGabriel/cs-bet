import { useEffect, useId, useRef } from 'react';
import { X } from 'lucide-react';
import type { CaseItem } from '../types/catalog';
import { SkinGrid } from './SkinGrid';

export function SpecialItemsModal({
  items,
  caseName,
  onClose,
}: {
  items: CaseItem[];
  caseName: string;
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
      className="credits-modal special-items-modal"
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
          aria-label="Fechar itens especiais"
          onClick={onClose}
          autoFocus
        >
          <X />
        </button>
        <span className="eyebrow">GRUPO DOURADO</span>
        <h2 id={title}>Itens especiais — {caseName}</h2>
        <p className="muted">
          Opções especiais desta caixa. Na simulação, todas as opções
          cadastradas têm a mesma chance dentro do grupo dourado.
        </p>
        {items.length ? (
          <SkinGrid
            items={items.map((item) =>
              item.phase
                ? { ...item, name: `${item.name} · ${item.phase}` }
                : item,
            )}
          />
        ) : (
          <p>Nenhum item especial disponível para esta caixa.</p>
        )}
      </div>
    </dialog>
  );
}
