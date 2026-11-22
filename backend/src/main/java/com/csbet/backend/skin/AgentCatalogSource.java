package com.csbet.backend.skin;

import com.csbet.backend.casecatalog.SourceCatalog.SourceRarity;
import com.csbet.backend.casecatalog.SourceDataException;
import java.io.*;
import java.net.URI;
import java.security.MessageDigest;
import java.util.*;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/** Snapshot ByMykel na mesma revisão do catálogo; imagens da Valve apenas por URL. */
public final class AgentCatalogSource {
    public static final String AGENT_TYPE = "agent";
    public record Agent(String id, String name, String teamId, String teamName, String marketHashName, String imageUrl, SourceRarity rarity) { }
    private AgentCatalogSource() { }

    public static List<Agent> bundled() {
        try (var manifest = resource("agents-manifest.json"); var data = resource("agents.json")) {
            var mapper = new JsonMapper(); var metadata = mapper.readTree(manifest); byte[] bytes = data.readAllBytes();
            if (!com.csbet.backend.casecatalog.CatalogSourceLoader.REVISION.equals(metadata.path("revision").asText())
                    || !"en".equals(metadata.path("language").asText())) throw new SourceDataException("Revisão de agentes incompatível com o catálogo.");
            String hash = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
            if (!hash.equals(metadata.path("bundledSha256").asText())) throw new SourceDataException("Snapshot de agentes divergente do manifesto.");
            var agents = read(new ByteArrayInputStream(bytes));
            if (agents.size() != metadata.path("items").asInt()) throw new SourceDataException("Contagem de agentes divergente.");
            return agents;
        } catch (SourceDataException failure) { throw failure; }
        catch (Exception failure) { throw new SourceDataException("Snapshot de agentes indisponível ou inválido."); }
    }
    static List<Agent> read(InputStream input) {
        try {
            var array = new JsonMapper().readTree(input);
            if (!array.isArray() || array.isEmpty() || array.size()>1000) throw new IllegalArgumentException();
            List<Agent> result = new ArrayList<>(); Set<String> ids = new HashSet<>(), hashes = new HashSet<>();
            for (var entry : array) {
                String id = text(entry,"id",120), name = text(entry,"name",250);
                String model = text(entry,"model_player",500), teamId = text(entry.path("team"),"id",40);
                String teamName = text(entry.path("team"),"name",120), market = text(entry,"market_hash_name",300);
                var rarity = entry.path("rarity"); String rarityId = text(rarity,"id",80), color = text(rarity,"color",7);
                String image = text(entry,"image",2048); var uri = URI.create(image);
                if (!id.startsWith("agent-") || !ids.add(id) || !hashes.add(market) || !model.startsWith("agents/models/")
                        || !Set.of("counter-terrorists","terrorists").contains(teamId)
                        || !teamName.equals(teamId.equals("terrorists") ? "Terrorist" : "Counter-Terrorist") || !rarityId.endsWith("_character")
                        || !color.matches("#[a-fA-F0-9]{6}") || !"https".equals(uri.getScheme()) || uri.getUserInfo()!=null
                        || !Set.of("community.akamai.steamstatic.com","cdn.steamstatic.com","raw.githubusercontent.com").contains(uri.getHost()))
                    throw new IllegalArgumentException();
                result.add(new Agent(id,name,teamId,teamName,market,image,new SourceRarity(rarityId,text(rarity,"name",100),color)));
            }
            return List.copyOf(result);
        } catch (Exception failure) { throw new SourceDataException("Dados de agentes inválidos; nenhuma importação realizada."); }
    }
    private static String text(JsonNode node,String field,int max) {
        var value = node.path(field);
        if (!value.isString() || value.asText().isBlank() || value.asText().length()>max) throw new IllegalArgumentException();
        return value.asText();
    }
    private static InputStream resource(String file) {
        var input = AgentCatalogSource.class.getResourceAsStream("/catalog/"+file);
        if (input==null) throw new SourceDataException("Arquivo de agentes ausente."); return input;
    }
}
