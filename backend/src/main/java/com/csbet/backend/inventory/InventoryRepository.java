package com.csbet.backend.inventory;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InventoryRepository extends JpaRepository<InventoryItem, Long> {
    // Busca sempre pelo proprietário; não carrega usuário/senha com os itens da página.
    @EntityGraph(attributePaths = {"skin", "skin.rarity"})
    Page<InventoryItem> findByOwnerId(Long ownerId, Pageable pageable);

    @EntityGraph(attributePaths = {"skin", "skin.rarity"})
    Page<InventoryItem> findByOwnerIdAndStatus(Long ownerId, InventoryStatus status, Pageable pageable);
}
