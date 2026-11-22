package com.csbet.backend.inventory;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import static com.csbet.backend.inventory.InventoryResponse.InventoryPage;

@Service
public class InventoryService {
    private final InventoryRepository repository;

    public InventoryService(InventoryRepository repository) { this.repository = repository; }

    @Transactional(readOnly = true)
    public InventoryPage findOwned(Long ownerId, int page, int size, InventoryStatus status) {
        if (page < 0 || page > 100000 || size < 1 || size > 60) throw new InvalidInventoryRequestException();
        var pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "obtainedAt", "id"));
        var result = status == null ? repository.findByOwnerId(ownerId, pageable)
                : repository.findByOwnerIdAndStatus(ownerId, status, pageable);
        return new InventoryPage(result.getContent().stream().map(InventoryResponse::from).toList(),
                page, size, result.getTotalElements(), result.getTotalPages());
    }
}
