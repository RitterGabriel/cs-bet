import { useEffect, useRef, useState } from 'react';
import { X } from 'lucide-react';
import {
  fetchMarketHistory,
  formatBRL,
  marketSourceLabel,
} from '../services/market';
import { ApiError } from '../services/api';
import { formatSimulatedCredits } from '../services/pricing';
import type { HistoryPoint } from '../services/pricing';

export function PriceHistoryDialog({
  name,
  onClose,
  variantId,
}: {
  variantId: number;
  name: string;
  onClose: () => void;
}) {
  const dialog = useRef<HTMLDialogElement>(null);
  const [points, setPoints] = useState<HistoryPoint[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [retry, setRetry] = useState(0);
  useEffect(() => {
    const element = dialog.current;
    const previous = document.activeElement as HTMLElement | null;
    element?.showModal();
    return () => {
      element?.close();
      previous?.focus();
    };
  }, []);
  useEffect(() => {
    let active = true;
    setLoading(true);
    setError('');
    const load = fetchMarketHistory(variantId).then((content) => ({
      content: content.map((point) => ({
        date: point.quotedAt,
        amount: point.amount,
        eventType: 'MARKET' as const,
        source: point.source,
        provider: point.provider,
      })),
    }));
    load
      .then((result) => {
        if (active) setPoints([...result.content].reverse());
      })
      .catch((failure) => {
        if (active)
          setError(
            failure instanceof ApiError
              ? failure.message
              : 'Histórico indisponível.',
          );
      })
      .finally(() => {
        if (active) setLoading(false);
      });
    return () => {
      active = false;
    };
  }, [variantId, retry]);
  return (
    <dialog
      ref={dialog}
      className="credits-modal price-history-dialog"
      aria-labelledby="price-history-title"
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
          aria-label="Fechar histórico de preços"
          autoFocus
        >
          <X />
        </button>
        <span className="eyebrow">SIMULAÇÃO EDUCACIONAL</span>
        <h2 id="price-history-title">Histórico de referência em R$</h2>
        <p>{name}</p>
        <p className="muted">
          Referências por variante, fonte e data. Market.CSGO: menor anúncio em
          USD convertido pela PTAX venda do Banco Central; não é preço da Steam.
          Até 30 observações recentes.
        </p>
        {loading ? (
          <p role="status">Carregando histórico...</p>
        ) : error ? (
          <div role="alert">
            <p>{error}</p>
            <button
              className="button secondary"
              onClick={() => setRetry((value) => value + 1)}
            >
              Tentar novamente
            </button>
          </div>
        ) : points.length === 0 ? (
          <p>Nenhuma cotação confiável registrada.</p>
        ) : (
          <PriceHistoryChart points={points} brl />
        )}
      </div>
    </dialog>
  );
}

export function PriceHistoryChart({
  points,
  brl = false,
}: {
  points: HistoryPoint[];
  brl?: boolean;
}) {
  // Conversão somente para coordenadas do desenho, sem cálculos de preços/valoração.
  const values = points.map((point) => Number(point.amount));
  const min = Math.min(...values),
    max = Math.max(...values);
  const coords = points.map((_, index) => ({
    x:
      points.length === 1
        ? 300
        : 20 +
          ((Date.parse(points[index].date) - Date.parse(points[0].date)) *
            560) /
            Math.max(
              1,
              Date.parse(points[points.length - 1].date) -
                Date.parse(points[0].date),
            ),
    y: 155 - (max === min ? 0.5 : (values[index] - min) / (max - min)) * 130,
  }));
  return (
    <>
      <p className="muted">
        Escala ajustada aos valores do período; consulte os preços exatos
        abaixo.
      </p>
      <svg
        className="price-chart"
        viewBox="0 0 600 180"
        role="img"
        aria-label={
          brl
            ? 'Evolução das cotações de referência; valores exatos na tabela abaixo'
            : 'Evolução dos preços simulados; valores exatos na tabela abaixo'
        }
      >
        <path d="M20 165H580" stroke="#52637a" fill="none" />
        {points.length > 1 && (
          <polyline
            points={coords.map((point) => `${point.x},${point.y}`).join(' ')}
            fill="none"
            stroke="#00D4FF"
            strokeWidth="3"
          />
        )}
        {coords.map((point, index) => (
          <circle
            key={points[index].date}
            cx={point.x}
            cy={point.y}
            r="4"
            fill="#00D4FF"
          />
        ))}
      </svg>
      <div className="price-history-table">
        <table>
          <caption>
            {brl
              ? 'Cotações de referência em R$'
              : 'Histórico em créditos simulados'}
          </caption>
          <thead>
            <tr>
              <th scope="col">Data</th>
              <th scope="col">{brl ? 'Referência' : 'Preço fictício'}</th>
              {brl && <th scope="col">Fonte</th>}
            </tr>
          </thead>
          <tbody>
            {points.map((point) => (
              <tr key={point.date}>
                <td>
                  {brl
                    ? new Intl.DateTimeFormat('pt-BR', {
                        dateStyle: 'short',
                        timeStyle: 'short',
                      }).format(new Date(point.date))
                    : point.date.split('-').reverse().join('/')}
                </td>
                <td>
                  {brl
                    ? formatBRL(point.amount)
                    : formatSimulatedCredits(point.amount)}
                </td>
                {brl && (
                  <td>
                    {marketSourceLabel(point.source)} · {point.provider}
                  </td>
                )}
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </>
  );
}
