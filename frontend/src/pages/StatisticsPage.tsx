import { Link } from 'react-router';
import {
  BarChart3,
  BookOpen,
  ChartNoAxesCombined,
  CircleHelp,
  LockKeyhole,
  Scale,
  ShieldCheck,
} from 'lucide-react';
import { useAuth } from '../contexts/AuthContext';

export function StatisticsPage() {
  const { user } = useAuth();
  return (
    <div className="container page-content">
      <div className="page-heading">
        <span className="eyebrow">CONHECIMENTO MUDA A PERSPECTIVA</span>
        <h1>
          Entenda as <em>probabilidades.</em>
        </h1>
        <p>
          Explore os conceitos que ajudam a interpretar resultados. Informação
          clara faz parte da experiência.
        </p>
      </div>
      <div className="education-grid">
        <article className="panel">
          <span className="feature-icon">
            <Scale />
          </span>
          <h2>Probabilidade</h2>
          <p>
            É uma medida da chance de um evento acontecer. Um evento com
            probabilidade de 1 em 4 corresponde a 25% por tentativa.
          </p>
          <div
            className="probability-visual"
            aria-label="Exemplo didático: um entre quatro"
          >
            <span />
            <span />
            <span />
            <span />
          </div>
          <small className="muted">
            Exemplo didático. Não representa uma caixa do CS2.
          </small>
        </article>
        <article className="panel">
          <span className="feature-icon purple">
            <ChartNoAxesCombined />
          </span>
          <h2>Variação dos resultados</h2>
          <p>
            Uma probabilidade de 25% não garante um resultado a cada quatro
            tentativas. Em poucas observações, a frequência pode variar
            bastante.
          </p>
          <div className="concept-note">
            <CircleHelp size={20} />
            <span>Probabilidade não é uma promessa de resultado.</span>
          </div>
        </article>
        <article className="panel">
          <span className="feature-icon">
            <BookOpen />
          </span>
          <h2>Tentativas independentes</h2>
          <p>
            Em um modelo de tentativas independentes, um resultado anterior não
            muda a chance do próximo. Uma sequência de resultados não cria uma
            “dívida” de sorte.
          </p>
          <div className="concept-note">
            <ShieldCheck size={20} />
            <span>Entender o modelo evita interpretações equivocadas.</span>
          </div>
        </article>
      </div>
      <div className="info-box statistics-info">
        <ShieldCheck size={22} />
        <p>
          Não exibimos probabilidades oficiais sem verificação. Quando esses
          dados estiverem disponíveis, cada caixa terá suas próprias
          informações, com origem e regras transparentes.
        </p>
      </div>
      <section className="panel personal-statistics">
        <span className="feature-icon">
          <BarChart3 />
        </span>
        <div>
          <span className="eyebrow">SUA JORNADA EM NÚMEROS</span>
          <h2>Estatísticas pessoais</h2>
          <p>
            Histórico de simulações e gráficos individuais estarão disponíveis
            em uma próxima etapa.
          </p>
        </div>
        {user ? (
          <span className="coming-soon">Em breve</span>
        ) : (
          <Link
            className="button secondary"
            to="/login"
            state={{ from: '/profile' }}
          >
            <LockKeyhole size={17} /> Acessar minha conta
          </Link>
        )}
      </section>
    </div>
  );
}
