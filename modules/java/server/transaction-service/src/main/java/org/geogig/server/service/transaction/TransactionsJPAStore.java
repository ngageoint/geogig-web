package org.geogig.server.service.transaction;

import java.util.Optional;
import java.util.UUID;

import org.geogig.server.model.Transaction;
import org.geogig.server.model.Transaction.Status;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.NonNull;

@Configuration
@EnableAutoConfiguration
@ComponentScan(basePackages = "org.geogig.server.service.transaction", lazyInit = true)
@EntityScan("org.geogig.server.service.transaction")
@EnableJpaRepositories("org.geogig.server.service.transaction")
public @Service class TransactionsJPAStore implements TransactionInfoStore {

    private TransactionsRepository repo;

    public TransactionsJPAStore(@Autowired TransactionsRepository repo) {
        this.repo = repo;
    }

    public @Override void create(@NonNull Transaction tx) {
        repo.save(tx);
    }

    public @Override void modify(@NonNull Transaction tx) {
        repo.save(tx);
    }

    public @Override Iterable<Transaction> findByUserId(@NonNull UUID userId) {
        return repo.findByUserId(userId);
    }

    public @Override Iterable<Transaction> findByRepositoryId(@NonNull UUID repoId) {
        return repo.findByRepositoryId(repoId);
    }

    public @Override Iterable<Transaction> findByRepositoryIdAndStatus(@NonNull UUID repoId,
            @NonNull Status status) {
        return repo.findByRepositoryIdAndStatus(repoId, status);
    }

    public @Override Optional<Transaction> findById(@NonNull UUID txId) {
        return repo.findById(txId);
    }

    public @Override Iterable<Transaction> findAll() {
        return repo.findAll();
    }

    @Transactional
    public @Override Transaction deleteById(@NonNull UUID txId) {
        Optional<Transaction> tx = findById(txId);
        if (tx.isPresent()) {
            repo.deleteById(txId);
        }
        return tx.orElse(null);
    }

    public @Override void delete(Transaction t) {
        repo.delete(t);
    }
}
