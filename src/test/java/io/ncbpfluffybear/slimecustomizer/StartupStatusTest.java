package io.ncbpfluffybear.slimecustomizer;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class StartupStatusTest {
    @Test
    void successfulRegistrationReportsReady() {
        StartupStatus status = new StartupStatus();
        status.stage("categories.yml");
        status.stage("items.yml");
        status.ready();

        assertTrue(status.isReady());
        assertEquals("ready", status.stage());
        assertNull(status.failure());
    }

    @Test
    void registrationFailureReportsStageAndReason() {
        StartupStatus status = new StartupStatus();
        status.stage("machines.yml");
        status.failed(new IllegalArgumentException("Unknown custom material"));

        assertFalse(status.isReady());
        assertEquals("machines.yml", status.stage());
        assertEquals("Unknown custom material", status.failure());
    }

    @Test
    void blankMessageStillReportsExceptionClass() {
        StartupStatus status = new StartupStatus();
        status.stage("items.yml");
        status.failed(new IllegalStateException());

        assertEquals("IllegalStateException", status.failure());
    }
}
