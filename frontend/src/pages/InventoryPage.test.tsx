import { describe, expect, it } from 'vitest';
import { renderToStaticMarkup } from 'react-dom/server';
import { MemoryRouter } from 'react-router';
import { InventoryContent } from './InventoryPage';
import type { InventoryPageResponse } from '../types/inventory';

const empty: InventoryPageResponse = {
  content: [],
  page: 0,
  size: 12,
  totalElements: 0,
  totalPages: 0,
};
const view = (props: Partial<Parameters<typeof InventoryContent>[0]>) =>
  renderToStaticMarkup(
    <MemoryRouter>
      <InventoryContent
        data={empty}
        loading={false}
        error=""
        onRetry={() => {}}
        onPageChange={() => {}}
        {...props}
      />
    </MemoryRouter>,
  );

describe('estados do inventário', () => {
  it('apresenta carregamento sem exibir dados antigos', () => {
    expect(view({ loading: true })).toContain('Carregando inventário');
    expect(view({ loading: true })).not.toContain('ainda está vazio');
  });
  it('apresenta inventário vazio e acesso ao catálogo', () => {
    const html = view({});
    expect(html).toContain('Seu inventário ainda está vazio');
    expect(html).toContain('href="/official"');
    expect(html).not.toContain('Unidade #');
  });
  it('exibe erro com opção de tentar novamente', () => {
    const html = view({ error: 'Não foi possível conectar ao servidor.' });
    expect(html).toContain('role="alert"');
    expect(html).toContain('Tentar novamente');
    expect(html).not.toContain('ainda está vazio');
  });
  it('mostra duas unidades da mesma skin em cards distintos', () => {
    const skin = {
      skinId: 50,
      externalId: 'skin-test',
      name: 'Fixture skin',
      imageUrl: null,
      rarity: { id: 'rare', name: 'Mil-Spec', color: '#4b69ff' },
      obtainedAt: '2026-11-21T10:00:00Z',
      status: 'AVAILABLE' as const,
    };
    const html = view({
      data: {
        ...empty,
        content: [
          { ...skin, id: 1 },
          { ...skin, id: 2 },
        ],
        totalElements: 2,
        totalPages: 1,
      },
    });
    expect(html.match(/<article /g)).toHaveLength(2);
    expect(html).toContain('Unidade #1');
    expect(html).toContain('Unidade #2');
    expect(html).not.toContain('Vender');
  });
});
