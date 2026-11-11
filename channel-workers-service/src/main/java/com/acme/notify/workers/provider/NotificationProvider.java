package com.acme.notify.workers.provider;

public interface NotificationProvider {
    String name();
    default boolean isEnabled() { return true; }
}
