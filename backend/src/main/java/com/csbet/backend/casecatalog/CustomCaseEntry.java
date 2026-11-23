package com.csbet.backend.casecatalog;
import com.csbet.backend.market.MarketVariant;
import com.csbet.backend.skin.CosmeticItem;
import jakarta.persistence.*;
import java.util.*;
@Entity
@Table(name="CSBET_CUSTOM_ENTRIES",schema="CSBET")
public class CustomCaseEntry {
    @Id @GeneratedValue(strategy=GenerationType.SEQUENCE,generator="customEntrySequence")
    @SequenceGenerator(name="customEntrySequence",sequenceName="CSBET.SEQ_CSBET_CUSTOM_ENTRIES",allocationSize=1) private Long id;
    @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="CASE_ID") private CustomCaseDefinition definition;
    @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="SKIN_ID") private CosmeticItem skin;
    private int weight;
    @ManyToMany @JoinTable(name="CSBET_CUSTOM_VARIANTS",schema="CSBET",joinColumns=@JoinColumn(name="ENTRY_ID"),inverseJoinColumns=@JoinColumn(name="VARIANT_ID"))
    private Set<MarketVariant> variants=new LinkedHashSet<>();
    protected CustomCaseEntry() { }
    public CustomCaseEntry(CustomCaseDefinition definition,CosmeticItem skin,int weight,List<MarketVariant> variants) {
        this.definition=definition;this.skin=skin;this.weight=weight;this.variants.addAll(variants);
    }
    public Long getId(){return id;}
    public CosmeticItem getSkin(){return skin;}
    public int getWeight(){return weight;}
    public List<MarketVariant> getVariants(){return variants.stream().sorted(Comparator.comparing(MarketVariant::getId)).toList();}
    public boolean isSpecial(){return !"none".equals(skin.getSpecialCategory());}
}
