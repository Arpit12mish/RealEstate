package com.brandPitara.sfs.cdn;

import com.brandPitara.sfs.cdn.event.ProjectCacheEvictionReason;
import com.brandPitara.sfs.cdn.event.ProjectPublicCacheEvictionEvent;
import com.brandPitara.sfs.cdn.event.ProjectPublicCacheEvictionListener;
import com.brandPitara.sfs.cdn.service.ProjectPublicCacheEvictionMetrics;
import com.brandPitara.sfs.cdn.service.ProjectPublicCacheEvictionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.core.task.SyncTaskExecutor;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.AbstractPlatformTransactionManager;
import org.springframework.transaction.support.DefaultTransactionStatus;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.transaction.event.TransactionalEventListenerFactory;

import static org.mockito.Mockito.*;

class ProjectPublicCacheEvictionTransactionTest {

    private AnnotationConfigApplicationContext context;
    private ProjectPublicCacheEvictionService service;
    private ApplicationEventPublisher publisher;
    private TransactionTemplate transactions;

    @BeforeEach
    void setUp() {
        service = mock(ProjectPublicCacheEvictionService.class);
        ProjectPublicCacheEvictionMetrics metrics = mock(ProjectPublicCacheEvictionMetrics.class);
        context = new AnnotationConfigApplicationContext();
        context.registerBean(TransactionalEventListenerFactory.class,
                TransactionalEventListenerFactory::new);
        context.registerBean(ProjectPublicCacheEvictionListener.class,
                () -> new ProjectPublicCacheEvictionListener(service, metrics, new SyncTaskExecutor()));
        context.refresh();
        publisher = context;
        transactions = new TransactionTemplate(new TestTransactionManager());
    }

    @Test
    void committedTransactionTriggersEviction() {
        transactions.executeWithoutResult(status -> publisher.publishEvent(
                new ProjectPublicCacheEvictionEvent(27L, ProjectCacheEvictionReason.PROJECT_UPDATED)));

        verify(service).evict(27L, ProjectCacheEvictionReason.PROJECT_UPDATED);
        context.close();
    }

    @Test
    void rolledBackTransactionDoesNotTriggerEviction() {
        transactions.executeWithoutResult(status -> {
            publisher.publishEvent(new ProjectPublicCacheEvictionEvent(
                    27L, ProjectCacheEvictionReason.PROJECT_DELETED));
            status.setRollbackOnly();
        });

        verifyNoInteractions(service);
        context.close();
    }

    private static final class TestTransactionManager extends AbstractPlatformTransactionManager {
        @Override protected Object doGetTransaction() { return new Object(); }
        @Override protected void doBegin(Object transaction, TransactionDefinition definition) { }
        @Override protected void doCommit(DefaultTransactionStatus status) { }
        @Override protected void doRollback(DefaultTransactionStatus status) { }
    }
}
