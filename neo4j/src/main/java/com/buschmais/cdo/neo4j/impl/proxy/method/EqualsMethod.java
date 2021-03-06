package com.buschmais.cdo.neo4j.impl.proxy.method;

import com.buschmais.cdo.neo4j.impl.proxy.InstanceManager;
import org.neo4j.graphdb.Node;

public class EqualsMethod implements ProxyMethod {

    private InstanceManager instanceManager;

    public EqualsMethod(InstanceManager instanceManager) {
        this.instanceManager = instanceManager;
    }

    @Override
    public Object invoke(Node node, Object[] args) {
        Object other = args[0];
        if (instanceManager.isNode(other)) {
            Node otherNode = instanceManager.getNode(other);
            boolean equal = (otherNode.getId() == node.getId());
            return Boolean.valueOf(equal);
        }
        return Boolean.valueOf(false);
    }
}
