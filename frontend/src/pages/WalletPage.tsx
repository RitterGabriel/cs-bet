import { ArrowDownLeft, ArrowRight, Check, Coins, FlaskConical, Wallet } from 'lucide-react';
import { useEffect, useRef, useState } from 'react';
import type { SubmitEvent } from 'react';
import { useAuth } from '../contexts/AuthContext';
import { ApiError } from '../services/api';
import { formatWalletAmount, validCreditAmount } from '../services/wallet';
import type { WalletHistory } from '../services/wallet';

export function WalletPage() {
  const { getWalletBalance, getWalletHistory, addCredits } = useAuth();
  const [balance, setBalance] = useState<string | null>(null);
  const [history, setHistory] = useState<WalletHistory | null>(null);
  const [page, setPage] = useState(0);
  const [loading, setLoading] = useState(true);
  const [busy, setBusy] = useState(false);
  const [amount, setAmount] = useState('');
  const [error, setError] = useState('');
  const [message, setMessage] = useState('');
  const [reload, setReload] = useState(0);
  // Mantém a mesma operação em uma nova tentativa após falha de comunicação.
  const pending = useRef<{ amount: string; id: string } | null>(null);
  const submitting = useRef(false);

  useEffect(() => {
    let active = true;
    setLoading(true);
    setError('');
    setHistory(null);
    Promise.all([getWalletBalance(), getWalletHistory(page)])
      .then(([wallet, history]) => {
        if (active) {
          setBalance(wallet.balance);
          setHistory(history);
        }
      })
      .catch(() => {
        if (active) setError('Não foi possível carregar a carteira. Tente novamente.');
      })
      .finally(() => {
        if (active) setLoading(false);
      });
    return () => { active = false; };
  }, [getWalletBalance, getWalletHistory, page, reload]);

  async function submit(event: SubmitEvent<HTMLFormElement>) {
    event.preventDefault();
    if (submitting.current) return;
    setMessage('');
    setError('');
    if (!validCreditAmount(amount)) {
      setError('Informe um valor positivo de até 10.000,00 com até duas casas decimais.');
      return;
    }
    submitting.current = true;
    setBusy(true);
    const normalized = amount.replace(',', '.');
    if (!pending.current || pending.current.amount !== normalized) {
      pending.current = { amount: normalized, id: crypto.randomUUID() };
    }
    try {
      const data = await addCredits(normalized, pending.current.id);
      setBalance(data.balance);
      pending.current = null;
      setAmount('');
      setMessage('Créditos fictícios adicionados.');
      setPage(0);
      setReload(value => value + 1);
    } catch (error) {
      setError(error instanceof ApiError && error.status === 400
        ? 'Valor ou identificador inválido. Confira os dados.'
        : 'Não foi possível confirmar a adição. Tente novamente com o mesmo valor para evitar duplicidade.');
    } finally {
      submitting.current = false;
      setBusy(false);
    }
  }

  return (
    <div className="container page-content wallet-page">
      <div className="page-heading wallet-heading">
        <span className="eyebrow">SEU ESPAÇO DE SIMULAÇÃO</span>
        <h1>Seu saldo. <em>Sua experiência.</em></h1>
        <p>Escolha um valor e explore a simulação no seu ritmo.</p>
      </div>
      {error && (
        <div role="alert">
          <p>{error}</p>
          <button className="button secondary" onClick={() => setReload(value => value + 1)}>
            Tentar novamente
          </button>
        </div>
      )}
      <p role="status">{message}</p>
      <div className="wallet-grid">
        <aside className="wallet-overview">
          <section className="panel wallet-balance-card">
            <div className="wallet-card-top">
              <span className="wallet-icon"><Wallet size={23} /></span>
              <span className="wallet-badge">100% fictício</span>
            </div>
            <h2>Seu saldo virtual</h2>
            <p className="wallet-balance-amount">
              {balance === null ? 'Carregando saldo...' : formatWalletAmount(balance)}
            </p>
            <span className="wallet-balance-caption">Disponível para simular</span>
            <div className="wallet-card-footer"><Coins size={16} /> Créditos gratuitos · Sem valor monetário</div>
          </section>
          <section className="wallet-explainer">
            <span className="wallet-icon"><FlaskConical size={21} /></span>
            <div>
              <h2>Experimente antes de gastar</h2>
              <p>A ideia é adicionar o valor que você colocaria em um site de verdade e explorar essa experiência aqui, usando apenas créditos fictícios.</p>
              <p className="wallet-disclaimer">Créditos fictícios para simulação. Não representam dinheiro real e não podem ser sacados.</p>
            </div>
          </section>
        </aside>
        <section className="panel wallet-add-card">
          <div className="wallet-section-heading">
            <span className="wallet-icon"><Coins size={23} /></span>
            <div><h2>Adicionar saldo virtual</h2><p>Quanto você gostaria de simular?</p></div>
          </div>
          <form onSubmit={submit} className="wallet-form">
            <fieldset className="wallet-presets" disabled={busy}>
              <legend>Escolha um valor</legend>
              <div className="wallet-preset-grid">
                {[10, 20, 50, 100].map(value => {
                  const selected = amount !== '' && Number(amount.replace(',', '.')) === value;
                  return (
                    <button
                      key={value}
                      type="button"
                      className={`wallet-preset${selected ? ' selected' : ''}`}
                      aria-pressed={selected}
                      aria-label={`Selecionar R$ ${value} fictícios`}
                      onClick={() => setAmount(String(value))}
                    >
                      <span className="wallet-preset-check">{selected && <Check size={13} />}</span>
                      <span className="wallet-preset-currency">R$</span>
                      <strong>{value}</strong>
                      <small>créditos fictícios</small>
                    </button>
                  );
                })}
              </div>
            </fieldset>
            <div className="wallet-custom-divider"><span>ou escolha outro valor</span></div>
            <label htmlFor="credits-amount">Valor personalizado</label>
            <div className="wallet-amount-input">
              <span aria-hidden="true">R$</span>
              <input
                id="credits-amount"
                inputMode="decimal"
                placeholder="0,00"
                value={amount}
                onChange={event => setAmount(event.target.value)}
                disabled={busy}
                required
                autoComplete="off"
                aria-describedby="credits-help"
              />
            </div>
            <small id="credits-help">Até R$ 10.000,00 fictícios por adição. Use até duas casas decimais.</small>
            <button className="button primary wallet-submit" disabled={busy || loading}>
              {busy ? 'Adicionando...' : 'Adicionar créditos fictícios'}
              <ArrowRight size={18} aria-hidden="true" />
            </button>
            <p className="wallet-form-note">É gratuito. Nenhum pagamento é necessário.</p>
          </form>
        </section>
      </div>
      <WalletHistoryContent history={history} loading={loading} />
      {history && history.totalPages > 1 && (
        <div className="wallet-pagination">
          <button className="button secondary" disabled={loading || busy || page === 0} onClick={() => setPage(page - 1)}>
            Anterior
          </button>
          <span>Página {page + 1} de {history.totalPages}</span>
          <button className="button secondary" disabled={loading || busy || page + 1 >= history.totalPages} onClick={() => setPage(page + 1)}>
            Próxima
          </button>
        </div>
      )}
    </div>
  );
}

