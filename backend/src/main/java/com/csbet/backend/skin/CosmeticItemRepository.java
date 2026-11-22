package com.csbet.backend.skin;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
public interface CosmeticItemRepository extends JpaRepository<CosmeticItem, Long> {
    List<CosmeticItem> findAllBySource(String source);
}
