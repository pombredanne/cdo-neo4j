package com.buschmais.cdo.neo4j.impl.proxy.method;

import org.neo4j.graphdb.Node;

public class HashCodeMethod implements ProxyMethod {

    @Override
    public Object invoke(Node node, Object[] args) {
        return Long.valueOf(node.getId());
    }
}
