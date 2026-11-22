import { describe, expect, it } from 'vitest';
import { renderToStaticMarkup } from 'react-dom/server';
import { MemoryRouter } from 'react-router';
import { App } from './App';
import { AuthProvider } from './contexts/AuthContext';
import { CaseDetailsPage } from './pages/CaseDetailsPage';
import { cases } from './data/cases';

const renderRoute = (path: string) =>
  renderToStaticMarkup(
    <MemoryRouter initialEntries={[path]}>
      <AuthProvider>
        <App />
      </AuthProvider>
    </MemoryRouter>,
  );

describe('imports e rotas da aplicação', () => {
  it('carrega o export nomeado e renderiza detalhes de uma caixa local', () => {
    expect(typeof CaseDetailsPage).toBe('function');
    const item = cases.find((entry) => entry.category === 'custom')!;
    const html = renderRoute(`/cases/${item.id}`);
    expect(html).toContain(item.name);
    expect(html).not.toContain('Página não encontrada');
  });

  it('expõe Mercado na navegação e mantém a rota pública', () => {
    const html = renderRoute('/market');
    expect(html).toContain('href="/market"');
    expect(html).toContain('Carregando mercado...');
    expect(html).toContain('menor cotação válida');
    expect(html).not.toContain('Histórico simulado anterior');
  });
  it('mantém a rota de caixas oficiais e seu estado de carregamento', () => {
    expect(renderRoute('/cases/crate-fever')).toContain('Carregando caixa...');
  });
});
