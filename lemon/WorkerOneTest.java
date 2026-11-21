package lemon;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public class WorkerOneTest {
    // should persist user's updateAt as system's now and return true
    // will return false if user's updatedAt could not be updated/persisted
    // if failed to persist users updateAt then notify about an issue
    // will not send 100 notification if database is not reachable
    // notification will differentiate between database inavailability and missing row in database
    static void main() {
        var test = new WorkerOneTest();
        test.shouldPersistNewUserUpdateAtAndReturnTrue();
        test.returnFalseWhenFailedToUpdateUserUpdateAt();
        System.out.println("ALL TESTS PASSED");
    }

    public void shouldPersistNewUserUpdateAtAndReturnTrue() {
        User user = new User();
        DatabaseManagerTestDouble dbMng = new DatabaseManagerTestDouble();
        ClockTestDouble testClock = new ClockTestDouble();
        WorkerOne worker = new WorkerOne(dbMng, testClock);
        long clockTimeMillis = 1111111111111L;
        testClock.setTime(clockTimeMillis);

        boolean result = worker.updateUser(user);

        assertTrue(result);
        dbMng.saveUserWithUpdatedAt(clockTimeMillis);
        dbMng.noMoreOtherCalls();
    }

    public void returnFalseWhenFailedToUpdateUserUpdateAt() {
        // then result is false
        User user = new User();
        DatabaseManagerTestDouble dbMng = new DatabaseManagerTestDouble();
        dbMng.onSaveUserDo(() -> {
            throw new RuntimeException("Something went wrong");
        });
        ClockTestDouble testClock = new ClockTestDouble();
        WorkerOne worker = new WorkerOne(dbMng, testClock);
        long clockTimeMillis = 1111111111111L;
        testClock.setTime(clockTimeMillis);

        boolean result = worker.updateUser(user);

        assertFalse(result);
        dbMng.saveUserWithUpdatedAt(clockTimeMillis);
        dbMng.noMoreOtherCalls();
    }

    private void assertFalse(boolean value) {
        if (value) throw new AssertionException("expected to be true");
    }

    private void assertTrue(boolean value) {
        if (!value) throw new AssertionException("expected to be true");
    }

    private static class DatabaseManagerTestDouble implements DatabaseManager {

        private Supplier<Void> doOnSave;

        public void onSaveUserDo(Supplier<Void> supplier) {
            this.doOnSave = supplier;
        }

        record MethodCall(String name, Object arg){}

        private final List<MethodCall> methodCalls = new ArrayList<>();


        @Override
        public void saveUser(User user) {
            methodCalls.add(new MethodCall("saveUser", user));
            if (doOnSave != null) doOnSave.get();
        }

        @Override
        public void sentEmail(String message) {
            methodCalls.add(new MethodCall("sentEmail", message));
        }

        void saveUserWithUpdatedAt(long epochMillis) {
            var call = methodCalls.stream()
                    .filter(c -> c.name().equals("saveUser"))
                    .findFirst()
                    .orElseThrow(() -> new AssertionException("Expected to 'saveUser' be called with epochMillis '%s' but method was not called".formatted(epochMillis)));
            var arg = ((User) call.arg()).getUpdatedAtMillis();
            if (arg != epochMillis)
                throw new AssertionException("Expected to 'saveUser' be called with epochMillis '%s' but was called with '%s'"
                        .formatted(epochMillis, arg));
            methodCalls.remove(call);
        }

        public void noMoreOtherCalls() {
            if (!methodCalls.isEmpty())
                throw new AssertionException("Expected no more calls to %s but actually it as called %s"
                        .formatted("DatabaseManager", methodCalls.stream().map(Record::toString).collect(Collectors.joining(", "))));
        }
    }

    static class AssertionException extends RuntimeException {
        public AssertionException(String message) {
            super(message);
        }

    }

    static class ClockTestDouble implements AppClock {
        private long timeMillis;

        public ClockTestDouble() {
            this.timeMillis = System.currentTimeMillis();
        }

        public void setTime(long timeMillis) {
            this.timeMillis = timeMillis;
        }

        @Override
        public long currentTimeMillis() {
            return this.timeMillis;
        }
    }
}