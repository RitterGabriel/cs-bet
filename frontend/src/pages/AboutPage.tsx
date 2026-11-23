import { ArrowUpRight, BookOpen, Box, Code2, Github, GraduationCap, Linkedin, ShieldCheck } from 'lucide-react';
import { Link } from 'react-router';
import { authorLinks } from '../data/author';
import '../styles/information.css';

export function AboutPage() {
  return <div className="container page-content about-page">
    <header className="about-hero">
      <div className="about-heading">
        <span className="eyebrow">EXPLORE. DESCUBRA. APRENDA.</span>
        <h1>Um espaço para explorar.<br /><em>Uma experiência para aprender.</em></h1>
        <p>O CS Bet é um simulador educacional de caixas de Counter-Strike 2. A ideia é conhecer os itens do jogo, experimentar uma abertura virtual e entender o que existe por trás dos resultados.</p>
        <Link to="/official" className="button primary"><Box size={17} /> Explorar as caixas <ArrowUpRight size={17} /></Link>
      </div>
      <div className="about-identity" aria-hidden="true"><span className="about-orbit" /><Box size={92} strokeWidth={1} /><span>CS<span>BET</span></span><small>SIMULAÇÃO · CONHECIMENTO · DESCOBERTA</small></div>
    </header>
    <div className="about-principles">
      <article><span className="feature-icon"><GraduationCap size={23} /></span><h2>Aprender com a experiência</h2><p>Observar probabilidades, comparar resultados e perceber como o acaso varia entre tentativas. Uma chance pequena continua pequena, mesmo depois de várias aberturas.</p></article>
      <article><span className="feature-icon purple"><BookOpen size={23} /></span><h2>Explorar o universo CS2</h2><p>Conhecer caixas, skins, raridades e variantes. As cotações servem como referência de mercado para a simulação e podem mudar com o tempo.</p></article>
      <article><span className="feature-icon"><Code2 size={23} /></span><h2>Construir para aprender</h2><p>O projeto também é um laboratório de desenvolvimento de software, construído com React, TypeScript, Java, Spring Boot e Oracle.</p></article>
    </div>
    <section className="panel about-purpose" aria-labelledby="about-purpose-title">
      <ShieldCheck size={27} />
      <div><h2 id="about-purpose-title">Tudo acontece no ambiente virtual.</h2><p>Você pode adicionar créditos fictícios, abrir caixas, montar um inventário e vender skins virtuais para continuar explorando a simulação. O histórico ajuda a acompanhar o que aconteceu e quanto foi usado em cada experiência.</p><p>Créditos fictícios para simulação. Não representam dinheiro real e não podem ser sacados. Os itens não podem ser transferidos para o CS2 ou negociados fora do site. Não há pagamentos, saques ou prêmios resgatáveis.</p></div>
    </section>
    <section className="about-author" aria-labelledby="about-author-title">
      <div><span className="eyebrow">POR TRÁS DO PROJETO</span><h2 id="about-author-title">Vamos nos conectar?</h2><p>Acompanhe o desenvolvimento e conheça outros projetos do criador do CS Bet.</p></div>
      <div className="about-social-links">
        <a className="button secondary" href={authorLinks.github} target="_blank" rel="noopener noreferrer"><Github size={18} /> GitHub <ArrowUpRight size={15} /></a>
        {authorLinks.linkedin && <a className="button secondary" href={authorLinks.linkedin} target="_blank" rel="noopener noreferrer"><Linkedin size={18} /> LinkedIn <ArrowUpRight size={15} /></a>}
      </div>
    </section>
    <p className="about-attribution">Projeto independente, sem afiliação à Valve ou ao Counter-Strike.</p>
  </div>;
}
