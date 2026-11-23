package com.csbet.backend.inventory;

import com.csbet.backend.market.MarketVariantRepository;
import com.csbet.backend.repository.UserRepository;
import com.csbet.backend.wallet.*;
import java.time.*;
import java.util.Objects;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class InventorySaleService {
    private final UserRepository users;
    private final InventoryRepository inventory;
    private final InventorySaleRepository sales;
    private final MarketVariantRepository variants;
    private final WalletMovementRepository movements;
    private final Clock clock;
    public InventorySaleService(UserRepository users, InventoryRepository inventory, InventorySaleRepository sales,
            MarketVariantRepository variants, WalletMovementRepository movements, Clock pricingClock) {
        this.users = users; this.inventory = inventory; this.sales = sales; this.variants = variants; this.movements = movements; this.clock = pricingClock;
    }
    @Transactional
    public InventorySaleResponse sell(Long ownerId, Long itemId, InventorySaleRequest request) {
        // Mesma ordem de bloqueios usada nas aberturas: carteira antes do item.
        var user = users.findByIdForUpdate(ownerId).orElseThrow(() -> new BadCredentialsException("Não autenticado."));
        var requestId = request.requestId().toString();
        var previous = sales.findByUserIdAndRequestId(ownerId, requestId);
        if (previous.isPresent()) {
            var saved = previous.get();
            if (!saved.getInventoryId().equals(itemId) || saved.getAmount().compareTo(request.expectedPrice()) != 0)
                throw new InventorySaleException("Identificador já utilizado para outra venda.");
            return InventorySaleResponse.from(saved);
        }
        var unit = inventory.findOwnedForUpdate(itemId, ownerId).orElseThrow(com.csbet.backend.casecatalog.CatalogNotFoundException::new);
        if (unit.getStatus() != InventoryStatus.AVAILABLE) throw new InventorySaleException("Esta unidade já foi vendida.");
        if (movements.findByUserIdAndRequestId(ownerId, requestId).isPresent()) throw new InventorySaleException("Identificador já utilizado em outra operação.");
        if (unit.getMarketVariantId() == null || unit.getWear() == null || unit.getStatTrak() == null)
            throw new InventorySaleException("Não há uma variante identificada para vender este item.");
        var variant = variants.findById(unit.getMarketVariantId()).orElseThrow(() -> new InventorySaleException("Cotação da variante indisponível."));
        var now = clock.instant();
        var price = priceFor(unit, variant, now);
        if (price.compareTo(request.expectedPrice()) != 0) throw new InventorySaleException("O preço mudou. Atualize o inventário antes de vender.");
        user.addCredits(price);
        unit.sell(price, LocalDateTime.ofInstant(now, ZoneOffset.UTC));
        inventory.saveAndFlush(unit);
        var movement = movements.saveAndFlush(WalletMovement.sale(user, price, requestId));
        var sale = sales.saveAndFlush(new InventorySale(user, unit, request, movement, now));
        return InventorySaleResponse.from(sale);
    }
    static java.math.BigDecimal priceFor(InventoryItem unit, com.csbet.backend.market.MarketVariant variant, Instant now) {
        if (unit.getWear() == null || unit.getStatTrak() == null || variant == null)
            throw new InventorySaleException("Não há uma variante cotada identificada para este item.");
        if (!Objects.equals(variant.getSkinId(), unit.getSkin().getId()) || variant.getWear() != unit.getWear()
                || variant.isStatTrak() != unit.getStatTrak() || variant.isSouvenir()
                || !"BRL".equals(variant.getCurrency()) || variant.getPriceAmount().signum() <= 0
                || !variant.getValidUntil().isAfter(LocalDateTime.ofInstant(now, ZoneOffset.UTC)))
            throw new InventorySaleException("A variante está sem cotação vigente. Atualize o inventário mais tarde.");
        return variant.getPriceAmount().setScale(2);
    }

}
