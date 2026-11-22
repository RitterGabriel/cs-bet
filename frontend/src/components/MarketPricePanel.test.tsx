import { describe, expect, it } from 'vitest';
import { renderToStaticMarkup } from 'react-dom/server';
import { MarketPriceLabel, MarketPricePanel } from './MarketPricePanel';
import { SimulationNotice, SIMULATION_NOTICE } from './SimulationNotice';
import { PriceHistoryChart } from './PriceHistoryDialog';
import { unavailableQuote } from '../services/market';
import type { MarketQuote } from '../services/market';

const quote: MarketQuote = {
  variantId: 1,
  status: 'AVAILABLE',
  currentPrice: '12.50',
  currency: 'BRL',
  source: 'STEAM_COMMUNITY_MARKET',
  provider: 'TEST_PROVIDER',
  quotedAt: new Date().toISOString(),
  validUntil: new Date(Date.now() + 3600000).toISOString(),
  wear: 'FT',
  statTrak: false,
  souvenir: false,
  marketHashName: 'Fixture (Field-Tested)',
};
describe('interface em R$', () => {
  it('exibe indisponível sem inventar valores', () => {
    const html = renderToStaticMarkup(<MarketPriceLabel />);
    expect(html).toContain('Preço indisponível');
    expect(html).not.toContain('R$');
  });
  it('não escolhe desgaste arbitrário para uma skin genérica', () => {
    const html = renderToStaticMarkup(
      <MarketPricePanel name="Fixture" quotes={[quote]} />,
    );
    expect(html).toContain('Selecione o desgaste');
    expect(html).toContain('Referências entre variantes');
    expect(html).toContain('12,50');
    expect(html).toContain(
      'Selecione uma variante para ver sua cotação exata.',
    );
    expect(html).not.toContain('Preço indisponível');
    expect(html).not.toContain('Histórico da referência');
  });
  it('mostra uma faixa entre variantes reais sem selecionar um desgaste ou misturar o preço de uma unidade', () => {
    const variants = [
      quote,
      {
        ...quote,
        variantId: 2,
        wear: 'FN' as const,
        statTrak: true,
        currentPrice: '100.00',
      },
    ];
    const html = renderToStaticMarkup(
      <MarketPricePanel name="Fixture" quotes={variants} />,
    );
    expect(html).toContain('12,50 – R$');
    expect(html).toContain('100,00');
    expect(html).toContain('Nova de fábrica · StatTrak');
    expect(html).toContain('Mercado Steam');
    const unit = renderToStaticMarkup(
      <MarketPricePanel name="Fixture" quotes={[variants[1]]} fixedVariant />,
    );
    expect(unit).toContain('100,00');
    expect(unit).not.toContain('12,50');
    expect(unit).not.toContain('Referências entre variantes');
  });
  it('não monta faixa com cotações expiradas ou ausentes', () => {
    const html = renderToStaticMarkup(
      <MarketPricePanel
        name="Fixture"
        quotes={[{ ...quote, validUntil: '2020-01-01T00:00:00Z' }]}
      />,
    );
    expect(html).toContain('Preço indisponível');
    expect(html).not.toContain('Referências entre variantes');
  });
  it('caixas não exibem seletor e preservam seu preço de referência', () => {
    for (const value of [
      unavailableQuote,
      { ...quote, wear: 'NONE' as const },
    ]) {
      const html = renderToStaticMarkup(
        <MarketPricePanel name="Fever Case" itemType="case" quotes={[value]} />,
      );
      expect(html).not.toContain('<select');
      expect(html).not.toContain('Selecione o desgaste');
      expect(html).not.toContain('Variante de referência');
      expect(html).toContain(
        value.currentPrice ? '12,50' : 'Preço indisponível',
      );
    }
  });
  it('skins sem desgaste conhecido não exibem seletor', () => {
    for (const value of [
      unavailableQuote,
      { ...quote, wear: 'NONE' as const },
    ]) {
      const html = renderToStaticMarkup(
        <MarketPricePanel name="Fixture" quotes={[value]} />,
      );
      expect(html).not.toContain('<select');
    }
  });
  it('exibe preço e origem quando a variante da unidade é conhecida', () => {
    const html = renderToStaticMarkup(
      <MarketPricePanel name="Fixture" quotes={[quote]} fixedVariant />,
    );
    expect(html).toContain('R$');
    expect(html).toContain('12,50');
    expect(html).toContain('Mercado Steam');
    expect(html).toContain('Testada em campo');
  });
  it('identifica preço da caixa convertido de Market.CSGO, sem seletor ou rótulo Steam', () => {
    const html = renderToStaticMarkup(
      <MarketPricePanel
        name="Fever Case"
        itemType="case"
        quotes={[
          {
            ...quote,
            wear: 'NONE',
            source: 'MARKET_CSGO',
            provider: 'MARKET_CSGO_LOWEST_ASK_BCB_PTAX',
            currentPrice: '2.82',
          },
        ]}
      />,
    );
    expect(html).toContain('2,82');
    expect(html).toContain('Market.CSGO · PTAX/BCB');
    expect(html).not.toContain('Mercado Steam');
    expect(html).not.toContain('<select');
  });
  it('cotações expiradas não aparecem como preço atual', () => {
    const html = renderToStaticMarkup(
      <MarketPriceLabel
        quote={{ ...quote, validUntil: '2020-01-01T00:00:00Z' }}
      />,
    );
    expect(html).toContain('Preço indisponível');
  });
  it('mantém o aviso solicitado e distingue histórico em R$ do histórico simulado', () => {
    expect(renderToStaticMarkup(<SimulationNotice />)).toContain(
      SIMULATION_NOTICE,
    );
    const html = renderToStaticMarkup(
      <PriceHistoryChart
        brl
        points={[
          {
            date: '2026-11-21T12:00:00Z',
            amount: '12.50',
            eventType: 'MARKET',
            source: 'STEAM_COMMUNITY_MARKET',
            provider: 'TEST_PROVIDER',
          },
        ]}
      />,
    );
    expect(html).toContain('Cotações de referência em R$');
    expect(html).toContain('TEST_PROVIDER');
    expect(html).not.toContain('créditos simulados');
  });
});
