package com.csbet.backend.opening;

import com.csbet.backend.casecatalog.*;
import com.csbet.backend.inventory.*;
import com.csbet.backend.market.*;
import com.csbet.backend.repository.UserRepository;
import com.csbet.backend.skin.CosmeticItemRepository;
import com.csbet.backend.wallet.*;
import java.time.*;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CaseOpeningService {
    private final CustomCaseService custom;
    private final UserRepository users;
    private final WeaponCaseRepository cases;
    private final CosmeticItemRepository skins;
    private final SimulationPreviewService simulation;
    private final MarketVariantRepository variants;
    private final VariantSelector selector;
    private final InventoryRepository inventory;
    private final WalletMovementRepository movements;
    private final CaseOpeningRepository openings;
    private final Clock clock;
    public CaseOpeningService(UserRepository users, WeaponCaseRepository cases, CosmeticItemRepository skins,
            SimulationPreviewService simulation, MarketVariantRepository variants, VariantSelector selector,
            InventoryRepository inventory, WalletMovementRepository movements, CaseOpeningRepository openings, Clock pricingClock, CustomCaseService custom) {
        this.custom = custom;
        this.users = users; this.cases = cases; this.skins = skins; this.simulation = simulation; this.variants = variants;
        this.selector = selector; this.inventory = inventory; this.movements = movements; this.openings = openings; this.clock = pricingClock;
    }
    @Transactional
    public OpeningResponse open(Long userId, OpeningRequest request) {
        // Mesmo bloqueio da carteira: validação, débito, unidade e histórico são indivisíveis.
        var user = users.findByIdForUpdate(userId).orElseThrow(() -> new BadCredentialsException("Não autenticado."));
        var requestId = request.requestId().toString();
        var previous = openings.findByUserIdAndRequestId(userId, requestId);
        if (previous.isPresent()) {
            var saved = previous.get();
            if (!saved.getBox().getExternalId().equals(request.caseId()) || saved.getCharged().compareTo(request.expectedPrice()) != 0)
                throw new OpeningUnavailableException("Identificador já utilizado para outra abertura.");
            return OpeningResponse.from(saved);
        }
        if (movements.findByUserIdAndRequestId(userId, requestId).isPresent())
            throw new OpeningUnavailableException("Identificador já utilizado em outra operação.");
        boolean isCustom=request.caseId().startsWith("custom-");
        var prepared=isCustom?custom.prepare(request.caseId()):null;
        var box=isCustom?prepared.definition().getBox():cases.findBySourceAndExternalId("BYMYKEL",request.caseId()).orElseThrow(CatalogNotFoundException::new);
        var now = clock.instant();
        java.math.BigDecimal price;
        if(isCustom)price=prepared.definition().getCreditPrice().setScale(2);
        else {
            var prices = variants.findByCaseIdIn(List.of(box.getId())).stream()
                    .filter(v -> "BRL".equals(v.getCurrency()) && v.getPriceAmount().signum() > 0
                            && v.getValidUntil().isAfter(LocalDateTime.ofInstant(now, ZoneOffset.UTC))).toList();
            if (prices.size() != 1) throw new OpeningUnavailableException("Preço da caixa indisponível. Nenhum crédito foi consumido.");
            price=prices.getFirst().getPriceAmount().setScale(2);
        }
        if (price.compareTo(request.expectedPrice()) != 0)
            throw new OpeningUnavailableException("O preço da caixa mudou. Atualize a página antes de abrir.");
        if (user.getBalance().compareTo(price) < 0)
            throw new OpeningUnavailableException("Saldo fictício insuficiente. Adicione créditos na carteira.");
        com.csbet.backend.skin.CosmeticItem skin;
        com.csbet.backend.market.MarketVariant selected;
        boolean special;
        if(isCustom) {
            var entry=custom.draw(prepared);skin=entry.getSkin();special=entry.isSpecial();
            selected=selector.select(entry.getVariants(),skin.getId(),"glove".equals(skin.getSpecialCategory()));
        } else {
            var preview = simulation.preview(new SimulationPreviewRequest(request.caseId(), 1));
            if (!"AVAILABLE".equals(preview.status()) || preview.items().size() != 1)
                throw new OpeningUnavailableException("Esta caixa não tem dados suficientes para abertura.");
            var winner=preview.items().getFirst();skin=skins.findById(winner.skinId()).orElseThrow(CatalogNotFoundException::new);special=winner.special();
            selected=selector.select(variants.findBySkinIdIn(List.of(skin.getId())),skin.getId(),"glove".equals(skin.getSpecialCategory()));
        }
        var reference = selected.getValidUntil().isAfter(LocalDateTime.ofInstant(now, ZoneOffset.UTC))
                && "BRL".equals(selected.getCurrency()) ? selected.getPriceAmount().setScale(2) : null;
        // A fase é metadado da identidade já selecionada; não ocorre um segundo sorteio de fase.
        var phase = SpecialItemMetadata.knownPhase(box, skin.getExternalId());
        var unit = inventory.saveAndFlush(new InventoryItem(user, skin, selected, phase));
        user.spendCredits(price);
        var movement = movements.saveAndFlush(WalletMovement.opening(user, price, requestId));
        var opening = openings.saveAndFlush(new CaseOpening(user, box, requestId, unit, movement, price, reference, special, now));
        return OpeningResponse.from(opening);
    }
}
