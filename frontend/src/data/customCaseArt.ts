// A apresentação pode evoluir sem alterar as regras publicadas das caixas.
export const customCaseArt: Readonly<Record<string, string>> = Object.fromEntries(
  [
    'eco-rush', 'all-win', 'pistol-round', 'ak-arsenal', 'awp-scope',
    'stattrak-lab', 'neon-vibes', 'knife-hunt', 'print-lab', 'red-collection',
    'glove-lab', 'blade-club', 'cyan-luxury', 'collectors-vault',
    'midnight', 'toxic-bloom', 'mecha-core', 'crimson-relay',
  ].map(slug => [`custom-${slug}`, `/images/custom/${slug}-v2.webp`]),
);
