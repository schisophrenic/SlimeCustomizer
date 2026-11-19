package io.ncbpfluffybear.slimecustomizer;

/**
 * Main-thread startup state reported by /sc status. Registration errors do
 * not deactivate the command or editor while server configuration is repaired.
 */
public final class StartupStatus {
    private String stage = "preflight";
    private String failure = null;
    private boolean ready;

    public void stage(String stage) {
        this.stage = stage;
    }

    public void ready() {
        this.stage = "ready";
        this.failure = null;
        this.ready = true;
    }

    public void failed(Throwable error) {
        this.ready = false;
        String message = error.getMessage();
        this.failure = message == null || message.isBlank()
                ? error.getClass().getSimpleName()
                : message;
    }

    public String stage() {
        return stage;
    }

    public String failure() {
        return failure;
    }

    public boolean isReady() {
        return ready;
    }
}
