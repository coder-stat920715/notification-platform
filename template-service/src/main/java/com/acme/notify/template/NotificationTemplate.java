package com.acme.notify.template;

import com.acme.notify.common.Channel;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "notification_template",
        uniqueConstraints = @UniqueConstraint(name = "uk_template",
                columnNames = {"template_key", "channel", "locale", "version"}))
@Getter
@Setter
@NoArgsConstructor
public class NotificationTemplate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "template_key")
    private String templateKey;

    @Enumerated(EnumType.STRING)
    private Channel channel;

    private String locale;
    private int version = 1;
    private boolean active = true;

    @Column(columnDefinition = "text")
    private String subjectTemplate;

    @Column(columnDefinition = "text")
    private String bodyTemplate;

    public static NotificationTemplate of(String key, Channel channel, String locale, String subject, String body) {
        NotificationTemplate t = new NotificationTemplate();
        t.templateKey = key;
        t.channel = channel;
        t.locale = locale;
        t.subjectTemplate = subject;
        t.bodyTemplate = body;
        return t;
    }
}
