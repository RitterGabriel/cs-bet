package com.csbet.backend.opening;

import com.csbet.backend.casecatalog.*;
import com.csbet.backend.entity.User;
import com.csbet.backend.market.*;
import com.csbet.backend.repository.UserRepository;
import com.csbet.backend.skin.*;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.*;
import org.springframework.transaction.support.TransactionTemplate;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/** Optativo: V8 aplicada. Dados exclusivos; o motor já validado é fixado para testar a transação Oracle. */
@EnabledIfEnvironmentVariable(named = "CSBET_ORACLE_TEST", matches = "true")
@SpringBootTest(properties = {"csbet.pricing.schedule-enabled=false", "csbet.market.enabled=false"})
class CaseOpeningOracleTest {
    @Autowired CaseOpeningService service;
    @Autowired CustomCaseRepository customDefinitions;
    @Autowired CustomCaseEntryRepository customEntries;
    Long customBoxId;
    @Autowired com.csbet.backend.inventory.InventoryRepository inventory;
    @Autowired com.csbet.backend.inventory.InventoryService ownedInventory;
    @Autowired OpeningBatchService batchService;
    @Autowired com.csbet.backend.inventory.BulkInventorySaleService bulkSales;
    @MockitoSpyBean com.csbet.backend.inventory.InventorySaleBatchRepository bulkReceipts;
    @Autowired com.csbet.backend.statistics.StatisticsRepository statistics;
    @Autowired com.csbet.backend.inventory.InventorySaleService saleService;
    @MockitoSpyBean com.csbet.backend.inventory.InventorySaleRepository sales;
    @Autowired UserRepository users;
    @Autowired WeaponCaseRepository cases;
    @Autowired CosmeticItemRepository skins;
    @Autowired RarityRepository rarities;
    @Autowired MarketVariantRepository variants;
    @Autowired JdbcTemplate jdbc;
    @Autowired TransactionTemplate transaction;
    @MockitoBean SimulationPreviewService simulation;
    @MockitoSpyBean CaseOpeningRepository openings;
    Long ownerId, otherId, boxId, skinId;
    String rarityId, caseExternal;
    final BigDecimal cost = new BigDecimal("2.71");
    @BeforeEach void setup() {
        assertThat(jdbc.queryForObject("SELECT SYS_CONTEXT('USERENV','CURRENT_SCHEMA') FROM DUAL", String.class)).isEqualTo("CSBET");
        ownerId = user(); otherId = user();
        String id = UUID.randomUUID().toString(); rarityId = "opening-test-" + id; caseExternal = "opening-test-" + id;
        var rarity = rarities.saveAndFlush(new Rarity(rarityId, "Test", "#4b69ff"));
        var box = new WeaponCase("BYMYKEL", caseExternal); box.update("Test case", null, null, "test", "en", "Test only");
        box = cases.saveAndFlush(box); boxId = box.getId();
        var skin = new CosmeticItem("BYMYKEL", "opening-skin-" + id);
        skin.update("Test skin", "Test", "Rifle", "none", null, rarity);
        skin = skins.saveAndFlush(skin); skinId = skin.getId();
        quote(null, boxId, caseExternal, Wear.NONE, cost);
        quote(skinId, null, skin.getExternalId(), Wear.FT, new BigDecimal("10.00"));
        var winner = new SimulationPreviewResponse.ItemCount(skinId, skin.getExternalId(), skin.getName(),
                new SimulationPreviewResponse.Rarity(rarityId, "Test", "#4b69ff"), false, 1, new BigDecimal("100"),
                "OFFICIAL_EQUAL_WITHIN_RARITY", true, new SimulationPreviewResponse.Variant("UNDETERMINED", null, null, null, "test"));
        when(simulation.preview(new SimulationPreviewRequest(caseExternal, 1))).thenReturn(new SimulationPreviewResponse(
                "AVAILABLE", null, caseExternal, 1, 1, List.of(), List.of(winner), List.of()));
    }
    Long user() {
        String name = "op-" + UUID.randomUUID();
        return users.saveAndFlush(new User(name, name + "@example.invalid", "unused-test-hash", new BigDecimal("3.00"), LocalDateTime.now())).getId();
    }
    void quote(Long skinId, Long caseId, String external, Wear wear, BigDecimal amount) {
        var now = Instant.now();
        var quote = new MarketPriceSource.Quote(skinId == null ? MarketPriceSource.Target.CASE : MarketPriceSource.Target.SKIN,
                external, "test-" + external + "-" + wear, wear, false, false, amount, "BRL", now);
        var variant = new MarketVariant(skinId, caseId, quote); variant.record(quote, "test", LocalDateTime.now(ZoneOffset.UTC), LocalDateTime.now(ZoneOffset.UTC).plusHours(1));
        variants.saveAndFlush(variant);
    }
    OpeningRequest request(UUID id) { return new OpeningRequest(caseExternal, id, cost); }
    void counts(long expected) {
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM CSBET_OPENINGS WHERE USER_ID=?", Long.class, ownerId)).isEqualTo(expected);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM CSBET_INVENTORY_ITEMS WHERE OWNER_ID=?", Long.class, ownerId)).isEqualTo(expected);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM CSBET_WALLET_MOVEMENTS WHERE USER_ID=? AND TYPE='CASE_OPENING'", Long.class, ownerId)).isEqualTo(expected);
    }
    @Test void concurrentRetriesCreateOneDebitOneUnitAndOneOpening() throws Exception {
        var request = request(UUID.randomUUID());
        try (var executor = Executors.newFixedThreadPool(2)) {
            var go = new CountDownLatch(1);
            Callable<OpeningResponse> call = () -> { go.await(); return service.open(ownerId, request); };
            var a = executor.submit(call); var b = executor.submit(call); go.countDown();
            assertThat(a.get(30, TimeUnit.SECONDS)).isEqualTo(b.get(30, TimeUnit.SECONDS));
        }
        counts(1);
        assertThat(users.findById(ownerId).orElseThrow().getBalance()).isEqualByComparingTo("0.29");
        assertThat(users.findById(otherId).orElseThrow().getBalance()).isEqualByComparingTo("3.00");
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM CSBET_INVENTORY_ITEMS WHERE OWNER_ID=?", Long.class, otherId)).isZero();
        verify(simulation).preview(new SimulationPreviewRequest(caseExternal, 1));
    }
    @Test void simultaneousDistinctRequestsCannotOverspend() throws Exception {
        int success = 0, refused = 0;
        try (var executor = Executors.newFixedThreadPool(2)) {
            var go = new CountDownLatch(1);
            Callable<OpeningResponse> call = () -> { go.await(); return service.open(ownerId, request(UUID.randomUUID())); };
            var futures = List.of(executor.submit(call), executor.submit(call)); go.countDown();
            for (var future : futures) try { future.get(30, TimeUnit.SECONDS); success++; }
            catch (ExecutionException failure) { assertThat(failure.getCause()).isInstanceOf(OpeningUnavailableException.class); refused++; }
        }
        assertThat(success).isEqualTo(1); assertThat(refused).isEqualTo(1); counts(1);
        assertThat(users.findById(ownerId).orElseThrow().getBalance()).isEqualByComparingTo("0.29");
    }
    @Test void rollbackRevertsBalanceInventoryAndBothHistories() {
        transaction.executeWithoutResult(status -> { service.open(ownerId, request(UUID.randomUUID())); counts(1); status.setRollbackOnly(); });
        counts(0); assertThat(users.findById(ownerId).orElseThrow().getBalance()).isEqualByComparingTo("3.00");
    }
    @Test void persistenceFailureAfterDebitRollsBackEverything() {
        doThrow(new IllegalStateException("Injected persistence failure")).when(openings).saveAndFlush(any());
        assertThatThrownBy(() -> service.open(ownerId, request(UUID.randomUUID()))).hasMessageContaining("Injected");
        counts(0); assertThat(users.findById(ownerId).orElseThrow().getBalance()).isEqualByComparingTo("3.00");
    }
    @Test void batchOfTenDebitsOncePerUnitAndReplayDoesNotDeliverAgain() {
        jdbc.update("UPDATE CSBET_USERS SET BALANCE=100 WHERE ID=?",ownerId);
        var request=new OpeningBatchRequest(caseExternal,UUID.randomUUID(),cost,10);
        var result=batchService.open(ownerId,request);
        assertThat(result.results()).hasSize(10); assertThat(result.charged()).isEqualByComparingTo("27.10");
        assertThat(result.balance()).isEqualByComparingTo("72.90");
        assertThat(result.results().stream().map(OpeningResponse::inventoryId)).doesNotHaveDuplicates();
        assertThat(batchService.open(ownerId,request)).isEqualTo(result); counts(10);
    }
    @Test void failureInSecondUnitRollsBackTheWholeBatch() {
        jdbc.update("UPDATE CSBET_USERS SET BALANCE=100 WHERE ID=?",ownerId);
        var preview=simulation.preview(new SimulationPreviewRequest(caseExternal,1));
        var called=new java.util.concurrent.atomic.AtomicInteger();
        when(simulation.preview(any())).thenAnswer(call -> { if(called.incrementAndGet()==2)throw new IllegalStateException("Injected batch failure");return preview; });
        assertThatThrownBy(() -> batchService.open(ownerId,new OpeningBatchRequest(caseExternal,UUID.randomUUID(),cost,5)))
            .hasMessageContaining("Injected batch failure");
        counts(0); assertThat(users.findById(ownerId).orElseThrow().getBalance()).isEqualByComparingTo("100.00");
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM CSBET_OPENING_BATCHES WHERE USER_ID=?",Long.class,ownerId)).isZero();
    }
    @Test void concurrentBatchRetriesAreAnAtomicSinglePurchase() throws Exception {
        jdbc.update("UPDATE CSBET_USERS SET BALANCE=100 WHERE ID=?",ownerId);
        var request=new OpeningBatchRequest(caseExternal,UUID.randomUUID(),cost,2);
        try(var executor=Executors.newFixedThreadPool(2)) {
            var go=new CountDownLatch(1);
            Callable<OpeningBatchResponse> call=()->{go.await();return batchService.open(ownerId,request);};
            var a=executor.submit(call);var b=executor.submit(call);go.countDown();
            assertThat(a.get(30,TimeUnit.SECONDS)).isEqualTo(b.get(30,TimeUnit.SECONDS));
        }
        counts(2);assertThat(users.findById(ownerId).orElseThrow().getBalance()).isEqualByComparingTo("94.58");
    }
    @Test void concurrentSaleRetriesCreditOnlyOnceAndKeepSnapshot() throws Exception {
        var opened=service.open(ownerId,request(UUID.randomUUID()));
        var sale=new com.csbet.backend.inventory.InventorySaleRequest(UUID.randomUUID(),new BigDecimal("10.00"));
        try(var executor=Executors.newFixedThreadPool(2)) {
            var go=new CountDownLatch(1);
            Callable<com.csbet.backend.inventory.InventorySaleResponse> call=()->{go.await();return saleService.sell(ownerId,opened.inventoryId(),sale);};
            var a=executor.submit(call);var b=executor.submit(call);go.countDown();
            assertThat(a.get(30,TimeUnit.SECONDS)).isEqualTo(b.get(30,TimeUnit.SECONDS));
        }
        assertThat(users.findById(ownerId).orElseThrow().getBalance()).isEqualByComparingTo("10.29");
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM CSBET_INVENTORY_SALES WHERE USER_ID=?",Long.class,ownerId)).isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM CSBET_WALLET_MOVEMENTS WHERE USER_ID=? AND TYPE='ITEM_SALE'",Long.class,ownerId)).isEqualTo(1);
        assertThatThrownBy(()->saleService.sell(ownerId,opened.inventoryId(),new com.csbet.backend.inventory.InventorySaleRequest(UUID.randomUUID(),sale.expectedPrice())))
            .hasMessageContaining("já foi vendida");
        jdbc.update("UPDATE CSBET_MARKET_VARIANTS SET PRICE_AMOUNT=20 WHERE SKIN_ID=?",skinId);
        assertThat(jdbc.queryForObject("SELECT SALE_PRICE FROM CSBET_INVENTORY_ITEMS WHERE ID=?",BigDecimal.class,opened.inventoryId())).isEqualByComparingTo("10.00");
        assertThat(users.findById(otherId).orElseThrow().getBalance()).isEqualByComparingTo("3.00");
    }
    @Test void failureRecordingSaleRollsBackCreditStatusAndMovement() {
        var opened=service.open(ownerId,request(UUID.randomUUID()));
        doThrow(new IllegalStateException("Injected sale failure")).when(sales).saveAndFlush(any());
        assertThatThrownBy(()->saleService.sell(ownerId,opened.inventoryId(),new com.csbet.backend.inventory.InventorySaleRequest(UUID.randomUUID(),new BigDecimal("10.00"))))
            .hasMessageContaining("Injected sale failure");
        assertThat(users.findById(ownerId).orElseThrow().getBalance()).isEqualByComparingTo("0.29");
        assertThat(jdbc.queryForObject("SELECT STATUS FROM CSBET_INVENTORY_ITEMS WHERE ID=?",String.class,opened.inventoryId())).isEqualTo("AVAILABLE");
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM CSBET_INVENTORY_SALES WHERE USER_ID=?",Long.class,ownerId)).isZero();
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM CSBET_WALLET_MOVEMENTS WHERE USER_ID=? AND TYPE='ITEM_SALE'",Long.class,ownerId)).isZero();
    }
    @Test void historyShowsWonSkinOnOpeningWithoutDuplicatingReceiptAndKeepsSale() {
        var opened=service.open(ownerId,request(UUID.randomUUID()));
        var all=com.csbet.backend.statistics.ActivityType.ALL;
        var obtained=com.csbet.backend.statistics.ActivityType.ITEM_OBTAINED;
        var history=statistics.activity(ownerId,all,0,20);
        assertThat(statistics.activityCount(ownerId,all)).isEqualTo(1);
        assertThat(statistics.activityCount(ownerId,obtained)).isZero();
        assertThat(history).hasSize(1);
        assertThat(history.getFirst().type()).isEqualTo(com.csbet.backend.statistics.ActivityType.CASE_OPENING);
        assertThat(history.getFirst().itemName()).isEqualTo(opened.item().name());
        assertThat(history.getFirst().amount()).isEqualByComparingTo(cost);
        assertThat(history.getFirst().resultingBalance()).isEqualByComparingTo("0.29");
        saleService.sell(ownerId,opened.inventoryId(),new com.csbet.backend.inventory.InventorySaleRequest(UUID.randomUUID(),new BigDecimal("10.00")));
        assertThat(statistics.activityCount(ownerId,all)).isEqualTo(2);
        assertThat(statistics.activity(ownerId,all,0,20)).extracting(com.csbet.backend.statistics.PersonalStatisticsResponse.Activity::type)
            .containsExactlyInAnyOrder(com.csbet.backend.statistics.ActivityType.CASE_OPENING,com.csbet.backend.statistics.ActivityType.ITEM_SOLD);
        assertThat(statistics.activity(otherId,all,0,20)).isEmpty();
        assertThat(statistics.activity(ownerId,all,1,1)).hasSize(1);
        // Unidades antigas sem abertura vinculada continuam com seu histórico próprio.
        jdbc.update("INSERT INTO CSBET_INVENTORY_ITEMS (ID, OWNER_ID, SKIN_ID, OBTAINED_AT, STATUS) VALUES (SEQ_CSBET_INVENTORY.NEXTVAL,?,?,CURRENT_TIMESTAMP,'AVAILABLE')", ownerId,skinId);
        assertThat(statistics.activityCount(ownerId,all)).isEqualTo(3);
        assertThat(statistics.activityCount(ownerId,obtained)).isEqualTo(1);
        assertThat(statistics.activity(ownerId,obtained,0,20)).hasSize(1);
    }

    @Test void sellAllIncludesUnitsAcrossPagesAndLeavesUnknownVariantsUntouched() {
        jdbc.update("UPDATE CSBET_USERS SET BALANCE=100 WHERE ID=?",ownerId);
        batchService.open(ownerId,new OpeningBatchRequest(caseExternal,UUID.randomUUID(),cost,5));
        jdbc.update("INSERT INTO CSBET_INVENTORY_ITEMS (ID, OWNER_ID, SKIN_ID, OBTAINED_AT, STATUS) VALUES (SEQ_CSBET_INVENTORY.NEXTVAL,?,?,CURRENT_TIMESTAMP,'AVAILABLE')",ownerId,skinId);
        var quote=bulkSales.quote(ownerId);
        assertThat(quote.items()).hasSize(5);assertThat(quote.skippedItems()).isEqualTo(1);
        assertThatThrownBy(()->bulkSales.sell(otherId,new com.csbet.backend.inventory.BulkSaleDtos.Request(UUID.randomUUID(),quote.items())))
            .isInstanceOf(com.csbet.backend.casecatalog.CatalogNotFoundException.class);
        assertThat(users.findById(otherId).orElseThrow().getBalance()).isEqualByComparingTo("3.00");
        var request=new com.csbet.backend.inventory.BulkSaleDtos.Request(UUID.randomUUID(),quote.items());
        var receipt=bulkSales.sell(ownerId,request);
        assertThat(receipt.soldItems()).isEqualTo(5);assertThat(receipt.amount()).isEqualByComparingTo("50.00");
        assertThat(receipt.balance()).isEqualByComparingTo("136.45");
        assertThat(bulkSales.sell(ownerId,request)).isEqualTo(receipt);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM CSBET_INVENTORY_ITEMS WHERE OWNER_ID=? AND STATUS='AVAILABLE'",Long.class,ownerId)).isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM CSBET_INVENTORY_SALES WHERE USER_ID=?",Long.class,ownerId)).isEqualTo(5);
        var summary=new com.csbet.backend.statistics.StatisticsRepository(new org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate(jdbc)).salesTotals(ownerId);
        assertThat(summary.count()).isEqualTo(5);assertThat(summary.total()).isEqualByComparingTo("50.00");
    }
    @Test void failureSavingBulkReceiptRevertsEverySaleCreditAndStatus() {
        jdbc.update("UPDATE CSBET_USERS SET BALANCE=100 WHERE ID=?",ownerId);
        batchService.open(ownerId,new OpeningBatchRequest(caseExternal,UUID.randomUUID(),cost,2));
        var quote=bulkSales.quote(ownerId);
        doThrow(new IllegalStateException("Injected bulk failure")).when(bulkReceipts).saveAndFlush(any());
        assertThatThrownBy(()->bulkSales.sell(ownerId,new com.csbet.backend.inventory.BulkSaleDtos.Request(UUID.randomUUID(),quote.items())))
            .hasMessageContaining("Injected bulk failure");
        assertThat(users.findById(ownerId).orElseThrow().getBalance()).isEqualByComparingTo("94.58");
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM CSBET_INVENTORY_ITEMS WHERE OWNER_ID=? AND STATUS='AVAILABLE'",Long.class,ownerId)).isEqualTo(2);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM CSBET_INVENTORY_SALES WHERE USER_ID=?",Long.class,ownerId)).isZero();
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM CSBET_WALLET_MOVEMENTS WHERE USER_ID=? AND TYPE='ITEM_SALE'",Long.class,ownerId)).isZero();
    }
    @Test void concurrentSellAllRetriesCreditTheWholeInventoryOnlyOnce() throws Exception {
        jdbc.update("UPDATE CSBET_USERS SET BALANCE=100 WHERE ID=?",ownerId);
        batchService.open(ownerId,new OpeningBatchRequest(caseExternal,UUID.randomUUID(),cost,2));
        var quote=bulkSales.quote(ownerId);var request=new com.csbet.backend.inventory.BulkSaleDtos.Request(UUID.randomUUID(),quote.items());
        try(var executor=Executors.newFixedThreadPool(2)) {
            var go=new CountDownLatch(1);
            Callable<com.csbet.backend.inventory.BulkSaleDtos.Receipt> call=()->{go.await();return bulkSales.sell(ownerId,request);};
            var a=executor.submit(call);var b=executor.submit(call);go.countDown();
            assertThat(a.get(30,TimeUnit.SECONDS)).isEqualTo(b.get(30,TimeUnit.SECONDS));
        }
        assertThat(users.findById(ownerId).orElseThrow().getBalance()).isEqualByComparingTo("114.58");
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM CSBET_SALE_BATCHES WHERE USER_ID=?",Long.class,ownerId)).isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM CSBET_INVENTORY_SALES WHERE USER_ID=?",Long.class,ownerId)).isEqualTo(2);
    }

    @Test void priceSortOrdersAllPagesWithUnknownPricesLastAndPreservesOwnerAndStatusFilters() {
        var skin=skins.findById(skinId).orElseThrow();var owner=users.findById(ownerId).orElseThrow();
        quote(skinId,null,skin.getExternalId(),Wear.FN,new BigDecimal("2.00"));
        quote(skinId,null,skin.getExternalId(),Wear.MW,new BigDecimal("30.00"));
        quote(skinId,null,skin.getExternalId(),Wear.WW,new BigDecimal("500.00"));
        var ids=new HashMap<Wear,Long>();
        var pool=variants.findBySkinIdIn(List.of(skinId));
        for(var variant:pool) ids.put(variant.getWear(),inventory.saveAndFlush(new com.csbet.backend.inventory.InventoryItem(owner,skin,variant,null)).getId());
        var ft=pool.stream().filter(v->v.getWear()==Wear.FT).findFirst().orElseThrow();
        var newerFt=inventory.saveAndFlush(new com.csbet.backend.inventory.InventoryItem(owner,skin,ft,null));
        inventory.saveAndFlush(new com.csbet.backend.inventory.InventoryItem(owner,skin));
        inventory.saveAndFlush(new com.csbet.backend.inventory.InventoryItem(users.findById(otherId).orElseThrow(),skin,ft,null));
        jdbc.update("UPDATE CSBET_MARKET_VARIANTS SET QUOTED_AT=CURRENT_TIMESTAMP-INTERVAL '2' DAY,FETCHED_AT=CURRENT_TIMESTAMP-INTERVAL '2' DAY,VALID_UNTIL=CURRENT_TIMESTAMP-INTERVAL '1' DAY WHERE SKIN_ID=? AND WEAR='WW'",skinId);
        for(var direction:List.of(com.csbet.backend.inventory.InventorySort.PRICE_ASC,com.csbet.backend.inventory.InventorySort.PRICE_DESC)) {
            var all=new ArrayList<com.csbet.backend.inventory.InventoryResponse>();
            for(int page=0;page<3;page++) {
                var result=ownedInventory.findOwned(ownerId,page,2,null,direction);
                assertThat(result.totalElements()).isEqualTo(6);assertThat(result.totalPages()).isEqualTo(3);all.addAll(result.content());
            }
            var values=all.stream().map(i->i.marketPrice().currentPrice()==null?null:i.marketPrice().currentPrice().toPlainString()).toList();
            if(direction==com.csbet.backend.inventory.InventorySort.PRICE_ASC)assertThat(values).containsExactly("2.00","10.00","10.00","30.00",null,null);
            else assertThat(values).containsExactly("30.00","10.00","10.00","2.00",null,null);
            assertThat(all.stream().map(com.csbet.backend.inventory.InventoryResponse::id)).doesNotHaveDuplicates();
            assertThat(all.get(1).id()).isEqualTo(newerFt.getId());
        }
        saleService.sell(ownerId,ids.get(Wear.MW),new com.csbet.backend.inventory.InventorySaleRequest(UUID.randomUUID(),new BigDecimal("30.00")));
        var sold=ownedInventory.findOwned(ownerId,0,12,com.csbet.backend.inventory.InventoryStatus.SOLD,com.csbet.backend.inventory.InventorySort.PRICE_DESC);
        assertThat(sold.totalElements()).isEqualTo(1);assertThat(sold.content().getFirst().id()).isEqualTo(ids.get(Wear.MW));
    }

    @Test void customOpeningUsesPublishedPriceAndRestrictedVariantsWithoutOfficialSimulation() {
        var box=new WeaponCase("CSBET","custom-test-"+UUID.randomUUID());box.updateCustom("Custom test","Test","/images/test.svg","TEST_CUSTOM_V1");
        box=cases.saveAndFlush(box);customBoxId=box.getId();
        var definition=customDefinitions.saveAndFlush(new CustomCaseDefinition(box,new BigDecimal("1.00"),"#00d4ff","TEST_CUSTOM_V1","0".repeat(64)));
        var skin=skins.findById(skinId).orElseThrow();var variant=variants.findBySkinIdIn(List.of(skinId)).getFirst();
        customEntries.saveAndFlush(new CustomCaseEntry(definition,skin,10000,List.of(variant)));
        String externalId=box.getExternalId();
        var request=new OpeningBatchRequest(externalId,UUID.randomUUID(),new BigDecimal("1.00"),2);
        var opened=batchService.open(ownerId,request);assertThat(opened.results()).hasSize(2);
        assertThat(opened.charged()).isEqualByComparingTo("2.00");assertThat(opened.balance()).isEqualByComparingTo("1.00");
        assertThat(opened.results()).allSatisfy(r->{assertThat(r.item().externalId()).isEqualTo(skin.getExternalId());assertThat(r.variant().wear()).isEqualTo(Wear.FT);});
        assertThat(batchService.open(ownerId,request)).isEqualTo(opened);counts(2);verifyNoInteractions(simulation);
        jdbc.update("UPDATE CSBET_CUSTOM_ENTRIES SET WEIGHT=9999 WHERE CASE_ID=?",customBoxId);
        assertThatThrownBy(()->service.open(ownerId,new OpeningRequest(externalId,UUID.randomUUID(),new BigDecimal("1.00"))))
            .isInstanceOf(CustomCaseUnavailableException.class);
        assertThat(users.findById(ownerId).orElseThrow().getBalance()).isEqualByComparingTo("1.00");counts(2);
    }
    @AfterEach void cleanup() {
        for (Long id : Arrays.asList(ownerId, otherId)) if (id != null) {
            jdbc.update("DELETE FROM CSBET_SALE_BATCHES WHERE USER_ID=?", id);
            jdbc.update("DELETE FROM CSBET_OPENING_BATCHES WHERE USER_ID=?", id);
            jdbc.update("DELETE FROM CSBET_INVENTORY_SALES WHERE USER_ID=?", id);
            jdbc.update("DELETE FROM CSBET_OPENINGS WHERE USER_ID=?", id);
            jdbc.update("DELETE FROM CSBET_INVENTORY_ITEMS WHERE OWNER_ID=?", id);
            jdbc.update("DELETE FROM CSBET_WALLET_MOVEMENTS WHERE USER_ID=?", id);
            users.deleteById(id);
        }
        if(customBoxId!=null) {
            jdbc.update("DELETE FROM CSBET_CUSTOM_VARIANTS WHERE ENTRY_ID IN (SELECT ID FROM CSBET_CUSTOM_ENTRIES WHERE CASE_ID=?)",customBoxId);
            jdbc.update("DELETE FROM CSBET_CUSTOM_ENTRIES WHERE CASE_ID=?",customBoxId);
            jdbc.update("DELETE FROM CSBET_CUSTOM_CASES WHERE CASE_ID=?",customBoxId);cases.deleteById(customBoxId);
        }
        if (skinId != null) { jdbc.update("DELETE FROM CSBET_MARKET_VARIANTS WHERE SKIN_ID=?", skinId); skins.deleteById(skinId); }
        if (boxId != null) { jdbc.update("DELETE FROM CSBET_MARKET_VARIANTS WHERE CASE_ID=?", boxId); cases.deleteById(boxId); }
        if (rarityId != null) rarities.deleteById(rarityId);
    }
}
