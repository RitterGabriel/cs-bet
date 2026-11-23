package com.csbet.backend.casecatalog;
import com.csbet.backend.market.Wear;
import java.math.BigDecimal;
import java.util.*;
import tools.jackson.databind.json.JsonMapper;
public record CustomCaseManifest(String version,List<Box> boxes) {
    public record Variant(Wear wear,boolean statTrak) { }
    public record Entry(String externalId,String name,int weight,List<Variant> variants) { }
    public record Box(String id,String name,BigDecimal price,String accent,String description,String image,List<Entry> entries) { }
    public static CustomCaseManifest bundled() {
        try(var stream=CustomCaseManifest.class.getResourceAsStream("/custom/cases-v1.json")) {
            var manifest=new JsonMapper().readValue(stream,CustomCaseManifest.class);
            if(manifest.version()==null||manifest.boxes()==null||manifest.boxes().isEmpty())throw new IllegalArgumentException();
            var ids=new HashSet<String>();
            for(var box:manifest.boxes()) {
                if(!box.id().startsWith("custom-")||!ids.add(box.id())||box.price().signum()<=0||box.price().scale()>2||!box.accent().matches("#[a-fA-F0-9]{6}"))throw new IllegalArgumentException();
                if(box.entries().isEmpty()||box.entries().stream().mapToLong(Entry::weight).sum()!=10000||box.entries().stream().map(Entry::externalId).distinct().count()!=box.entries().size())throw new IllegalArgumentException();
                for(var entry:box.entries())if(entry.weight()<=0||entry.variants().isEmpty()||new HashSet<>(entry.variants()).size()!=entry.variants().size())throw new IllegalArgumentException();
            }
            return manifest;
        }catch(Exception failure){throw new IllegalStateException("Definições das caixas personalizadas inválidas.",failure);}
    }
}
