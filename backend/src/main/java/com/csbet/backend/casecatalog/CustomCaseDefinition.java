package com.csbet.backend.casecatalog;
import jakarta.persistence.*;
import java.math.BigDecimal;
@Entity
@Table(name="CSBET_CUSTOM_CASES",schema="CSBET")
public class CustomCaseDefinition {
    @Id private Long caseId;
    @MapsId @OneToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="CASE_ID") private WeaponCase box;
    @Column(precision=18,scale=2,nullable=false) private BigDecimal creditPrice;
    @Column(length=7,nullable=false) private String accent;
    @Column(length=40,nullable=false) private String ruleVersion;
    @Column(length=64,nullable=false) private String fingerprint;
    protected CustomCaseDefinition() { }
    public CustomCaseDefinition(WeaponCase box,BigDecimal price,String accent,String version,String fingerprint) {
        this.box=box;creditPrice=price;this.accent=accent;ruleVersion=version;this.fingerprint=fingerprint;
    }
    public Long getCaseId(){return caseId;}
    public WeaponCase getBox(){return box;}
    public BigDecimal getCreditPrice(){return creditPrice;}
    public String getAccent(){return accent;}
    public String getRuleVersion(){return ruleVersion;}
    public String getFingerprint(){return fingerprint;}
}
