package com.brandPitara.sfs.integration;

import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.function.Supplier;

/** Short database phases surrounding, but never containing, external provider I/O. */
@Component
public class ExternalProviderTransactions {

    private final TransactionTemplate readOnly;
    private final TransactionTemplate write;

    public ExternalProviderTransactions(PlatformTransactionManager transactionManager) {
        this.readOnly = new TransactionTemplate(transactionManager);
        this.readOnly.setReadOnly(true);
        this.write = new TransactionTemplate(transactionManager);
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
