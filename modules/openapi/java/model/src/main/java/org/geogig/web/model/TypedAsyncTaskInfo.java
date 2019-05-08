package org.geogig.web.model;

public class TypedAsyncTaskInfo<R extends TaskResult> extends AsyncTaskInfo {

    public @Override R getResult() {
        return (R) super.getResult();
    }
}
