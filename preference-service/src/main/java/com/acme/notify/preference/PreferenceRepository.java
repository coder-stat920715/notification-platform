package com.acme.notify.preference;

import com.acme.notify.common.Category;
import com.acme.notify.common.Channel;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PreferenceRepository extends JpaRepository<UserNotificationPreference, Long> {
    List<UserNotificationPreference> findByUserId(String userId);
    Optional<UserNotificationPreference> findByUserIdAndChannelAndCategory(String userId, Channel channel, Category category);
}
