package com.acme.notify.common;

public enum Category {
    OTP(true), SECURITY(true), TRANSACTIONAL(false), MARKETING(false), SOCIAL_DIGEST(false);

    private final boolean critical;

    Category(boolean critical) { this.critical = critical; }

    /** Critical categories bypass quiet hours, frequency caps and opt-outs. */
    public boolean isCritical() { return critical; }

    public boolean isDigestible() { return this == SOCIAL_DIGEST; }
}
