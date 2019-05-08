package org.geogig.server.service.repositories;

import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.geogig.server.model.RepoInfo;
import org.springframework.stereotype.Service;

import com.google.common.collect.Lists;

import lombok.NonNull;

@Service("RepositoryInfoStore")
public interface RepositoryInfoStore {

    RepoInfo create(RepoInfo repo);

    RepoInfo save(RepoInfo repo);

    void deleteById(UUID id);

    Iterable<RepoInfo> findAll();

    Optional<RepoInfo> findById(UUID id);

    List<RepoInfo> getByOwner(UUID ownerId);

    Optional<RepoInfo> getByOwner(UUID id, String repoName);

    List<RepoInfo> getByStore(UUID storeId);

    int getCountByStore(UUID storeId);

    public default RepoInfo getOrFail(UUID id) {
        Optional<RepoInfo> repo = findById(id);
        return repo
                .orElseThrow(() -> new NoSuchElementException("Repository " + id + " not found"));
    }

    public default Set<RepoInfo> getConstellationOf(UUID id) {
        final RepoInfo repo = getOrFail(id);
        final List<RepoInfo> all = Lists.newArrayList(findAll());

        final RepoInfo root = findRootOf(repo, all);

        boolean recursive = true;
        Set<RepoInfo> constellation = findForksOf(root, recursive, all);
        constellation.add(root);
        return constellation;
    }

    public default Set<RepoInfo> getForksOf(@NonNull UUID id, boolean recursive) {
        List<RepoInfo> all = Lists.newArrayList(findAll());
        return findForksOf(id, recursive, all);
    }

    public default @NonNull RepoInfo findRootOf(@NonNull RepoInfo repo, List<RepoInfo> all) {
        UUID originId = repo.getForkedFrom();
        if (originId == null) {
            return repo;
        }
        final RepoInfo origin = getOrFail(originId);
        final UUID parentOwner = origin.getOwnerId();
        final String parentName = origin.getIdentity();

        Predicate<RepoInfo> predicate = (r) -> {
            UUID owner = r.getOwnerId();
            String name = r.getIdentity();
            return parentOwner.equals(owner) && parentName.equals(name);
        };

        RepoInfo parent = all.stream().parallel().filter(predicate).findFirst()//
                .orElse(null);// may the parent no longer exist?

        return parent == null ? repo : findRootOf(parent, all);
    }

    public default Set<RepoInfo> findForksOf(@NonNull RepoInfo root, final boolean recursive,
            List<RepoInfo> all) {
        return findForksOf(root.getId(), recursive, all);
    }

    public default Set<RepoInfo> findForksOf(@NonNull UUID root, final boolean recursive,
            List<RepoInfo> all) {

        Predicate<RepoInfo> predicate = (r) -> {
            UUID originId = r.getForkedFrom();
            return root.equals(originId);
        };

        Set<RepoInfo> forks = all.stream().filter(predicate).collect(Collectors.toSet());
        if (recursive) {
            for (RepoInfo fork : new ArrayList<>(forks)) {
                forks.addAll(findForksOf(fork.getId(), true, all));
            }
        }
        return forks;
    }

    boolean existsById(UUID repoId);

}
