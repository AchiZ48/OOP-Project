class WorldMessage {
    private final String text;
    private final double duration;
    private double elapsed;

    WorldMessage(String text, double duration) {
        this.text = text;
        this.duration = Math.max(0.5, duration);
        this.elapsed = 0.0;
    }

    void update(double dt) {
        elapsed += dt;
    }

    void restart() {
        elapsed = 0.0;
    }

    boolean isExpired() {
        return elapsed >= duration;
    }

    float alpha() {
        if (duration <= 0.0) {
            return 0f;
        }
        double remaining = Math.max(0.0, duration - elapsed);
        double ratio = Math.min(1.0, Math.max(0.0, remaining / duration));
        return (float) Math.max(0.25, ratio);
    }

    String text() {
        return text;
    }
}
