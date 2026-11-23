package com.csbet.backend.inventory;
import com.csbet.backend.market.*;
import com.csbet.backend.repository.UserRepository;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.time.Clock;
import java.util.*;
import java.util.stream.Collectors;
import org.springframework.data.domain.*;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class BulkInventorySaleService {
    private final UserRepository users;
    private final InventoryRepository inventory;
    private final MarketVariantRepository variants;
    private final InventorySaleService sales;
    private final InventorySaleBatchRepository batches;
    private final Clock clock;
    public BulkInventorySaleService(UserRepository users, InventoryRepository inventory, MarketVariantRepository variants,
            InventorySaleService sales, InventorySaleBatchRepository batches, Clock pricingClock) {
        this.users=users;this.inventory=inventory;this.variants=variants;this.sales=sales;this.batches=batches;this.clock=pricingClock;
    }
    @Transactional(readOnly = true)
    public BulkSaleDtos.Quote quote(Long ownerId) {
        users.findById(ownerId).orElseThrow(() -> new BadCredentialsException("Não autenticado."));
        var units=inventory.findByOwnerIdAndStatus(ownerId,InventoryStatus.AVAILABLE,
                PageRequest.of(0,BulkSaleDtos.MAX_ITEMS+1,Sort.by("id")));
        if(units.getTotalElements()>BulkSaleDtos.MAX_ITEMS)
            throw new InventorySaleException("A venda em lote aceita até 2.000 unidades. Venda algumas unidades antes de continuar.");
        var ids=units.stream().map(InventoryItem::getMarketVariantId).filter(Objects::nonNull).distinct().toList();
        var quotes=ids.isEmpty()?Map.<Long,MarketVariant>of():variants.findAllById(ids).stream().collect(Collectors.toMap(MarketVariant::getId,v->v));
        var lines=new ArrayList<BulkSaleDtos.Line>();
        var total=new BigDecimal("0.00");int skipped=0;var now=clock.instant();
        for(var unit:units) {
            try {
                var price=InventorySaleService.priceFor(unit,unit.getMarketVariantId()==null?null:quotes.get(unit.getMarketVariantId()),now);
                lines.add(new BulkSaleDtos.Line(unit.getId(),price));total=total.add(price);
            } catch(InventorySaleException unavailable) { skipped++; }
        }
        return new BulkSaleDtos.Quote(List.copyOf(lines),skipped,total.setScale(2));
    }
    @Transactional
    public BulkSaleDtos.Receipt sell(Long ownerId,BulkSaleDtos.Request request) {
        if(request.items()==null||request.items().isEmpty()||request.items().size()>BulkSaleDtos.MAX_ITEMS||!request.isUnique())
            throw new InventorySaleException("Selecione unidades distintas para vender.");
        var user=users.findByIdForUpdate(ownerId).orElseThrow(() -> new BadCredentialsException("Não autenticado."));
        var lines=request.items().stream().sorted(Comparator.comparing(BulkSaleDtos.Line::inventoryId)).toList();
        var fingerprint=fingerprint(lines);
        var previous=batches.findByUserIdAndRequestId(ownerId,request.requestId().toString());
        if(previous.isPresent()) {
            if(!previous.get().matches(fingerprint))throw new InventorySaleException("Identificador já utilizado para outro conjunto de itens.");
            return previous.get().receipt();
        }
        var total=new BigDecimal("0.00");var balance=user.getBalance();
        // Um erro de preço, propriedade ou persistência reverte TODAS as vendas deste lote.
        for(var line:lines) {
            var id=UUID.nameUUIDFromBytes(("csbet-sell-all:"+ownerId+":"+request.requestId()+":"+line.inventoryId()).getBytes(StandardCharsets.UTF_8));
            var receipt=sales.sell(ownerId,line.inventoryId(),new InventorySaleRequest(id,line.expectedPrice()));
            total=total.add(receipt.amount());balance=receipt.balance();
        }
        return batches.saveAndFlush(new InventorySaleBatch(user,request.requestId().toString(),fingerprint,lines.size(),total,balance,clock.instant())).receipt();
    }
    private String fingerprint(List<BulkSaleDtos.Line> lines) {
        var canonical=lines.stream().map(l->l.inventoryId()+":"+l.expectedPrice().setScale(2).toPlainString()).collect(Collectors.joining(";"));
        try { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(canonical.getBytes(StandardCharsets.UTF_8))); }
        catch(NoSuchAlgorithmException impossible) { throw new IllegalStateException(impossible); }
    }
}
