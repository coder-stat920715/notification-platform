package com.acme.notify.workers;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

/** Local echo receiver so the demo user's webhook channel has somewhere to deliver. Remove in production. */
@RestController
@Slf4j
public class DemoWebhookController {

    @PostMapping("/demo/webhook")
    public ResponseEntity<Void> receive(@RequestBody String body,
                                        @RequestHeader("X-Signature") String signature,
                                        @RequestHeader("X-Notification-Id") String id) {
        log.info("Demo webhook received id={} signature={} body={}", id, signature, body);
        return ResponseEntity.ok().build();
    }
}
