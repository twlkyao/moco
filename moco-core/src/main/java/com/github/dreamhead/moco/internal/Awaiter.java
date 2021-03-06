package com.github.dreamhead.moco.internal;

import java.util.concurrent.*;

public class Awaiter {
    private final ScheduledExecutorService executor = Executors.newScheduledThreadPool(1);
    private final CountDownLatch latch;
    private final int timeout;
    private Throwable throwable;

    public Awaiter(final Callable<Boolean> condition, int timeout) {
        this.timeout = timeout;
        this.latch = new CountDownLatch(1);
        Runnable runner = new Runnable() {
            @Override
            public void run() {
                try {
                    if (condition.call()) {
                        latch.countDown();
                    }
                } catch (Exception e) {
                    throwable = e;
                    latch.countDown();
                }
            }
        };

        executor.scheduleWithFixedDelay(runner, -1, 100, TimeUnit.MILLISECONDS);
    }

    public void await() {
        try {
            doAwait();
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }

    private void doAwait() throws Throwable {
        try {
            latch.await(this.timeout, TimeUnit.SECONDS);
            if (throwable != null) {
                throw throwable;
            }
        } finally {
            executor.shutdown();
            if (!executor.awaitTermination(1, TimeUnit.SECONDS)) {
                executor.shutdownNow();
            }
        }
    }

    public static void awaitUntil(final Callable<Boolean> callable, int timeout) {
        new Awaiter(callable, timeout).await();
    }
}
