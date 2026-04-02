package ru.mephi.thread;

import ru.mephi.util.CustomLogger;

import java.util.concurrent.RejectedExecutionException;

public class OldTaskRejector implements CustomRejectionHandler{
    @Override
    public void rejectedExecution(Runnable task, CustomThreadPool executor, int queueIndex) {
        ResizableQueue queue = executor.getQueues().get(queueIndex);

        Runnable removed = queue.pollOldest();

        if (removed != null) {
            CustomLogger.log("[Rejected] Removed oldest task: " + removed);

            if (queue.offer(task)) {
                CustomLogger.log("[Rejected] New task inserted after removing oldest: " + task);
                return;
            }
        }

        CustomLogger.log("[Rejected] Failed even after removing oldest: " + task);
    }
}
