import { describe, expect, it, vi } from 'vitest';
import { renderToStaticMarkup } from 'react-dom/server';
import { MemoryRouter, Route, Routes } from 'react-router';
import { ProtectedRoute } from './ProtectedRoute';

const state = vi.hoisted(() => ({ user: null as null | { id: number } }));
vi.mock('../contexts/AuthContext', () => ({ useAuth: () => state }));
const render = () =>
  renderToStaticMarkup(
    <MemoryRouter initialEntries={['/inventory']}>
      <Routes>
        <Route element={<ProtectedRoute />}>
          <Route path="inventory" element={<p>Inventário privado</p>} />
        </Route>
        <Route path="login" element={<p>Login</p>} />
      </Routes>
    </MemoryRouter>,
  );

describe('rota pessoal protegida', () => {
  it('não renderiza inventário sem sessão', () => {
    state.user = null;
    expect(render()).not.toContain('Inventário privado');
  });
  it('renderiza inventário quando autenticado', () => {
    state.user = { id: 1 };
    expect(render()).toContain('Inventário privado');
  });
});
