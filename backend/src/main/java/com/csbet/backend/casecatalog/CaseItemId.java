package com.csbet.backend.casecatalog;

import jakarta.persistence.*;
import java.io.Serializable;
import java.util.Objects;

@Embeddable
public class CaseItemId implements Serializable {
    @Column(name = "CASE_ID") private Long caseId;
    @Column(name = "ITEM_ID") private Long itemId;
    protected CaseItemId() { }
    public CaseItemId(Long caseId, Long itemId) { this.caseId = caseId; this.itemId = itemId; }
    @Override public boolean equals(Object other) {
        return other instanceof CaseItemId key && Objects.equals(caseId, key.caseId) && Objects.equals(itemId, key.itemId);
    }
    @Override public int hashCode() { return Objects.hash(caseId, itemId); }
}
