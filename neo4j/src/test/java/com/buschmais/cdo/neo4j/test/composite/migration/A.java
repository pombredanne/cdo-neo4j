package com.buschmais.cdo.neo4j.test.composite.migration;

import com.buschmais.cdo.neo4j.annotation.Label;

@Label("A")
public interface A {

    String getValue();

    void setValue(String value);

}