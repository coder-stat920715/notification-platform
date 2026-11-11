package com.acme.notify.tracker;

import jakarta.servlet.http.HttpServletRequest;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.Base64;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

/** Verifies Twilio's X-Twilio-Signature: Base64(HMAC-SHA1(authToken, url + sorted POST params)). */
@Component
public class SignatureVerifier {

    private final boolean enabled;
    private final String authToken;
    private final String publicBaseUrl;

    public SignatureVerifier(@Value("${notify.twilio.verify-signature:false}") boolean enabled,
                             @Value("${notify.twilio.auth-token:}") String authToken,
                             @Value("${notify.twilio.public-base-url:http://localhost:8086}") String publicBaseUrl) {
        this.enabled = enabled;
        this.authToken = authToken;
        this.publicBaseUrl = publicBaseUrl;
    }

    public void verifyTwilio(HttpServletRequest request, String signature) {
        if (!enabled) return;
        String query = request.getQueryString();
        String url = publicBaseUrl + request.getRequestURI() + (query == null ? "" : "?" + query);

        Set<String> queryKeys = query == null ? Set.of()
                : Set.copyOf(Arrays.stream(query.split("&")).map(p -> p.split("=", 2)[0]).toList());
        Map<String, String> postParams = new TreeMap<>();
        request.getParameterMap().forEach((k, v) -> { if (!queryKeys.contains(k)) postParams.put(k, v[0]); });

        StringBuilder data = new StringBuilder(url);
        postParams.forEach((k, v) -> data.append(k).append(v));
        String expected = hmacSha1Base64(authToken, data.toString());
        boolean ok = signature != null && MessageDigest.isEqual(
                expected.getBytes(StandardCharsets.UTF_8), signature.getBytes(StandardCharsets.UTF_8));
        if (!ok) throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Invalid signature");
    }

    private static String hmacSha1Base64(String key, String data) {
        try {
            Mac mac = Mac.getInstance("HmacSHA1");
            mac.init(new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), "HmacSHA1"));
            return Base64.getEncoder().encodeToString(mac.doFinal(data.getBytes(StandardCharsets.UTF_8)));
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException(e);
        }
    }
}
