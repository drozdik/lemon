package lemon;

public class User {
    private long updatedAtMillis;
    public void setTimestamp(long epochMillis) {
        this.updatedAtMillis = epochMillis;
    }

    public long getUpdatedAtMillis() {
        return updatedAtMillis;
    }
}
