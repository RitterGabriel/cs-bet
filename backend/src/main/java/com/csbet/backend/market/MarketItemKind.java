package com.csbet.backend.market;

public enum MarketItemKind {
    ALL("1=1"), SKINS("s.weaponType<>'agent'"), AGENTS("s.weaponType='agent'");
    private final String predicate;
    MarketItemKind(String predicate) { this.predicate=predicate; }
    String predicate() { return predicate; }
    static MarketItemKind fromFilter(String value) {
        return switch(value) {
            case "all" -> ALL;
            case "skins" -> SKINS;
            case "agents" -> AGENTS;
            default -> throw new IllegalArgumentException("Tipo de item inválido.");
        };
    }
}
