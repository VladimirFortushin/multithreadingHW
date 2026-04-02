package ru.mephi;

import ru.mephi.thread.CustomThreadFactory;
import ru.mephi.thread.CustomThreadPool;
import ru.mephi.thread.OldTaskRejector;
import ru.mephi.util.CustomLogger;

import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.TimeUnit;

public class Main {
    public static void main(String[] args) throws InterruptedException {
        CustomThreadFactory factory = new CustomThreadFactory("CustomPool");

        CustomThreadPool pool = new CustomThreadPool(
                2, 4, 5, 5000, factory
        );

        for (int i = 1; i <= 10; i++) {
            int taskNum = i;
            pool.execute(() -> {
                CustomLogger.log("Task " + taskNum + " started");
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                CustomLogger.log("Task " + taskNum + " finished");
            });
        }

        Thread.sleep(10000);
        pool.shutdown();
    }
}