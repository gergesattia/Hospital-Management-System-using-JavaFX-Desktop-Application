package utils.ai;

public class VerifyResult {
    private final boolean match;
    private final double distance;
    private final String status;

    public VerifyResult(boolean match, double distance, String status) {
        this.match = match;
        this.distance = distance;
        this.status = status;
    }

    public boolean isMatch() { return match; }
    public double getDistance() { return distance; }
    public String getStatus() { return status; }
}
