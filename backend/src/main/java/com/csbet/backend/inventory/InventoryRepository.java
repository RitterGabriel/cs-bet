package com.csbet.backend.inventory;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InventoryRepository extends JpaRepository<InventoryItem, Long> {
    @org.springframework.data.jpa.repository.Query("""
            select new com.csbet.backend.inventory.InventoryValuation(
                sum(price.priceAmount), count(item.id),
                sum(case when price.id is null then 1L else 0L end))
            from InventoryItem item left join MarketVariant price on price.id = item.marketVariantId
                and price.skinId = item.skin.id and price.currency = 'BRL' and price.validUntil > :now
            where item.owner.id = :ownerId and item.status = :status
            """)
    InventoryValuation estimateAvailable(@org.springframework.data.repository.query.Param("ownerId") Long ownerId,
            @org.springframework.data.repository.query.Param("status") InventoryStatus status,
            @org.springframework.data.repository.query.Param("now") java.time.LocalDateTime now);

    // Busca sempre pelo proprietário; não carrega usuário/senha com os itens da página.
    @EntityGraph(attributePaths = {"skin", "skin.rarity"})
    Page<InventoryItem> findByOwnerId(Long ownerId, Pageable pageable);

    @EntityGraph(attributePaths = {"skin", "skin.rarity"})
    Page<InventoryItem> findByOwnerIdAndStatus(Long ownerId, InventoryStatus status, Pageable pageable);
}
