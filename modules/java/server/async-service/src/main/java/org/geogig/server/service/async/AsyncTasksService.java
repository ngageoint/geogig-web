package org.geogig.server.service.async;

import java.time.OffsetDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import org.geogig.server.model.AsyncTask;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service("AsyncTasksService")
public class AsyncTasksService {

    private @Autowired ApplicationContext appContext;

    private ConcurrentMap<UUID, Task<?>> tasks = new ConcurrentHashMap<>();

    // TODO: schedule, save to "task store", send event, and make the actual worker service pick it
    // up for execution
    public <T> Task<T> submit(Job<T> job) {
        UUID caller = job.getCallerUser();
        AsyncTask taskInfo = new AsyncTask();
        taskInfo.setId(UUID.randomUUID());
        taskInfo.setStartedByUserId(caller);
        taskInfo.setScheduledAt(OffsetDateTime.now());
        taskInfo.setStatus(AsyncTask.Status.SCHEDULED);
        taskInfo.setDescription(job.getDescription());
        UUID txId = job.getTransaction().orElse(null);
        taskInfo.setTransactionId(txId);

        Task<T> task = new Task<>(taskInfo, job);
        tasks.put(taskInfo.getId(), task);

        CompletableFuture<T> future = CompletableFuture.supplyAsync(() -> run(taskInfo.getId()));
        task.setFuture(future);

        log.info("Scheduled task {}({})", taskInfo.getId(), taskInfo.getStatus());
        return task;
    }

    private <T> T run(UUID taskId) {
        @SuppressWarnings("unchecked")
        final Task<T> task = (Task<T>) tasks.get(taskId);
        final AsyncTask taskInfo = task.taskInfo;

        CompletableFuture<T> future;
        try {
            future = (CompletableFuture<T>) task.job.run(appContext);
        } catch (RuntimeException e) {
            future = CompletableFuture.failedFuture(e);
        }
        taskInfo.setStatus(AsyncTask.Status.RUNNING);
        OffsetDateTime now = OffsetDateTime.now();
        taskInfo.setStartedAt(now);
        taskInfo.setLastUpdated(now);

        log.info("Running task '{}'({}/{}), tx: {}", taskInfo.getDescription(), taskInfo.getId(),
                taskInfo.getStatus(), taskInfo.getTransactionId());

        future = future.whenComplete((r, e) -> {
            if (e == null) {
                task.setResult(r);
                taskInfo.setStatus(AsyncTask.Status.COMPLETE);
            } else {
                Throwable ex = e;
                if (e instanceof CompletionException) {
                    ex = e.getCause();
                }
                taskInfo.setStatus(AsyncTask.Status.FAILED);
                taskInfo.setErrorMessage(ex.getMessage());
            }
            taskInfo.setLastUpdated(now);
            taskInfo.setFinishedAt(now);
            log.info("Task '{}({})' completed ({})", taskInfo.getDescription(), taskInfo.getId(),
                    taskInfo.getStatus());
        });

        return future.join();
    }

    public <T> CompletableFuture<T> runImmediately(Supplier<T> task) {
        return CompletableFuture.supplyAsync(task);
    }

    public Optional<Task<?>> getTask(UUID taskId) {
        return getTask(taskId, Boolean.FALSE);
    }

    public Optional<Task<?>> getTask(UUID taskId, Boolean prune) {
        Task<?> task = tasks.get(taskId);
        return Optional.ofNullable(task);
    }

    public Optional<AsyncTaskProgress> getTaskProgress(UUID taskId) {
        Task<?> task = tasks.get(taskId);
        return getTaskProgress(task);
    }

    private Optional<AsyncTaskProgress> getTaskProgress(Task<?> task) {
        AsyncTaskProgress progress = null;
        if (task != null) {
            progress = task.getProgress();
        }
        return Optional.ofNullable(progress);
    }

    public void prune(UUID taskId) {
        throw new UnsupportedOperationException("not implemented");
    }

    public List<AsyncTask> getTasks() {
        List<AsyncTask> list = tasks.values().stream().map((t) -> t.getTaskInfo())
                .collect(Collectors.toList());
        Collections.sort(list, (a, b) -> a.getScheduledAt().compareTo(b.getScheduledAt()));
        return list;
    }

    public Optional<AsyncTask> abort(UUID taskId) {
        throw new UnsupportedOperationException("not implemented");
    }

}
