package com.acme.notify.workers.provider;

import java.net.http.HttpClient;
import java.time.Duration;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestClientCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.JdkClientHttpRequestFactory;

@Configuration
public class ProviderConfig {

    @Bean
    ProviderRouter<EmailProvider> emailRouter(List<EmailProvider> providers) {
        return new ProviderRouter<>("email", providers);
    }

    @Bean
    ProviderRouter<SmsProvider> smsRouter(List<SmsProvider> providers) {
        return new ProviderRouter<>("sms", providers);
    }

    @Bean
    ProviderRouter<PushProvider> pushRouter(List<PushProvider> providers) {
        return new ProviderRouter<>("push", providers);
    }

    /** Tight timeouts on every outbound HTTP call: vendor latency must never stall a worker thread. */
    @Bean
    RestClientCustomizer vendorTimeouts(@Value("${notify.http.connect-timeout-ms:500}") long connectMs,
                                        @Value("${notify.http.read-timeout-ms:2500}") long readMs) {
        HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofMillis(connectMs)).build();
        JdkClientHttpRequestFactory factory = new JdkClientHttpRequestFactory(client);
        factory.setReadTimeout(Duration.ofMillis(readMs));
        ClientHttpRequestFactory shared = factory;
        return builder -> builder.requestFactory(shared);
    }
}
