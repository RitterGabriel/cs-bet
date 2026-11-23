import { beforeEach, afterEach, describe, expect, it, vi } from 'vitest';
import { getCatalogCase, getCustomCase, listCustomCases, mapCustomCase } from './catalog';
import { openingSkin, openingSpecial } from '../test/openingFixture';
const fetchMock=vi.fn();
const box={id:'custom-all-win',name:'All Win',description:'99% básico, 1% faca',imageUrl:'/images/custom-all-win.svg',accent:'#e4b34a',openingPrice:'3.00',available:true,itemCount:2,ruleVersion:'CSBET_CUSTOM_V1'};
const item=(special=false)=>{const skin=special?openingSpecial:openingSkin;return {id:skin.id,name:skin.name,weapon:'Test',weaponType:special?'knife':'rifle',specialCategory:special?'knife':'none',imageUrl:skin.image,rarity:skin.rarityData,special,customChance:special?'1.00':'99.00',marketPrices:skin.marketPrices};};
beforeEach(()=>{vi.stubGlobal('fetch',fetchMock);vi.stubGlobal('window',{setTimeout,clearTimeout});});
afterEach(()=>{vi.resetAllMocks();vi.unstubAllGlobals();});
describe('catálogo personalizado publicado pelo backend',()=>{
 it('usa a arte própria sem alterar preço, disponibilidade ou regras do servidor',()=>{
  expect(mapCustomCase(box)).toMatchObject({image:'/images/custom/all-win-v2.webp',customPrice:'3.00',customReady:true,sourceRevision:'CSBET_CUSTOM_V1'});
  expect(mapCustomCase({...box,id:'custom-future',imageUrl:'/images/future.svg'}).image).toBe('/images/future.svg');
 });
 it('consulta a API própria com busca, preço e paginação sem inventar conteúdos locais',async()=>{
  fetchMock.mockResolvedValueOnce(Response.json({content:[box],page:1,size:12,totalElements:14,totalPages:2}));
  const result=await listCustomCases(1,'All','price_asc');expect(fetchMock.mock.calls[0][0]).toBe('/api/custom-cases?page=1&search=All&sort=price_asc');
  expect(result.content[0]).toMatchObject({category:'custom',customPrice:'3.00',customReady:true,isReal:false});
 });
 it('mantém chances próprias separadas das regras oficiais e exibe os itens especiais concretos',async()=>{
  fetchMock.mockResolvedValueOnce(Response.json({box,items:[item(),item(true)],notice:'Regras CS Bet'}));
  const result=await getCatalogCase('custom-all-win');
  expect(result.items[0].customChance).toBe('99.00');expect(result.specialItems[0].customChance).toBe('1.00');
  expect(result.items[0].dropChance).toBeNull();expect(result.specialReward).toBeUndefined();
  expect(fetchMock.mock.calls[0][0]).toBe('/api/custom-cases/custom-all-win');
 });
 it('rejeita soma de chances inválida em uma caixa marcada como disponível',async()=>{
  fetchMock.mockResolvedValueOnce(Response.json({box,items:[{...item(),customChance:'98.00'},item(true)]}));
  await expect(getCustomCase('custom-all-win')).rejects.toThrow('incompletas');
 });
});
