package com.thuanbui.foundry;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.thuanbui.foundry.config.FeatureProperties;
import com.thuanbui.foundry.security.FeatureFlag;
import com.thuanbui.foundry.security.FeatureFlagAspect;
import com.thuanbui.foundry.services.FeatureDisabledException;
import com.thuanbui.foundry.services.FeatureFlagService;
import java.util.Map;
import org.aspectj.lang.ProceedingJoinPoint;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class FeatureFlagAspectTest {

    @Mock
    private ProceedingJoinPoint joinPoint;

    private FeatureFlagAspect aspect;

    @BeforeEach
    void setUp() {
        FeatureProperties props = new FeatureProperties(Map.of(
                "notes", new FeatureProperties.Toggle(true),
                "echo", new FeatureProperties.Toggle(false)));
        FeatureFlagService service = new FeatureFlagService(props);
        aspect = new FeatureFlagAspect(service);
    }

    @Test
    void checkFeatureFlag_shouldThrow_whenDisabled() {
        FeatureFlag flag = createFeatureFlag("echo");
        assertThrows(FeatureDisabledException.class, () -> {
            aspect.checkFeatureFlag(joinPoint, flag);
        });
    }

    @Test
    void checkFeatureFlag_shouldThrow_whenUnknown() {
        FeatureFlag flag = createFeatureFlag("unknown");
        assertThrows(FeatureDisabledException.class, () -> aspect.checkFeatureFlag(joinPoint, flag));
    }

    @Test
    void checkFeatureFlag_shouldProceed_whenEnabled() throws Throwable {
        FeatureFlag flag = createFeatureFlag("notes");
        assertDoesNotThrow(() -> {
            aspect.checkFeatureFlag(joinPoint, flag);
        });
    }

    private FeatureFlag createFeatureFlag(String value) {
        return new FeatureFlag() {
            @Override
            public String value() {
                return value;
            }

            @Override
            public Class<? extends java.lang.annotation.Annotation> annotationType() {
                return FeatureFlag.class;
            }
        };
    }
}
