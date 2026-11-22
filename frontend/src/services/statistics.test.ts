import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { formatPersonalAmount, getPersonalStatistics, validatePersonalStatistics } from './statistics';
import { statisticsFixture } from '../test/statisticsFixture';
const fetchMock = vi.fn();
const owner = { id: 1, username: 'owner', email: 'owner@example.invalid', balance: 900, createdAt: '2026-11-22T12:00:00Z' };
beforeEach(() => {
  vi.stubGlobal('fetch', fetchMock);
  vi.stubGlobal('window', { setTimeout, clearTimeout });
});
afterEach(() => { vi.unstubAllGlobals(); vi.resetAllMocks(); });
function session() {
  fetchMock.mockResolvedValueOnce(Response.json({ accessToken: 'token', tokenType: 'Bearer', expiresIn: 3600 }));
  fetchMock.mockResolvedValueOnce(Response.json(owner));
}
describe('estatísticas pessoais', () => {
  it('usa o JWT e envia filtros sem aceitar identidade da carteira no endpoint', async () => {
    session(); fetchMock.mockResolvedValueOnce(Response.json(statisticsFixture));
    await expect(getPersonalStatistics(1, 0, 'ALL')).resolves.toEqual(statisticsFixture);
    const [url, options] = fetchMock.mock.calls[2];
    expect(url).toBe('/api/statistics/me?page=0&size=20&type=ALL');
    expect(url).not.toContain('userId');
    expect(options.headers.Authorization).toBe('Bearer token');
    expect(options.method).toBeUndefined();
    expect(fetchMock.mock.calls.map(call => call[0])).toEqual(['/api/auth/session', '/api/users/me', url]);
  });
  it('não consulta dados quando o cookie pertence a outra conta', async () => {
    session();
    await expect(getPersonalStatistics(2, 0, 'ALL')).rejects.toMatchObject({ status: 401 });
    expect(fetchMock).toHaveBeenCalledTimes(2);
  });
  it('propaga sessão expirada e falhas HTTP sem apresentar totais fictícios', async () => {
    fetchMock.mockResolvedValueOnce(new Response(null, { status: 401 }));
    await expect(getPersonalStatistics(1, 0, 'ALL')).rejects.toMatchObject({ status: 401 });
    session(); fetchMock.mockResolvedValueOnce(new Response(null, { status: 500 }));
    await expect(getPersonalStatistics(1, 0, 'ALL')).rejects.toMatchObject({ status: 500 });
  });
  it('preserva ausência de dados em vez de preencher gastos e aberturas com zero', () => {
    const result = validatePersonalStatistics(statisticsFixture);
    expect(result.summary.openedCases).toBeNull();
    expect(result.summary.openingSpend).toBeNull();
    expect(formatPersonalAmount(null)).toBe('Não disponível');
    expect(formatPersonalAmount('0.00')).toBe('R$\u00a00,00');
    expect(formatPersonalAmount('9999999999999999.99')).toBe('R$\u00a09.999.999.999.999.999,99');
    expect(formatPersonalAmount('1000')).toBe('R$\u00a01.000,00');
  });
  it('rejeita resposta incompleta e páginas diferentes da solicitação', async () => {
    expect(() => validatePersonalStatistics({ ...statisticsFixture, summary: { ...statisticsFixture.summary, openingSpend: '100.00' } })).toThrow();
    expect(() => validatePersonalStatistics({ ...statisticsFixture, activity: { ...statisticsFixture.activity, totalPages: 9 } })).toThrow();
    session(); fetchMock.mockResolvedValueOnce(Response.json(statisticsFixture));
    await expect(getPersonalStatistics(1, 1, 'ALL')).rejects.toThrow('página');
  });
});
