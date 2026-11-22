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
    public record WalletTotals(long movements, BigDecimal added) { }
    public record SalesTotals(long count, BigDecimal total, long unvalued) { }

    private static final String ACTIVITY = """
            SELECT m.TYPE AS EVENT_TYPE, m.ID AS EVENT_ID, m.CREATED_AT AS EVENT_AT,
                   CAST(NULL AS VARCHAR2(250 CHAR)) AS ITEM_NAME,
                   m.AMOUNT AS AMOUNT, m.RESULTING_BALANCE AS RESULTING_BALANCE
            FROM CSBET.CSBET_WALLET_MOVEMENTS m WHERE m.USER_ID = :ownerId
            UNION ALL
            SELECT 'ITEM_OBTAINED', i.ID, i.OBTAINED_AT, s.NAME,
                   CAST(NULL AS NUMBER(18,2)), CAST(NULL AS NUMBER(18,2))
            FROM CSBET.CSBET_INVENTORY_ITEMS i JOIN CSBET.CSBET_COSMETIC_ITEMS s ON s.ID = i.SKIN_ID
            WHERE i.OWNER_ID = :ownerId
            UNION ALL
            SELECT 'ITEM_SOLD', i.ID, i.SOLD_AT, s.NAME, i.SALE_PRICE, CAST(NULL AS NUMBER(18,2))
            FROM CSBET.CSBET_INVENTORY_ITEMS i JOIN CSBET.CSBET_COSMETIC_ITEMS s ON s.ID = i.SKIN_ID
            WHERE i.OWNER_ID = :ownerId AND i.STATUS = 'SOLD'
            """;

    public WalletTotals walletTotals(Long ownerId) {
        return jdbc.queryForObject("""
                SELECT COUNT(*) AS MOVEMENTS,
                       SUM(CASE WHEN TYPE = 'CREDIT_ADDITION' THEN AMOUNT ELSE 0 END) AS ADDED
                FROM CSBET.CSBET_WALLET_MOVEMENTS WHERE USER_ID = :ownerId
                """, Map.of("ownerId", ownerId), (rs, row) -> new WalletTotals(
                        rs.getLong("MOVEMENTS"), zeroIfNull(rs.getBigDecimal("ADDED"))));
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
