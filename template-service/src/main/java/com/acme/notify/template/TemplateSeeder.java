package com.acme.notify.template;

import com.acme.notify.common.Channel;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

/** Seeds starter templates (en, plus a Hindi OTP) for every channel. */
@Component
@RequiredArgsConstructor
public class TemplateSeeder implements CommandLineRunner {

    private final TemplateRepository repository;

    @Override
    public void run(String... args) {
        seed("otp", "Your verification code", "Your code is [[${code}]]. It expires in [[${minutes}]] minutes.");
        seed("welcome", "Welcome, [[${name}]]!", "Hi [[${name}]], thanks for joining us.");
        seed("marketing.promo", "[[${headline}]]", "[[${headline}]] - use code [[${promoCode}]] before it expires.");
        seed("social.like", "New like", "[[${actorName}]] liked your comment.");
        seed("social.like.digest", "[[${count}]] people liked your comment",
                "[[${count}]] people liked your comment, including [[${actors}]].");
        if (repository.countByTemplateKey("otp") > 0
                && repository.findFirstByTemplateKeyAndChannelAndLocaleAndActiveTrueOrderByVersionDesc(
                        "otp", Channel.SMS, "hi").isEmpty()) {
            for (Channel c : Channel.values()) {
                repository.save(NotificationTemplate.of("otp", c, "hi", "आपका सत्यापन कोड",
                        "आपका कोड [[${code}]] है। यह [[${minutes}]] मिनट में समाप्त हो जाएगा।"));
            }
        }
    }

    private void seed(String key, String subject, String body) {
        if (repository.countByTemplateKey(key) > 0) return;
        for (Channel c : Channel.values()) {
            repository.save(NotificationTemplate.of(key, c, "en", subject, body));
        }
    }
}
