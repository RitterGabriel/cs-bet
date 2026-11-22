import { useEffect, useRef } from 'react';
import { Coins, Info, X } from 'lucide-react';
import { formatCredits } from '../services/validation';

export function CreditsModal({
  balance,
  onClose,
}: {
  balance: number;
  onClose: () => void;
}) {
  const dialog = useRef<HTMLDialogElement>(null);
  useEffect(() => {
    const element = dialog.current;
    const previous = document.activeElement as HTMLElement | null;
    element?.showModal();
    return () => {
      element?.close();
      previous?.focus();
    };
  }, []);
  return (
    <dialog
      ref={dialog}
      className="credits-modal"
      aria-labelledby="credits-title"
      onCancel={(event) => {
        event.preventDefault();
        onClose();
      }}
      onClick={(event) => {
        if (event.target === event.currentTarget) onClose();
      }}
    >
      <div className="modal-content">
        <button
          className="icon-button close-button"
          onClick={onClose}
          aria-label="Fechar créditos fictícios"
          autoFocus
        >
          <X />
        </button>
        <span className="feature-icon">
          <Coins size={28} />
        </span>
        <h2 id="credits-title">Créditos fictícios</h2>
        <p className="muted">Seu saldo atual na simulação</p>
        <strong className="balance-large">
          {formatCredits(balance)} <small>créditos</small>
        </strong>
        <div className="info-box">
          <Info size={20} />
          <p>
            Os créditos são gratuitos, exclusivos para simulação e não possuem
            valor monetário. Não podem ser comprados, sacados ou convertidos em
            prêmios.
          </p>
        </div>
        <div className="coming-soon">Adição de créditos em breve</div>
      </div>
    </dialog>
  );
}
