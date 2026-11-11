package com.acme.notify.workers;

import java.time.Duration;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.context.expression.MethodBasedEvaluationContext;
import org.springframework.core.DefaultParameterNameDiscoverer;
import org.springframework.core.ParameterNameDiscoverer;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.stereotype.Component;

/**
 * Redis-backed idempotency. States: PROCESSING (short lease) then DONE (long TTL).
 * A duplicate that finds DONE is skipped. A duplicate that finds PROCESSING is NOT acked: it throws so the
 * message retries later; this keeps the at-least-once guarantee if the first worker crashed mid-send.
 */
@Aspect
@Component
@RequiredArgsConstructor
@Slf4j
public class IdempotencyAspect {

    private static final String PREFIX = "notif:idem:";

    private final StringRedisTemplate redis;
    private final ExpressionParser parser = new SpelExpressionParser();
    private final ParameterNameDiscoverer names = new DefaultParameterNameDiscoverer();

    @Around("@annotation(idem)")
    public Object guard(ProceedingJoinPoint pjp, IdempotentNotification idem) throws Throwable {
        MethodSignature signature = (MethodSignature) pjp.getSignature();
        var context = new MethodBasedEvaluationContext(pjp.getTarget(), signature.getMethod(), pjp.getArgs(), names);
        String key = PREFIX + parser.parseExpression(idem.keyExpression()).getValue(context, String.class);

        Boolean acquired = redis.opsForValue()
                .setIfAbsent(key, "PROCESSING", Duration.ofSeconds(idem.processingLeaseSeconds()));
        if (!Boolean.TRUE.equals(acquired)) {
            String state = redis.opsForValue().get(key);
            if ("DONE".equals(state)) {
                log.info("Duplicate suppressed: {}", key);
                return null;
            }
            throw new TransientDeliveryException("Another worker is processing " + key);
        }
        try {
            Object result = pjp.proceed();
            redis.opsForValue().set(key, "DONE", Duration.ofSeconds(idem.doneTtlSeconds()));
            return result;
        } catch (Throwable t) {
            redis.delete(key);          // release so the retry can run
            throw t;
        }
    }
}
