package org.geogig.server.service.transaction;

import java.util.UUID;

import javax.transaction.Transactional;

import org.geogig.server.model.Transaction;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import lombok.NonNull;

public @Repository interface TransactionsRepository extends CrudRepository<Transaction, UUID> {

    public Iterable<Transaction> findByUserId(UUID userId);

    public Iterable<Transaction> findByRepositoryId(@NonNull UUID repoId);

    public Iterable<Transaction> findByRepositoryIdAndStatus(UUID repoId,
            Transaction.Status status);

    public @Transactional void deleteByIdAndRepositoryId(int prid, UUID repoId);
}
