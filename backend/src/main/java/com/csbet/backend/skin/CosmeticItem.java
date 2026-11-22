package com.csbet.backend.skin;

import jakarta.persistence.*;

@Entity
@Table(name = "CSBET_COSMETIC_ITEMS", schema = "CSBET")
public class CosmeticItem {
    @Id @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "cosmeticSequence")
    @SequenceGenerator(name = "cosmeticSequence", sequenceName = "CSBET.SEQ_CSBET_COSMETIC_ITEMS", allocationSize = 1)
    private Long id;
    @Column(nullable = false, length = 40) private String source;
    @Column(nullable = false, length = 120) private String externalId;
    @Column(nullable = false, length = 250) private String name;
    @Column(nullable = false, length = 120) private String weapon;
    @Column(nullable = false, length = 120) private String weaponType;
    @Column(nullable = false, length = 20) private String specialCategory;
    @Column(length = 2048) private String imageUrl;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "RARITY_ID", nullable = false) private Rarity rarity;

    protected CosmeticItem() { }
    public CosmeticItem(String source, String externalId) { this.source = source; this.externalId = externalId; }
    public void update(String name, String weapon, String weaponType, String specialCategory, String imageUrl, Rarity rarity) {
        this.name = name; this.weapon = weapon; this.weaponType = weaponType;
        this.specialCategory = specialCategory; this.imageUrl = imageUrl; this.rarity = rarity;
    }
    public Long getId() { return id; }
    public String getExternalId() { return externalId; }
    public String getName() { return name; }
    public String getWeapon() { return weapon; }
    public String getWeaponType() { return weaponType; }
    public String getSpecialCategory() { return specialCategory; }
    public String getImageUrl() { return imageUrl; }
    public Rarity getRarity() { return rarity; }
}
