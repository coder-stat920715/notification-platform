package com.acme.notify.template;

import com.acme.notify.common.Channel;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;

/** Separate bean so @Cacheable is applied through the Spring proxy. */
@Component
@RequiredArgsConstructor
public class TemplateLookup {

    private final TemplateRepository repository;

    @Cacheable(value = "templates", key = "#key + ':' + #channel + ':' + #locale", unless = "#result == null")
    public NotificationTemplate find(String key, Channel channel, String locale) {
        return repository
                .findFirstByTemplateKeyAndChannelAndLocaleAndActiveTrueOrderByVersionDesc(key, channel, locale)
                .orElse(null);
    }
}
