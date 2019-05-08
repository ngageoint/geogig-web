package org.geogig.web.integration.client;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.geogig.web.client.Branch;
import org.geogig.web.client.Layer;
import org.geogig.web.client.Repo;
import org.geogig.web.client.Store;
import org.geogig.web.client.User;
import org.geogig.web.integration.AbstractIntegrationTest;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import com.google.common.collect.ImmutableSet;

public class BranchesTest extends AbstractIntegrationTest {

    private Store store;

    private User user;

    private Repo repo;

    public @Rule ExpectedException ex = ExpectedException.none();

    public @Before void before() {
        String storeName = testName.getMethodName() + "_store";
        store = testSupport.createStore(storeName, "Default store for all");
        user = testSupport.createUser("gabe", "s3cr3t", "Gabriel Roldan", "gabe@example.com",
                storeName);

        repo = user.createRepo("naturalEearth");
    }

    private Layer createPoiLayer() {
        return createPoiLayer(repo);
    }

    private Layer createRoadsLayer() {
        return createRoadsLayer(repo);
    }

    public @Test void tryDeleteCurrentBranch() throws Exception {
        ex.expect(RuntimeException.class);
        repo.branches().delete("master");
    }

    public @Test void tryCreateExistingBranch() throws Exception {
        ex.expect(RuntimeException.class);
        repo.branches().createBranch("master", "master", null);
    }

    public @Test void tryBranchNonExistingBranch() throws Exception {
        ex.expect(NoSuchElementException.class);
        repo.branches().createBranch("nonExistingBranch", "branch2", null);
    }

    public @Test void createBranch() throws Exception {
        createPoiLayer();
        Optional<Branch> curr = repo.branches().currentBranch();
        assertNotNull(curr);
        assertTrue(curr.isPresent());
        Branch master = curr.get();
        assertEquals("master", master.getName());
        assertNotNull(master.getCommit());
        Branch newBranch = master.branch("branch1", "experimental branch");
        assertNotNull(newBranch);
        assertEquals("branch1", newBranch.getName());
        assertEquals("experimental branch", newBranch.getDescription());
        assertEquals(master.getCommit(), newBranch.getCommit());
    }

    public @Test void createBranchEmmptyRepo() throws Exception {
        Branch master = repo.branches().currentBranch().get();
        assertNull(master.getCommit());
        ex.expect(RuntimeException.class);
        master.branch("shouldntBeCreatedBranch");
    }

    public @Test void createBranchesTest() throws Exception {

        createPoiLayer();

        Branch master = repo.branches().currentBranch().get();
        Branch branch1 = master.branch("branch1");
        assertEquals(master.getCommit(), branch1.getCommit());

        createRoadsLayer();
        Branch branch2 = master.branch("branch2");
        assertNotEquals(branch1.getCommit(), branch2.getCommit());

        Branch branch11 = branch1.branch("branch11");
        Branch branch21 = branch2.branch("branch21");
        assertEquals(branch1.getCommit(), branch11.getCommit());
        assertEquals(branch2.getCommit(), branch21.getCommit());
    }

    public @Test void listBranchesNewRepo() throws Exception {
        List<Branch> branches = repo.branches().getAll();
        assertNotNull(branches);
        assertEquals(1, branches.size());
        Branch branch = branches.get(0);
        assertEquals("master", branch.getName());
        assertNull(branch.getDescription());
        assertNull(branch.getCommit());
    }

    public @Test void listBranchesTest() throws Exception {
        createPoiLayer();
        Branch master = repo.branches().currentBranch().get();
        Branch branch1 = master.branch("branch1");

        createRoadsLayer();
        Branch branch2 = master.branch("branch2");

        Branch branch11 = branch1.branch("branch11");
        Branch branch21 = branch2.branch("branch21");

        List<Branch> all = repo.branches().getAll();
        assertEquals(5, all.size());
        Set<String> names = all.stream().map((b) -> b.getName()).collect(Collectors.toSet());
        assertEquals(ImmutableSet.of("master", "branch1", "branch11", "branch2", "branch21"),
                names);
    }

    public @Test void deleteBranchTest() throws Exception {
        createPoiLayer();
        Branch master = repo.branches().currentBranch().get();
        assertFalse(repo.branches().tryGet("branch1").isPresent());

        assertFalse(repo.branches().tryGet("branch1").isPresent());
        master.branch("branch1");

        assertFalse(repo.branches().tryGet("branch2").isPresent());
        master.branch("branch2");

        assertTrue(repo.branches().tryGet("branch1").isPresent());
        repo.branches().delete("branch1");
        assertFalse(repo.branches().tryGet("branch1").isPresent());

        assertTrue(repo.branches().tryGet("branch2").isPresent());
        repo.branches().delete("branch2");
        assertFalse(repo.branches().tryGet("branch2").isPresent());
    }

    public @Test void deleteBranchTestInsideTransaction() throws Exception {
        createPoiLayer();
        Branch master = repo.branches().currentBranch().get();
        Branch branch = master.branch("branch1");

        Repo transaction = repo.clone().startTransaction();
        transaction.branches().delete(branch.getName());

        assertTrue(repo.branches().tryGet(branch.getName()).isPresent());
        assertFalse(transaction.branches().tryGet(branch.getName()).isPresent());

        transaction.commitSync();

        assertFalse(repo.branches().tryGet(branch.getName()).isPresent());
    }

    public @Test void testBranchOnTransactions() throws Exception {
        createPoiLayer();
        Branch master = repo.branches().currentBranch().get();
        Repo tx1 = repo.clone();
        Repo tx2 = repo.clone();

        tx1.startTransaction();
        Branch no_tx_branch1 = master.branch("no_tx_branch1");

        assertFalse(tx1.branches().tryGet(no_tx_branch1.getName()).isPresent());
        assertTrue(tx2.branches().tryGet(no_tx_branch1.getName()).isPresent());

        tx2.startTransaction();

        Branch no_tx_branch2 = master.branch("no_tx_branch2");
        assertFalse(tx1.branches().tryGet(no_tx_branch2.getName()).isPresent());
        assertFalse(tx2.branches().tryGet(no_tx_branch2.getName()).isPresent());

        Branch tx1_branch1 = tx1.branches().createBranch("master", "tx1_branch1", null);
        Branch tx2_branch1 = tx2.branches().createBranch("master", "tx2_branch1", null);

        assertFalse(repo.branches().tryGet(tx1_branch1.getName()).isPresent());
        assertFalse(repo.branches().tryGet(tx2_branch1.getName()).isPresent());

        assertFalse(tx2.branches().tryGet(tx1_branch1.getName()).isPresent());
        assertFalse(tx1.branches().tryGet(tx2_branch1.getName()).isPresent());

        tx2.branches().delete(no_tx_branch1.getName());
        assertFalse(tx2.branches().tryGet(no_tx_branch1.getName()).isPresent());
        assertTrue(repo.branches().tryGet(no_tx_branch1.getName()).isPresent());

        tx1.commitSync();
        assertFalse(tx2.branches().tryGet(tx1_branch1.getName()).isPresent());
        assertTrue(repo.branches().tryGet(tx1_branch1.getName()).isPresent());

        tx2.commitSync();
        assertTrue(repo.branches().tryGet(tx1_branch1.getName()).isPresent());
        assertFalse(repo.branches().tryGet(no_tx_branch1.getName()).isPresent());
    }
}
