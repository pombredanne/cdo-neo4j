package com.buschmais.cdo.neo4j.test.mapping;

import com.buschmais.cdo.api.CdoManager;
import com.buschmais.cdo.neo4j.test.AbstractCdoManagerTest;
import com.buschmais.cdo.neo4j.test.mapping.composite.A;
import org.hamcrest.core.IsCollectionContaining;
import org.junit.Test;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.core.IsCollectionContaining.hasItem;
import static org.junit.Assert.assertThat;

public class PrimitivePropertyMappingTest extends AbstractCdoManagerTest {

    @Override
    protected Class<?>[] getTypes() {
        return new Class<?>[]{A.class};
    }

    @Test
    public void primitiveProperty() {
        CdoManager cdoManager = getCdoManager();
        cdoManager.begin();
        A a = cdoManager.create(A.class);
        a.setString("value");
        cdoManager.commit();
        cdoManager.begin();
        assertThat(a.getString(), equalTo("value"));
        a.setString("updatedValue");
        cdoManager.commit();
        cdoManager.begin();
        assertThat(a.getString(), equalTo("updatedValue"));
        a.setString(null);
        cdoManager.commit();
        cdoManager.begin();
        assertThat(a.getString(), equalTo(null));
        cdoManager.commit();
    }

    @Test
    public void mappedPrimitiveProperty() {
        CdoManager cdoManager = getCdoManager();
        cdoManager.begin();
        A a = cdoManager.create(A.class);
        a.setMappedString("mappedValue");
        cdoManager.commit();
        cdoManager.begin();
        TestResult result = executeQuery("match (a:A) return a.MAPPED_STRING as v");
        assertThat(result.getColumn("v"), hasItem("mappedValue"));
    }
}
