package ru.mephi.thread;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

public class ResizableQueue {
    private final BlockingQueue<Runnable> queue = new LinkedBlockingQueue<>();
    private volatile int capacity;
    private final int maxCapacity;

    public ResizableQueue(int initialCapacity) {
        this.capacity = initialCapacity;
        this.maxCapacity = initialCapacity * 2;
    }

    public BlockingQueue<Runnable> getQueue() {
        return queue;
    }

    public synchronized boolean offer(Runnable task) {
        if (queue.size() >= capacity) {
            return false;
        }
        return queue.offer(task);
    }

    public Runnable poll(long timeout, TimeUnit unit) throws InterruptedException {
        return queue.poll(timeout, unit);
    }

    //FIFO
    public synchronized Runnable pollOldest() {
        return queue.poll();
    }

    public int size() {
        return queue.size();
    }

    public synchronized boolean tryToIncreaseCapacity() {
        if (capacity < maxCapacity) {
            capacity = capacity * 2;
            return true;
        }
        return false;
    }

    public void put(Runnable task) throws InterruptedException {
        while (true) {
            synchronized (this) {
                if (queue.size() < capacity) {
                    queue.offer(task);
                    return;
                }
            }
            Thread.sleep(10);
        }
    }
}
