import { describe, expect, it } from 'vitest';
import { renderToStaticMarkup } from 'react-dom/server';
import { MarketPriceNotice, MARKET_PRICE_NOTICE } from './MarketPriceNotice';
import { MarketPricePanel } from './MarketPricePanel';
import { unavailableQuote, formatQuoteDate } from '../services/market';

describe('aviso de variação de preços', () => {
  it('mostra um aviso compartilhado com fonte e data, sem repetição nos cards', () => {
    const quote = {
      ...unavailableQuote,
      variantId: 1,
      source: 'MARKET_CSGO',
      quotedAt: '2026-11-22T02:54:07Z',
    };
    const html = renderToStaticMarkup(
      <>
        <MarketPriceNotice
          quotes={[quote, { ...quote, quotedAt: '2026-11-21T12:00:00Z' }]}
        />
        <MarketPricePanel name="Primeira" quotes={[]} />
        <MarketPricePanel name="Segunda" quotes={[]} />
      </>,
    );
    expect(html.split(MARKET_PRICE_NOTICE)).toHaveLength(2);
    expect(html).toContain('Market.CSGO · PTAX/BCB');
    expect(html).toContain(
      `Última cotação: ${formatQuoteDate(quote.quotedAt)}`,
    );
  });
  it('não inventa uma data quando não existem cotações', () => {
    const html = renderToStaticMarkup(<MarketPriceNotice quotes={[]} />);
    expect(html).toContain(MARKET_PRICE_NOTICE);
    expect(html).not.toContain('Última cotação:');
  });
});
