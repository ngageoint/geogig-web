package org.geogig.server.service.branch;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Collectors;

import org.geogig.server.model.RepoInfo;
import org.geogig.server.model.StoredDiffSummary;
import org.geogig.server.model.StoredDiffSummary.Bounds;
import org.geogig.server.service.transaction.TransactionService;
import org.locationtech.geogig.model.ObjectId;
import org.locationtech.geogig.model.RevTree;
import org.locationtech.geogig.plumbing.ResolveTreeish;
import org.locationtech.geogig.plumbing.diff.DiffSummaryOp;
import org.locationtech.geogig.plumbing.diff.DiffSummaryOp.LayerDiffSummary;
import org.locationtech.geogig.repository.Context;
import org.locationtech.geogig.repository.RepositoryConnectionException;
import org.locationtech.jts.geom.Envelope;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.google.common.base.Preconditions;

import lombok.NonNull;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service("DiffSummaryService")
public class DiffSummaryService {

    private @Autowired TransactionService tx;

    private @Autowired LayerDiffSummaryStore diffSummaryRepository;

    static @Value class Key {
        final @NonNull UUID leftRepo, rightRepo;

        final @NonNull String leftTreeish, rightTreeish;

        final UUID leftRepoTx, rightRepoTx;
    }

    private ConcurrentMap<Key, CompletableFuture<DiffSummary>> running = new ConcurrentHashMap<>();

    public CompletableFuture<DiffSummary> diffSummary(@NonNull UUID repo,
            @NonNull String leftTreeish, @NonNull String rightTreeish, UUID tx) {
        return diffSummary(repo, repo, leftTreeish, rightTreeish, tx, tx);
    }

    public CompletableFuture<DiffSummary> diffSummary(@NonNull UUID leftRepo,
            @NonNull UUID rightRepo, @NonNull String leftTreeish, @NonNull String rightTreeish,
            UUID leftRepoTx, UUID rightRepoTx) {

        Key key = new Key(leftRepo, rightRepo, leftTreeish, rightTreeish, leftRepoTx, rightRepoTx);

        CompletableFuture<DiffSummary> future = running.computeIfAbsent(key,
                k -> CompletableFuture.supplyAsync(() -> runInternal(k)));

        future = future.whenComplete((res, ex) -> running.remove(key));
        return future;
    }

    private DiffSummary runInternal(@NonNull Key params) {

        final RepoInfo leftRepoInfo = tx.getRepos().getOrFail(params.leftRepo);
        final RepoInfo rightRepoInfo = params.leftRepo.equals(params.rightRepo) ? leftRepoInfo
                : tx.getRepos().getOrFail(params.rightRepo);

        final Context leftSource;
        final Context rightSource;
        final ObjectId leftRootTreeId, rightRootTreeId;
        try {
            leftSource = tx.resolveContext(leftRepoInfo, params.leftRepoTx);
            if (params.leftRepo.equals(params.rightRepo)
                    && Objects.equals(params.leftRepoTx, params.rightRepoTx)) {
                rightSource = leftSource;
            } else {
                rightSource = tx.resolveContext(rightRepoInfo, params.rightRepoTx);
            }
            leftRootTreeId = leftSource.command(ResolveTreeish.class).setTreeish(params.leftTreeish)
                    .call().orNull();
            rightRootTreeId = rightSource.command(ResolveTreeish.class)
                    .setTreeish(params.rightTreeish).call().orNull();
            Preconditions.checkArgument(leftRootTreeId != null,
                    "refspec %s at the left of the comparison does not resolve to a tree");
            Preconditions.checkArgument(rightRootTreeId != null,
                    "refspec %s at the right of the comparison does not resolve to a tree");
        } catch (NoSuchElementException e) {
            throw e;
        } catch (RepositoryConnectionException e) {
            throw new RuntimeException(e);
        }

        Optional<List<StoredDiffSummary>> stored;
        stored = diffSummaryRepository.find(leftRootTreeId, rightRootTreeId);
        List<StoredDiffSummary> result;
        if (stored.isPresent()) {
            log.debug("Using stored layer diff summary for {}/{}", leftRootTreeId, rightRootTreeId);
            result = stored.get();
        } else {
            log.debug("No pre-computed layer diff summary found for {}/{}", leftRootTreeId,
                    rightRootTreeId);

            RevTree leftTree = leftSource.objectDatabase().getTree(leftRootTreeId);
            RevTree rightTree = rightSource.objectDatabase().getTree(rightRootTreeId);

            DiffSummaryOp command;
            command = DiffSummaryOp.builder()//
                    .leftSource(leftSource.objectDatabase())//
                    .rightSource(rightSource.objectDatabase())//
                    .leftTree(leftTree)//
                    .rightTree(rightTree)//
                    .build();

            command.setContext(leftSource);

            List<LayerDiffSummary> computedResult = command.call();

            result = computedResult.stream()
                    .map(ld -> toDiffSummary(ld, leftRootTreeId, rightRootTreeId))
                    .collect(Collectors.toList());
            diffSummaryRepository.save(result);
        }
        return new DiffSummary(leftRepoInfo, rightRepoInfo, result);
    }

    private StoredDiffSummary toDiffSummary(DiffSummaryOp.LayerDiffSummary d,
            ObjectId leftRootTreeId, ObjectId rightRootTreeId) {

        StoredDiffSummary layerSummary = StoredDiffSummary.builder()//
                .path(d.getPath())//
                .leftRootId(leftRootTreeId.toString())//
                .rightRootId(rightRootTreeId.toString())//
                .leftPathTree(d.getLeftTreeish().toString())//
                .rightPathTree(d.getRightTreeish().toString())//
                .featuresAdded(d.getFeaturesAdded())//
                .featuresChanged(d.getFeaturesChanged())//
                .featuresRemoved(d.getFeaturesRemoved())//
                .leftBounds(toBounds(d.getLeftBounds()))//
                .rightBounds(toBounds(d.getRightBounds()))//
                .build();
        return layerSummary;
    }

    private Bounds toBounds(Envelope e) {
        if (e == null || e.isNull()) {
            return null;
        }
        return new Bounds(e.getMinX(), e.getMinY(), e.getMaxX(), e.getMaxY());
    }
}
