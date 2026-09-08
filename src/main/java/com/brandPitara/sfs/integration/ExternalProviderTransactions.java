package com.brandPitara.sfs.integration;

import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.function.Supplier;

/**
 * Short database phases surrounding, but never containing, external provider I/O.
 * <p>
 * Both templates use {@code PROPAGATION_REQUIRES_NEW} deliberately, not the default
 * {@code REQUIRED}: this class's whole contract is "never hold a DB connection across an
 * external HTTP call." If a caller ever invokes {@link #read}/{@link #write} from inside an
 * already-active transaction, REQUIRED would silently join it - keeping that outer connection
 * checked out for the full external-call duration and reintroducing the exact Hikari
 * pool-exhaustion pattern this class exists to prevent. REQUIRES_NEW suspends any ambient
 * transaction first, so the guarantee holds regardless of caller context instead of depending
 * on every caller separately remembering to mark its own method NOT_SUPPORTED.
 */
@Component
public class ExternalProviderTransactions {

    private final TransactionTemplate readOnly;
    private final TransactionTemplate write;

    public ExternalProviderTransactions(PlatformTransactionManager transactionManager) {
        this.readOnly = new TransactionTemplate(transactionManager);
        this.readOnly.setReadOnly(true);
        this.readOnly.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        this.write = new TransactionTemplate(transactionManager);
        this.write.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    }

    public <T> T read(Supplier<T> work) {
        return readOnly.execute(status -> work.get());
    }

    public <T> T write(Supplier<T> work) {
        return write.execute(status -> work.get());
    }

    public void write(Runnable work) {
        write.executeWithoutResult(status -> work.run());
    }
}
