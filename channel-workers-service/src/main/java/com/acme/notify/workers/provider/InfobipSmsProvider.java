package com.acme.notify.workers.provider;

import com.acme.notify.workers.DeliveryResult;
import com.acme.notify.workers.PermanentDeliveryException;
import com.acme.notify.workers.RenderedNotification;
import com.acme.notify.workers.TransientProviderException;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
@Order(2)
@ConditionalOnProperty(name = "notify.providers.mode", havingValue = "live")
public class InfobipSmsProvider implements SmsProvider {

    private final RestClient client;
    private final String sender;

    public InfobipSmsProvider(RestClient.Builder builder,
                              @Value("${vendors.infobip.base-url}") String baseUrl,
                              @Value("${vendors.infobip.api-key}") String apiKey,
                              @Value("${vendors.infobip.sender}") String sender) {
        this.client = builder.baseUrl(baseUrl).defaultHeader(HttpHeaders.AUTHORIZATION, "App " + apiKey).build();
        this.sender = sender;
    }

    @Override public String name() { return "infobip"; }

    @Override
    @CircuitBreaker(name = "infobip", fallbackMethod = "fallback")
    public DeliveryResult send(RenderedNotification n) {
        Map<String, Object> body = Map.of("messages", List.of(Map.of(
                "destinations", List.of(Map.of("to", n.recipient(), "messageId", n.notificationId())),
                "from", sender, "text", n.body())));
        client.post().uri("/sms/2/text/advanced").body(body).retrieve()
                .onStatus(HttpStatusCode::is4xxClientError, (rq, rs) -> {
                    throw new PermanentDeliveryException("Infobip rejected request: " + rs.getStatusCode());
                })
                .onStatus(HttpStatusCode::is5xxServerError, (rq, rs) -> {
                    throw new TransientProviderException("Infobip " + rs.getStatusCode());
                })
                .toBodilessEntity();
        return DeliveryResult.sent(name(), n.notificationId());
    }

    public DeliveryResult fallback(RenderedNotification n, PermanentDeliveryException e) { throw e; }

    public DeliveryResult fallback(RenderedNotification n, Throwable t) {
        return DeliveryResult.retryable(name(), t.getMessage());
    }
}
