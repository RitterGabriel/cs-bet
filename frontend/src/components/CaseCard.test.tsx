import { describe, expect, it } from 'vitest';
import { renderToStaticMarkup } from 'react-dom/server';
import { MemoryRouter } from 'react-router';
import { CaseCard } from './CaseCard';
import { cases } from '../data/cases';
import { unavailableQuote } from '../services/market';

describe('cards da vitrine de caixas', () => {
  it('preserva o acesso aos detalhes e usa apenas a cotação real disponível', () => {
    const item = { ...cases[0], id: 'crate-4880', isReal: true,
      marketPrice: { ...unavailableQuote, variantId: 1, status: 'AVAILABLE' as const, currentPrice: '2.82', source: 'MARKET_CSGO', provider: 'TEST',
        quotedAt: new Date().toISOString(), validUntil: new Date(Date.now() + 3600000).toISOString() } };
    const html = renderToStaticMarkup(<MemoryRouter><CaseCard item={item} /></MemoryRouter>);
    expect(html).toContain('href="/cases/crate-4880"');
    expect(html).toContain('Ver detalhes de Revolution Case');
    expect(html).toContain('2,82');
    expect(html).toContain('Referência de mercado');
    expect(html).not.toContain('Abrir caixa');
  });
  it('não inventa preços para cotações expiradas e coleções demonstrativas', () => {
    const html = renderToStaticMarkup(<MemoryRouter><CaseCard item={{ ...cases[0], isReal: true, marketPrice: unavailableQuote }} /></MemoryRouter>);
    expect(html).toContain('Preço indisponível');
    expect(html).not.toContain('R$');
    const custom = renderToStaticMarkup(<MemoryRouter><CaseCard item={cases.find(item => item.category === 'custom')!} /></MemoryRouter>);
    expect(custom).toContain('Coleção demonstrativa');
    expect(custom).not.toContain('Referência de mercado');
  });
});
