package org.geogig.server.service.transaction;

import java.util.Optional;
import java.util.UUID;

import org.geogig.server.model.Transaction;
import org.springframework.stereotype.Service;

@Service("TransactionInfoStore")
public interface TransactionInfoStore {

    public Optional<Transaction> findById(UUID txId);

    public Iterable<Transaction> findAll();

    public Iterable<Transaction> findByUserId(UUID userId);

    public Iterable<Transaction> findByRepositoryId(UUID repoId);

    public Iterable<Transaction> findByRepositoryIdAndStatus(UUID repoId,
            Transaction.Status status);

    public void create(Transaction tx);

    public void modify(Transaction tx);

    public Transaction deleteById(UUID txId);

    public void delete(Transaction t);
}
