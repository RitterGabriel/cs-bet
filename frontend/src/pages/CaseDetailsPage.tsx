import { useEffect, useState } from 'react';
import { Link, useParams } from 'react-router';
import { Box, ChevronRight, Info, Layers3, Sparkles } from 'lucide-react';
import type { CSSProperties } from 'react';
import type { CatalogCase, CaseItem } from '../types/catalog';
import { cases, categoryLabels } from '../data/cases';
import { getOfficialCase } from '../services/catalog';
import { ApiError } from '../services/api';
import { CatalogImage } from '../components/CatalogImage';
import { NotFoundPage } from './NotFoundPage';

const rarityLabels = {
  industrial: 'Industrial',
  'mil-spec': 'Mil-Spec',
  restricted: 'Restrito',
  classified: 'Confidencial',
  covert: 'Oculto',
  special: 'Especial',
};
const rarityOrder = [
  'rarity_common_weapon',
  'rarity_uncommon_weapon',
  'rarity_rare_weapon',
  'rarity_mythical_weapon',
  'rarity_legendary_weapon',
  'rarity_ancient_weapon',
  'rarity_ancient',
];

export function CaseDetailsPage() {
  const { id = '' } = useParams();
  const local = cases.find(
    (entry) => entry.category === 'custom' && entry.id === id,
  );
  const [remote, setRemote] = useState<CatalogCase | null>(null);
  const [loading, setLoading] = useState(!local);
  const [error, setError] = useState('');
  const [notFound, setNotFound] = useState(false);
  const [retry, setRetry] = useState(0);
  useEffect(() => {
    if (local) return;
    let active = true;
    setLoading(true);
    setError('');
    setNotFound(false);
    getOfficialCase(id)
      .then((item) => {
        if (active) setRemote(item);
      })
      .catch((failure) => {
        if (!active) return;
        if (failure instanceof ApiError && failure.status === 404)
          setNotFound(true);
        else
          setError(
            failure instanceof ApiError
              ? failure.message
              : 'Não foi possível carregar esta caixa.',
          );
      })
      .finally(() => {
        if (active) setLoading(false);
      });
    return () => {
      active = false;
    };
  }, [id, local, retry]);
  if (local) return <CaseDetailsContent item={local} />;
  if (notFound) return <NotFoundPage />;
  if (loading)
    return (
      <div className="container page-content">
        <div className="empty-state" role="status">
          <Box size={32} />
          <h1>Carregando caixa...</h1>
        </div>
      </div>
    );
  if (error)
    return (
      <div className="container page-content">
        <div className="empty-state" role="alert">
          <h1>Detalhes indisponíveis</h1>
          <p>{error}</p>
          <button
            className="button secondary"
            onClick={() => setRetry((value) => value + 1)}
          >
            Tentar novamente
          </button>
        </div>
      </div>
    );
  return remote && remote.id === id ? (
    <CaseDetailsContent item={remote} />
  ) : (
    <NotFoundPage />
  );
}

function ItemCard({ entry, special }: { entry: CaseItem; special?: boolean }) {
  return (
    <article
      className={`item-card ${special ? 'special-item' : ''} rarity-${entry.rarity}`}
      style={
        {
          '--item-rarity': entry.rarityData?.color ?? '#8aabc8',
        } as CSSProperties
      }
    >
      <CatalogImage src={entry.image} alt={entry.name} remote item />
      <div className="item-card-copy">
        <span className="item-weapon">{entry.weapon}</span>
        <h3>{entry.name}</h3>
        <span className="item-rarity">
          {entry.rarityData?.name ?? rarityLabels[entry.rarity]}
        </span>
        {special && (
          <span className="special-label">
            {entry.specialCategory === 'knife'
              ? 'Faca especial'
              : entry.specialCategory === 'glove'
                ? 'Luvas especiais'
                : 'Item especial'}
          </span>
        )}
      </div>
    </article>
  );
}

