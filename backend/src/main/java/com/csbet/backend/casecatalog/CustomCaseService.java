package com.csbet.backend.casecatalog;
import com.csbet.backend.market.MarketDtos;
import java.math.BigDecimal;
import java.security.SecureRandom;
import java.time.Clock;
import java.util.*;
import java.util.random.RandomGenerator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
@Service
@Transactional(readOnly=true)
public class CustomCaseService {
    private final CustomCaseRepository definitions;
    private final CustomCaseEntryRepository entries;
    private final Clock clock;
    private final RandomGenerator random;
    @Autowired public CustomCaseService(CustomCaseRepository definitions,CustomCaseEntryRepository entries,Clock pricingClock) {
        this(definitions,entries,pricingClock,new SecureRandom());
    }
    CustomCaseService(CustomCaseRepository definitions,CustomCaseEntryRepository entries,Clock clock,RandomGenerator random) {
        this.definitions=definitions;this.entries=entries;this.clock=clock;this.random=random;
    }
    public record Prepared(CustomCaseDefinition definition,List<CustomCaseEntry> entries) { }
    public Prepared prepare(String id) {
        var definition=definitions.findByBoxExternalId(id).orElseThrow(CatalogNotFoundException::new);
        var pool=entries.findContents(definition.getCaseId());
        if(!configured(definition,pool))throw new CustomCaseUnavailableException("Esta caixa personalizada está indisponível. Nenhum crédito foi consumido.");
        return new Prepared(definition,List.copyOf(pool));
    }
    public CustomCaseEntry draw(Prepared prepared){return select(prepared.entries(),random.nextInt(10000));}
    static CustomCaseEntry select(List<CustomCaseEntry> entries,int ticket) {
        if(ticket<0||ticket>=10000)throw new IllegalArgumentException("Ticket inválido.");
        int cumulative=0;
        for(var entry:entries){cumulative+=entry.getWeight();if(ticket<cumulative)return entry;}
        throw new CustomCaseUnavailableException("Chances personalizadas incompletas.");
    }
    private boolean configured(CustomCaseDefinition definition,List<CustomCaseEntry> pool) {
        if(!"CSBET".equals(definition.getBox().getSource())||!"custom".equals(definition.getBox().getCategory())
                ||definition.getCreditPrice().signum()<=0||pool.isEmpty()||pool.stream().mapToLong(CustomCaseEntry::getWeight).sum()!=10000
                ||pool.stream().map(e->e.getSkin().getId()).distinct().count()!=pool.size())return false;
        return pool.stream().allMatch(e->e.getWeight()>0&&!"agent".equals(e.getSkin().getWeaponType())&&!e.getVariants().isEmpty()
                &&e.getVariants().stream().allMatch(v->Objects.equals(v.getSkinId(),e.getSkin().getId())&&!v.isSouvenir()
                    &&!("glove".equals(e.getSkin().getSpecialCategory())&&v.isStatTrak())));
    }
    private CustomCaseDtos.Summary summary(CustomCaseDefinition definition,List<CustomCaseEntry> pool) {
        var box=definition.getBox();return new CustomCaseDtos.Summary(box.getExternalId(),box.getName(),box.getDescription(),box.getImageUrl(),definition.getAccent(),
                definition.getCreditPrice().setScale(2),configured(definition,pool),pool.size(),definition.getRuleVersion());
    }
    public CustomCaseDtos.Detail detail(String id) {
        var definition=definitions.findByBoxExternalId(id).orElseThrow(CatalogNotFoundException::new);
        var pool=entries.findContents(definition.getCaseId());var now=clock.instant();
        return new CustomCaseDtos.Detail(summary(definition,pool),pool.stream().map(e->{
            var skin=e.getSkin();var rarity=skin.getRarity();
            return new CustomCaseDtos.Item(skin.getExternalId(),skin.getName(),skin.getWeapon(),skin.getWeaponType(),skin.getSpecialCategory(),skin.getImageUrl(),
                    new CatalogDtos.RarityResponse(rarity.getId(),rarity.getName(),rarity.getColor()),e.isSpecial(),BigDecimal.valueOf(e.getWeight(),2),
                    e.getVariants().stream().map(v->MarketDtos.QuoteView.from(v,now)).toList());
        }).toList(),"Chances e preço fictício definidos pelo CS Bet, não pela Valve. Variantes restritas às opções exibidas nesta caixa. O valor da skin pode ser inferior ao custo da abertura.");
    }
    public CustomCaseDtos.Page list(int page,String search,String sort) {
        if(page<0||page>100000||search.length()>150||!Set.of("featured","name","name_desc","price_asc","price_desc").contains(sort))throw new InvalidCatalogRequestException();
        var normalized=search.strip().toLowerCase(Locale.ROOT);
        var result=definitions.findAllByOrderByCaseIdAsc().stream().filter(d->d.getBox().getName().toLowerCase(Locale.ROOT).contains(normalized))
                .map(d->summary(d,entries.findContents(d.getCaseId()))).toList();
        Comparator<CustomCaseDtos.Summary> ordering=switch(sort){
            case "name"->Comparator.comparing(CustomCaseDtos.Summary::name,String.CASE_INSENSITIVE_ORDER);
            case "name_desc"->Comparator.comparing(CustomCaseDtos.Summary::name,String.CASE_INSENSITIVE_ORDER).reversed();
            case "price_asc"->Comparator.comparing(CustomCaseDtos.Summary::openingPrice);
            case "price_desc"->Comparator.comparing(CustomCaseDtos.Summary::openingPrice).reversed();
            default->Comparator.comparing(d->d.id().equals("custom-all-win")?0:1);
        };
        result=result.stream().sorted(ordering.thenComparing(CustomCaseDtos.Summary::id)).toList();int size=12;
        int start=(int)Math.min((long)page*size,result.size());int end=Math.min(start+size,result.size());
        return new CustomCaseDtos.Page(result.subList(start,end),page,size,result.size(),(result.size()+size-1)/size);
    }
}
