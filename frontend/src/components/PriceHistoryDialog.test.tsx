import { describe, expect, it } from 'vitest';
import { renderToStaticMarkup } from 'react-dom/server';
import { PriceHistoryChart } from './PriceHistoryDialog';
import { SimulatedPriceLabel } from './SimulatedPriceLabel';

describe('histórico e identificação dos créditos', () => {
  it('exibe gráfico com tabela acessível dos valores exatos', () => {
    const html = renderToStaticMarkup(
      <PriceHistoryChart
        points={[
          { date: '2026-11-21', amount: '100.00', eventType: 'INITIAL' },
          { date: '2026-11-22', amount: '101.25', eventType: 'DAILY' },
        ]}
      />,
    );
    expect(html).toContain('role="img"');
    expect(html).toContain('Histórico em créditos simulados');
    expect(html).toContain('101,25');
    expect(html).toContain('22/11/2026');
  });
  it('não inventa preço quando ainda não inicializado', () => {
    const html = renderToStaticMarkup(<SimulatedPriceLabel price={null} />);
    expect(html).toContain('em preparação');
    expect(html).not.toContain('0,00');
  });
});
