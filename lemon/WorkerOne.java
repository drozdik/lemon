package lemon;
public class WorkerOne {

    private final DatabaseManager databaseManager;
    private final AppClock appClock;

    public WorkerOne(DatabaseManager databaseManager, AppClock appClock) {
        this.databaseManager = databaseManager;
        this.appClock = appClock;
    }

    public boolean updateUser(User user) {
        long now = appClock.currentTimeMillis();
        user.setTimestamp(now);
        try {
            databaseManager.saveUser(user);
        } catch (Exception e) {
            databaseManager.sentEmail(e.getMessage());
            return false;
        }
        return true;
    }
}
