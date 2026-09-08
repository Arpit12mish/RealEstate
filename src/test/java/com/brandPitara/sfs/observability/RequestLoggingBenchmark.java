package com.brandPitara.sfs.observability;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.encoder.PatternLayoutEncoder;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.Appender;
import ch.qos.logback.core.FileAppender;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.servlet.HandlerMapping;
import org.testcontainers.containers.PostgreSQLContainer;

import java.lang.management.ManagementFactory;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Opt-in benchmark harness. The class deliberately does not end in Test, so
 * Maven's normal full-suite discovery does not execute it. Run explicitly:
 * ./mvnw -q -Dtest=RequestLoggingBenchmark -Dsfs.logging.benchmark.mode=sync test
 */
class RequestLoggingBenchmark {

    private static final int REQUESTS = Integer.getInteger("sfs.logging.benchmark.requests", 2_000);
    private static final int[] CLIENTS = {1, 10, 25};

    @Test
    void benchmark() throws Exception {
        String mode = System.getProperty("sfs.logging.benchmark.mode", "sync");
        Path outputDirectory = Path.of("target", "request-logging-benchmark", mode);
        Files.createDirectories(outputDirectory);

        try (PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
                .withDatabaseName("request_logging_benchmark")
                .withUsername("benchmark")
                .withPassword("benchmark");
             HikariDataSource dataSource = dataSource(postgres)) {
            postgres.start();
            JdbcTemplate jdbc = new JdbcTemplate(dataSource);
            jdbc.execute("create table benchmark_item(id bigint primary key, active boolean not null, payload varchar(100))");
            jdbc.update("insert into benchmark_item select g, true, 'payload-' || g from generate_series(1, 1000) g");

            for (String endpoint : List.of("lightweight", "database")) {
                for (int clients : CLIENTS) {
                    runScenario(mode, endpoint, clients, outputDirectory, jdbc, dataSource);
                }
            }
        }
    }

    private void runScenario(
            String mode,
            String endpoint,
            int clients,
            Path outputDirectory,
            JdbcTemplate jdbc,
            HikariDataSource dataSource
    ) throws Exception {
        FilterChain endpointChain = (request, response) -> {
            if ("database".equals(endpoint)) {
                jdbc.queryForObject("select count(*) from benchmark_item where active = true", Long.class);
            }
            ((MockHttpServletResponse) response).setStatus(200);
            ((MockHttpServletResponse) response).setContentLength(32);
        };

        for (int i = 0; i < 100; i++) {
            endpointChain.doFilter(new MockHttpServletRequest(), new MockHttpServletResponse());
        }

        Path logFile = outputDirectory.resolve(endpoint + "-c" + clients + ".log");
        Files.deleteIfExists(logFile);
        RequestLoggingTelemetry.resetForTests();
        Appender<ILoggingEvent> appender = configureAppender(mode, logFile);

        LogSanitizer sanitizer = new LogSanitizer();
        CorrelationIdFilter correlationFilter = new CorrelationIdFilter(sanitizer);
        ApiRequestLoggingFilter requestFilter = new ApiRequestLoggingFilter(sanitizer);
        ReflectionTestUtils.setField(requestFilter, "slowApiThresholdMs", Long.MAX_VALUE);

        long[] latencyNanos = new long[REQUESTS];
        AtomicInteger cursor = new AtomicInteger();
        AtomicInteger inFlight = new AtomicInteger();
        AtomicInteger peakThreads = new AtomicInteger();
        AtomicInteger peakActive = new AtomicInteger();
        AtomicInteger peakPending = new AtomicInteger();
        AtomicInteger hikariTimeouts = new AtomicInteger();
        AtomicBoolean sampling = new AtomicBoolean(true);

        Thread sampler = new Thread(() -> {
            while (sampling.get()) {
                peakActive.accumulateAndGet(dataSource.getHikariPoolMXBean().getActiveConnections(), Math::max);
                peakPending.accumulateAndGet(dataSource.getHikariPoolMXBean().getThreadsAwaitingConnection(), Math::max);
                Thread.onSpinWait();
            }
        }, "request-logging-benchmark-pool-sampler");
        sampler.setDaemon(true);
        sampler.start();

        com.sun.management.OperatingSystemMXBean os =
                (com.sun.management.OperatingSystemMXBean) ManagementFactory.getOperatingSystemMXBean();
        long cpuStarted = os.getProcessCpuTime();
        long started = System.nanoTime();

        ExecutorService executor = Executors.newFixedThreadPool(clients);
        List<Callable<Void>> tasks = new ArrayList<>(REQUESTS);
        for (int i = 0; i < REQUESTS; i++) {
            tasks.add(() -> {
                int slot = cursor.getAndIncrement();
                int active = inFlight.incrementAndGet();
                peakThreads.accumulateAndGet(active, Math::max);
                long requestStarted = System.nanoTime();
                try {
                    MockHttpServletRequest request = new MockHttpServletRequest("GET", "/benchmark/" + endpoint + "/42");
                    request.setAttribute(HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE, "/benchmark/" + endpoint + "/{id}");
                    request.setRemoteAddr("10.20.30.40");
                    request.addHeader("User-Agent", "benchmark-client");
                    MockHttpServletResponse response = new MockHttpServletResponse();
                    correlationFilter.doFilter(request, response,
                            (wrappedRequest, wrappedResponse) -> requestFilter.doFilter(
                                    wrappedRequest, wrappedResponse, endpointChain));
                } catch (RuntimeException ex) {
                    if (hasSqlTimeout(ex)) hikariTimeouts.incrementAndGet();
                    throw ex;
                } finally {
                    latencyNanos[slot] = System.nanoTime() - requestStarted;
                    inFlight.decrementAndGet();
                }
                return null;
            });
        }

        List<Future<Void>> futures = executor.invokeAll(tasks);
        for (Future<Void> future : futures) future.get();
        executor.shutdown();
        executor.awaitTermination(30, TimeUnit.SECONDS);

        long elapsedNanos = System.nanoTime() - started;
        long cpuNanos = os.getProcessCpuTime() - cpuStarted;
        sampling.set(false);
        sampler.join(1_000);
        appender.stop();

        Arrays.sort(latencyNanos);
        long logBytes = Files.exists(logFile) ? Files.size(logFile) : 0;
        double seconds = elapsedNanos / 1_000_000_000.0;
        double cpuPercentOneCore = seconds == 0 ? 0 : (cpuNanos / 1_000_000_000.0) / seconds * 100.0;

        System.out.printf(Locale.ROOT,
                "logging-benchmark mode=%s endpoint=%s clients=%d requests=%d rps=%.1f p50Ms=%.3f p95Ms=%.3f p99Ms=%.3f cpuOneCorePct=%.1f logBytes=%d peakRequestThreads=%d hikariActive=%d hikariPending=%d hikariTimeouts=%d queuePeak=%d discarded=%d appenderFailures=%d%n",
                mode, endpoint, clients, REQUESTS, REQUESTS / seconds,
                percentileMillis(latencyNanos, 0.50), percentileMillis(latencyNanos, 0.95),
                percentileMillis(latencyNanos, 0.99), cpuPercentOneCore, logBytes,
                peakThreads.get(), peakActive.get(), peakPending.get(), hikariTimeouts.get(),
                RequestLoggingTelemetry.queuePeak(), RequestLoggingTelemetry.discardedCount(),
                RequestLoggingTelemetry.appenderFailures("request"));
    }

