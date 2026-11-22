import type { CatalogCase } from '../types/catalog';

export const cases: CatalogCase[] = [
  {
    id: 'revolution',
    name: 'Revolution Case',
    category: 'official',
    theme: 'REVOLUTION',
    accent: '#c7a0ff',
    image: '/images/revolution.svg',
    description:
      'Caixa oficial do CS2. Esta prévia usa uma ilustração demonstrativa; os itens e as probabilidades ainda não foram cadastrados.',
    items: [],
    specialItems: [],
  },
  {
    id: 'fracture',
    name: 'Fracture Case',
    category: 'official',
    theme: 'FRACTURE',
    accent: '#ffbb65',
    image: '/images/fracture.svg',
    description:
      'Caixa oficial do CS2. Explore a prévia visual enquanto preparamos o cadastro do seu conteúdo real. A imagem é ilustrativa.',
    items: [],
    specialItems: [],
  },
  {
    id: 'dreams-nightmares',
    name: 'Dreams & Nightmares Case',
    category: 'official',
    theme: 'DREAMS & NIGHTMARES',
    accent: '#65e6d3',
    image: '/images/dreams-nightmares.svg',
    description:
      'Caixa oficial do CS2 apresentada com uma ilustração de demonstração. O conteúdo oficial ainda não está disponível nesta prévia.',
    items: [],
    specialItems: [],
  },
  {
    id: 'ak47',
    name: 'Coleção AK-47',
    category: 'custom',
    theme: 'AK-47 COLLECTION',
    accent: '#ff946f',
    image: '/images/ak47.svg',
    description:
      'Uma coleção temática fictícia do CS Bet dedicada à AK-47. Uma ideia de curadoria para explorar o universo dos itens cosméticos.',
    items: [],
    specialItems: [],
  },
  {
    id: 'awp',
    name: 'Coleção AWP',
    category: 'custom',
    theme: 'AWP COLLECTION',
    accent: '#8eb8ff',
    image: '/images/awp.svg',
    description:
      'Precisão como inspiração. Esta coleção temática é uma criação fictícia do CS Bet, com conteúdo ainda em preparação.',
    items: [],
    specialItems: [],
  },
  {
    id: 'neon',
    name: 'Coleção Neon',
    category: 'custom',
    theme: 'NEON COLLECTION',
    accent: '#d49aff',
    image: '/images/neon.svg',
    description:
      'Cores elétricas e contrastes intensos inspiram esta coleção fictícia do CS Bet. Os itens ainda serão cadastrados.',
    items: [],
    specialItems: [],
  },
  {
    id: 'classics',
    name: 'Coleção Clássicos',
    category: 'custom',
    theme: 'CLASSICS COLLECTION',
    accent: '#d8c598',
    image: '/images/classics.svg',
    description:
      'Uma homenagem visual aos clássicos. Coleção fictícia criada para o CS Bet, sem vínculo com uma coleção oficial do jogo.',
    items: [],
    specialItems: [],
  },
];

export const categoryLabels = {
  official: 'Oficial CS2',
  custom: 'Criação CS Bet',
} as const;
