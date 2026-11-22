package com.csbet.backend.casecatalog;

import java.io.*;
import java.net.URI;
import java.net.http.*;
import java.nio.file.*;
import java.security.MessageDigest;
import java.time.Duration;
import java.util.*;
import tools.jackson.databind.json.JsonMapper;

/** Snapshot reproduzível: metadados projetados, licença e hashes em resources/catalog. */
public class CatalogSourceLoader {
    public static final String REVISION = "a9657126c997f5182306c5b78a01e92eb4153a08";
    private final SourceCatalogReader reader = new SourceCatalogReader();
    private final HttpClient client;
    public CatalogSourceLoader() { this(HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(15)).build()); }
    CatalogSourceLoader(HttpClient client) { this.client = client; }

    public SourceCatalog bundled() {
        try (InputStream manifestInput = resource("manifest.json")) {
            var manifest = new JsonMapper().readTree(manifestInput);
            byte[] crates = verified("crates", manifest.path("files").path("crates").path("bundledSha256").asText());
            byte[] skins = verified("skins", manifest.path("files").path("skins").path("bundledSha256").asText());
            return reader.read(new ByteArrayInputStream(crates), new ByteArrayInputStream(skins),
                    manifest.path("revision").asText(), manifest.path("language").asText());
        } catch (SourceDataException exception) { throw exception; }
        catch (Exception exception) { throw new SourceDataException("Snapshot local indisponível ou inválido."); }
    }

    public SourceCatalog local(Path crates, Path skins, String revision, String language) {
        try (InputStream boxes = Files.newInputStream(crates); InputStream items = Files.newInputStream(skins)) {
            return reader.read(boxes, items, revision, language);
        } catch (IOException exception) { throw new SourceDataException("Não foi possível ler os arquivos locais da fonte."); }
    }

    public SourceCatalog remote(String revision, String preferredLanguage) {
        if (!revision.matches("[a-f0-9]{40}") || !Set.of("en", "pt-BR", "pt-PT").contains(preferredLanguage)) {
            throw new SourceDataException("Informe uma revisão Git completa e um idioma suportado.");
        }
        // Nunca mistura idiomas/revisões nem usa páginas da Steam. pt-BR indisponível -> par en.
        List<String> languages = preferredLanguage.equals("en") ? List.of("en") : List.of(preferredLanguage, "en");
        for (String language : languages) {
            byte[] crates = download(client, revision, language, "crates");
            if (crates == null) continue;
            byte[] skins = download(client, revision, language, "skins");
            if (skins == null) continue;
            return reader.read(new ByteArrayInputStream(crates), new ByteArrayInputStream(skins), revision, language);
        }
        throw new SourceDataException("Arquivos não encontrados na revisão informada. Use o snapshot local ou confira a fonte.");
    }

    private byte[] download(HttpClient client, String revision, String language, String file) {
        URI uri = URI.create("https://raw.githubusercontent.com/ByMykel/CSGO-API/" + revision + "/public/api/" + language + "/" + file + ".json");
        try {
            var request = HttpRequest.newBuilder(uri).timeout(Duration.ofSeconds(30)).GET().build();
            var response = client.send(request, HttpResponse.BodyHandlers.ofByteArray());
            if (response.statusCode() == 404) return null;
            if (response.statusCode() != 200 || response.body().length > 25_000_000) {
                throw new SourceDataException("A fonte retornou erro HTTP ou excedeu o limite de tamanho. Nenhum dado foi importado.");
            }
            return response.body();
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new SourceDataException("Consulta à fonte interrompida.");
        } catch (IOException exception) {
            throw new SourceDataException("Falha de rede ao consultar a fonte. Use o snapshot local ou tente novamente.");
        }
    }
    private static byte[] verified(String file, String expected) throws Exception {
        try (InputStream stream = resource(file + ".json")) {
            byte[] bytes = stream.readAllBytes();
            String hash = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
            if (!hash.equals(expected)) throw new SourceDataException("Hash do snapshot inválido; importação cancelada.");
            return bytes;
        }
    }
    private static InputStream resource(String name) {
        InputStream stream = CatalogSourceLoader.class.getResourceAsStream("/catalog/" + name);
        if (stream == null) throw new SourceDataException("Arquivo do snapshot não encontrado.");
        return stream;
    }
}
