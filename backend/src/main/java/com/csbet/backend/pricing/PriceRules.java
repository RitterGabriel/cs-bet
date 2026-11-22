package com.csbet.backend.pricing;

import java.math.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDate;
import java.util.Map;

/**
 * EDUCATIONAL_V1: créditos arbitrários, sem relação com marketplaces ou dinheiro.
 * Base por raridade: Consumer=20, Industrial=50, Mil-Spec=100, Restricted=500,
 * Classified=2500, Covert=10000, Extraordinary=25000. Desconhecida=100.
 * SHA-256(ID externo + dia) define -200..200 pontos-base (-2%..2%).
 * Arredondamento HALF_UP, piso de 0,01 e faixa de 50%..150% da base.
 * Não simula uma distribuição estatística nem fornece probabilidades de caixas.
 */
public final class PriceRules {
    public static final String VERSION = "EDUCATIONAL_V1";
    private static final Map<String, BigDecimal> BASES = Map.of(
            "rarity_common_weapon", new BigDecimal("20.00"),
            "rarity_uncommon_weapon", new BigDecimal("50.00"),
            "rarity_rare_weapon", new BigDecimal("100.00"),
            "rarity_mythical_weapon", new BigDecimal("500.00"),
            "rarity_legendary_weapon", new BigDecimal("2500.00"),
            "rarity_ancient_weapon", new BigDecimal("10000.00"),
            "rarity_ancient", new BigDecimal("25000.00"));
    private PriceRules() { }
    public static BigDecimal base(String rarityId) { return BASES.getOrDefault(rarityId, new BigDecimal("100.00")); }
    public static BigDecimal next(String externalId, LocalDate day, BigDecimal current, BigDecimal base) {
        try {
            byte[] hash = MessageDigest.getInstance("SHA-256").digest((externalId + ":" + day).getBytes(StandardCharsets.UTF_8));
            int basisPoints = new BigInteger(1, hash).mod(BigInteger.valueOf(401)).intValue() - 200;
            BigDecimal factor = BigDecimal.ONE.add(BigDecimal.valueOf(basisPoints, 4));
            BigDecimal low = base.multiply(new BigDecimal("0.50")).max(new BigDecimal("0.01"));
            BigDecimal high = base.multiply(new BigDecimal("1.50"));
            BigDecimal dailyLow = current.multiply(new BigDecimal("0.98")).setScale(2, RoundingMode.CEILING);
            BigDecimal dailyHigh = current.multiply(new BigDecimal("1.02")).setScale(2, RoundingMode.FLOOR);
            return current.multiply(factor).setScale(2, RoundingMode.HALF_UP)
                    .max(dailyLow).min(dailyHigh)
                    .max(low.setScale(2, RoundingMode.CEILING)).min(high.setScale(2, RoundingMode.FLOOR));
        } catch (java.security.NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 indisponível.");
        }
    }
}
