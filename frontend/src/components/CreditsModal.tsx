import { useEffect, useRef } from 'react';
import { Coins, Info, X } from 'lucide-react';
import { formatVirtualBalance } from '../services/market';
import { SimulationNotice } from './SimulationNotice';

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
          aria-label="Fechar saldo virtual"
          autoFocus
        >
          <X />
        </button>
        <span className="feature-icon">
          <Coins size={28} />
        </span>
        <h2 id="credits-title">Saldo virtual</h2>
        <p className="muted">Seu saldo atual na simulação</p>
        <strong className="balance-large">
          {formatVirtualBalance(balance)}
        </strong>
        <div className="info-box">
          <Info size={20} />
          <SimulationNotice />
        </div>
        <div className="coming-soon">Adição de saldo em breve</div>
      </div>
    </dialog>
  );
}
