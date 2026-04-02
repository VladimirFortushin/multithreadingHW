package ru.mephi.thread;

import java.util.concurrent.Callable;
import java.util.concurrent.Executor;
import java.util.concurrent.Future;

interface CustomExecutor extends Executor {
    <T> Future<T> submit(Callable<T> callable);
    void shutdown();
    void shutdownNow();
}
