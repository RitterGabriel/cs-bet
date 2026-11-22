package com.csbet.backend.market;

import com.csbet.backend.casecatalog.WeaponCase;
import com.csbet.backend.skin.CosmeticItem;
import jakarta.persistence.EntityManager;
import java.time.LocalDateTime;
import java.util.*;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Repository;

/** Paginação no Oracle: subconsultas escalares evitam multiplicar skins por variante/caixa. */
@Repository
public class MarketCatalogRepository {
    private final EntityManager entityManager;
    public MarketCatalogRepository(EntityManager entityManager) { this.entityManager = entityManager; }
    public static String skinMinimum(boolean wear) {
        return "(select min(v.priceAmount) from MarketVariant v where v.skinId=s.id and v.validUntil>:now"
                + (wear ? " and v.wear=:wear" : "") + ")";
    }
    public static String skinWhere(boolean weapon, boolean rarity, boolean wear) {
        return " from CosmeticItem s join fetch s.rarity r where s.source='BYMYKEL' and lower(s.name) like :search escape '!'"
                + (weapon ? " and s.weapon=:weapon" : "") + (rarity ? " and r.id=:rarity" : "")
                + (wear ? " and exists(select w.id from MarketVariant w where w.skinId=s.id and w.wear=:wear)" : "");
    }
    public static String skinQuery(String sort, boolean weapon, boolean rarity, boolean wear) {
        String minimum = skinMinimum(wear);
        String order = switch (sort) {
            case "name" -> "s.name asc, s.externalId asc";
            case "name_desc" -> "s.name desc, s.externalId asc";
            case "price_asc", "price_desc" -> "case when " + minimum + " is null then 1 else 0 end, minimumPrice "
                    + (sort.equals("price_asc") ? "asc" : "desc") + ", s.name asc, s.externalId asc";
            default -> throw new IllegalArgumentException("Ordenação inválida.");
        };
        return "select s, " + minimum + " as minimumPrice" + skinWhere(weapon, rarity, wear) + " order by " + order;
    }
    public Page<CosmeticItem> skins(String search, String weapon, String rarity, Wear wear, String sort, Pageable page, LocalDateTime now) {
        boolean hasWeapon = !weapon.isEmpty(), hasRarity = !rarity.isEmpty(), hasWear = wear != null;
        var query = entityManager.createQuery(skinQuery(sort, hasWeapon, hasRarity, hasWear), Object[].class);
        var count = entityManager.createQuery("select count(s)" + skinWhere(hasWeapon, hasRarity, hasWear).replace("join fetch", "join"), Long.class);
        for (var q : List.of(query, count)) {
            q.setParameter("search", escapedSearch(search));
            if (hasWeapon) q.setParameter("weapon", weapon);
            if (hasRarity) q.setParameter("rarity", rarity);
            if (hasWear) q.setParameter("wear", wear);
        }
        query.setParameter("now", now).setFirstResult(Math.toIntExact(page.getOffset())).setMaxResults(page.getPageSize());
        return new PageImpl<>(query.getResultList().stream().map(row -> (CosmeticItem) row[0]).toList(), page, count.getSingleResult());
    }
    public List<String> weapons() {
        return entityManager.createQuery("select distinct s.weapon from CosmeticItem s where s.source='BYMYKEL' order by s.weapon", String.class).getResultList();
    }
    public List<MarketCatalogDtos.RarityFilter> rarities() {
        return entityManager.createQuery("select distinct r.id,r.name,r.color from CosmeticItem s join s.rarity r where s.source='BYMYKEL' order by r.name", Object[].class)
                .getResultList().stream().map(row -> new MarketCatalogDtos.RarityFilter((String)row[0],(String)row[1],(String)row[2])).toList();
    }
    public static String caseQuery(String sort) {
        if (!Set.of("price_asc", "price_desc").contains(sort)) throw new IllegalArgumentException("Ordenação inválida.");
        return "select b from WeaponCase b left join MarketVariant v on v.caseId=b.id and v.validUntil>:now"
                + " where b.category=:category and lower(b.name) like :search escape '!'"
                + " order by case when v.priceAmount is null then 1 else 0 end, v.priceAmount "
                + (sort.equals("price_asc") ? "asc" : "desc") + ", b.name asc, b.externalId asc";
    }
    public Page<WeaponCase> cases(String search, String category, String sort, Pageable page, LocalDateTime now) {
        var query = entityManager.createQuery(caseQuery(sort), WeaponCase.class).setParameter("now",now)
                .setParameter("category",category).setParameter("search",escapedSearch(search))
                .setFirstResult(Math.toIntExact(page.getOffset())).setMaxResults(page.getPageSize());
        long count = entityManager.createQuery("select count(b) from WeaponCase b where b.category=:category and lower(b.name) like :search escape '!'",Long.class)
                .setParameter("category",category).setParameter("search",escapedSearch(search)).getSingleResult();
        return new PageImpl<>(query.getResultList(),page,count);
    }
    private static String escapedSearch(String value) {
        return "%" + value.strip().toLowerCase(Locale.ROOT).replace("!","!!").replace("%","!%").replace("_","!_") + "%";
    }
}
