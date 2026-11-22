export const OFFICIAL_ODDS_SOURCE =
  'https://www.csgo.com.cn/news/gamebroad/20170911/206155.shtml';
export interface OfficialDropChance {
  percent: string;
  basis: 'OFFICIAL_RULES_CALCULATED';
  scope: 'SKIN_ALL_VARIANTS';
  sourceUrl: string;
}
export interface OfficialGroupChance extends Omit<OfficialDropChance, 'scope'> {
  scope: 'SPECIAL_GROUP';
}
export interface SpecialReward {
  dropChance: OfficialGroupChance | null;
  selectionRule: 'CSBET_UNIFORM_CATALOG_ITEM';
  configured: boolean;
}
function isOfficialChance(
  value: OfficialDropChance | OfficialGroupChance | null | undefined,
): boolean {
  return (
    !!value &&
    value.basis === 'OFFICIAL_RULES_CALCULATED' &&
    value.sourceUrl === OFFICIAL_ODDS_SOURCE &&
    typeof value.percent === 'string' &&
    /^\d{1,3}(\.\d{1,8})?$/.test(value.percent) &&
    Number(value.percent) > 0 &&
    Number(value.percent) <= 100
  );
}
export function verifiedDropChance(
  value?: OfficialDropChance | null,
): OfficialDropChance | null {
  return isOfficialChance(value) && value?.scope === 'SKIN_ALL_VARIANTS'
    ? value
    : null;
}
export function verifiedSpecialReward(
  value?: SpecialReward | null,
): SpecialReward | null {
  return value?.selectionRule === 'CSBET_UNIFORM_CATALOG_ITEM' &&
    value.configured === true &&
    value.dropChance?.scope === 'SPECIAL_GROUP' &&
    isOfficialChance(value.dropChance)
    ? value
    : null;
}
export function formatDropChance(
  value: OfficialDropChance | OfficialGroupChance,
) {
  return `≈ ${new Intl.NumberFormat('pt-BR', { maximumFractionDigits: 4 }).format(Number(value.percent))}%`;
}
