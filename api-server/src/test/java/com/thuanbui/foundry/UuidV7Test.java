package com.thuanbui.foundry;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import com.github.f4b6a3.uuid.UuidCreator;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class UuidV7Test {

    @Test
    void shouldGenerateUuidV7() {
        UUID uuid = UuidCreator.getTimeOrderedEpoch();
        assertEquals(7, uuid.version(), "UUID should be version 7");
    }

    @Test
    void shouldGenerateUniqueUuids() {
        UUID first = UuidCreator.getTimeOrderedEpoch();
        UUID second = UuidCreator.getTimeOrderedEpoch();
        assertNotEquals(first, second, "Each UUIDv7 should be unique");
    }
}
