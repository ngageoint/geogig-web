package org.geogig.server.service.presentation;

import java.io.File;
import java.net.URI;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.annotation.Nullable;

import org.geogig.server.model.AsyncTask;
import org.geogig.server.model.AsyncTask.Status;
import org.geogig.server.model.PullRequest;
import org.geogig.server.model.PullRequestStatus;
import org.geogig.server.model.RepoInfo;
import org.geogig.server.model.RepoStats;
import org.geogig.server.model.Store;
import org.geogig.server.model.StoredDiffSummary;
import org.geogig.server.model.StoredDiffSummary.Bounds;
import org.geogig.server.model.Transaction;
import org.geogig.server.model.User;
import org.geogig.server.model.User.UserType;
import org.geogig.server.service.async.AsyncTaskProgress;
import org.geogig.server.service.async.Task;
import org.geogig.server.service.branch.Branch;
import org.geogig.server.service.branch.ConflictTuple;
import org.geogig.server.service.branch.DiffSummary;
import org.geogig.server.service.branch.RevertResult;
import org.geogig.server.service.pr.PullRequestRequest.PullRequestRequestBuilder;
import org.geogig.server.service.repositories.RepositoryManagementService;
import org.geogig.server.service.rpc.MergeRequest;
import org.geogig.server.service.rpc.MergeRequest.MergeRequestBuilder;
import org.geogig.server.service.rpc.MergeResult;
import org.geogig.server.service.rpc.RepositoryRPCService;
import org.geogig.server.service.stores.StoreService;
import org.geogig.server.service.transaction.TransactionService;
import org.geogig.server.service.user.UserService;
import org.geogig.server.stats.StatsService;
import org.geogig.web.model.AsyncTaskInfo;
import org.geogig.web.model.AsyncTaskInfo.StatusEnum;
import org.geogig.web.model.BoundingBox;
import org.geogig.web.model.BranchInfo;
import org.geogig.web.model.BranchInfoStatus;
import org.geogig.web.model.ConflictDetail;
import org.geogig.web.model.ConflictInfo;
import org.geogig.web.model.Error;
import org.geogig.web.model.FileStoreInfo;
import org.geogig.web.model.IdentifiedObject;
import org.geogig.web.model.LayerDiffSummary;
import org.geogig.web.model.LayersMergeReport;
import org.geogig.web.model.MergeReport;
import org.geogig.web.model.PostgresStoreInfo;
import org.geogig.web.model.ProgressInfo;
import org.geogig.web.model.PullRequestInfo;
import org.geogig.web.model.PullRequestRequest;
import org.geogig.web.model.PullRequestStatus.MergeableEnum;
import org.geogig.web.model.RepositoryInfo;
import org.geogig.web.model.RepositoryInfoStats;
import org.geogig.web.model.RevisionCommit;
import org.geogig.web.model.StoreConnectionInfo;
import org.geogig.web.model.StoreInfo;
import org.geogig.web.model.TaskResult;
import org.geogig.web.model.TransactionInfo;
import org.geogig.web.model.TransactionStatus;
import org.geogig.web.model.UserInfo;
import org.geogig.web.model.UserInfoPrivateProfile;
import org.geogig.web.model.UserInfoPublicProfile;
import org.locationtech.geogig.model.ObjectId;
import org.locationtech.geogig.model.RevFeature;
import org.locationtech.geogig.model.RevObject;
import org.locationtech.geogig.plumbing.merge.MergeScenarioReport;
import org.locationtech.geogig.plumbing.merge.MergeScenarioReport.TreeReport;
import org.locationtech.geogig.repository.Conflict;
import org.locationtech.geogig.storage.postgresql.config.ConnectionConfig;
import org.locationtech.geogig.storage.postgresql.config.EnvironmentBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.google.common.base.Preconditions;
import com.google.common.base.Strings;

import lombok.NonNull;

