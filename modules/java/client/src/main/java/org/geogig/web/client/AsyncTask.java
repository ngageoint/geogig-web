package org.geogig.web.client;

import static org.geogig.web.model.AsyncTaskInfo.StatusEnum.COMPLETE;

import java.util.function.Consumer;

import org.geogig.web.model.AsyncTaskInfo;
import org.geogig.web.model.AsyncTaskInfo.StatusEnum;
import org.geogig.web.model.Error;
import org.geogig.web.model.ProgressInfo;
import org.geogig.web.model.TaskResult;
import org.geogig.web.model.TransactionInfo;

import com.google.common.base.Preconditions;

/**
 * @param <R> task result type when completed successfully
 */
public class AsyncTask<R> extends Identified<AsyncTaskInfo> {

    public AsyncTask(Client client, AsyncTaskInfo info) {
        super(client, info, () -> info.getId());
    }

    public AsyncTask<R> refresh() {
        AsyncTaskInfo updatedInfo = client.async().get(getId());
        super.updateInfo(updatedInfo);
        return this;
    }

    public ProgressInfo getProgress() {
        return client.async().getProgress(getId());
    }

    public AsyncTask<R> awaitTermination() {
        return awaitTermination(null);
    }

    public AsyncTask<R> awaitTermination(Consumer<ProgressInfo> progressListener) {
        while (!isFinished(getInfo())) {
//            log.trace("Task {}({}) not finished({}), awaiting termination...", getId(),
//                    getDescription(), getStatus());
            try {
                if (progressListener != null) {
                    try {
                        ProgressInfo progress = getProgress();
                        if (progress != null) {
                            progressListener.accept(progress);
                        }
                    } catch (RuntimeException pe) {
//                        log.debug("Error getting task progress", pe);
                    }
                }
                Thread.sleep(250);
                refresh();
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
//        log.trace("Task finished: {}, awaiting termination...", getInfo());
        return this;
    }

    private String getDescription() {
        return getInfo().getDescription();
    }

    private boolean isFinished(AsyncTaskInfo info) {
        StatusEnum status = info.getStatus();
        switch (status) {
        case ABORTED:
        case COMPLETE:
        case FAILED:
            return true;
        case ABORTING:
        case RUNNING:
        case SCHEDULED:
            return false;
        default:
            throw new IllegalStateException("Unknown async task status: " + status + ": " + info);
        }
    }

    /**
     * @return the last known status of the task, call {@link #refresh() refresh().getStatus()} to
     *         force an update, or {@link #awaitTermination()} to block until finished.
     */
    public StatusEnum getStatus() {
        return getInfo().getStatus();
    }

    public boolean isFinished() {
        AsyncTaskInfo info = getInfo();
        if (!isFinished(info)) {
            info = this.refresh().getInfo();
        }
        return isFinished(info);
    }

    /**
     * @return {@code true} if the task completed successfully
     */
    public boolean isComplete() {
        AsyncTaskInfo info = getInfo();
        if (!isFinished(info)) {
            info = this.refresh().getInfo();
        }
        return info.getStatus().equals(COMPLETE);
    }

    public R getResult() {
        Preconditions.checkState(isComplete(),
                "getResult() shall only be called if isComplete() == true, status: "
                        + getInfo().getStatus());
        TaskResult result = getInfo().getResult();
        if (null == result) {
            return null;
        }
        try {
            @SuppressWarnings("unchecked")
            R casted = (R) result;
            return casted;
        } catch (ClassCastException e) {
            throw new IllegalStateException("Result is of type " + result.getClass().getName(), e);
        }
    }

    public Error getError() {
        return getInfo().getError();
    }

    public TransactionInfo getTransaction() {
        return getInfo().getTransaction();
    }

}
