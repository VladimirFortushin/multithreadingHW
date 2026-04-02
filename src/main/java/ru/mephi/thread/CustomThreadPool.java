package ru.mephi.thread;

import ru.mephi.util.CustomLogger;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

public class CustomThreadPool implements CustomExecutor {

    private final int corePoolSize;
    private final int maxPoolSize;
    private final long keepAliveTimeMillis;
    private final int queueSize;
    private int minSpareThreads;

    private final List<ResizableQueue> queues;
    private final Set<Worker> workers = ConcurrentHashMap.newKeySet();
    private final CustomThreadFactory threadFactory;
    private final OldTaskRejector rejectHandler = new OldTaskRejector();
    private final AtomicInteger rrCounter = new AtomicInteger(0);

    public List<ResizableQueue> getQueues() {
        return queues;
    }

    private volatile boolean isShutdown = false;

    public CustomThreadPool(int corePoolSize, int maxPoolSize, int queueSize, long keepAliveTimeMillis,
                            CustomThreadFactory threadFactory) {

        this.corePoolSize = corePoolSize;
        this.minSpareThreads = corePoolSize;
        this.maxPoolSize = maxPoolSize;
        this.queueSize = queueSize;
        this.keepAliveTimeMillis = keepAliveTimeMillis;
        this.threadFactory = threadFactory;

        this.queues = new ArrayList<>();
        for (int i = 0; i < maxPoolSize; i++) {
            queues.add(new ResizableQueue(queueSize));
        }
        for (int i = 0; i < corePoolSize; i++) {
            addWorker(i);
        }
    }

    private void addWorker(int queueIndex) {
        if (workers.size() >= maxPoolSize) return;

        Worker worker = new Worker(queues.get(queueIndex).getQueue(), this, keepAliveTimeMillis);
        workers.add(worker);
        Thread thread = threadFactory.newThread(worker);
        thread.start();
    }

    @Override
    public void execute(Runnable command) {
        if (isShutdown) throw new RejectedExecutionException("Pool is shutdown");
        int index = rrCounter.getAndIncrement() % queues.size();
        ResizableQueue queue = queues.get(index);
        if (queue.offer(command)) {
            CustomLogger.log("[Pool] Task added for queue №" + index);
            if (getIdleThreadsCount() < minSpareThreads && workers.size() < maxPoolSize) {
                addWorker(index);
                CustomLogger.log("[Pool] Added thread for queue №" + index);
            }
            return;
        }

        if (queue.tryToIncreaseCapacity()) {
            CustomLogger.log("[Pool] Doubling capacity for queue №" + index);
            if (queue.offer(command)) return;
        }

        if (workers.size() < maxPoolSize) {
            addWorker(index);
            if (queue.offer(command)) return;
        }

        CustomLogger.log("[Pool] Backpressure: blocking producer");
        try {
            queue.put(command);
            return;
        } catch (InterruptedException ignored) {}

        if (!queue.offer(command)) {
            CustomLogger.log("[Pool] rejecting command " + command);
            rejectHandler.rejectedExecution(command, this, index);
        }
    }

    private int getIdleThreadsCount() {
        return (int) workers.stream().filter(Worker::isIdle).count();
    }

    @Override
    public <T> Future<T> submit(Callable<T> callable) {
        FutureTask<T> task = new FutureTask<>(callable);
        execute(task);
        return task;
    }

    public boolean shouldTerminateWorker() {
        return workers.size() > corePoolSize;
    }

    public void workerTerminated(Worker worker) {
        workers.remove(worker);
    }

    @Override
    public void shutdown() {
        isShutdown = true;
    }

    @Override
    public void shutdownNow() {
        isShutdown = true;
        for (Worker w : workers) {
            w.stopWorker();
        }
    }
}