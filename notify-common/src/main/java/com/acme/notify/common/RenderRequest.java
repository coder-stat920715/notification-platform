package com.acme.notify.common;

import java.util.Map;

public record RenderRequest(String templateKey, Channel channel, String locale, Map<String, Object> variables) {}
