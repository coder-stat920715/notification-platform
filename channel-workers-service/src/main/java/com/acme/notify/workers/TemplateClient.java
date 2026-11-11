package com.acme.notify.workers;

import com.acme.notify.common.RenderRequest;
import com.acme.notify.common.RenderedContent;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Component
public class TemplateClient {

    private final RestClient client;

    public TemplateClient(RestClient.Builder builder, @Value("${notify.services.template-url}") String baseUrl) {
        this.client = builder.baseUrl(baseUrl).build();
    }

    public RenderedContent render(RenderRequest request) {
        try {
            RenderedContent content = client.post().uri("/internal/templates/render").body(request)
                    .retrieve().body(RenderedContent.class);
            if (content == null) throw new TransientDeliveryException("Empty render response");
            return content;
        } catch (HttpClientErrorException e) {
            if (e.getStatusCode() == HttpStatus.NOT_FOUND) {
                throw new PermanentDeliveryException("Template not found: " + request.templateKey());
            }
            throw new PermanentDeliveryException("Template request rejected: " + e.getStatusCode());
        } catch (RestClientException e) {
            throw new TransientDeliveryException("Template service unavailable: " + e.getMessage());
        }
    }
}
