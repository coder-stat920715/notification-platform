package com.acme.notify.workers.provider;

import com.acme.notify.workers.DeliveryResult;
import com.acme.notify.workers.PermanentDeliveryException;
import com.acme.notify.workers.RenderedNotification;
import com.acme.notify.workers.TransientProviderException;
import com.fasterxml.jackson.databind.JsonNode;
import io.github.resilience4j.bulkhead.annotation.Bulkhead;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;

/** Twilio via its REST API (no SDK needed). Delivery receipts arrive at /callbacks/twilio on the tracker. */
@Component
@Order(1)
@ConditionalOnProperty(name = "notify.providers.mode", havingValue = "live")
public class TwilioSmsProvider implements SmsProvider {

    private final RestClient client;
    private final String accountSid;
    private final String from;
    private final String callbackBase;

    public TwilioSmsProvider(RestClient.Builder builder,
                             @Value("${vendors.twilio.account-sid}") String accountSid,
                             @Value("${vendors.twilio.auth-token}") String authToken,
                             @Value("${vendors.twilio.from}") String from,
                             @Value("${vendors.twilio.callback-base-url}") String callbackBase) {
        this.client = builder.baseUrl("https://api.twilio.com")
                .defaultHeaders(h -> h.setBasicAuth(accountSid, authToken)).build();
        this.accountSid = accountSid;
        this.from = from;
        this.callbackBase = callbackBase;
    }

    @Override public String name() { return "twilio"; }

    @Override
    @CircuitBreaker(name = "twilio", fallbackMethod = "fallback")
    @Bulkhead(name = "twilio")
    public DeliveryResult send(RenderedNotification n) {
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("To", n.recipient());
        form.add("From", from);
        form.add("Body", n.body());
        form.add("StatusCallback", callbackBase + "/callbacks/twilio?notification_id=" + n.notificationId());

        JsonNode response = client.post()
                .uri("/2010-04-01/Accounts/{sid}/Messages.json", accountSid)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED).body(form).retrieve()
                .onStatus(HttpStatusCode::is4xxClientError, (rq, rs) -> {
                    throw new PermanentDeliveryException("Twilio rejected request: " + rs.getStatusCode());
                })
                .onStatus(HttpStatusCode::is5xxServerError, (rq, rs) -> {
                    throw new TransientProviderException("Twilio " + rs.getStatusCode());
                })
                .body(JsonNode.class);
        return DeliveryResult.sent(name(), response == null ? null : response.path("sid").asText(null));
    }

    public DeliveryResult fallback(RenderedNotification n, PermanentDeliveryException e) { throw e; }

    public DeliveryResult fallback(RenderedNotification n, Throwable t) {
        return DeliveryResult.retryable(name(), t.getMessage());
    }
}
