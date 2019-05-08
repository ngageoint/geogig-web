package org.geogig.server.service.stores;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.util.NoSuchElementException;
import java.util.UUID;

import org.geogig.server.model.Store;
import org.geogig.server.test.ConfigTestConfiguration;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.locationtech.geogig.repository.RepositoryConnectionException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

import com.google.common.collect.Iterables;
import com.google.common.collect.Sets;

@RunWith(SpringRunner.class)
@ContextConfiguration(classes = ConfigTestConfiguration.class)
@DataJpaTest
public class StoreServiceTest {

    private @Autowired StoreService service;

    private Store s1, s2, s3, s4;// pre built stores with null id

    public @Rule TemporaryFolder tmp = new TemporaryFolder();

    public @Rule ExpectedException ex = ExpectedException.none();

    public @Before void before() throws IOException {
        s1 = build("s1");
        s2 = build("s2");
        s3 = build("s3");
        s4 = build("s4");
    }

    private Store build(String name) throws IOException {
        String baseURI = tmp.newFolder(name).toURI().toString();
        return Store.builder().id(null).baseURI(baseURI).identity(name)
                .description("description of " + name).enabled(true).build();
    }

    public @Test(expected = IllegalArgumentException.class) void createNonNullId() {
        s1.setId(UUID.randomUUID());
        create(s1);
    }

    public @Test(expected = IllegalArgumentException.class) void createNullName() {
        s1.setIdentity(null);
        create(s1);
    }

    public @Test void createDuplicateName() {
        s1 = create(s1);
        s2 = create(s2);
        s3.setIdentity(s2.getIdentity());
        ex.expect(IllegalArgumentException.class);
        ex.expectMessage("already exists");
        create(s3);
    }

    public @Test(expected = IllegalArgumentException.class) void createNullURI() {
        s1.setBaseURI(null);
        create(s1);
    }

    public @Test void createInvalidURI() {
        String invalidURI = new File(s1.getBaseURI(), "non-existent-subdir").toURI().toString();
        s1.setBaseURI(invalidURI);
        ex.expect(IllegalArgumentException.class);
        ex.expectMessage("Parent directory does not exist");
        create(s1);
    }

    public @Test void createInvalidURISucceedsIfStoreIsDisabled() {
        String invalidURI = new File(s1.getBaseURI(), "non-existent-subdir").toURI().toString();
        s1.setBaseURI(invalidURI);
        s1.setEnabled(false);
        testCreate(s1);
    }

    public @Test void create() {
        testCreate(s1);
        testCreate(s2);
        testCreate(s3);
        testCreate(s4);
    }

    private void testCreate(Store store) {
        Store created = create(store);
        assertNotNull(created);
        assertNotNull(created.getId());
        assertNotNull(created.getCreatedAt());
        assertNotNull(created.getUpdatedAt());

        assertEquals(store.getBaseURI(), created.getBaseURI());
        assertEquals(store.getDescription(), created.getDescription());
        assertEquals(store.isEnabled(), created.isEnabled());
        assertEquals(store.getIdentity(), created.getIdentity());
    }

    private Store create(Store store) {
        Store created = service.create(store);
        return created;
    }

    public @Test void removeByName() {
        s1 = create(s1);
        s2 = create(s2);
        assertTrue(service.get(s1.getId()).isPresent());
        service.removeByName(s1.getIdentity());
        assertFalse(service.get(s1.getId()).isPresent());
        assertTrue(service.get(s2.getId()).isPresent());
    }

    public @Test void removeById() {
        s1 = create(s1);
        s2 = create(s2);
        assertTrue(service.get(s1.getId()).isPresent());
        service.removeById(s1.getId());
        assertFalse(service.get(s1.getId()).isPresent());
        assertTrue(service.get(s2.getId()).isPresent());
    }

    public @Test(expected = IllegalArgumentException.class) void testModifyNullId() {
        s1 = create(s1);
        s1.setId(null);
        service.modify(s1);
    }

