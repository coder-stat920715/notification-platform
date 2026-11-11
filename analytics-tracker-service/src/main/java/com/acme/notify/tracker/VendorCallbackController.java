package com.acme.notify.tracker;

import com.acme.notify.common.Channel;
import com.acme.notify.common.DeliveryStatus;
import com.acme.notify.common.StatusEvent;
import jakarta.servlet.http.HttpServletRequest;
import java.time.Instant;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Receives vendor delivery receipts and client read receipts and maps them onto the state machine. */
@RestController
@RequestMapping("/callbacks")
@RequiredArgsConstructor
public class VendorCallbackController {

    private final StatusRecorder recorder;
    private final AuditRepository audit;
    private final SignatureVerifier verifier;

    @PostMapping("/twilio")
    public ResponseEntity<Void> twilio(
            @RequestParam("notification_id") UUID notificationId,
            @RequestParam(name = "MessageStatus", defaultValue = "") String messageStatus,
            @RequestParam(name = "MessageSid", required = false) String messageSid,
            @RequestParam(name = "ErrorCode", required = false) String errorCode,
            @RequestHeader(name = "X-Twilio-Signature", required = false) String signature,
            HttpServletRequest request) {

        verifier.verifyTwilio(request, signature);

        DeliveryStatus status = switch (messageStatus) {
            case "delivered" -> DeliveryStatus.DELIVERED;
            case "failed", "undelivered" -> DeliveryStatus.FAILED;
            default -> null;           // queued / sent / sending carry no new information for us
        };
        if (status != null) {
            audit.findTopByNotificationIdAndChannelOrderByIdDesc(notificationId, Channel.SMS).ifPresent(last ->
                    recorder.record(new StatusEvent(notificationId, last.getUserId(), Channel.SMS, status,
                            "twilio", messageSid, 0, errorCode, Instant.now())));
        }
        return ResponseEntity.ok().build();
    }

    @PostMapping("/read/{notificationId}")
    public ResponseEntity<Void> read(@PathVariable UUID notificationId, @RequestParam Channel channel) {
        audit.findTopByNotificationIdAndChannelOrderByIdDesc(notificationId, channel).ifPresent(last ->
                recorder.record(new StatusEvent(notificationId, last.getUserId(), channel, DeliveryStatus.READ,
                        "client", null, 0, null, Instant.now())));
        return ResponseEntity.noContent().build();
    }
}
