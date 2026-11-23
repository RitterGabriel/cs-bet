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

    @EntityGraph(attributePaths = {"skin", "skin.rarity"})
    @org.springframework.data.jpa.repository.Query(value = """
            select i from InventoryItem i
            left join MarketVariant price on price.id = i.marketVariantId
                and price.skinId = i.skin.id and price.currency = 'BRL' and price.validUntil > :now
            where i.owner.id = :ownerId and (:status is null or i.status = :status)
            order by case when price.id is null then 1 else 0 end,
                case when :direction = 'PRICE_ASC' then price.priceAmount end asc,
                case when :direction = 'PRICE_DESC' then price.priceAmount end desc,
                i.obtainedAt desc, i.id desc
            """, countQuery = """
            select count(i) from InventoryItem i
            where i.owner.id = :ownerId and (:status is null or i.status = :status)
            """)
    Page<InventoryItem> findOwnedByPrice(@org.springframework.data.repository.query.Param("ownerId") Long ownerId,
            @org.springframework.data.repository.query.Param("status") InventoryStatus status,
            @org.springframework.data.repository.query.Param("now") java.time.LocalDateTime now,
            @org.springframework.data.repository.query.Param("direction") String direction, Pageable pageable);

    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @org.springframework.data.jpa.repository.Query("select i from InventoryItem i where i.id = :id and i.owner.id = :ownerId")
    java.util.Optional<InventoryItem> findOwnedForUpdate(@org.springframework.data.repository.query.Param("id") Long id,
            @org.springframework.data.repository.query.Param("ownerId") Long ownerId);

    // Busca sempre pelo proprietário; não carrega usuário/senha com os itens da página.
    @EntityGraph(attributePaths = {"skin", "skin.rarity"})
    Page<InventoryItem> findByOwnerId(Long ownerId, Pageable pageable);

    @EntityGraph(attributePaths = {"skin", "skin.rarity"})
    Page<InventoryItem> findByOwnerIdAndStatus(Long ownerId, InventoryStatus status, Pageable pageable);
}
