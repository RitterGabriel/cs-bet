import { Link } from 'react-router';
import { Crosshair } from 'lucide-react';
export function NotFoundPage() {
  return (
    <div className="container not-found">
      <Crosshair size={54} />
      <span className="eyebrow">ERRO 404</span>
      <h1>Fora do mapa.</h1>
      <p>
        Esta página ou caixa não foi encontrada. Que tal explorar o catálogo?
      </p>
      <Link className="button primary" to="/">
        Voltar ao catálogo
      </Link>
    </div>
  );
}
