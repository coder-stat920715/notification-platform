package com.acme.notify.workers.provider;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.messaging.FirebaseMessaging;
import java.io.IOException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.sesv2.SesV2Client;

/** Vendor SDK clients, only created in live mode. Credentials come from the environment, never source. */
@Configuration
@ConditionalOnProperty(name = "notify.providers.mode", havingValue = "live")
public class LiveVendorConfig {

    @Bean(destroyMethod = "close")
    SesV2Client sesClient(@Value("${vendors.ses.region:us-east-1}") String region) {
        return SesV2Client.builder().region(Region.of(region)).build();   // default AWS credential chain
    }

    @Bean
    FirebaseMessaging firebaseMessaging() throws IOException {
        FirebaseOptions options = FirebaseOptions.builder()
                .setCredentials(GoogleCredentials.getApplicationDefault())   // GOOGLE_APPLICATION_CREDENTIALS
                .build();
        FirebaseApp app = FirebaseApp.getApps().isEmpty() ? FirebaseApp.initializeApp(options) : FirebaseApp.getInstance();
        return FirebaseMessaging.getInstance(app);
    }
}
