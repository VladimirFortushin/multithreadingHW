package ru.mephi.thread;

public interface CustomRejectionHandler {
    void rejectedExecution(Runnable task, CustomThreadPool executor, int queueIndex);
}
