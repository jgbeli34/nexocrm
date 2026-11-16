package com.nexocrm.integration.webhook;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.HexFormat;

/**
 * Firma HMAC-SHA256 de los payloads (mismo esquema que usan Stripe o GitHub).
 * El receptor recalcula la firma con el secreto compartido para verificar
 * que el mensaje viene de NexoCRM y no fue alterado.
 */
public final class WebhookSigner {

    public static final String SIGNATURE_HEADER = "X-NexoCRM-Signature";
    public static final String EVENT_HEADER = "X-NexoCRM-Event";
    public static final String DELIVERY_HEADER = "X-NexoCRM-Delivery";
    public static final String SUBSCRIPTION_HEADER = "X-NexoCRM-Subscription";

    private static final SecureRandom RANDOM = new SecureRandom();

    private WebhookSigner() {
    }

    public static String sign(String payload, String secret) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            return "sha256=" + HexFormat.of().formatHex(mac.doFinal(payload.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new IllegalStateException("No se pudo firmar el payload", e);
        }
    }

    /** Comparación en tiempo constante para evitar ataques de temporización. */
    public static boolean verify(String payload, String secret, String signature) {
        if (signature == null) {
            return false;
        }
        byte[] expected = sign(payload, secret).getBytes(StandardCharsets.UTF_8);
        return MessageDigest.isEqual(expected, signature.getBytes(StandardCharsets.UTF_8));
    }

    public static String newSecret() {
        byte[] bytes = new byte[24];
        RANDOM.nextBytes(bytes);
        return "whsec_" + HexFormat.of().formatHex(bytes);
    }
}
