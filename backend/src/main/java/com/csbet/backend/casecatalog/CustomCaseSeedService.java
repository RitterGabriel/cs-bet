package com.csbet.backend.casecatalog;
import com.csbet.backend.market.*;
import com.csbet.backend.skin.CosmeticItemRepository;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.json.JsonMapper;
@Service
public class CustomCaseSeedService {
    private final WeaponCaseRepository cases;
    private final CustomCaseRepository definitions;
    private final CustomCaseEntryRepository entries;
    private final CosmeticItemRepository skins;
    private final MarketVariantRepository variants;
    public CustomCaseSeedService(WeaponCaseRepository cases,CustomCaseRepository definitions,CustomCaseEntryRepository entries,
            CosmeticItemRepository skins,MarketVariantRepository variants) {
        this.cases=cases;this.definitions=definitions;this.entries=entries;this.skins=skins;this.variants=variants;
    }
    @Transactional
    public int seed(CustomCaseManifest manifest) {
        for(var spec:manifest.boxes()) {
            String fingerprint=fingerprint(manifest.version(),spec);
            var existing=definitions.findByBoxExternalId(spec.id());
            if(existing.isPresent()) {
                if(!existing.get().getFingerprint().equals(fingerprint))throw new IllegalStateException("Caixa publicada não pode ser sobrescrita. Crie outra versão: "+spec.id());
                continue;
            }
            var box=cases.findBySourceAndExternalId("CSBET",spec.id()).orElseGet(()->new WeaponCase("CSBET",spec.id()));
            box.updateCustom(spec.name(),spec.description(),spec.image(),manifest.version());cases.saveAndFlush(box);
            var definition=definitions.saveAndFlush(new CustomCaseDefinition(box,spec.price(),spec.accent(),manifest.version(),fingerprint));
            for(var selected:spec.entries()) {
                var skin=skins.findBySourceAndExternalId("BYMYKEL",selected.externalId()).orElseThrow(()->new IllegalStateException("Importe o catálogo antes de criar "+spec.id()));
                var known=variants.findBySkinIdIn(List.of(skin.getId()));var allowed=new ArrayList<MarketVariant>();
                for(var option:selected.variants()) {
                    var matches=known.stream().filter(v->v.getWear()==option.wear()&&v.isStatTrak()==option.statTrak()&&!v.isSouvenir()).toList();
                    if(matches.size()!=1||("glove".equals(skin.getSpecialCategory())&&option.statTrak()))throw new IllegalStateException("Variante não cadastrada: "+selected.name()+" "+option);
                    allowed.add(matches.getFirst());
                }
                entries.saveAndFlush(new CustomCaseEntry(definition,skin,selected.weight(),allowed));
            }
        }
        return manifest.boxes().size();
    }
    private String fingerprint(String version,CustomCaseManifest.Box box) {
        try{return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest((version+new JsonMapper().writeValueAsString(box)).getBytes(StandardCharsets.UTF_8)));}
        catch(NoSuchAlgorithmException failure){throw new IllegalStateException(failure);}
    }
}
