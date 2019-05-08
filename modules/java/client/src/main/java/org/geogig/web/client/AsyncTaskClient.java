package org.geogig.web.client;

import java.util.UUID;

import org.geogig.web.client.internal.ApiException;
import org.geogig.web.client.internal.AsyncApi;
import org.geogig.web.model.AsyncTaskInfo;
import org.geogig.web.model.ProgressInfo;

public class AsyncTaskClient extends AbstractServiceClient<AsyncApi> {

    AsyncTaskClient(Client client) {
        super(client, client.async);
    }

    public ProgressInfo getProgress(UUID taskId) {
        try {
            ProgressInfo progress = api.getTaskProgress(taskId);
            return progress;
        } catch (ApiException e) {
            throw Client.propagate(e);
        }
    }

    public AsyncTaskInfo get(UUID taskId) {
        try {
            return api.getTaskInfo(taskId, false);
        } catch (ApiException e) {
            throw new RuntimeException(e);
        }
    }

}
