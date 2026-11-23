import { useEffect, useRef, useState } from 'react';
import { Link, useLocation } from 'react-router';
import { Box, LoaderCircle, LockKeyhole, Play, SkipForward } from 'lucide-react';
import { useAuth } from '../../contexts/AuthContext';
import { useDemoOpening } from '../../hooks/useDemoOpening';
import { DEMO_OPENING_NOTICE, demoOpeningAvailable, openingQuantities, pendingOpeningQuantity } from '../../services/demoOpening';
import type { OpeningQuantity } from '../../services/demoOpening';
import { OpeningRoulette } from './OpeningRoulette';
import { OpeningResult } from './OpeningResult';
import { formatBRL, quoteAvailable } from '../../services/market';
import type { CatalogCase } from '../../types/catalog';
import '../../styles/opening.css';

export function DemoOpeningPanel({ box }: { box: CatalogCase }) {
  const { user, isLoading: sessionLoading, refreshUser, getInventory } = useAuth();
  const location = useLocation();
  const openingButton = useRef<HTMLButtonElement>(null);
  const [quantity, setQuantity] = useState<OpeningQuantity>(1);
  const { state, start, finish, skip } = useDemoOpening(box, sessionLoading ? undefined : user?.id, quantity);
  const pendingQuantity = pendingOpeningQuantity(box.id, user?.id);
  useEffect(() => { if (pendingQuantity !== null) setQuantity(pendingQuantity); }, [pendingQuantity]);
  const synced = useRef<number | undefined>(undefined);
  useEffect(() => {
    if (!state.result?.inventoryId || synced.current === state.result.inventoryId) return;
    synced.current = state.result.inventoryId;
    void Promise.allSettled([refreshUser(), getInventory(0)]);
  }, [state.result, refreshUser, getInventory]);
  const price = box.customReady ? box.customPrice ?? null : box.marketPrice && quoteAvailable(box.marketPrice) ? box.marketPrice.currentPrice : null;
  const totalPrice = price ? (() => {
    const cents = BigInt(price.replace('.', '')) * BigInt(quantity);
    return `${cents / 100n}.${(cents % 100n).toString().padStart(2, '0')}`;
  })() : null;
  const insufficient = price !== null && user !== null && user.balance < Number(price) * quantity && pendingQuantity === null;
  const available = demoOpeningAvailable(box) && state.phase !== 'unavailable';
  const waiting = state.phase === 'requesting';
  const moving = state.phase === 'preparing' || state.phase === 'animating';
  const busy = waiting || moving;
  return <section className="panel demo-opening-panel" aria-label="Abertura de caixa" aria-busy={busy}>
    <div className="opening-panel-heading"><div><span className="eyebrow"><Box size={13} /> ABERTURA VIRTUAL</span><h2>{box.name}</h2></div><span className="opening-free-label">{quantity} {quantity === 1 ? 'CAIXA' : 'CAIXAS'} · CRÉDITOS FICTÍCIOS</span></div>
    <div className={`opening-roulettes${quantity === 10 ? ' is-multiple is-ten' : quantity > 1 ? ' is-multiple' : ''}`}>
      {Array.from({ length: quantity }, (_, index) => <div className="opening-lane" key={index}>
        {quantity > 1 && <span className="opening-lane-label">Caixa {index + 1}</span>}
        <OpeningRoulette items={box.items.length ? box.items : box.specialItems} hasSpecialGroup={box.specialItems.length > 0} reel={state.reels[index] ?? null} phase={state.phase} runId={state.runId} failedImages={state.failedImages} onComplete={runId => finish(runId, index)} />
      </div>)}
    </div>
    <div className="opening-panel-controls">
      <div className="opening-quantity" role="group" aria-label="Quantidade de caixas">
        {openingQuantities.map(count => <button key={count} type="button" aria-pressed={quantity === count}
          disabled={busy || (pendingQuantity !== null && pendingQuantity !== count)} onClick={() => setQuantity(count)}>{count}</button>)}
      </div>
      {sessionLoading ? <p role="status">Verificando sessão...</p> : !user ?
        <Link className="button primary" to="/login" state={{ from: location.pathname }}><LockKeyhole size={16} /> Entrar para abrir</Link> :
        <button ref={openingButton} type="button" className="button primary" disabled={busy || !available || !price || insufficient} onClick={() => { void start(); }}>
          {waiting ? <><LoaderCircle size={17} className="spin" /> Confirmando abertura...</> : moving ? <><LoaderCircle size={17} className="spin" /> {state.phase === 'preparing' ? 'Preparando imagens...' : 'Revelando resultado...'}</> : <><Play size={16} /> Abrir {quantity === 1 ? 'caixa' : `${quantity} caixas`} {price ? `· ${formatBRL(totalPrice!)}` : ''}</>}
        </button>}
      {moving && <button type="button" className="button secondary" onClick={skip}><SkipForward size={16} /> Pular animação</button>}
    </div>
    {!price && <p className="opening-unavailable" role="status">Abertura indisponível enquanto a caixa estiver sem cotação vigente.</p>}
    {insufficient && <p className="opening-unavailable" role="status">Saldo fictício insuficiente. <Link to="/wallet">Adicionar créditos</Link></p>}
    {(state.phase === 'revealed' || state.phase === 'complete') && state.results.length > 0 && <div className="opening-results">{state.results.map(result => <OpeningResult key={result.inventoryId} result={result} failedImages={state.failedImages} />)}</div>}
    <p className="opening-state-message" role="status" aria-live="polite">{waiting ? 'Validando saldo e confirmando sua abertura.' : state.phase === 'preparing' ? 'Preparando a apresentação. Você já pode pular a animação.' : state.phase === 'animating' ? 'Animação em andamento. O resultado já foi definido.' : state.phase === 'revealed' || state.phase === 'complete' ? state.results.length === 1 ? `Skin recebida: ${state.result?.item.name}.` : `${state.results.length} skins adicionadas ao inventário.` : ''}</p>
    {(!available || state.phase === 'unavailable') && <p className="opening-unavailable" role="status">Esta caixa não tem dados suficientes para uma abertura demonstrativa verificável.</p>}
    {state.error && <div className="opening-error" role="alert"><p>{state.error}</p>{state.authError && <Link to="/login" state={{ from: location.pathname }}>Entrar novamente</Link>}</div>}
    <p className="opening-educational-notice">{DEMO_OPENING_NOTICE}</p>
    <details className="opening-rules"><summary>Como funciona o sorteio</summary><p>A roleta é visual. A frequência dos cards dourados na animação não representa a chance de obter um item especial. O resultado é definido no servidor antes da animação. {box.category === 'custom' ? 'Os itens seguem as chances próprias publicadas nesta caixa, sem aplicar as chances de raridade das caixas oficiais. As variantes possíveis são apenas as exibidas no conteúdo da caixa.' : 'As raridades seguem as probabilidades documentadas; itens especiais usam a regra interna 1/N.'} Desgaste é uniforme entre os desgastes cadastrados; com ou sem StatTrak é uniforme entre as opções cadastradas para aquele desgaste. São regras internas do CS Bet, não distribuições oficiais de variantes. Não há itens Souvenir. A fase, quando existente, pertence ao item selecionado. Cotações vencidas não excluem variantes do sorteio; se faltar cotação da variante recebida, seu preço será exibido como indisponível.</p></details>
  </section>;
}
