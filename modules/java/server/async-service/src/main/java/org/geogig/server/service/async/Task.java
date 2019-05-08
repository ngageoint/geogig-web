package org.geogig.server.service.async;

import java.util.concurrent.CompletableFuture;

import org.geogig.server.model.AsyncTask;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

public @RequiredArgsConstructor @Getter class Task<T> {
    final AsyncTask taskInfo;

    final Job<T> job;

    private @Setter CompletableFuture<T> future;

    private Object result;

    public AsyncTaskProgress getProgress() {
        return job.getProgressListener();
    }

    public void setResult(Object result) {
        this.result = result;
    }
}