export function WalletHistoryContent({ history, loading }: {
  history: WalletHistory | null;
  loading: boolean;
}) {
  return (
    <section className="panel wallet-history">
      <div className="wallet-section-heading">
        <span className="wallet-icon"><ArrowDownLeft size={22} /></span>
        <div><h2>Histórico de movimentações</h2><p>Adições de créditos e gastos em aberturas virtuais.</p></div>
      </div>
      {loading ? <p role="status">Carregando histórico...</p> : history && (
        history.content.length === 0 ? <p>Nenhuma movimentação registrada.</p> : (
          <div className="wallet-table">
            <table>
              <thead>
                <tr><th>Data</th><th>Tipo</th><th>Valor fictício</th><th>Saldo resultante</th></tr>
              </thead>
              <tbody>
                {history.content.map(item => (
                  <tr key={item.id}>
                    <td>{new Date(item.createdAt).toLocaleString('pt-BR')}</td>
                    <td><span className="wallet-movement-type">{item.type === 'CASE_OPENING' ? 'Abertura de caixa' : item.type === 'ITEM_SALE' ? 'Venda fictícia de skin' : 'Adição de créditos'}</span></td>
                    <td>{item.type === 'CASE_OPENING' ? '− ' : '+ '}{formatWalletAmount(item.amount)}</td>
                    <td>{formatWalletAmount(item.resultingBalance)}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )
      )}
    </section>
  );
}
