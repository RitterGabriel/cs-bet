import { Info } from 'lucide-react';
import { formatQuoteDate, quoteProvenance } from '../services/market';
import type { MarketQuote } from '../services/market';

export const MARKET_PRICE_NOTICE =
  'Os preços são referências de mercado e podem variar conforme as ofertas disponíveis e a cotação do dólar. Os valores são atualizados periodicamente e podem diferir dos preços atuais do marketplace.';

export function MarketPriceNotice({ quotes }: { quotes: MarketQuote[] }) {
  const provenance = quoteProvenance(quotes);
  return (
    <aside
      className="market-price-notice"
      aria-label="Sobre os preços de referência"
    >
      <Info size={18} aria-hidden="true" />
      <div>
        <p>{MARKET_PRICE_NOTICE}</p>
        {provenance && (
          <small>
            {provenance.sources} · Última cotação:{' '}
            {formatQuoteDate(provenance.latest)}
          </small>
        )}
      </div>
    </aside>
  );
}
