package ru.mephi.thread;

import ru.mephi.util.CustomLogger;

import java.util.concurrent.atomic.AtomicInteger;

public class CustomThreadFactory {
    private final AtomicInteger counter = new AtomicInteger(1);
    private final String poolName;

    public CustomThreadFactory(String poolName) {
        this.poolName = poolName;
    }

    public Thread newThread(Runnable r) {
        String name = poolName + "-worker-" + counter.getAndIncrement();
        CustomLogger.log("[ThreadFactory] Creating new thread: " + name);
        return new Thread(() -> {
            try {
                r.run();
            } finally {
                CustomLogger.log("[ThreadFactory] " + name + " terminated.");
            }
        }, name);
    }
}
