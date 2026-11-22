import { formatSimulatedCredits } from '../services/pricing';
import type { SimulatedPrice } from '../services/pricing';

export function SimulatedPriceLabel({
  price,
}: {
  price?: SimulatedPrice | null;
}) {
  return (
    <span className="simulated-price">
      {price ? (
        <>
          <strong>{formatSimulatedCredits(price.currentPrice)}</strong> créditos
          simulados
        </>
      ) : (
        'Preço simulado em preparação'
      )}
    </span>
  );
}
