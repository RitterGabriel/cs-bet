package com.csbet.backend.casecatalog;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "CSBET_CASES", schema = "CSBET")
public class WeaponCase {
    @Id @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "caseSequence")
    @SequenceGenerator(name = "caseSequence", sequenceName = "CSBET.SEQ_CSBET_CASES", allocationSize = 1)
    private Long id;
    @Column(nullable = false, length = 40) private String source;
    @Column(nullable = false, length = 120) private String externalId;
    @Column(nullable = false, length = 250) private String name;
    @Column(nullable = false, length = 20) private String category;
    @Column(name = "CONTAINER_TYPE", nullable = false, length = 40) private String type;
    @Column(length = 4000) private String description;
    @Column(length = 2048) private String imageUrl;
    @Column(nullable = false, length = 40) private String sourceRevision;
    @Column(name = "SOURCE_LANGUAGE", nullable = false, length = 10) private String language;
    @Column(nullable = false) private LocalDateTime importedAt;
    @Column(nullable = false, length = 1000) private String specialNotice;

    protected WeaponCase() { }
    public WeaponCase(String source, String externalId) { this.source = source; this.externalId = externalId; }
    public void update(String name, String description, String imageUrl, String revision, String language, String specialNotice) {
        this.name = name; this.description = description; this.imageUrl = imageUrl;
        this.sourceRevision = revision; this.language = language; this.specialNotice = specialNotice;
        this.category = "official"; this.type = "Case"; this.importedAt = LocalDateTime.now(java.time.Clock.systemUTC());
    }
    public void updateCustom(String name,String description,String image,String version) {
        if(!"CSBET".equals(source))throw new IllegalArgumentException("Origem personalizada inválida.");
        update(name,description,image,version,"pt-BR","Caixa fictícia criada pelo CS Bet. Chances próprias por item; variantes limitadas às opções publicadas.");
        category="custom";
    }
    public Long getId() { return id; }
    public String getExternalId() { return externalId; }
    public String getSource() { return source; }
    public String getName() { return name; }
    public String getCategory() { return category; }
    public String getType() { return type; }
    public String getDescription() { return description; }
    public String getImageUrl() { return imageUrl; }
    public String getSourceRevision() { return sourceRevision; }
    public String getLanguage() { return language; }
    public LocalDateTime getImportedAt() { return importedAt; }
    public String getSpecialNotice() { return specialNotice; }
}
