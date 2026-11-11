package com.acme.notify.workers.provider;

import com.acme.notify.workers.DeliveryResult;
import com.acme.notify.workers.PermanentDeliveryException;
import com.acme.notify.workers.RenderedNotification;
import com.acme.notify.workers.TransientProviderException;
import io.github.resilience4j.bulkhead.annotation.Bulkhead;
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
@Order(1)
@ConditionalOnProperty(name = "notify.providers.mode", havingValue = "live")
public class SendGridEmailProvider implements EmailProvider {

    private final RestClient client;
    private final String from;

    public SendGridEmailProvider(RestClient.Builder builder,
                                 @Value("${vendors.sendgrid.api-key}") String apiKey,
                                 @Value("${vendors.email.from}") String from) {
        this.client = builder.baseUrl("https://api.sendgrid.com")
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey).build();
        this.from = from;
    }

    @Override public String name() { return "sendgrid"; }

    @Override
    @CircuitBreaker(name = "sendgrid", fallbackMethod = "fallback")
    @Bulkhead(name = "sendgrid")
    public DeliveryResult send(RenderedNotification n) {
        Map<String, Object> body = Map.of(
                "personalizations", List.of(Map.of("to", List.of(Map.of("email", n.recipient())))),
                "from", Map.of("email", from),
                "subject", n.subject(),
                "content", List.of(Map.of("type", "text/html", "value", n.body())),
                "custom_args", Map.of("notification_id", n.notificationId()));
        var response = client.post().uri("/v3/mail/send").body(body).retrieve()
                .onStatus(HttpStatusCode::is4xxClientError, (rq, rs) -> {
                    throw new PermanentDeliveryException("SendGrid rejected request: " + rs.getStatusCode());
                })
                .onStatus(HttpStatusCode::is5xxServerError, (rq, rs) -> {
                    throw new TransientProviderException("SendGrid " + rs.getStatusCode());
                })
                .toBodilessEntity();
        return DeliveryResult.sent(name(), response.getHeaders().getFirst("X-Message-Id"));
    }

    public DeliveryResult fallback(RenderedNotification n, PermanentDeliveryException e) { throw e; }

    public DeliveryResult fallback(RenderedNotification n, Throwable t) {
        return DeliveryResult.retryable(name(), t.getMessage());
    }
}
