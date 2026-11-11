package com.acme.notify.common;

import java.util.EnumMap;
import java.util.Map;

public record ContactView(String userId, String email, String phone, String deviceToken, String webhookUrl) {

    /** Channel to destination address, omitting channels the user has no address for. */
    public Map<Channel, String> asRecipients() {
        Map<Channel, String> m = new EnumMap<>(Channel.class);
        if (email != null && !email.isBlank()) m.put(Channel.EMAIL, email);
        if (phone != null && !phone.isBlank()) m.put(Channel.SMS, phone);
        if (deviceToken != null && !deviceToken.isBlank()) m.put(Channel.PUSH, deviceToken);
        if (webhookUrl != null && !webhookUrl.isBlank()) m.put(Channel.WEBHOOK, webhookUrl);
        m.put(Channel.IN_APP, userId);
        return m;
    }
}
