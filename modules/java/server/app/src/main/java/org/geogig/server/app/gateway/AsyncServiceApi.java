package org.geogig.server.app.gateway;

import java.util.List;
import java.util.UUID;

import org.geogig.server.model.AsyncTask;
import org.geogig.server.service.async.AsyncTasksService;
import org.geogig.server.service.presentation.PresentationService;
import org.geogig.web.model.AsyncTaskInfo;
import org.geogig.web.model.ProgressInfo;
import org.geogig.web.server.api.AsyncApi;
import org.geogig.web.server.api.AsyncApiDelegate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import com.google.common.collect.Lists;

/**
 * API delegate implementation for the swagger-codegen auto-generated {@link AsyncApi}, handles the
 * REST request/response/error handling aspects of the API, and delegates business logic to a
 * {@link AsyncTasksService}.
 */
public @Service class AsyncServiceApi extends AbstractService implements AsyncApiDelegate {

    private @Autowired AsyncTasksService service;

    private @Autowired PresentationService presentation;

    public @Override ResponseEntity<AsyncTaskInfo> abortTask(UUID taskId) {
        return super.okOrNotFound(service.abort(taskId).map(t -> presentation.toInfo(t)));
    }

    public @Override ResponseEntity<AsyncTaskInfo> getTaskInfo(UUID taskId, Boolean prune) {
        return super.okOrNotFound(service.getTask(taskId, prune).map(t -> presentation.toInfo(t)));
    }

    public @Override ResponseEntity<ProgressInfo> getTaskProgress(UUID taskId) {
        return super.okOrNotFound(
                service.getTaskProgress(taskId).map(t -> presentation.toProgress(t)));
    }

    public @Override ResponseEntity<List<AsyncTaskInfo>> listTasks() {
        List<AsyncTask> tasks = service.getTasks();
        return super.ok(Lists.transform(tasks, t -> presentation.toInfo(t)));
    }

    public @Override ResponseEntity<Void> pruneTask(UUID taskId) {
        return super.run(HttpStatus.NO_CONTENT, () -> service.prune(taskId));
    }

}
