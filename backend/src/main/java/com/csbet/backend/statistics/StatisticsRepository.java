package com.csbet.backend.statistics;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;
import static com.csbet.backend.statistics.PersonalStatisticsResponse.*;

/** Consultas Oracle somente de leitura sobre históricos existentes, sempre pelo proprietário. */
@Repository
public class StatisticsRepository {
    private final NamedParameterJdbcTemplate jdbc;
    public StatisticsRepository(NamedParameterJdbcTemplate jdbc) { this.jdbc = jdbc; }
    public record WalletTotals(long movements, BigDecimal added, long opened, BigDecimal spent) {
        public WalletTotals(long movements, BigDecimal added) { this(movements, added, 0, new BigDecimal("0.00")); }
    }
    public record SalesTotals(long count, BigDecimal total, long unvalued) { }

    private static final String ITEM_NAME = """
            CASE WHEN s.ID IS NOT NULL THEN
                CASE WHEN i.STAT_TRAK = 'Y' THEN 'StatTrak™ ' END || s.NAME
                || CASE WHEN i.PHASE IS NOT NULL THEN ' · ' || i.PHASE END
                || CASE WHEN i.WEAR IS NOT NULL AND i.WEAR <> 'NONE' THEN ' (' || i.WEAR || ')' END
            END
            """;
    private static final String ACTIVITY = """
            SELECT m.TYPE AS EVENT_TYPE, m.ID AS EVENT_ID, m.CREATED_AT AS EVENT_AT,
                   %s AS ITEM_NAME,
                   m.AMOUNT AS AMOUNT, m.RESULTING_BALANCE AS RESULTING_BALANCE
            FROM CSBET.CSBET_WALLET_MOVEMENTS m
            LEFT JOIN CSBET.CSBET_OPENINGS o ON o.WALLET_MOVEMENT_ID = m.ID AND o.USER_ID = m.USER_ID
            LEFT JOIN CSBET.CSBET_INVENTORY_ITEMS i ON i.ID = o.INVENTORY_ID AND i.OWNER_ID = m.USER_ID
            LEFT JOIN CSBET.CSBET_COSMETIC_ITEMS s ON s.ID = i.SKIN_ID
            WHERE m.USER_ID = :ownerId AND m.TYPE <> 'ITEM_SALE'
            UNION ALL
            SELECT 'ITEM_OBTAINED', i.ID, i.OBTAINED_AT, %s,
                   CAST(NULL AS NUMBER(18,2)), CAST(NULL AS NUMBER(18,2))
            FROM CSBET.CSBET_INVENTORY_ITEMS i JOIN CSBET.CSBET_COSMETIC_ITEMS s ON s.ID = i.SKIN_ID
            WHERE i.OWNER_ID = :ownerId
                AND NOT EXISTS (SELECT 1 FROM CSBET.CSBET_OPENINGS o
                    WHERE o.INVENTORY_ID = i.ID AND o.USER_ID = i.OWNER_ID)
            UNION ALL
            SELECT 'ITEM_SOLD', i.ID, i.SOLD_AT, %s, i.SALE_PRICE, w.RESULTING_BALANCE
            FROM CSBET.CSBET_INVENTORY_ITEMS i JOIN CSBET.CSBET_COSMETIC_ITEMS s ON s.ID = i.SKIN_ID
            LEFT JOIN CSBET.CSBET_INVENTORY_SALES sale ON sale.INVENTORY_ID = i.ID AND sale.USER_ID = i.OWNER_ID
            LEFT JOIN CSBET.CSBET_WALLET_MOVEMENTS w ON w.ID = sale.WALLET_MOVEMENT_ID AND w.USER_ID = i.OWNER_ID
            WHERE i.OWNER_ID = :ownerId AND i.STATUS = 'SOLD'
            """.formatted(ITEM_NAME, ITEM_NAME, ITEM_NAME);

    public WalletTotals walletTotals(Long ownerId) {
        return jdbc.queryForObject("""
                SELECT COUNT(*) AS MOVEMENTS,
                       SUM(CASE WHEN TYPE = 'CREDIT_ADDITION' THEN AMOUNT ELSE 0 END) AS ADDED,
                       SUM(CASE WHEN TYPE = 'CASE_OPENING' THEN 1 ELSE 0 END) AS OPENED,
                       SUM(CASE WHEN TYPE = 'CASE_OPENING' THEN AMOUNT ELSE 0 END) AS SPENT
                FROM CSBET.CSBET_WALLET_MOVEMENTS WHERE USER_ID = :ownerId
                """, Map.of("ownerId", ownerId), (rs, row) -> new WalletTotals(
                        rs.getLong("MOVEMENTS"), zeroIfNull(rs.getBigDecimal("ADDED")), rs.getLong("OPENED"), zeroIfNull(rs.getBigDecimal("SPENT"))));
    }
    public SalesTotals salesTotals(Long ownerId) {
        return jdbc.queryForObject("""
                SELECT COUNT(*) AS SALES, SUM(SALE_PRICE) AS TOTAL,
                       COUNT(*) - COUNT(SALE_PRICE) AS UNVALUED
                FROM CSBET.CSBET_INVENTORY_ITEMS WHERE OWNER_ID = :ownerId AND STATUS = 'SOLD'
                """, Map.of("ownerId", ownerId), (rs, row) -> new SalesTotals(
                        rs.getLong("SALES"), zeroIfNull(rs.getBigDecimal("TOTAL")), rs.getLong("UNVALUED")));
    }
    public long activityCount(Long ownerId, ActivityType type) {
        Long total = jdbc.queryForObject("SELECT COUNT(*) FROM (" + ACTIVITY + ") WHERE (:type = 'ALL' OR EVENT_TYPE = :type)",
                Map.of("ownerId", ownerId, "type", type.name()), Long.class);
        return total == null ? 0 : total;
    }
    public List<Activity> activity(Long ownerId, ActivityType type, int page, int size) {
        return jdbc.query("SELECT * FROM (" + ACTIVITY + ") WHERE (:type = 'ALL' OR EVENT_TYPE = :type) "
                        + "ORDER BY EVENT_AT DESC NULLS LAST, EVENT_TYPE, EVENT_ID DESC "
                        + "OFFSET :offset ROWS FETCH NEXT :size ROWS ONLY",
                Map.of("ownerId", ownerId, "type", type.name(), "offset", (long) page * size, "size", size),
                (rs, row) -> activity(rs));
    }
    private Activity activity(ResultSet rs) throws SQLException {
        var type = ActivityType.valueOf(rs.getString("EVENT_TYPE"));
        var time = rs.getTimestamp("EVENT_AT");
        return new Activity(type.name() + ":" + rs.getLong("EVENT_ID"), type,
                time == null ? null : time.toLocalDateTime().atOffset(ZoneOffset.UTC), rs.getString("ITEM_NAME"),
                rs.getBigDecimal("AMOUNT"), rs.getBigDecimal("RESULTING_BALANCE"));
    }
    private BigDecimal zeroIfNull(BigDecimal value) { return value == null ? new BigDecimal("0.00") : value.setScale(2); }
}
