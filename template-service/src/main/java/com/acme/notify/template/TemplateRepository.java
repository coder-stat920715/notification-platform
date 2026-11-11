package com.acme.notify.template;

import com.acme.notify.common.Channel;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TemplateRepository extends JpaRepository<NotificationTemplate, Long> {
    Optional<NotificationTemplate> findFirstByTemplateKeyAndChannelAndLocaleAndActiveTrueOrderByVersionDesc(
            String templateKey, Channel channel, String locale);

    long countByTemplateKey(String templateKey);
}
