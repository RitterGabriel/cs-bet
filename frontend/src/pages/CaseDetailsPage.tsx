import { useEffect, useState } from "react";
import { Link, useParams } from "react-router";
import { Box, ChevronRight, Info, Layers3 } from "lucide-react";
import type { CSSProperties } from "react";
import type { CatalogCase } from "../types/catalog";
import { cases, categoryLabels } from "../data/cases";
import { getOfficialCase } from "../services/catalog";
import { ApiError } from "../services/api";
import { CatalogImage } from "../components/CatalogImage";
import { MarketPriceNotice } from "../components/MarketPriceNotice";
import { MarketPricePanel } from "../components/MarketPricePanel";
import { OFFICIAL_ODDS_SOURCE } from "../services/officialOdds";
import { GoldSpecialCard } from "../components/GoldSpecialCard";
import { SkinGrid } from "../components/SkinGrid";
import { DemoOpeningPanel } from "../components/opening/DemoOpeningPanel";
import { NotFoundPage } from "./NotFoundPage";

export function CaseDetailsPage() {
  const { id = "" } = useParams();
  const local = cases.find(
    (entry) => entry.category === "custom" && entry.id === id,
  );
  const [remote, setRemote] = useState<CatalogCase | null>(null);
  const [loading, setLoading] = useState(!local);
  const [error, setError] = useState("");
  const [notFound, setNotFound] = useState(false);
  const [retry, setRetry] = useState(0);
  useEffect(() => {
    if (local) return;
    let active = true;
    setLoading(true);
    setError("");
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
              : "Não foi possível carregar esta caixa.",
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

export function CaseDetailsContent({ item }: { item: CatalogCase }) {
  const skins = [
    ...new Map(item.items.map((entry) => [entry.id, entry])).values(),
  ];
  return (
    <div className="container page-content">
      <nav className="breadcrumbs" aria-label="Localização">
        <Link to={item.category === "official" ? "/official" : "/custom"}>
          Catálogo
        </Link>
        <ChevronRight size={14} />
        <span>{item.name}</span>
      </nav>
      <section
        className={`case-detail-hero${item.isReal ? " case-opening-hero" : ""}`}
        style={{ "--case-accent": item.accent } as CSSProperties}
      >
        <div className="detail-art">
          <CatalogImage src={item.image} alt={item.name} remote={item.isReal} />
          <span>
            {item.isReal
              ? "IMAGEM DE REFERÊNCIA SUJEITA A DIREITOS · RESERVA ILUSTRATIVA QUANDO INDISPONÍVEL"
              : "ILUSTRAÇÃO DEMONSTRATIVA"}
          </span>
        </div>
        <div className="detail-copy">
          <span className="pill">
            {item.category === "official" ? (
              <Box size={15} />
            ) : (
              <Layers3 size={15} />
            )}
            {categoryLabels[item.category]}
          </span>
          <h1>{item.name}</h1>
          {item.isReal && (
            <MarketPricePanel
              itemType="case"
              quotes={item.marketPrice ? [item.marketPrice] : []}
              name={item.name}
            />
          )}
          {item.isReal && (
            <span className="eyebrow">
              {item.descriptionFromSource
                ? "DESCRIÇÃO DA FONTE"
                : "CATÁLOGO OFICIAL"}{" "}
              · {item.language}
            </span>
          )}
          <p>{item.description}</p>
          <div className="info-box">
            <Info size={20} />
            <p>
              {item.isReal
                ? "Abra com créditos fictícios e receba uma skin virtual no seu inventário."
                : "Coleção fictícia para consulta. Os itens ainda estão em preparação."}
            </p>
          </div>
        </div>
      </section>
      {item.isReal && <DemoOpeningPanel key={item.id} box={item} />}
      {item.isReal && (
        <MarketPriceNotice
          quotes={[
            ...(item.marketPrice ? [item.marketPrice] : []),
            ...[...item.items, ...item.specialItems].flatMap(
              (entry) => entry.marketPrices ?? [],
            ),
          ]}
        />
      )}
      <section className="panel" aria-labelledby="contents-title">
        <div className="panel-title">
          <Box size={21} />
          <h2 id="contents-title">Conteúdo da caixa</h2>
          <span className="mini-tag">
            {skins.length || item.specialItems.length
              ? `${skins.length} skins${item.specialItems.length ? " + 1 item especial" : ""}`
              : "Em preparação"}
          </span>
        </div>
        {item.isReal && (
          <p className="case-odds-note">
            Chances aproximadas calculadas pelas{" "}
            <a href={OFFICIAL_ODDS_SOURCE} target="_blank" rel="noreferrer">
              regras oficiais da Perfect World
            </a>
            , sem separar desgaste ou StatTrak. O dourado representa o grupo
            especial. Ao recebê-lo, uma seleção interna uniforme escolhe um dos
            itens especiais cadastrados na caixa — regra da simulação CS Bet.
          </p>
        )}
        {skins.length || item.specialItems.length ? (
          <SkinGrid
            items={skins}
            showDropChance={item.isReal}
            specialReward={
              item.specialItems.length > 0 ? (
                <GoldSpecialCard reward={item.specialReward} items={item.specialItems} caseName={item.name} />
              ) : undefined
            }
          />
        ) : (
          <div className="empty-state compact">
            <Box size={35} />
            <h3>Os detalhes estão a caminho</h3>
            <p>
              {item.isReal
                ? "O conteúdo regular não foi informado. Esta informação não representa um conjunto completo."
                : "Os itens desta coleção fictícia ainda estão em preparação."}
            </p>
          </div>
        )}
      </section>
      <section className="panel">
        <div className="panel-title">
          <Layers3 size={21} />
          <h2>Raridade não é probabilidade</h2>
        </div>
        <p className="muted">
          As cores identificam as categorias informadas pela fonte. A borda e a
          etiqueta de cada item preservam sua raridade original. As chances
          exibidas são aproximações calculadas conforme a fonte oficial, não
          garantias de resultado.
        </p>
        <Link className="text-link" to="/statistics">
          Conheça os conceitos estatísticos
        </Link>
      </section>
      {item.isReal && (
        <div className="catalog-source">
          <p>
            Dados:{" "}
            <a
              href="https://github.com/ByMykel/CSGO-API"
              target="_blank"
              rel="noreferrer"
            >
              ByMykel/CSGO-API
            </a>{" "}
            · MIT · idioma {item.language} · revisão{" "}
            <code>{item.sourceRevision?.slice(0, 12)}</code>
          </p>
          <p>
            {item.imageNotice} Imagens indisponíveis recebem uma ilustração
            local.
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
