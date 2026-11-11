package com.acme.notify.api;

import com.acme.notify.common.NotificationStatusView;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationApiService service;

    @PostMapping
    public ResponseEntity<SendNotificationResponse> send(
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            @Valid @RequestBody SendNotificationRequest request) {
        return ResponseEntity.accepted().body(service.accept(idempotencyKey, request));
    }

    @GetMapping("/{id}")
    public NotificationStatusView status(@PathVariable UUID id) {
        return service.status(id);
    }
}
