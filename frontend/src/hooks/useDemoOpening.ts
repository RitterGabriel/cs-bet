import { useCallback, useEffect, useRef, useState } from 'react';
import { ApiError } from '../services/api';
import { demoOpeningAvailable, requestOpeningBatch } from '../services/demoOpening';
import type { OpeningQuantity } from '../services/demoOpening';
import { buildOpeningReel, prepareOpeningImages } from '../utils/openingReel';
import type { CatalogCase } from '../types/catalog';
import type { DemoOpeningState } from '../types/opening';

const initialState = (): DemoOpeningState => ({ phase: 'idle', runId: 0, result: null, reel: null,
  results: [], reels: [], failedImages: new Set(), error: '', authError: false });

export function useDemoOpening(box: CatalogCase, userId: number | undefined, quantity: OpeningQuantity = 1) {
  const [state, setState] = useState<DemoOpeningState>(initialState);
  const current = useRef(state);
  const generation = useRef(0);
  const busy = useRef(false);
  const finished = useRef(new Set<number>());
  const commit = useCallback((next: DemoOpeningState) => { current.current = next; setState(next); }, []);

  useEffect(() => {
    generation.current++;
    busy.current = false;
    commit(initialState());
    return () => { generation.current++; busy.current = false; };
  }, [box.id, userId, quantity, commit]);

  const start = useCallback(async () => {
    if (busy.current || !userId || !demoOpeningAvailable(box)) return;
    busy.current = true;
    finished.current.clear();
    const attempt = ++generation.current;
    commit({ ...initialState(), runId: attempt, phase: 'requesting' });
    try {
      const outcome = await requestOpeningBatch(box, userId, quantity);
      if (attempt !== generation.current) return;
      if (outcome.status === 'UNAVAILABLE') {
        busy.current = false;
        commit({ ...initialState(), runId: attempt, phase: 'unavailable' });
        return;
      }
      const results = outcome.results;
      const reels = results.map(result => buildOpeningReel(box.items.length ? box.items : box.specialItems, result.item, box.specialItems.length > 0));
      const [reel] = reels;
      commit({ ...initialState(), runId: attempt, phase: 'preparing', result: results[0], reel, results, reels });
      const failedImages = await prepareOpeningImages(reels.flatMap(reel => reel.cards));
      if (attempt !== generation.current) return;
      // Pular durante o preparo mantém o mesmo resultado; falhas de imagem ainda recebem a reserva local.
      commit({ ...current.current, failedImages,
        phase: current.current.phase === 'preparing' ? 'animating' : current.current.phase });
    } catch (failure) {
      if (attempt !== generation.current) return;
      busy.current = false;
      const status = failure instanceof ApiError ? failure.status : 0;
      commit({ ...initialState(), runId: attempt, phase: 'error', authError: status === 401,
        error: status === 401 ? 'Sua sessão expirou ou mudou. Atualize a página para entrar novamente.'
          : status === 403 ? 'Você não tem permissão para experimentar esta caixa.'
          : status === 409 && failure instanceof ApiError ? failure.message
          : status === 404 ? 'Esta caixa não está mais disponível.'
          : 'Não foi possível confirmar a abertura. Tente novamente para recuperar o mesmo resultado, sem cobrança duplicada.' });
    }
  }, [box, userId, quantity, commit]);

  const finish = useCallback((runId: number, index = 0, skipAll = false) => {
    const active = current.current;
    if (runId !== generation.current || active.runId !== runId || !active.result || !['preparing', 'animating'].includes(active.phase)) return;
    finished.current.add(index);
    if (!skipAll && finished.current.size < active.results.length) return;
    busy.current = false;
    commit({ ...active, phase: 'revealed' });
  }, [commit]);
  const skip = useCallback(() => finish(current.current.runId, 0, true), [finish]);
  const closeResult = useCallback(() => {
    if (current.current.phase === 'revealed') commit({ ...current.current, phase: 'complete' });
  }, [commit]);
  return { state, start, finish, skip, closeResult };
}
