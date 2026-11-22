import { useState } from 'react';
import {
  formatBRL,
  variantPriceRange,
  quoteProvenance,
  formatQuoteDate,
  marketSourceLabel,
  quoteAvailable,
  unavailableQuote,
  wearLabels,
} from '../services/market';
import type { MarketQuote } from '../services/market';
import { PriceHistoryDialog } from './PriceHistoryDialog';

export function MarketPriceLabel({
  quote = unavailableQuote,
}: {
  quote?: MarketQuote;
}) {
  return (
    <span className="simulated-price">
      <strong>
        {quoteAvailable(quote)
          ? formatBRL(quote.currentPrice!)
          : 'Preço indisponível'}
      </strong>
    </span>
  );
}
export function MarketPricePanel({
  quotes,
  name,
  fixedVariant = false,
  itemType = 'skin',
}: {
  quotes: MarketQuote[];
  name: string;
  fixedVariant?: boolean;
  itemType?: 'skin' | 'case';
}) {
  const [selected, setSelected] = useState('');
  const [history, setHistory] = useState(false);
  const isCase = itemType === 'case';
  const hasWearVariants = quotes.some(
    (item) => item.wear != null && item.wear !== 'NONE',
  );
  const quote =
    fixedVariant || isCase
      ? (quotes[0] ?? unavailableQuote)
      : (quotes.find((item) => String(item.variantId) === selected) ??
        quotes.find(
          (item) => item.wear === 'NONE' && !item.statTrak && !item.souvenir,
        ) ??
        unavailableQuote);
  const range =
    !isCase && !fixedVariant && quote.variantId === null
      ? variantPriceRange(quotes)
      : null;
  const provenance = range ? quoteProvenance(range.quotes) : null;
  return (
    <div className="market-price-panel">
      {!isCase && fixedVariant && quote.wear && (
        <small className="quote-origin">
          {wearLabels[quote.wear]}
          {quote.statTrak ? ' · StatTrak' : ''}
          {quote.souvenir ? ' · Souvenir' : ''}
        </small>
      )}
      {!isCase && !fixedVariant && hasWearVariants && (
        <label className="market-wear-label">
          Variante de referência
          <select
            aria-label={`Variante de referência de ${name}`}
            value={selected}
            onChange={(event) => {
              setSelected(event.target.value);
              setHistory(false);
            }}
          >
            <option value="">Selecione o desgaste</option>
            {quotes.map((item) => (
              <option key={item.variantId} value={String(item.variantId)}>
                {item.wear ? wearLabels[item.wear] : 'Não informado'}
                {item.statTrak ? ' · StatTrak' : ''}
                {item.souvenir ? ' · Souvenir' : ''}
                {' · '}
                {quoteAvailable(item)
                  ? formatBRL(item.currentPrice!)
                  : 'Preço indisponível'}
              </option>
            ))}
          </select>
        </label>
      )}
      {range ? (
        <div className="market-variant-range">
          <small>Referências entre variantes</small>
          <strong>
            {formatBRL(range.minimum)}
            {range.minimum !== range.maximum
              ? ` – ${formatBRL(range.maximum)}`
              : ''}
          </strong>
          <small>Selecione uma variante para ver sua cotação exata.</small>
          {provenance && (
            <small className="quote-origin">
              {provenance.sources} · {formatQuoteDate(provenance.latest)}
            </small>
          )}
        </div>
      ) : (
        <MarketPriceLabel quote={quote} />
      )}
      {quote.quotedAt && (
        <small className="quote-origin">
          {marketSourceLabel(quote.source)} · {formatQuoteDate(quote.quotedAt)}
          {quote.status === 'STALE' ? ' · Desatualizada' : ''}
        </small>
      )}
      {quote.variantId !== null && (
        <button
          className="price-history-button"
          onClick={() => setHistory(true)}
        >
          Histórico da referência
        </button>
      )}
      {history && quote.variantId !== null && (
        <PriceHistoryDialog
          variantId={quote.variantId}
          name={name}
          onClose={() => setHistory(false)}
        />
      )}
    </div>
  );
}
