package com.thuanbui.foundry.security;

import com.thuanbui.foundry.services.FeatureDisabledException;
import com.thuanbui.foundry.services.FeatureFlagService;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

@Aspect
@Component
public class FeatureFlagAspect {

    private final FeatureFlagService featureFlagService;

    public FeatureFlagAspect(FeatureFlagService featureFlagService) {
        this.featureFlagService = featureFlagService;
    }

    @Around("@annotation(featureFlag)")
    public Object checkFeatureFlag(ProceedingJoinPoint joinPoint, FeatureFlag featureFlag) throws Throwable {
        if (!featureFlagService.isEnabled(featureFlag.value())) {
            throw new FeatureDisabledException(featureFlag.value());
        }
        return joinPoint.proceed();
    }
}
