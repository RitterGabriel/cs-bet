package com.csbet.backend.casecatalog;

import java.io.IOException;
import java.net.http.*;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;
import static org.assertj.core.api.Assertions.*;

class CatalogSourceLoaderTest {
    @SuppressWarnings("unchecked")
    private HttpResponse<byte[]> response(int status, byte[] bytes) {
        HttpResponse<byte[]> response = mock(HttpResponse.class);
        when(response.statusCode()).thenReturn(status); when(response.body()).thenReturn(bytes);
        return response;
    }
    @Test
    void missingPortugueseFallsBackToEnglishAtTheSameRevision() throws Exception {
        var client = mock(HttpClient.class);
        byte[] crates, skins;
        try (var c = getClass().getResourceAsStream("/catalog/crates.json");
             var s = getClass().getResourceAsStream("/catalog/skins.json")) {
            crates = c.readAllBytes(); skins = s.readAllBytes();
        }
        var missing = response(404, new byte[0]);
        var cratesResponse = response(200, crates);
        var skinsResponse = response(200, skins);
        when(client.send(any(HttpRequest.class), org.mockito.ArgumentMatchers.<HttpResponse.BodyHandler<byte[]>>any()))
                .thenReturn(missing, cratesResponse, skinsResponse);
        var catalog = new CatalogSourceLoader(client).remote(CatalogSourceLoader.REVISION, "pt-BR");
        assertThat(catalog.language()).isEqualTo("en");
        assertThat(catalog.cases()).hasSize(42);
        var requests = ArgumentCaptor.forClass(HttpRequest.class);
        verify(client, times(3)).send(requests.capture(), org.mockito.ArgumentMatchers.<HttpResponse.BodyHandler<byte[]>>any());
        assertThat(requests.getAllValues()).allSatisfy(request -> assertThat(request.uri().toString()).contains(CatalogSourceLoader.REVISION));
        assertThat(requests.getAllValues().getFirst().uri().toString()).contains("/pt-BR/");
        assertThat(requests.getAllValues().getLast().uri().toString()).contains("/en/");
    }
    @Test
    void networkAndHttpFailuresAreExplicitAndDoNotLeakResponseBodies() throws Exception {
        var client = mock(HttpClient.class);
        when(client.send(any(HttpRequest.class), org.mockito.ArgumentMatchers.<HttpResponse.BodyHandler<byte[]>>any()))
                .thenThrow(new IOException("sensitive internal details"));
        assertThatThrownBy(() -> new CatalogSourceLoader(client).remote(CatalogSourceLoader.REVISION, "en"))
                .isInstanceOf(SourceDataException.class).hasMessageContaining("Falha de rede")
                .hasMessageNotContaining("sensitive");
        reset(client);
        var unavailable = response(503, "private html".getBytes());
        when(client.send(any(HttpRequest.class), org.mockito.ArgumentMatchers.<HttpResponse.BodyHandler<byte[]>>any()))
                .thenReturn(unavailable);
        assertThatThrownBy(() -> new CatalogSourceLoader(client).remote(CatalogSourceLoader.REVISION, "en"))
                .isInstanceOf(SourceDataException.class).hasMessageContaining("erro HTTP").hasMessageNotContaining("private");
    }
}
