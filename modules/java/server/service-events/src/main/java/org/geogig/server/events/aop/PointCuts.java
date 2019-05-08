package org.geogig.server.events.aop;

import org.aspectj.lang.annotation.Pointcut;

import lombok.experimental.UtilityClass;

/**
 * Constants to unify pointcut definitions on a single place
 */
public @UtilityClass class PointCuts {

    static final String MODEL_BASE = "org.geogig.server.model";

    static final String SERVICE_BASE = "org.geogig.server.service";

    //@formatter:off
    static final String STORE_SERVICE = SERVICE_BASE + ".stores.StoreService";
    static final String STORE = MODEL_BASE + ".Store";
    static final String STORE_CREATE_PCD = "execution(public " + STORE + " " + STORE_SERVICE + ".create(..))";
    static final String STORE_MODIFY_PCD = "execution(public " + STORE + " " + STORE_SERVICE + ".modify(..))";
    static final String STORE_DELETE_PCD = "execution(public " + STORE + " " + STORE_SERVICE + ".remove*(..))";
    public static @Pointcut(STORE_CREATE_PCD) void storeCreateExecution() {}
    public static @Pointcut(STORE_MODIFY_PCD) void storeModifyExecution() {}
    public static @Pointcut(STORE_DELETE_PCD) void storeDeleteExecution() {}

    
    static final String USER_SERVICE = SERVICE_BASE + ".user.UserService";
    static final String USER = MODEL_BASE + ".User";
    static final String USER_CREATE_PCD = "execution(public " + USER + " " + USER_SERVICE + ".create(..))";
    static final String USER_MODIFY_PCD = "execution(public " + USER + " " + USER_SERVICE + ".modify(..))";
    static final String USER_DELETE_PCD = "execution(public " + USER + " " + USER_SERVICE + ".deleteBy*(..))";
    public static @Pointcut(USER_CREATE_PCD) void userCreateExecution() {}
    public static @Pointcut(USER_MODIFY_PCD) void userModifyExecution() {}
    public static @Pointcut(USER_DELETE_PCD) void userDeleteExecution() {}

    static final String REPO_SERVICE = SERVICE_BASE + ".repositories.RepositoryManagementService";
    static final String REPO = MODEL_BASE + ".RepoInfo";
    static final String REPO_CREATE_PCD = "execution(public " + REPO + " " + REPO_SERVICE + ".create(..))";
    static final String REPO_MODIFY_PCD = "execution(public " + REPO + " " + REPO_SERVICE + ".update(..))";
    static final String REPO_DELETE_PCD = "execution(public " + REPO + " " + REPO_SERVICE + ".remove(..))";
    static final String REPO_FORK_PCD = "execution(public java.util.concurrent.CompletableFuture " + REPO_SERVICE + ".fork(..))";
    public static @Pointcut(REPO_CREATE_PCD) void repositoryCreateExecution() {}
    public static @Pointcut(REPO_MODIFY_PCD) void repositoryModifyExecution() {}
    public static @Pointcut(REPO_DELETE_PCD) void repositoryDeleteExecution() {}
    public static @Pointcut(REPO_FORK_PCD) void repositoryForkExecution() {}
   
    static final String TX_SERVICE = SERVICE_BASE + ".transaction.TransactionService";
    static final String TX = MODEL_BASE + ".Transaction";
    static final String TX_CREATE_PCD = "execution(public " + TX + " " + TX_SERVICE + ".beginTransaction(..))";
//    static final String TX_MODIFY_PCD = "execution(public " + TX + " " + TX_SERVICE + ".update*(..))";
    static final String TX_DELETE_PCD = "execution(public " + TX + " " + TX_SERVICE + ".deleteTransaction(..))";
    static final String TX_COMMIT_PCD = "execution(public java.util.concurrent.CompletableFuture " + TX_SERVICE + ".commit(..))";
    static final String TX_ABORT_PCD = "execution(public " + TX + " " + TX_SERVICE + ".abortTransaction(..))";
    public static @Pointcut(TX_CREATE_PCD) void transactionBeginExecution() {}
//    public static @Pointcut(TX_MODIFY_PCD) void transactionModifyExecution() {}
    public static @Pointcut(TX_DELETE_PCD) void transactionDeleteExecution() {}
    public static @Pointcut(TX_COMMIT_PCD) void transactionCommitExecution() {}
    public static @Pointcut(TX_ABORT_PCD) void transactionAbortExecution() {}
    
    
    static final String PR_SERVICE = SERVICE_BASE + ".pr.PullRequestService";
    static final String PR_WORKER_SERVICE = SERVICE_BASE + ".pr.PullRequestWorkerService";
    static final String PR = MODEL_BASE + ".PullRequest";
    static final String PR_CREATE_PCD = "execution(public " + PR + " " + PR_SERVICE + ".create(..))";
    static final String PR_CLOSE_PCD = "execution(public " + PR+ " " + PR_SERVICE + ".close(..))";
    static final String PR_MODIFY_PCD = "execution(public " + PR+ " " + PR_SERVICE + ".updatePullRequest(..))";    
    static final String PR_MERGE_PCD = "execution(public * " + PR_SERVICE + ".merge(" + USER + ", " + PR + "))";
    static final String PR_STATUS_PCD = "execution(public java.util.concurrent.CompletableFuture " + PR_WORKER_SERVICE + " .prepare*(..))";
    public static @Pointcut(PR_CREATE_PCD) void prCreateExecution() {}
    public static @Pointcut(PR_MODIFY_PCD) void prModifyExecution() {}
    public static @Pointcut(PR_MERGE_PCD) void prMergeExecution() {}
    public static @Pointcut(PR_CLOSE_PCD) void prCloseExecution() {}
    public static @Pointcut(PR_STATUS_PCD) void prUpdateStatusExecution() {}
    //@formatter:on
}
