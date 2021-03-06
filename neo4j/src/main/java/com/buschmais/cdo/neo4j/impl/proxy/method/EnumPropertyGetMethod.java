package com.buschmais.cdo.neo4j.impl.proxy.method;

import com.buschmais.cdo.neo4j.impl.metadata.EnumPropertyMetadata;
import com.buschmais.cdo.neo4j.impl.proxy.InstanceManager;
import org.neo4j.graphdb.DynamicLabel;
import org.neo4j.graphdb.Label;
import org.neo4j.graphdb.Node;

import java.util.Map;

public class EnumPropertyGetMethod extends AbstractPropertyMethod<EnumPropertyMetadata> {

    public EnumPropertyGetMethod(EnumPropertyMetadata metadata, InstanceManager instanceManager) {
        super(metadata, instanceManager);
    }

    public Object invoke(Node node, Object[] args) {
        for (Enum<?> enumerationValue : getMetadata().getEnumerationType().getEnumConstants()) {
            if (node.hasLabel(DynamicLabel.label(enumerationValue.name()))) {
                return enumerationValue;
            }
        }
        return null;
    }
}