    public @Test(expected = IllegalArgumentException.class) void testModifyNullIdentity() {
        s1 = create(s1);
        s1.setIdentity(null);
        service.modify(s1);
    }

    public @Test void testModify() {
        s1 = create(s1);
        s2 = create(s2);
        s2.setIdentity("newidentity");
        service.modify(s2);
        assertEquals(s2, service.getOrFail(s2.getId()));

        s2.setEnabled(false);
        s2.setDescription("new description");
        assertEquals(s2, service.getOrFail(s2.getId()));
    }

    public @Test void testModifyInvalidConnectionStringUnableToConnectIfEnabled() {
        s1 = create(s1);
        s2 = create(s2);

        String invalidURI = new File(s1.getBaseURI(), "invalid-subdir").toURI().toString();
        s1.setEnabled(false);
        s1.setBaseURI(invalidURI);
        service.modify(s1);
        assertEquals(s1, service.getOrFail(s1.getId()));

        s1.setEnabled(true);
        ex.expect(IllegalArgumentException.class);
        service.modify(s1);
    }

    public @Test void testConnectByName()
            throws NoSuchElementException, RepositoryConnectionException {
        s1 = create(s1);
        s2 = create(s2);
        assertNotNull(service.connect(s1.getIdentity()));
        assertNotNull(service.connect(s2.getIdentity()));
        service.removeById(s2.getId());
        ex.expect(NoSuchElementException.class);
        service.connect(s2.getIdentity());
    }

    public @Test void testConnectUUID()
            throws NoSuchElementException, RepositoryConnectionException {
        s1 = create(s1);
        s2 = create(s2);
        assertNotNull(service.connect(s1.getId()));
        assertNotNull(service.connect(s2.getId()));
        service.removeById(s2.getId());
        ex.expect(NoSuchElementException.class);
        service.connect(s2.getId());
    }

    public @Test void testGetAll() {
        assertEquals(0, Iterables.size(service.getAll()));
        s1 = create(s1);
        s2 = create(s2);
        assertEquals(Sets.newHashSet(s1, s2), Sets.newHashSet(service.getAll()));
        s3 = create(s3);
        s4 = create(s4);
        service.removeById(s2.getId());
        assertEquals(Sets.newHashSet(s1, s3, s4), Sets.newHashSet(service.getAll()));
    }

    public @Test void testGetOrFail() {
        s1 = create(s1);
        s2 = create(s2);
        assertEquals(s1, service.getOrFail(s1.getId()));
        ex.expect(NoSuchElementException.class);
        service.getOrFail(UUID.randomUUID());
    }

    public @Test void testGet() {
        s1 = create(s1);
        s2 = create(s2);
        assertTrue(service.get(s1.getId()).isPresent());
        assertTrue(service.get(s2.getId()).isPresent());
        assertFalse(service.get(UUID.randomUUID()).isPresent());

        s3 = create(s3);
        s4 = create(s4);
        assertTrue(service.get(s3.getId()).isPresent());
        assertTrue(service.get(s4.getId()).isPresent());
    }

    public @Test void testGetByName() {
        s1 = create(s1);
        s2 = create(s2);
        assertTrue(service.getByName(s1.getIdentity()).isPresent());
        assertTrue(service.getByName(s2.getIdentity()).isPresent());
        assertFalse(service.getByName(s3.getIdentity()).isPresent());
        assertFalse(service.getByName(s4.getIdentity()).isPresent());
        create(s3);
        assertTrue(service.getByName(s3.getIdentity()).isPresent());
    }

    public @Test void testGetByNameOrFail() {
        s1 = create(s1);
        s2 = create(s2);
        assertEquals(s1, service.getByNameOrFail(s1.getIdentity()));
        assertEquals(s2, service.getByNameOrFail(s2.getIdentity()));
        ex.expect(NoSuchElementException.class);
        service.getByNameOrFail(s3.getIdentity());
    }
}
