package com.brandPitara.sfs.integration;

import org.junit.jupiter.api.Test;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Proves both {@link ExternalProviderTransactions} templates request PROPAGATION_REQUIRES_NEW,
 * not the TransactionTemplate default of REQUIRED. This is the whole safety property this class
 * exists for: a caller invoking read()/write() from inside an already-active transaction must
 * get a suspended/fresh transaction, never join the ambient one and hold its connection open
 * across the external HTTP call this class wraps. See NestedRequiresNewPoolStarvationReproductionTest
 * for the full real-database proof of why REQUIRED-vs-REQUIRES_NEW matters for Hikari pool
 * exhaustion; this test only needs to prove the propagation attribute itself is correct.
 */
class ExternalProviderTransactionsTest {

    @Test
    void readUsesRequiresNewPropagationAndReadOnly() {
        PlatformTransactionManager transactionManager = mock(PlatformTransactionManager.class);
        TransactionStatus status = mock(TransactionStatus.class);
        var definitionCaptor = org.mockito.ArgumentCaptor.forClass(TransactionDefinition.class);
        when(transactionManager.getTransaction(definitionCaptor.capture())).thenReturn(status);

        ExternalProviderTransactions transactions = new ExternalProviderTransactions(transactionManager);
        transactions.read(() -> "result");

        verify(transactionManager).commit(status);
        TransactionDefinition used = definitionCaptor.getValue();
        assertThat(used.getPropagationBehavior()).isEqualTo(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        assertThat(used.isReadOnly()).isTrue();
    }

    @Test
    void writeSupplierUsesRequiresNewPropagationAndIsNotReadOnly() {
        PlatformTransactionManager transactionManager = mock(PlatformTransactionManager.class);
        TransactionStatus status = mock(TransactionStatus.class);
        var definitionCaptor = org.mockito.ArgumentCaptor.forClass(TransactionDefinition.class);
        when(transactionManager.getTransaction(definitionCaptor.capture())).thenReturn(status);

        ExternalProviderTransactions transactions = new ExternalProviderTransactions(transactionManager);
        transactions.write(() -> "result");

        verify(transactionManager).commit(status);
        TransactionDefinition used = definitionCaptor.getValue();
        assertThat(used.getPropagationBehavior()).isEqualTo(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        assertThat(used.isReadOnly()).isFalse();
    }

    @Test
    void writeRunnableUsesRequiresNewPropagation() {
        PlatformTransactionManager transactionManager = mock(PlatformTransactionManager.class);
        TransactionStatus status = mock(TransactionStatus.class);
        var definitionCaptor = org.mockito.ArgumentCaptor.forClass(TransactionDefinition.class);
        when(transactionManager.getTransaction(definitionCaptor.capture())).thenReturn(status);

        ExternalProviderTransactions transactions = new ExternalProviderTransactions(transactionManager);
        Runnable work = () -> { };
        transactions.write(work);

        verify(transactionManager).commit(status);
        assertThat(definitionCaptor.getValue().getPropagationBehavior())
                .isEqualTo(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    }

    @Test
    void aSecondNestedCallStillRequestsItsOwnFreshTransactionDefinition() {
        // Simulates the exact misuse scenario the doc comment warns about: calling write()
        // again while "inside" a transaction already returned by getTransaction() the first
        // time. Each call must independently request REQUIRES_NEW - proving the guarantee
        // does not depend on any per-call state that could be forgotten or bypassed.
        PlatformTransactionManager transactionManager = mock(PlatformTransactionManager.class);
        TransactionStatus outerStatus = mock(TransactionStatus.class);
        TransactionStatus innerStatus = mock(TransactionStatus.class);
        when(transactionManager.getTransaction(any())).thenReturn(outerStatus, innerStatus);

        ExternalProviderTransactions transactions = new ExternalProviderTransactions(transactionManager);
        transactions.write(() -> "outer");
        transactions.write(() -> "inner");

        var definitionCaptor = org.mockito.ArgumentCaptor.forClass(TransactionDefinition.class);
        verify(transactionManager, org.mockito.Mockito.times(2)).getTransaction(definitionCaptor.capture());
        for (TransactionDefinition definition : definitionCaptor.getAllValues()) {
            assertThat(definition.getPropagationBehavior()).isEqualTo(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        }
    }
}
