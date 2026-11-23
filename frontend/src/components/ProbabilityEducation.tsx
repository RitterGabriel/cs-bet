import { BookOpen, ChartNoAxesCombined, CircleHelp, Scale, ShieldCheck } from 'lucide-react';
import '../styles/information.css';

export function ProbabilityEducation() {
  return <section className="probability-education" aria-labelledby="probability-title">
    <div className="education-heading">
      <span className="eyebrow">CONHECIMENTO MUDA A PERSPECTIVA</span>
      <h2 id="probability-title">Entenda as <em>probabilidades.</em></h2>
      <p>Explore os conceitos que ajudam a interpretar resultados. Informação clara faz parte da experiência.</p>
    </div>
    <div className="education-grid">
      <article className="panel">
        <span className="feature-icon"><Scale /></span>
        <h3>Probabilidade</h3>
        <p>É uma medida da chance de um evento acontecer. Um evento com probabilidade de 1 em 4 corresponde a 25% por tentativa.</p>
        <div className="probability-visual" role="img" aria-label="Exemplo didático: um entre quatro, ou 25%"><span /><span /><span /><span /></div>
        <small className="muted">Exemplo didático. Não representa uma caixa do CS2.</small>
      </article>
      <article className="panel">
        <span className="feature-icon purple"><ChartNoAxesCombined /></span>
        <h3>Variação dos resultados</h3>
        <p>Uma probabilidade de 25% não garante um resultado a cada quatro tentativas. Em poucas observações, a frequência pode variar bastante.</p>
        <div className="concept-note"><CircleHelp size={20} /><span>Probabilidade não é uma promessa de resultado.</span></div>
      </article>
      <article className="panel">
        <span className="feature-icon"><BookOpen /></span>
        <h3>Tentativas independentes</h3>
        <p>Em um modelo de tentativas independentes, um resultado anterior não muda a chance do próximo. Uma sequência de resultados não cria uma “dívida” de sorte.</p>
        <div className="concept-note"><ShieldCheck size={20} /><span>Entender o modelo evita interpretações equivocadas.</span></div>
      </article>
    </div>
    <div className="info-box statistics-info"><ShieldCheck size={22} /><p>Nas caixas verificadas, a chance do grupo especial segue a publicação oficial. A conversão do dourado em um item especial usa seleção uniforme própria do CS Bet, com chances iguais entre as opções especiais da caixa. Desgaste e StatTrak seguem uma regra interna de simulação, sem representar distribuições oficiais de variantes.</p></div>
  </section>;
}