function CaseDetailsContent({ item }: { item: CatalogCase }) {
  const grouped = [
    ...new Set(item.items.map((entry) => entry.rarityData?.id ?? entry.rarity)),
  ]
    .sort((a, b) => rarityOrder.indexOf(a) - rarityOrder.indexOf(b))
    .map((id) => ({
      id,
      entries: item.items.filter(
        (entry) => (entry.rarityData?.id ?? entry.rarity) === id,
      ),
    }));
  return (
    <div className="container page-content">
      <nav className="breadcrumbs" aria-label="Localização">
        <Link to={item.category === 'official' ? '/official' : '/custom'}>
          Catálogo
        </Link>
        <ChevronRight size={14} />
        <span>{item.name}</span>
      </nav>
      <section
        className="case-detail-hero"
        style={{ '--case-accent': item.accent } as CSSProperties}
      >
        <div className="detail-art">
          <CatalogImage src={item.image} alt={item.name} remote={item.isReal} />
          <span>
            {item.isReal
              ? 'IMAGEM DE REFERÊNCIA SUJEITA A DIREITOS · RESERVA ILUSTRATIVA QUANDO INDISPONÍVEL'
              : 'ILUSTRAÇÃO DEMONSTRATIVA'}
          </span>
        </div>
        <div className="detail-copy">
          <span className="pill">
            {item.category === 'official' ? (
              <Box size={15} />
            ) : (
              <Layers3 size={15} />
            )}
            {categoryLabels[item.category]}
          </span>
          <h1>{item.name}</h1>
          {item.isReal && (
            <span className="eyebrow">
              {item.descriptionFromSource ? 'DESCRIÇÃO DA FONTE' : 'CATÁLOGO OFICIAL'} · {item.language}
            </span>
          )}
          <p>{item.description}</p>
          <div className="info-box">
            <Info size={20} />
            <p>
              Catálogo para consulta e aprendizado. A abertura de caixas não
              está disponível.
            </p>
          </div>
        </div>
      </section>
      <section className="panel" aria-labelledby="contents-title">
        <div className="panel-title">
          <Box size={21} />
          <h2 id="contents-title">Conteúdo da caixa</h2>
          <span className="mini-tag">
            {item.items.length
              ? `${item.items.length} itens na fonte`
              : 'Em preparação'}
          </span>
        </div>
        {grouped.length ? (
          grouped.map((group) => (
            <section
              className="rarity-group"
              key={group.id}
              aria-label={
                group.entries[0].rarityData?.name ??
                rarityLabels[group.entries[0].rarity]
              }
            >
              <h3
                className={`rarity-group-title rarity-${group.entries[0].rarity}`}
              >
                {group.entries[0].rarityData?.name ??
                  rarityLabels[group.entries[0].rarity]}{' '}
                <span>{group.entries.length}</span>
              </h3>
              <div className="item-grid">
                {group.entries.map((entry) => (
                  <ItemCard key={entry.id} entry={entry} />
                ))}
              </div>
            </section>
          ))
        ) : (
          <div className="empty-state compact">
            <Box size={35} />
            <h3>Os detalhes estão a caminho</h3>
            <p>
              {item.isReal
                ? 'O conteúdo regular não foi informado. Esta informação não representa um conjunto completo.'
                : 'Os itens desta coleção fictícia ainda estão em preparação.'}
            </p>
          </div>
        )}
      </section>
      <section className="panel special-panel" aria-labelledby="special-title">
        <div className="panel-title">
          <Sparkles size={21} />
          <h2 id="special-title">Itens especiais desta caixa</h2>
          <span className="mini-tag">
            {item.specialItems.length} referências
          </span>
        </div>
        <div className="info-box">
          <Info size={20} />
          <p>
            {item.specialNotice ??
              'Não há dados de itens especiais cadastrados para esta coleção fictícia.'}
          </p>
        </div>
        {item.specialItems.length > 0 && (
          <div className="item-grid special-grid">
            {item.specialItems.map((entry) => (
              <ItemCard key={entry.id} entry={entry} special />
            ))}
          </div>
        )}
      </section>
      <section className="panel">
        <div className="panel-title">
          <Layers3 size={21} />
          <h2>Raridade não é probabilidade</h2>
        </div>
        <p className="muted">
          As cores identificam as categorias informadas pela fonte. Itens
          especiais são destacados em dourado como grupo da caixa; a raridade
          original de cada item continua indicada. Não atribuímos percentuais
          por item ou por raridade sem uma fonte verificável.
        </p>
        <Link className="text-link" to="/statistics">
          Conheça os conceitos estatísticos
        </Link>
      </section>
      {item.isReal && (
        <div className="catalog-source">
          <p>
            Dados:{' '}
            <a
              href="https://github.com/ByMykel/CSGO-API"
              target="_blank"
              rel="noreferrer"
            >
              ByMykel/CSGO-API
            </a>{' '}
            · MIT · idioma {item.language} · revisão{' '}
            <code>{item.sourceRevision?.slice(0, 12)}</code>
          </p>
          <p>
            {item.imageNotice} Imagens indisponíveis recebem uma ilustração local.
          </p>
          <p>
            Conteúdo conforme o snapshot da fonte, sem garantia de variantes
            exaustivas ou atualização em tempo real.
          </p>
        </div>
      )}
    </div>
  );
}
