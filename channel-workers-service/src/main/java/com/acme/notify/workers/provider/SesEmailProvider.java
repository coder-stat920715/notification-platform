package com.acme.notify.workers.provider;

import com.acme.notify.workers.DeliveryResult;
import com.acme.notify.workers.PermanentDeliveryException;
import com.acme.notify.workers.RenderedNotification;
import com.acme.notify.workers.TransientProviderException;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.services.sesv2.SesV2Client;
import software.amazon.awssdk.services.sesv2.model.BadRequestException;
import software.amazon.awssdk.services.sesv2.model.Body;
import software.amazon.awssdk.services.sesv2.model.Content;
import software.amazon.awssdk.services.sesv2.model.Destination;
import software.amazon.awssdk.services.sesv2.model.EmailContent;
import software.amazon.awssdk.services.sesv2.model.Message;
import software.amazon.awssdk.services.sesv2.model.MessageRejectedException;
import software.amazon.awssdk.services.sesv2.model.MessageTag;
import software.amazon.awssdk.services.sesv2.model.SendEmailRequest;

@Component
@Order(2)
@ConditionalOnProperty(name = "notify.providers.mode", havingValue = "live")
public class SesEmailProvider implements EmailProvider {

    private final SesV2Client ses;
    private final String from;

    public SesEmailProvider(SesV2Client ses, @Value("${vendors.email.from}") String from) {
        this.ses = ses;
        this.from = from;
    }

    @Override public String name() { return "ses"; }

    @Override
    @CircuitBreaker(name = "ses", fallbackMethod = "fallback")
    public DeliveryResult send(RenderedNotification n) {
        try {
            var request = SendEmailRequest.builder()
                    .fromEmailAddress(from)
                    .destination(Destination.builder().toAddresses(n.recipient()).build())
                    .content(EmailContent.builder().simple(Message.builder()
                            .subject(Content.builder().data(n.subject()).build())
                            .body(Body.builder().html(Content.builder().data(n.body()).build()).build())
                            .build()).build())
                    .emailTags(MessageTag.builder().name("notification_id").value(n.notificationId()).build())
                    .build();
            return DeliveryResult.sent(name(), ses.sendEmail(request).messageId());
        } catch (MessageRejectedException | BadRequestException e) {
            throw new PermanentDeliveryException("SES rejected: " + e.getMessage());
        } catch (SdkException e) {
            throw new TransientProviderException("SES error: " + e.getMessage());
        }
    }

    public DeliveryResult fallback(RenderedNotification n, PermanentDeliveryException e) { throw e; }

    public DeliveryResult fallback(RenderedNotification n, Throwable t) {
        return DeliveryResult.retryable(name(), t.getMessage());
    }
}