public @Service class PresentationService {

    private @Autowired UserService users;

    private @Autowired RepositoryManagementService repos;

    private @Autowired RepositoryRPCService repoRpc;

    private @Autowired TransactionService transactions;

    private @Autowired StoreService stores;

    private @Autowired StatsService statsService;

    public IdentifiedObject toIdentifiedObject(@NonNull Store o) {
        return toIdentifiedObject(o.getId(), o.getIdentity());
    }

    public Store toStore(@NonNull IdentifiedObject iobj) {
        if (iobj.getId() != null) {
            return stores.getOrFail(iobj.getId());
        } else if (iobj.getIdentity() != null) {
            return stores.getByNameOrFail(iobj.getIdentity());
        }
        throw new IllegalArgumentException("Neither the store id or name was provided");
    }

    public IdentifiedObject toIdentifiedObject(@NonNull User o) {
        return toIdentifiedObject(o.getId(), o.getIdentity());
    }

    public IdentifiedObject toIdentifiedObject(@NonNull RepoInfo o) {
        return toIdentifiedObject(o.getId(), o.getIdentity());
    }

    public IdentifiedObject toIdentifiedObject(UUID id, String name) {
        return new IdentifiedObject().id(id).identity(name);
    }

    public StoreInfo toInfo(@NonNull Store store) {
        return toInfo(store, false);
    }

    public StoreInfo toInfo(@NonNull Store store, boolean summary) {
        StoreInfo info = new StoreInfo();
        info.id(store.getId()).identity(store.getIdentity()).description(store.getDescription())
                .enabled(store.isEnabled());
        if (!summary) {
            info.connectionInfo(toConnectionInfo(store.getBaseURI()));
        }
        return info;
    }

    public Store toModel(@NonNull StoreInfo info) {
        Store s = new Store();
        s.setId(info.getId());
        s.setIdentity(info.getIdentity());
        s.setEnabled(info.isEnabled() == null ? false : info.isEnabled().booleanValue());
        s.setDescription(info.getDescription());
        s.setBaseURI(toURI(info.getConnectionInfo()));
        return s;
    }

    public StoreConnectionInfo toConnectionInfo(@NonNull String baseURI) {
        if (Strings.isNullOrEmpty(baseURI)) {
            return null;
        }
        final URI uri = URI.create(baseURI);
        final String scheme = uri.getScheme();
        Preconditions.checkArgument(!Strings.isNullOrEmpty(scheme),
                "base URI schem not provided, expected file:// or postgresql://, %s", baseURI);

        StoreConnectionInfo info = null;
        if ("file".equals(scheme)) {
            String directory = new File(uri).toURI().toString();
            FileStoreInfo f = new FileStoreInfo().directory(directory);
            info = f;
        } else if ("postgresql".equals(scheme)) {
            PostgresStoreInfo p = new PostgresStoreInfo();
            ConnectionConfig s = EnvironmentBuilder.parse(uri);
            p.server(s.getServer()).port(s.getPortNumber()).database(s.getDatabaseName())
                    .schema(s.getSchema()).user(s.getUser()).password(s.getPassword());
            info = p;
        }
        return info;
    }

    private String toURI(@NonNull StoreConnectionInfo info) {
        if (info instanceof FileStoreInfo) {
            String directory = ((FileStoreInfo) info).getDirectory();
            URI uri = URI.create(directory);
            String scheme = uri.getScheme();
            if (null == scheme) {
                uri = new File(directory).toURI();
            }
            return uri.toString();
        } else if (info instanceof PostgresStoreInfo) {
            PostgresStoreInfo psi = (PostgresStoreInfo) info;
            String rootURI = String.format("postgresql://%s:%d/%s/%s?user=%s&password=%s",
                    psi.getServer(), //
                    psi.getPort(), //
                    psi.getDatabase(), //
                    psi.getSchema(), //
                    psi.getUser(), //
                    psi.getPassword());
            return rootURI;
        }
        return null;
    }

    public AsyncTaskInfo toInfo(@NonNull Task<?> task) {
        AsyncTask taskInfo = task.getTaskInfo();
        AsyncTaskInfo asyncTaskInfo = toInfo(taskInfo).progress(toProgress(task.getProgress()));

        final @Nullable Object result = task.getResult();
        if (result != null && !Transaction.class.isInstance(result)) {
            TaskResult presentationResult = toTaskResult(result);
            asyncTaskInfo.setResult(presentationResult);
        }
        return asyncTaskInfo;
    }

    private TaskResult toTaskResult(@NonNull Object result) {
        if (result instanceof MergeResult) {
            return toInfo(((MergeResult) result));
        }
        if (result instanceof RepoInfo) {
            return toInfo((RepoInfo) result);
        }
        if (result instanceof org.geogig.server.service.branch.RevertResult) {
            return toInfo((org.geogig.server.service.branch.RevertResult) result);
        }
        if (result instanceof PullRequestStatus) {
            org.geogig.web.model.PullRequestStatus pstatus = toInfo((PullRequestStatus) result);
            return pstatus;
        }
        throw new IllegalArgumentException("Unknown object type: " + result.getClass().getName());
    }

    public org.geogig.web.model.PullRequestStatus toInfo(PullRequestStatus st) {
        org.geogig.web.model.PullRequestStatus pstatus = new org.geogig.web.model.PullRequestStatus();

        pstatus.commitsBehindTargetBranch(st.getCommitsBehindTargetBranch())//
                .commitsBehindTargetBranch(st.getCommitsBehindTargetBranch())//
                .mergeable(MergeableEnum.valueOf(st.getMergeable().toString()))//
                .numConflicts(st.getNumConflicts())//
                .mergeCommit(st.getMergeCommitId().orElse(null))//
                .transaction(st.getTransaction())//
                .affectedLayers(st.getAffectedLayers());

        // st.getReport();

        return pstatus;
    }

    public ProgressInfo toProgress(@NonNull Task<?> task) {
        return toProgress(task.getProgress());
    }

    public ProgressInfo toProgress(@Nullable AsyncTaskProgress progress) {
        ProgressInfo info = new ProgressInfo();
        if (null != progress) {
            info.setProgressDescription(progress.getProgressDescription());
            info.setMaxProgress(progress.getMaxProgress());
            info.setProgress(progress.getProgress());
            info.setTaskDescription(progress.getTaskDescription());
        }
        return info;
    }

    public AsyncTaskInfo toInfo(@NonNull AsyncTask asyncTask) {
        AsyncTaskInfo info = new AsyncTaskInfo();
        info.setId(asyncTask.getId());
        Status status = asyncTask.getStatus();
        info.setStatus(StatusEnum.valueOf(status.toString()));
        if (Status.FAILED == status) {
            info.setError(new Error().message(asyncTask.getErrorMessage()));
        }

        info.setDescription(asyncTask.getDescription());

        info.setLastUpdated(asyncTask.getLastUpdated());
        info.setScheduledAt(asyncTask.getScheduledAt());
        info.setStartedAt(asyncTask.getStartedAt());
        info.setFinishedAt(asyncTask.getFinishedAt());

        UserInfo startedBy = toUserInfo(asyncTask.getStartedByUserId()).orElse(null);
        UserInfo abortedBy = toUserInfo(asyncTask.getAbortedByUserId()).orElse(null);

        UUID repoid = asyncTask.getRepository();
        RepositoryInfo repo = repoid == null ? null
                : repos.get(repoid).map(this::toInfo).orElse(null);
        info.setRepository(repo);

        UUID transactionId = asyncTask.getTransactionId();
        if (transactionId != null) {
            TransactionInfo transaction = toInfo(transactions.getOrFail(transactionId));
            info.transaction(transaction);
        }
        info.setStartedBy(startedBy);
        info.setAbortedBy(abortedBy);
        info.setRepository(repo);
        return info;
    }

    public Optional<UserInfo> toUserInfo(UUID userId) {
        Optional<User> optional = userId == null ? Optional.empty() : users.get(userId);
        return optional.map(u -> toInfo(u));
    }

    public User toModel(@NonNull UserInfo i) {
        User u = new User();
        u.setId(i.getId());
        u.setIdentity(i.getIdentity());
        u.setSiteAdmin(i.isSiteAdmin() == null ? false : i.isSiteAdmin().booleanValue());
        u.setType(UserType.valueOf(i.getType().toString()));
        UserInfoPrivateProfile privateP = i.getPrivateProfile();
        UserInfoPublicProfile publicP = i.getPublicProfile();
        if (privateP != null) {
            List<IdentifiedObject> additionalStoreInfos = privateP.getAdditionalStores();
            IdentifiedObject defaultStoreInfo = privateP.getDefaultStore();
            Store defaultStore = defaultStoreInfo == null ? null : toStore(defaultStoreInfo);
            List<Store> additionalStores = additionalStoreInfos == null ? null
                    : additionalStoreInfos.stream().map(this::toStore).collect(Collectors.toList());
            String emailAddress = privateP.getEmailAddress();
            String fullName = privateP.getFullName();
            String location = privateP.getLocation();

            u.setEmailAddress(emailAddress);
            u.setFullName(fullName);
            u.setLocation(location);
            u.setDefaultStore(defaultStore == null ? null : defaultStore.getId());
            if (additionalStores != null) {
                u.setAdditionalStores(
                        additionalStores.stream().map(s -> s.getId()).collect(Collectors.toSet()));
            }
        }
        if (publicP != null) {
            String avatarUrl = publicP.getAvatarUrl();
            String company = publicP.getCompany();
            String gravatarId = publicP.getGravatarId();
            u.setAvatarUrl(avatarUrl);
            u.setCompany(company);
            u.setGravatarId(gravatarId);
        }
        return u;
    }

    public UserInfo toInfo(@NonNull User user) {
        UserInfo info = new UserInfo();
        info.setId(user.getId());
        info.setIdentity(user.getIdentity());
        info.setSiteAdmin(user.isSiteAdmin());
        UserType type = user.getType();
        info.setType(org.geogig.web.model.UserType.valueOf(type.toString()));

        UserInfoPrivateProfile privateP = new UserInfoPrivateProfile();
        UserInfoPublicProfile publicP = new UserInfoPublicProfile();
        info.setPrivateProfile(privateP);
        info.setPublicProfile(publicP);

        privateP.setCreatedAt(offsetTime(user.getCreatedAt()));
        privateP.setEmailAddress(user.getEmailAddress());
        privateP.setFullName(user.getFullName());
        privateP.setLocation(user.getLocation());
        privateP.setUpdatedAt(offsetTime(user.getUpdatedAt()));

        UUID defaultStore = user.getDefaultStore();
        privateP.setDefaultStore(defaultStore == null ? null
                : stores.get(defaultStore).map(this::toIdentifiedObject).orElse(null));

        Set<UUID> additionalStores = user.getAdditionalStores();
        if (additionalStores != null) {
            Stream<Store> s = additionalStores.stream().map(id -> stores.get(id).orElse(null))
                    .filter(t -> t != null);
            privateP.setAdditionalStores(
                    s.map(this::toIdentifiedObject).collect(Collectors.toList()));
        }

        String avatarUrl = user.getAvatarUrl();
        String company = user.getCompany();
        String gravatarId = user.getGravatarId();
        publicP.setAvatarUrl(avatarUrl);
        publicP.setCompany(company);
        publicP.setGravatarId(gravatarId);

        return info;
    }

    public RepositoryInfo toInfo(@NonNull RepoInfo repo) {
        RepositoryInfo info = new RepositoryInfo();
        info.setId(repo.getId());
        info.setIdentity(repo.getIdentity());
        info.setEnabled(repo.isEnabled());
        info.setDescription(repo.getDescription());
        info.setCreatedAt(offsetTime(repo.getCreatedAt()));
        if (repo.getOwnerId() != null) {
            info.setOwner(toIdentifiedObject(users.getOrFail(repo.getOwnerId())));
        }
        info.setStore(toIdentifiedObject(stores.getOrFail(repo.getStoreId())));
        if (repo.getForkedFrom() != null) {
            Optional<RepoInfo> origin = repos.get(repo.getForkedFrom());
            if (!origin.isPresent()) {
                origin = repos.findOrphanById(repo.getForkedFrom());
            }
            RepositoryInfo originInfo = origin.map(this::toInfo).orElse(null);
            info.setForkedFrom(originInfo);
        }

        Optional<RepoStats> repoStats = statsService.getRepoStats(repo);
        info.setStats(toInfo(repoStats.orElse(null)));
        return info;
    }

    public @Nullable RepositoryInfoStats toInfo(@Nullable RepoStats stats) {
        RepositoryInfoStats info = null;
        if (stats != null) {
            info = new RepositoryInfoStats()//
                    .numBranches(stats.getNumBranches())//
                    .numForks(stats.getNumForks())//
                    .numPullRequestsOpen(stats.getNumPullRequestsOpen());
        }
        return info;
    }

    public RepoInfo toModel(RepositoryInfo info) {
        RepoInfo m = new RepoInfo();
        m.setId(info.getId());
        m.setIdentity(info.getIdentity());
        m.setDescription(info.getDescription());
        // m.setDeleted(info.dele);
        m.setOwnerId(info.getOwner() == null ? null : info.getOwner().getId());
        m.setStoreId(info.getStore() == null ? null : info.getStore().getId());
        m.setForkedFrom(info.getForkedFrom() == null ? null : info.getForkedFrom().getId());
        m.setEnabled(info.isEnabled() == null ? true : info.isEnabled().booleanValue());
        return m;
    }

    public TransactionInfo toInfo(@NonNull Transaction t) {
        TransactionInfo i = new TransactionInfo();

        UserInfo createdBy = users.get(t.getCreatedByUserId()).map(this::toInfo).orElse(null);
        RepositoryInfo repository = repos.get(t.getRepositoryId()).map(this::toInfo).orElse(null);
        UUID terminatedByUserId = t.getTerminatedByUserId();
        UserInfo terminatedBy = null;
        if (Objects.equals(t.getCreatedByUserId(), t.getTerminatedByUserId())) {
            terminatedBy = createdBy;
        } else if (terminatedByUserId != null) {
            terminatedBy = users.get(terminatedByUserId).map(this::toInfo).orElse(null);
        }

        i.id(t.getId())//
                .createdAt(t.getCreatedAt())//
                .createdBy(createdBy)//
                .repository(repository)//
                .status(TransactionStatus.valueOf(t.getStatus().toString()))//
                .terminatedAt(t.getTerminatedAt())//
                .terminatedBy(terminatedBy);
        return i;
    }

    public List<LayerDiffSummary> toInfo(@NonNull DiffSummary summary) {
        RepoInfo leftRepo = summary.getLeftRepo();
        RepoInfo rightRepo = summary.getRightRepo();
        List<StoredDiffSummary> layerDiffSummary = summary.getLayerDiffSummary();
        List<LayerDiffSummary> ldf = layerDiffSummary.stream()
                .map(sdf -> toInfo(sdf, leftRepo, rightRepo)).collect(Collectors.toList());
        return ldf;
    }

    public LayerDiffSummary toInfo(@NonNull StoredDiffSummary ld, RepoInfo leftRepo,
            RepoInfo rigthRepo) {

        LayerDiffSummary s = new LayerDiffSummary();
        s.path(ld.getPath()).featuresAdded(ld.getFeaturesAdded())
                .featuresChanged(ld.getFeaturesChanged()).featuresRemoved(ld.getFeaturesRemoved());
        s.leftBounds(toBounds(ld.getLeftBounds()));
        s.rightBounds(toBounds(ld.getRightBounds()));
        ObjectId leftTreeish = ObjectId.valueOf(ld.getLeftPathTree());
        ObjectId rightTreeish = ObjectId.valueOf(ld.getRightPathTree());
        RevObject lo = leftTreeish == null ? null : repoRpc.getObject(leftRepo, leftTreeish);
        RevObject ro = rightTreeish == null ? null : repoRpc.getObject(rigthRepo, rightTreeish);
        s.leftTreeish(GeogigObjectModelBridge.map(lo));
        s.rightTreeish(GeogigObjectModelBridge.map(ro));
        return s;
    }

    private BoundingBox toBounds(Bounds b) {
        BoundingBox bbox = null;
        if (b != null) {
            bbox = new BoundingBox();
            bbox.add(b.getMinX());
            bbox.add(b.getMinY());
            bbox.add(b.getMaxX());
            bbox.add(b.getMaxY());
        }
        return bbox;
    }

    public MergeRequest toModel(@NonNull org.geogig.web.model.MergeRequest info) {
        MergeRequestBuilder r = MergeRequest.builder();

        boolean noFf = info.isNoFf() == null ? false : info.isNoFf();
        String base = info.getBase();
        String commitMessage = info.getCommitMessage();
        String head = info.getHead();
        MergeRequest request = r.base(base).commitMessage(commitMessage).head(head).noFf(noFf)
                .build();
        return request;
    }

    private org.geogig.web.model.RevertResult toInfo(@NonNull RevertResult rs) {
        org.geogig.web.model.RevertResult r = new org.geogig.web.model.RevertResult();

        boolean success = rs.isSuccess();
        String branch = rs.getBranch();
        UUID repoId = rs.getRepository();
        RepositoryInfo repository = repos.get(repoId).map(this::toInfo).orElse(null);
        LayersMergeReport layerReport = null;// TODO
        RevisionCommit revertCommit = GeogigObjectModelBridge
                .toCommit(repoRpc.getCommit(repoId, rs.getRevertCommit()));
        RevisionCommit revertedCommit = GeogigObjectModelBridge
                .toCommit(repoRpc.getCommit(repoId, rs.getRevertedCommit()));

        r.success(success)//
                .branch(branch)//
                .repository(repository)//
                .layerReport(layerReport)//
                .revertCommit(revertCommit)//
                .revertedCommit(revertedCommit);
        return r;
    }

    private org.geogig.web.model.MergeResult toInfo(@NonNull MergeResult mr) {

        org.geogig.web.model.MergeResult r = new org.geogig.web.model.MergeResult();
        boolean success = mr.isSuccess();
        RevisionCommit commonAncestor = GeogigObjectModelBridge.toCommit(mr.getCommonAncestor());
        RevisionCommit oursCommit = GeogigObjectModelBridge.toCommit(mr.getOursCommit());
        RevisionCommit theirsCommit = GeogigObjectModelBridge.toCommit(mr.getTheirsCommit());
        RevisionCommit mergeCommit = GeogigObjectModelBridge.toCommit(mr.getMergeCommit());

        MergeReport mergeReport = null;
        LayersMergeReport layerReport = null;

        MergeScenarioReport mergeScenarioReport = mr.getReport();
        if (mergeScenarioReport != null) {
            mergeReport = toInfo(mergeScenarioReport);
            List<TreeReport> treeReports = mergeScenarioReport.getTreeReports();
            layerReport = toInfo(treeReports);
        }
        r.success(success)//
                .commonAncestor(commonAncestor)//
                .layerReport(layerReport)//
                .mergeCommit(mergeCommit)//
                .mergeReport(mergeReport)//
                .oursCommit(oursCommit)//
                .theirsCommit(theirsCommit);
        return r;
    }

    private MergeReport toInfo(MergeScenarioReport mergeScenarioReport) {
        MergeReport mergeReport;
        mergeReport = new MergeReport();
        mergeReport.automergedCount(mergeScenarioReport.getMerged())//
                .conflictCount(mergeScenarioReport.getConflicts())//
                .unconflictedCount(mergeScenarioReport.getUnconflictedFeatures());
        return mergeReport;
    }

    private LayersMergeReport toInfo(List<TreeReport> treeReports) {
        LayersMergeReport lmr = new LayersMergeReport();
        for (TreeReport tr : treeReports) {
            lmr.put(tr.getPath(), toMergeReport(tr));
        }
        return lmr;
    }

    private MergeReport toMergeReport(TreeReport tr) {
        MergeReport mr = new MergeReport();
        mr.setConflictCount(tr.getConflicts());
        mr.setAutomergedCount(tr.getMerges());
        mr.setUnconflictedCount(tr.getUnconflictedFeatures());
        return mr;
    }

    public ConflictInfo toInfo(@NonNull ConflictTuple model) {
        Conflict c = model.getConflict();
        ConflictInfo info = GeogigObjectModelBridge.toConflict(c);

        info.setAncestor(GeogigObjectModelBridge.toId(c.getAncestor()));
        info.setOurs(GeogigObjectModelBridge.toId(c.getOurs()));
        info.setTheirs(GeogigObjectModelBridge.toId(c.getTheirs()));

        RevFeature ours = model.getOurs();
        RevFeature theirs = model.getTheirs();
        RevFeature ancestor = model.getAncestor();
        if (ours != null || theirs != null) {
            ConflictDetail detail = new ConflictDetail();
            info.setDetail(detail);
            detail.setAncestor(GeogigObjectModelBridge.toFeature(ancestor));
            detail.setOurs(GeogigObjectModelBridge.toFeature(ours));
            detail.setTheirs(GeogigObjectModelBridge.toFeature(theirs));
            detail.setBothEdited(model.getConflictAttributes());
        }

        return info;
    }

    public BranchInfo toBranchInfo(@NonNull Branch b) {
        RevisionCommit commit = GeogigObjectModelBridge.toCommit(b.getCommit());
        BranchInfo branchInfo = new BranchInfo().name(b.getName()).commit(commit)
                .description(b.getDescription());
        if (b.getRemoteName().isPresent()) {
            String tracking = b.getRemoteBranch().orElse(b.getName());
            BranchInfoStatus status = new BranchInfoStatus()//
                    .tracking(tracking)//
                    .commitsAhead(b.getCommitsAhead().orElse(0))//
                    .commitsBehind(b.getCommitsBehind().orElse(0));
            branchInfo.status(status);
        }
        return branchInfo;
    }

    public org.geogig.server.service.pr.PullRequestRequest toModel(@NonNull RepoInfo targetRepo,
            @NonNull PullRequestRequest info) {

        String sourceRepositoryOwner = info.getSourceRepositoryOwner();
        String sourceRepositryName = info.getSourceRepositryName();
        UUID issuerUser = users.getByNameOrFail(sourceRepositoryOwner).getId();
        UUID issuerRepo = repos.getOrFail(sourceRepositoryOwner, sourceRepositryName).getId();

        PullRequestRequestBuilder pr = org.geogig.server.service.pr.PullRequestRequest.builder();
        pr.title(info.getTitle());
        pr.description(info.getDescription());
        pr.issuerUser(issuerUser);
        pr.issuerRepo(issuerRepo);
        pr.issuerBranch(info.getSourceRepositoryBranch());
        pr.targetRepo(targetRepo.getId());
        pr.targetBranch(info.getTargetBranch());
        return pr.build();
    }

    public PullRequestInfo toInfo(@NonNull PullRequest pr) {
        PullRequestInfo info = new PullRequestInfo();

        info.id(pr.getId());
        info.sourceBranch(pr.getIssuerBranch());
        info.targetBranch(pr.getTargetBranch());
        info.title(pr.getTitle());
        info.description(pr.getDescription());
        info.status(org.geogig.web.model.PullRequestInfo.StatusEnum
                .fromValue(pr.getStatus().toString()));

        info.createdAt(offsetTime(pr.getCreatedAt()));
        info.updatedAt(offsetTime(pr.getUpdatedAt()));
        // TODO: remove PullRequestInfo.closedAt, it's redundant with updatedAt and status ==
        // closed|merged
        if (!pr.isOpen()) {
            info.closedAt(offsetTime(pr.getUpdatedAt()));
        }
        info.targetRepo(repos.get(pr.getRepositoryId()).map(this::toInfo).orElse(null));
        info.sourceRepo(repos.get(pr.getIssuerRepo()).map(this::toInfo).orElse(null));
        info.createdBy(users.get(pr.getIssuerUser()).map(this::toInfo).orElse(null));
        if (pr.getClosedByUserId() != null) {
            info.setClosedBy(users.get(pr.getClosedByUserId()).map(this::toInfo).orElse(null));
        }
        return info;
    }

    private OffsetDateTime offsetTime(Instant i) {
        return i == null ? null : OffsetDateTime.ofInstant(i, ZoneId.systemDefault());
    }
}
