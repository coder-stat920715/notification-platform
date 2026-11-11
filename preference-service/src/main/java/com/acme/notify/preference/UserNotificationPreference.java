package com.acme.notify.preference;

import com.acme.notify.common.Category;
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
import java.time.LocalTime;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "user_notification_preference",
        uniqueConstraints = @UniqueConstraint(name = "uk_pref", columnNames = {"user_id", "channel", "category"}))
@Getter
@Setter
@NoArgsConstructor
public class UserNotificationPreference {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id")
    private String userId;

    @Enumerated(EnumType.STRING)
    private Channel channel;

    @Enumerated(EnumType.STRING)
    private Category category;

    private boolean enabled = true;
    private LocalTime quietStart;
    private LocalTime quietEnd;
    private String timeZone = "UTC";
    private Integer maxPerHour;
    private String preferredLocale = "en";
}
