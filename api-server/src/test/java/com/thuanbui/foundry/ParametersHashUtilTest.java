package com.thuanbui.foundry;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import com.thuanbui.foundry.util.ParametersHashUtil;
import java.util.Map;
import org.junit.jupiter.api.Test;

class ParametersHashUtilTest {

    @Test
    void shouldProduceDeterministicHash() {
        Map<String, Object> params1 = Map.of(
                "cacheVersion", 1,
                "day", 15,
                "gender", "MALE",
                "month", 6,
                "year", 1990
        );
        String hash1 = ParametersHashUtil.compute(params1);
        String hash2 = ParametersHashUtil.compute(params1);
        assertEquals(hash1, hash2, "Same parameters should produce same hash");
        assertEquals(64, hash1.length(), "SHA-256 hex should be 64 chars");
    }

    @Test
    void shouldBeOrderIndependent() {
        Map<String, Object> params1 = Map.of("a", 1, "b", 2);
        Map<String, Object> params2 = Map.of("b", 2, "a", 1);
        assertEquals(ParametersHashUtil.compute(params1),
                ParametersHashUtil.compute(params2),
                "Hash should be order-independent");
    }

    @Test
    void shouldDifferForDifferentInputs() {
        Map<String, Object> params1 = Map.of("year", 1990, "month", 6);
        Map<String, Object> params2 = Map.of("year", 1991, "month", 6);
        assertNotEquals(ParametersHashUtil.compute(params1),
                ParametersHashUtil.compute(params2),
                "Different inputs should produce different hashes");
    }
}
