package com.acme.notify.workers.handler;

import com.acme.notify.common.Channel;
import com.acme.notify.workers.DeliveryResult;
import com.acme.notify.workers.PermanentDeliveryException;
import com.acme.notify.workers.RenderedNotification;
import com.acme.notify.workers.TransientProviderException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.time.Instant;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.Map;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/** Signs the JSON envelope with HMAC-SHA256 so receivers can verify it (headers X-Signature, X-Timestamp). */
@Component
public class WebhookChannelHandler implements NotificationChannelHandler {

    private final RestClient client;
    private final ObjectMapper mapper;
    private final String signingSecret;

    public WebhookChannelHandler(RestClient.Builder builder, ObjectMapper mapper,
                                 @Value("${notify.webhook.signing-secret}") String signingSecret) {
        this.client = builder.build();
        this.mapper = mapper;
        this.signingSecret = signingSecret;
    }

    @Override public Channel channel() { return Channel.WEBHOOK; }

    @Override
    public DeliveryResult send(RenderedNotification n) {
        Map<String, Object> envelope = new LinkedHashMap<>();
        envelope.put("id", n.notificationId());
        envelope.put("userId", n.userId());
        envelope.put("subject", n.subject());
        envelope.put("body", n.body());
        String payload = toJson(envelope);
        String timestamp = String.valueOf(Instant.now().getEpochSecond());

        client.post().uri(java.net.URI.create(n.recipient()))
                .contentType(MediaType.APPLICATION_JSON)
                .header("X-Signature", hmacSha256(signingSecret, timestamp + "." + payload))
                .header("X-Timestamp", timestamp)
                .header("X-Notification-Id", n.notificationId())
                .body(payload).retrieve()
                .onStatus(HttpStatusCode::is4xxClientError, (rq, rs) -> {
                    throw new PermanentDeliveryException("Webhook rejected: " + rs.getStatusCode());
                })
                .onStatus(HttpStatusCode::is5xxServerError, (rq, rs) -> {
                    throw new TransientProviderException("Webhook " + rs.getStatusCode());
                })
                .toBodilessEntity();
        return DeliveryResult.sent("webhook", n.notificationId());
    }

    private String toJson(Object o) {
        try {
            return mapper.writeValueAsString(o);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException(e);
        }
    }

    static String hmacSha256(String secret, String data) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            return HexFormat.of().formatHex(mac.doFinal(data.getBytes(StandardCharsets.UTF_8)));
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException(e);
        }
    }
}
