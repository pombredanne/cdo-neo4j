package com.buschmais.cdo.neo4j.test.mapping;

import com.buschmais.cdo.api.CdoManager;
import com.buschmais.cdo.neo4j.test.AbstractCdoManagerTest;
import com.buschmais.cdo.neo4j.test.mapping.composite.A;
import com.buschmais.cdo.neo4j.test.mapping.composite.B;
import org.junit.Test;

import java.util.List;
import java.util.Set;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.core.IsCollectionContaining.hasItem;
import static org.junit.Assert.assertThat;

public class CollectionPropertyMappingTest extends AbstractCdoManagerTest {

    @Override
    protected Class<?>[] getTypes() {
        return new Class<?>[]{A.class, B.class};
    }

    @Test
    public void setProperty() {
        CdoManager cdoManager = getCdoManager();
        cdoManager.begin();
        A a = cdoManager.create(A.class);
        B b = cdoManager.create(B.class);
        Set<B> setOfB = a.getSetOfB();
        assertThat(setOfB.add(b), equalTo(true));
        assertThat(setOfB.add(b), equalTo(false));
        assertThat(setOfB.size(), equalTo(1));
        cdoManager.commit();
        cdoManager.begin();
        assertThat(setOfB.remove(b), equalTo(true));
        assertThat(setOfB.remove(b), equalTo(false));
        cdoManager.commit();
    }

    @Test
    public void mappedSetProperty() {
        CdoManager cdoManager = getCdoManager();
        cdoManager.begin();
        A a = cdoManager.create(A.class);
        B b = cdoManager.create(B.class);
        a.getMappedSetOfB().add(b);
        cdoManager.commit();
        cdoManager.begin();
        TestResult result = executeQuery("match (a:A)-[:MAPPED_SET_OF_B]->(b) return b");
        assertThat(result.getColumn("b"), hasItem(b));
        cdoManager.commit();
    }

    @Test
    public void listProperty() {
        CdoManager cdoManager = getCdoManager();
        cdoManager.begin();
        A a = cdoManager.create(A.class);
        B b = cdoManager.create(B.class);
        List<B> listOfB = a.getListOfB();
        assertThat(listOfB.add(b), equalTo(true));
        assertThat(listOfB.add(b), equalTo(true));
        assertThat(listOfB.size(), equalTo(2));
        cdoManager.commit();
        cdoManager.begin();
        assertThat(listOfB.remove(b), equalTo(true));
        assertThat(listOfB.remove(b), equalTo(true));
        assertThat(listOfB.remove(b), equalTo(false));
        cdoManager.commit();
    }

    @Test
    public void mappedListProperty() {
        CdoManager cdoManager = getCdoManager();
        cdoManager.begin();
        A a = cdoManager.create(A.class);
        B b = cdoManager.create(B.class);
        a.getMappedListOfB().add(b);
        cdoManager.commit();
        cdoManager.begin();
        TestResult result = executeQuery("match (a:A)-[:MAPPED_LIST_OF_B]->(b) return b");
        assertThat(result.getColumn("b"), hasItem(b));
        cdoManager.commit();
    }
}
