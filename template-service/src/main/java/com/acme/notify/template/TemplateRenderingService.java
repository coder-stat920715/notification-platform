package com.acme.notify.template;

import com.acme.notify.common.Channel;
import com.acme.notify.common.RenderRequest;
import com.acme.notify.common.RenderedContent;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;
import org.thymeleaf.templatemode.TemplateMode;
import org.thymeleaf.templateresolver.StringTemplateResolver;

/**
 * Renders stored templates with Thymeleaf. Only trusted, reviewed templates from the database are
 * rendered (never request-supplied template text), which avoids template injection.
 */
@Service
@RequiredArgsConstructor
public class TemplateRenderingService {

    private final TemplateLookup lookup;
    private final TemplateEngine htmlEngine = engine(TemplateMode.HTML);
    private final TemplateEngine textEngine = engine(TemplateMode.TEXT);

    public RenderedContent render(RenderRequest request) {
        Locale locale = Locale.forLanguageTag(request.locale() == null ? "en" : request.locale());
        NotificationTemplate template = resolve(request.templateKey(), request.channel(), locale);

        Context ctx = new Context(locale, request.variables() == null ? Map.of() : request.variables());
        TemplateEngine bodyEngine = request.channel() == Channel.EMAIL ? htmlEngine : textEngine;
        String subject = template.getSubjectTemplate() == null ? ""
                : textEngine.process(template.getSubjectTemplate(), ctx);
        String body = bodyEngine.process(template.getBodyTemplate(), ctx);
        return new RenderedContent(subject, body);
    }

    /** Fallback chain: en-US, then en, then the default locale. */
    private NotificationTemplate resolve(String key, Channel channel, Locale locale) {
        Set<String> candidates = new LinkedHashSet<>();
        candidates.add(locale.toLanguageTag());
        candidates.add(locale.getLanguage());
        candidates.add("en");
        for (String candidate : candidates) {
            NotificationTemplate t = lookup.find(key, channel, candidate);
            if (t != null) return t;
        }
        throw new TemplateNotFoundException("No template '%s' for %s (%s)".formatted(key, channel, locale));
    }

    private static TemplateEngine engine(TemplateMode mode) {
        StringTemplateResolver resolver = new StringTemplateResolver();
        resolver.setTemplateMode(mode);
        resolver.setCacheable(true);
        TemplateEngine engine = new TemplateEngine();
        engine.setTemplateResolver(resolver);
        return engine;
    }
}
