package com.nexocrm;

import com.nexocrm.integration.webhook.WebhookSigner;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class WebhookSignerTest {

    @Test
    void firmaYVerificaElPayload() {
        String secret = WebhookSigner.newSecret();
        String payload = "{\"event\":\"DEAL_WON\"}";
        String signature = WebhookSigner.sign(payload, secret);

        assertThat(signature).startsWith("sha256=").hasSize(7 + 64);
        assertThat(WebhookSigner.verify(payload, secret, signature)).isTrue();
    }

    @Test
    void rechazaPayloadAlteradoOSecretoIncorrecto() {
        String secret = WebhookSigner.newSecret();
        String signature = WebhookSigner.sign("{\"amount\":100}", secret);

        assertThat(WebhookSigner.verify("{\"amount\":999}", secret, signature)).isFalse();
        assertThat(WebhookSigner.verify("{\"amount\":100}", WebhookSigner.newSecret(), signature)).isFalse();
        assertThat(WebhookSigner.verify("{\"amount\":100}", secret, null)).isFalse();
    }

    @Test
    void conocidoContraVectorDeReferencia() {
        // HMAC-SHA256("key", "The quick brown fox jumps over the lazy dog")
        assertThat(WebhookSigner.sign("The quick brown fox jumps over the lazy dog", "key"))
                .isEqualTo("sha256=f7bc83f430538424b13298e6aa6fb143ef4d59a14946175997479dbc2d1a3cd8");
    }
}
