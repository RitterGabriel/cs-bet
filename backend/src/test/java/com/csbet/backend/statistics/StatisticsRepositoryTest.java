package com.csbet.backend.statistics;

import java.math.BigDecimal;
import java.sql.*;
import java.time.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static com.csbet.backend.statistics.PersonalStatisticsResponse.*;

class StatisticsRepositoryTest {
    @Test
    @SuppressWarnings("unchecked")
    void unifiedHistoryBindsOwnerForAllSourcesAndPaginatesWithStableOrdering() throws Exception {
        var jdbc = mock(NamedParameterJdbcTemplate.class);
        var row = mock(ResultSet.class);
        when(row.getString("EVENT_TYPE")).thenReturn("ITEM_SOLD");
        when(row.getLong("EVENT_ID")).thenReturn(7L);
        when(row.getString("ITEM_NAME")).thenReturn("Snapshot skin");
        when(row.getBigDecimal("AMOUNT")).thenReturn(new BigDecimal("12.50"));
        when(row.getTimestamp("EVENT_AT")).thenReturn(Timestamp.valueOf("2026-11-22 12:00:00"));
        when(jdbc.query(anyString(), anyMap(), any(RowMapper.class))).thenAnswer(call ->
                List.of(((RowMapper<Activity>) call.getArgument(2)).mapRow(row, 0)));
        var repository = new StatisticsRepository(jdbc);
        var result = repository.activity(42L, ActivityType.ITEM_SOLD, 2, 20);
        assertThat(result.getFirst().id()).isEqualTo("ITEM_SOLD:7");
        assertThat(result.getFirst().occurredAt()).isEqualTo(OffsetDateTime.parse("2026-11-22T12:00:00Z"));
        assertThat(result.getFirst().amount()).isEqualByComparingTo("12.50");
        var sql = ArgumentCaptor.forClass(String.class);
        var parameters = ArgumentCaptor.forClass(Map.class);
        verify(jdbc).query(sql.capture(), parameters.capture(), any(RowMapper.class));
        assertThat(sql.getValue()).contains("m.USER_ID = :ownerId", "i.OWNER_ID = :ownerId", "NULLS LAST", "EVENT_TYPE, EVENT_ID DESC", "OFFSET :offset ROWS FETCH NEXT :size ROWS ONLY");
        assertThat(sql.getValue().split(":ownerId", -1)).hasSize(4);
        assertThat(parameters.getValue()).containsEntry("ownerId", 42L).containsEntry("type", "ITEM_SOLD")
                .containsEntry("offset", 40L).containsEntry("size", 20);
        verifyNoMoreInteractions(jdbc);
    }
    @Test
    @SuppressWarnings("unchecked")
    void legacySalesRetainMissingDateAndPriceInsteadOfInventingThem() throws Exception {
        var jdbc = mock(NamedParameterJdbcTemplate.class);
        var row = mock(ResultSet.class);
        when(row.getString("EVENT_TYPE")).thenReturn("ITEM_SOLD");
        when(row.getLong("EVENT_ID")).thenReturn(8L);
        when(jdbc.query(anyString(), anyMap(), any(RowMapper.class))).thenAnswer(call ->
                List.of(((RowMapper<Activity>) call.getArgument(2)).mapRow(row, 0)));
        var result = new StatisticsRepository(jdbc).activity(1L, ActivityType.ALL, 0, 20).getFirst();
        assertThat(result.occurredAt()).isNull();
        assertThat(result.amount()).isNull();
        assertThat(result.resultingBalance()).isNull();
    }
    @Test
    @SuppressWarnings("unchecked")
    void noWalletRecordsMeansZeroAddedCreditsAndTotalsUseOnlyTheOwner() throws Exception {
        var jdbc = mock(NamedParameterJdbcTemplate.class);
        var row = mock(ResultSet.class);
        when(jdbc.queryForObject(anyString(), anyMap(), any(RowMapper.class))).thenAnswer(call ->
                ((RowMapper<StatisticsRepository.WalletTotals>) call.getArgument(2)).mapRow(row, 0));
        var totals = new StatisticsRepository(jdbc).walletTotals(5L);
        assertThat(totals.movements()).isZero();
        assertThat(totals.added()).isEqualByComparingTo("0.00");
        verify(jdbc).queryForObject(contains("WHERE USER_ID = :ownerId"), eq(Map.of("ownerId", 5L)), any(RowMapper.class));
    }
}
