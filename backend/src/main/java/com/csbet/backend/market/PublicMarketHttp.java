package com.csbet.backend.market;

import java.io.IOException;
import java.net.*;
import java.util.Set;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/** HTTP público, sem cookies/credenciais, redirects ou retentativas automáticas. */
final class PublicMarketHttp implements MarketJsonClient {
    private static final int MAX_BYTES = 8 * 1024 * 1024;
    private final JsonMapper mapper = new JsonMapper();
    public JsonNode get(URI uri) throws IOException {
        if (!"https".equals(uri.getScheme()) || !Set.of("market.csgo.com", "olinda.bcb.gov.br").contains(uri.getHost())
                || uri.getUserInfo() != null || uri.getPort() != -1) throw new IOException("Destino de cotações inválido.");
        var connection = (HttpURLConnection) uri.toURL().openConnection();
        connection.setInstanceFollowRedirects(false);
        connection.setConnectTimeout(10000);
        connection.setReadTimeout(15000);
        connection.setRequestProperty("Accept", "application/json");
        connection.setRequestProperty("User-Agent", "CSBet-Educational-References/1.0");
        try {
            int status = connection.getResponseCode();
            if (status != 200) throw new IOException("Fonte de referência respondeu HTTP " + status + ".");
            String type = connection.getContentType();
            if (type == null || !type.toLowerCase(java.util.Locale.ROOT).contains("application/json"))
                throw new IOException("A fonte não retornou JSON.");
            try (var input = connection.getInputStream()) {
                byte[] body = input.readNBytes(MAX_BYTES + 1);
                if (body.length > MAX_BYTES) throw new IOException("Resposta de referência excedeu o limite.");
                return mapper.readTree(body);
            }
        } finally { connection.disconnect(); }
    }
}

@FunctionalInterface
interface MarketJsonClient {
    JsonNode get(URI uri) throws Exception;
}
