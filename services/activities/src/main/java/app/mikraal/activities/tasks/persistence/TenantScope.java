package app.mikraal.activities.tasks.persistence;

import java.util.UUID;
import java.util.function.Supplier;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Runs work in a transaction bound to one group. Row-level security reads {@code app.current_group}, set with
 * transaction scope, so every statement inside only sees and writes that group's rows.
 */
@Component
public class TenantScope {

    private final TransactionTemplate transactions;
    private final JdbcClient jdbc;

    TenantScope(PlatformTransactionManager transactionManager, JdbcClient jdbc) {
        this.transactions = new TransactionTemplate(transactionManager);
        this.jdbc = jdbc;
    }

    public <T> T call(UUID groupId, Supplier<T> work) {
        return transactions.execute(status -> {
            jdbc.sql("SELECT set_config('app.current_group', ?, true)").param(groupId.toString()).query().singleValue();
            return work.get();
        });
    }

    public void run(UUID groupId, Runnable work) {
        call(groupId, () -> {
            work.run();
            return null;
        });
    }
}