    @SuppressWarnings("unchecked")
    private Appender<ILoggingEvent> configureAppender(String mode, Path logFile) throws Exception {
        LoggerContext context = (LoggerContext) LoggerFactory.getILoggerFactory();
        Logger logger = context.getLogger(LoggingConstants.LOGGER_API);
        logger.detachAndStopAllAppenders();
        logger.setAdditive(false);
        logger.setLevel(ch.qos.logback.classic.Level.INFO);

        PatternLayoutEncoder encoder = new PatternLayoutEncoder();
        encoder.setContext(context);
        encoder.setPattern("%d{yyyy-MM-dd'T'HH:mm:ss.SSS} %level [%X{requestId}] %msg%n");
        encoder.start();

        FileAppender<ILoggingEvent> file = new FileAppender<>();
        file.setName("BENCHMARK_FILE");
        file.setContext(context);
        file.setFile(logFile.toAbsolutePath().toString());
        file.setAppend(false);
        file.setEncoder(encoder);
        file.start();

        if ("sync".equals(mode)) {
            logger.addAppender(file);
            return file;
        }

        Class<?> type = Class.forName("com.brandPitara.sfs.observability.BoundedRequestAsyncAppender");
        Object async = type.getConstructor().newInstance();
        type.getMethod("setContext", ch.qos.logback.core.Context.class).invoke(async, context);
        type.getMethod("setName", String.class).invoke(async, "BENCHMARK_ASYNC");
        type.getMethod("setQueueSize", int.class).invoke(async, 2_048);
        type.getMethod("setDiscardingThreshold", int.class).invoke(async, 0);
        type.getMethod("setNeverBlock", boolean.class).invoke(async, true);
        type.getMethod("setIncludeCallerData", boolean.class).invoke(async, false);
        type.getMethod("setMaxFlushTime", int.class).invoke(async, 5_000);
        type.getMethod("addAppender", ch.qos.logback.core.Appender.class).invoke(async, file);
        type.getMethod("start").invoke(async);
        logger.addAppender((Appender<ILoggingEvent>) async);
        return (Appender<ILoggingEvent>) async;
    }

    private HikariDataSource dataSource(PostgreSQLContainer<?> postgres) {
        postgres.start();
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(postgres.getJdbcUrl());
        config.setUsername(postgres.getUsername());
        config.setPassword(postgres.getPassword());
        config.setMaximumPoolSize(3);
        config.setMinimumIdle(1);
        config.setConnectionTimeout(2_000);
        config.setPoolName("RequestLoggingBenchmarkPool");
        return new HikariDataSource(config);
    }

    private boolean hasSqlTimeout(Throwable throwable) {
        Throwable current = throwable;
        while (current != null) {
            if (current instanceof java.sql.SQLTransientConnectionException) return true;
            current = current.getCause();
        }
        return false;
    }

    private double percentileMillis(long[] values, double percentile) {
        int index = Math.min(values.length - 1, (int) Math.ceil(values.length * percentile) - 1);
        return values[index] / 1_000_000.0;
    }
}
