import { useEffect, useMemo, useRef } from 'react';
import type { CSSProperties } from 'react';
import { CatalogImage } from '../CatalogImage';
import { Star } from 'lucide-react';
import { openingOffset, openingPreviewItems } from '../../utils/openingReel';
import type { CaseItem } from '../../types/catalog';
import type { OpeningPhase, OpeningReelPlan } from '../../types/opening';

interface Props {
  items: CaseItem[];
  hasSpecialGroup?: boolean;
  reel: OpeningReelPlan | null;
  phase: OpeningPhase;
  runId: number;
  failedImages: Set<string>;
  onComplete: (runId: number) => void;
}

export function OpeningRoulette({ items, hasSpecialGroup = false, reel, phase, runId, failedImages, onComplete }: Props) {
  const viewport = useRef<HTMLDivElement>(null);
  const track = useRef<HTMLDivElement>(null);
  const winner = useRef<HTMLDivElement>(null);
  const onCompleteRef = useRef(onComplete);
  onCompleteRef.current = onComplete;
  const preview = useMemo(() => openingPreviewItems(items, hasSpecialGroup), [items, hasSpecialGroup]);
  const cards = reel?.cards ?? preview;

  useEffect(() => {
    const windowElement = viewport.current;
    const rail = track.current;
    const selected = winner.current;
    if (!windowElement || !rail) return;
    if (!reel || !selected) { rail.style.transform = 'translateX(0px)'; return; }
    const offsetFor = (card: HTMLElement) => {
      const railRect = rail.getBoundingClientRect();
      const cardRect = card.getBoundingClientRect();
      return openingOffset(windowElement.getBoundingClientRect().width - 2 * (windowElement.clientLeft || 0), cardRect.left - railRect.left, cardRect.width);
    };
    const endTransform = () => `translateX(${offsetFor(selected)}px)`;
    if (phase === 'revealed' || phase === 'complete') {
      const align = () => { rail.style.transform = endTransform(); };
      align();
      if (typeof ResizeObserver === 'undefined') return;
      const observer = new ResizeObserver(align);
      observer.observe(windowElement);
      return () => observer.disconnect();
    }
    const first = rail.children[Math.min(4, cards.length - 1)] as HTMLElement | undefined;
    const start = first ? `translateX(${offsetFor(first)}px)` : 'translateX(0px)';
    rail.style.transform = start;
    if (phase !== 'animating') return;

    let active = true;
    let completed = false;
    let animation: Animation | undefined;
    let observer: ResizeObserver | undefined;
    const preference = window.matchMedia?.('(prefers-reduced-motion: reduce)');
    const finish = () => {
      if (!active || completed) return;
      completed = true;
      rail.style.transform = endTransform();
      animation?.cancel();
      onCompleteRef.current(runId);
    };
    const onMotionChange = () => { if (preference?.matches) finish(); };
    if (preference?.matches || typeof rail.animate !== 'function') {
      finish();
    } else {
      animation = rail.animate([{ transform: start }, { transform: endTransform() }], {
        duration: 5200, easing: 'cubic-bezier(0.08, 0.78, 0.12, 1)', fill: 'forwards',
      });
      animation.finished.then(finish, () => { /* Cancelamento por pular, trocar de caixa ou desmontar. */ });
      let width = windowElement.getBoundingClientRect().width;
      if (typeof ResizeObserver !== 'undefined') {
        observer = new ResizeObserver(() => {
          const nextWidth = windowElement.getBoundingClientRect().width;
          if (Math.abs(nextWidth - width) > .5) { width = nextWidth; finish(); }
        });
        observer.observe(windowElement);
      }
      preference?.addEventListener?.('change', onMotionChange);
    }
    return () => {
      active = false;
      observer?.disconnect();
      preference?.removeEventListener?.('change', onMotionChange);
      animation?.cancel();
    };
  }, [reel, phase, runId, cards.length]);

  return <div className={`opening-viewport phase-${phase}`} ref={viewport} aria-hidden="true">
    <div className="opening-center-line" />
    <span className="opening-marker marker-top" /><span className="opening-marker marker-bottom" />
    <div className="opening-track" ref={track}>
      {cards.map((item, index) => <div
        key={`${runId}-${index}`}
        ref={reel && index === reel.winningIndex ? winner : undefined}
        data-opening-item={item.id}
        data-opening-winner={reel && index === reel.winningIndex ? 'true' : undefined}
        className={`opening-reel-card${item.decorativeSpecial ? ' is-visual-special' : ''}`}
        style={{ '--reel-rarity': item.rarityData?.color ?? '#4b69ff' } as CSSProperties}
      >
        {item.decorativeSpecial ? <div className="opening-special-symbol"><Star size={50} strokeWidth={1.4} /></div> : <CatalogImage src={failedImages.has(item.image) ? '' : item.image} alt="" remote item loading="eager" />}
        <span>{item.rarityData?.name ?? item.rarity}</span><strong>{item.name}</strong>
      </div>)}
    </div>
    {!cards.length && <span className="opening-placeholder">Conteúdo indisponível</span>}
  </div>;
}
