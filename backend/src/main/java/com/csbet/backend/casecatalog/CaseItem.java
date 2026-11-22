package com.csbet.backend.casecatalog;

import com.csbet.backend.skin.CosmeticItem;
import jakarta.persistence.*;

@Entity
@Table(name = "CSBET_CASE_ITEMS", schema = "CSBET")
public class CaseItem {
    @EmbeddedId private CaseItemId id;
    @MapsId("caseId") @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "CASE_ID") private WeaponCase weaponCase;
    @MapsId("itemId") @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "ITEM_ID") private CosmeticItem item;
    @Column(nullable = false, length = 1) private String special;
    protected CaseItem() { }
    public CaseItem(WeaponCase weaponCase, CosmeticItem item, boolean special) {
        this.id = new CaseItemId(weaponCase.getId(), item.getId());
        this.weaponCase = weaponCase; this.item = item; this.special = special ? "Y" : "N";
    }
    public CosmeticItem getItem() { return item; }
    public boolean isSpecial() { return "Y".equals(special); }
}
