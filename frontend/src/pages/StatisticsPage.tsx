import { useEffect, useState } from 'react';
import { Link } from 'react-router';
import { ArrowDownLeft, ArrowUpRight, Box, Coins, History, LoaderCircle, LockKeyhole, Package, RefreshCw, ShieldCheck, Tag } from 'lucide-react';
import { ProbabilityEducation } from '../components/ProbabilityEducation';
import { useAuth } from '../contexts/AuthContext';
import { ApiError } from '../services/api';
import { activityLabels, formatPersonalAmount, getPersonalStatistics } from '../services/statistics';
import type { ActivityType, PersonalStatistics } from '../services/statistics';

export function StatisticsPage() {
  const { user, isLoading: sessionLoading } = useAuth();
  const [loaded, setLoaded] = useState<{ ownerId: number; data: PersonalStatistics } | null>(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');
  const [page, setPage] = useState(0);
  const [type, setType] = useState<ActivityType>('ALL');
  const [reload, setReload] = useState(0);
  const ownerId = user?.id;
  const data = loaded?.ownerId === ownerId ? loaded?.data : null;

  useEffect(() => { setPage(0); setType('ALL'); }, [ownerId]);
  useEffect(() => {
    let active = true;
    setLoaded(null);
    setError('');
    if (!ownerId || sessionLoading) { setLoading(false); return; }
    setLoading(true);
    getPersonalStatistics(ownerId, page, type).then(result => {
      if (active) setLoaded({ ownerId, data: result });
    }).catch(failure => {
      if (active) setError(failure instanceof ApiError && failure.status === 401
        ? 'Sua sessão expirou ou mudou. Atualize a página para entrar novamente.'
        : 'Não foi possível carregar seu histórico. Tente novamente.');
    }).finally(() => { if (active) setLoading(false); });
    return () => { active = false; };
  }, [ownerId, sessionLoading, page, type, reload]);

  const s = data?.summary;
  return <div className="container page-content personal-statistics-page">
    <ProbabilityEducation />
    <div className="page-heading personal-statistics-heading">
      <span className="eyebrow">SUA JORNADA EM NÚMEROS</span>
      <h1>Minhas <em>estatísticas.</em></h1>
      <p>Seu saldo, inventário, vendas e movimentações. Um histórico da sua experiência no CS Bet.</p>
    </div>
    {sessionLoading ? <p role="status">Verificando sessão...</p> : !user ? (
      <section className="panel statistics-signin"><LockKeyhole size={28} /><h2>Um painel só seu</h2><p>Entre para consultar suas estatísticas e o histórico da sua conta.</p><Link to="/login" state={{ from: '/statistics' }} className="button primary">Acessar minha conta</Link></section>
    ) : <>
      <div className="statistics-toolbar"><span><ShieldCheck size={16} /> Valores fictícios. Sem dinheiro real.</span>
        <button className="button secondary" disabled={loading} onClick={() => setReload(value => value + 1)}><RefreshCw size={16} className={loading ? 'spin' : ''} /> Atualizar dados</button>
      </div>
      {loading && <p role="status" className="statistics-loading"><LoaderCircle size={18} className="spin" /> Carregando suas estatísticas e movimentações...</p>}
      {error && <div className="statistics-error" role="alert"><p>{error}</p><button className="button secondary" onClick={() => setReload(value => value + 1)}>Tentar novamente</button></div>}
      {data && s && <>
        <div className="personal-metrics-grid">
          <section className="panel personal-metric"><Coins size={21} /><span>Saldo virtual atual</span><strong>{formatPersonalAmount(s.balance)}</strong><small>Créditos disponíveis na carteira</small></section>
          <section className="panel personal-metric"><ArrowDownLeft size={21} /><span>Créditos adicionados</span><strong>{formatPersonalAmount(s.creditsAdded)}</strong><small>{s.walletMovements.toLocaleString('pt-BR')} {s.walletMovements === 1 ? 'movimentação' : 'movimentações'} na carteira</small></section>
          <section className="panel personal-metric"><Package size={21} /><span>Valor do inventário</span><strong>{formatPersonalAmount(s.inventoryEstimatedValue)}</strong><small>{s.availableItems.toLocaleString('pt-BR')} {s.availableItems === 1 ? 'item disponível' : 'itens disponíveis'}{s.unpricedAvailableItems > 0 ? ` · ${s.unpricedAvailableItems} sem cotação` : ' · Referência de mercado'}</small></section>
          <section className="panel personal-metric"><Tag size={21} /><span>Vendas fictícias registradas</span><strong>{s.soldItems.toLocaleString('pt-BR')}</strong><small>{s.unvaluedSales > 0 ? `${s.unvaluedSales} ${s.unvaluedSales === 1 ? 'venda' : 'vendas'} sem valor registrado` : `Total: ${formatPersonalAmount(s.salesTotal)}`}</small></section>
          <section className="panel personal-metric metric-unavailable"><Box size={21} /><span>Caixas abertas</span><strong>{s.openingHistoryAvailable && s.openedCases !== null ? s.openedCases.toLocaleString('pt-BR') : 'Não disponível'}</strong><small>{s.openingHistoryAvailable ? 'Aberturas registradas' : 'Ainda não existe registro de aberturas'}</small></section>
          <section className="panel personal-metric metric-unavailable"><ArrowUpRight size={21} /><span>Créditos gastos em caixas</span><strong>{s.openingHistoryAvailable ? formatPersonalAmount(s.openingSpend) : 'Não disponível'}</strong><small>{s.openingHistoryAvailable ? 'Valores fictícios efetivamente registrados' : 'Sem histórico de débitos por abertura'}</small></section>
        </div>
        {!s.openingHistoryAvailable && <p className="statistics-data-note">Aberturas com consumo de créditos ainda não estão implementadas. Por isso, caixas abertas e gastos não têm valores disponíveis. Créditos adicionados e itens recebidos não são contados como gastos ou aberturas.</p>}
        <section className="panel personal-activity" aria-labelledby="activity-title">
          <div className="personal-activity-heading"><div><span className="eyebrow">TUDO EM UM SÓ LUGAR</span><h2 id="activity-title"><History size={21} /> Histórico da conta</h2></div>
            <label>Tipo de movimentação<select value={type} onChange={event => { setType(event.target.value as ActivityType); setPage(0); }}>
              <option value="ALL">Todas as movimentações</option><option value="CREDIT_ADDITION">Créditos adicionados</option><option value="CASE_OPENING">Aberturas de caixas</option><option value="ITEM_OBTAINED">Itens recebidos</option><option value="ITEM_SOLD">Vendas fictícias</option>
            </select></label>
          </div>
          <p className="muted statistics-history-help">Registros da sua conta, do mais recente para o mais antigo. O valor de uma venda é o registrado na operação, não a cotação atual.</p>
          {data.activity.content.length === 0 ? <div className="statistics-empty"><History size={28} /><h3>Nenhuma movimentação encontrada</h3><p>{type === 'ALL' ? 'Quando houver registros na sua conta, eles aparecerão aqui.' : 'Ainda não há registros deste tipo na sua conta.'}</p></div> : <>
            <p className="statistics-scroll-hint">Deslize a tabela para ver todos os detalhes.</p>
            <div className="statistics-table-scroll"><table className="statistics-table"><caption className="sr-only">Histórico pessoal de créditos, itens recebidos e vendas</caption>
              <thead><tr><th scope="col">Data</th><th scope="col">Movimentação</th><th scope="col">Item</th><th scope="col">Valor fictício</th><th scope="col">Saldo após</th></tr></thead>
              <tbody>{data.activity.content.map(event => <tr key={event.id}>
                <td>{event.occurredAt === null ? 'Data não registrada' : new Date(event.occurredAt).toLocaleString('pt-BR')}</td>
                <th scope="row"><span className={`statistics-event-label event-${event.type.toLowerCase()}`}>{activityLabels[event.type]}</span></th>
                <td>{event.itemName ?? '—'}</td><td>{event.amount !== null ? formatPersonalAmount(event.amount) : event.type === 'ITEM_SOLD' ? 'Valor não registrado' : '—'}</td>
                <td>{event.resultingBalance === null ? '—' : formatPersonalAmount(event.resultingBalance)}</td>
              </tr>)}</tbody>
            </table></div>
          </>}
          <div className="statistics-pagination"><span>{data.activity.totalElements.toLocaleString('pt-BR')} registros{data.activity.totalPages > 0 ? ` · Página ${data.activity.page + 1} de ${data.activity.totalPages}` : ''}</span>
            {data.activity.totalPages > 1 && <div><button className="button secondary" disabled={page === 0 || loading} onClick={() => setPage(value => value - 1)}>Anterior</button><button className="button secondary" disabled={page + 1 >= data.activity.totalPages || loading} onClick={() => setPage(value => value + 1)}>Próxima</button></div>}
          </div>
        </section>
        <aside className="statistics-reference-note"><ShieldCheck size={19} /><p>O inventário usa cotações de referência para estimar seu valor. Essa estimativa não é saldo da carteira nem representa ganho ou perda. Todos os créditos e itens são fictícios, sem valor monetário real.</p></aside>
      </>}
    </>}
  </div>;
}
