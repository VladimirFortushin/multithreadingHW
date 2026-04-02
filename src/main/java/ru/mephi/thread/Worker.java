package ru.mephi.thread;
import ru.mephi.util.CustomLogger;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

public class Worker implements Runnable {
    private final BlockingQueue<Runnable> taskQueue;
    private final CustomThreadPool executor;
    private final long keepAliveTime;
    private volatile boolean idle = true;

    public boolean isIdle() {
        return idle;
    }

    private volatile boolean running = true;

    public Worker(BlockingQueue<Runnable> taskQueue, CustomThreadPool executor, long keepAliveTime) {
        this.taskQueue = taskQueue;
        this.executor = executor;
        this.keepAliveTime = keepAliveTime;
    }

    @Override
    public void run() {
        try {
            while (running) {
                idle = true;
                Runnable task = taskQueue.poll(keepAliveTime, TimeUnit.MILLISECONDS);
                idle = false;
                if (task == null) {
                    if (executor.shouldTerminateWorker()) {
                        CustomLogger.log("[" + Thread.currentThread().getName() + "] idle timeout, stopping.");
                        break;
                    }
                    continue;
                }

                CustomLogger.log("[" + Thread.currentThread().getName() + "] executes " + task);
                try {
                    task.run();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } finally {
            executor.workerTerminated(this);
            CustomLogger.log("[" + Thread.currentThread().getName() + "] terminated.");
        }
    }

    public void stopWorker() {
        running = false;
    }
}
