package com.buschmais.cdo.neo4j.impl;

import com.buschmais.cdo.api.CdoManagerException;
import com.buschmais.cdo.api.QueryResult;
import com.buschmais.cdo.neo4j.api.EmbeddedNeo4jCdoManager;
import com.buschmais.cdo.neo4j.impl.metadata.NodeMetadata;
import com.buschmais.cdo.neo4j.impl.metadata.NodeMetadataProvider;
import com.buschmais.cdo.neo4j.impl.metadata.PrimitivePropertyMetadata;
import com.buschmais.cdo.neo4j.impl.proxy.InstanceManager;
import org.neo4j.cypher.javacompat.ExecutionEngine;
import org.neo4j.cypher.javacompat.ExecutionResult;
import org.neo4j.graphdb.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Closeable;
import java.io.IOException;
import java.util.*;

public class EmbeddedNeo4jCdoManagerImpl implements EmbeddedNeo4jCdoManager {

    private static final Logger LOGGER = LoggerFactory.getLogger(EmbeddedNeo4jCdoManagerImpl.class);

    private final NodeMetadataProvider nodeMetadataProvider;
    private final InstanceManager instanceManager;
    private final GraphDatabaseService database;
    private final ExecutionEngine executionEngine;
    private Transaction transaction;

    public EmbeddedNeo4jCdoManagerImpl(NodeMetadataProvider nodeMetadataProvider, GraphDatabaseService database, InstanceManager instanceManager) {
        this.nodeMetadataProvider = nodeMetadataProvider;
        this.database = database;
        this.instanceManager = instanceManager;
        this.executionEngine = new ExecutionEngine(database);
    }

    @Override
    public void begin() {
        transaction = database.beginTx();
    }

    @Override
    public void commit() {
        transaction.success();
        transaction.close();
    }

    @Override
    public void rollback() {
        transaction.failure();
        transaction.close();
    }

    @Override
    public <T> Iterable<T> find(final Class<T> type, final Object value) {
        NodeMetadata nodeMetadata = nodeMetadataProvider.getNodeMetadata(type);
        Label label = nodeMetadata.getLabel();
        if (label == null) {
            throw new CdoManagerException("Type " + type.getName() + " has no label.");
        }
        PrimitivePropertyMetadata indexedProperty = nodeMetadata.getIndexedProperty();
        if (indexedProperty == null) {
            throw new CdoManagerException("Type " + nodeMetadata.getType().getName() + " has no indexed property.");
        }
        final ResourceIterable<Node> nodesByLabelAndProperty = database.findNodesByLabelAndProperty(label, indexedProperty.getPropertyName(), value);
        return new Iterable<T>() {
            @Override
            public Iterator<T> iterator() {
                final ResourceIterator<Node> iterator = nodesByLabelAndProperty.iterator();
                return new Iterator<T>() {

                    @Override
                    public boolean hasNext() {
                        return iterator.hasNext();
                    }

                    @Override
                    public T next() {
                        Node node = iterator.next();
                        return instanceManager.getInstance(node);
                    }

                    @Override
                    public void remove() {
                        throw new UnsupportedOperationException("Cannot remove instance.");
                    }
                };
            }
        };
    }

    @Override
    public <T> T create(Class<?>... types) {
        Node node = database.createNode();
        for (Class<?> type : types) {
            NodeMetadata nodeMetadata = nodeMetadataProvider.getNodeMetadata(type);
            for (Label label : nodeMetadata.getAggregatedLabels()) {
                node.addLabel(label);
            }
        }
        return (T) instanceManager.getInstance(node, Arrays.asList(types));
    }

    @Override
    public <T, M> M migrate(T instance, Class<?>... targetTypes) {
        return migrate(instance, null, targetTypes);
    }

    @Override
    public <T, M> M migrate(T instance, MigrationHandler<T, M> migrationHandler, Class<?>... targetTypes) {
        Node node = instanceManager.getNode(instance);
        List<Class<?>> types = instanceManager.getTypes(node);
        Set<Label> labels = new HashSet<>();
        for (Class<?> type : types) {
            NodeMetadata nodeMetadata = nodeMetadataProvider.getNodeMetadata(type);
            labels.addAll(nodeMetadata.getAggregatedLabels());
        }
        Set<Label> targetLabels = new HashSet<>();
        for (Class<?> targetType : targetTypes) {
            NodeMetadata targetMetadata = nodeMetadataProvider.getNodeMetadata(targetType);
            targetLabels.addAll(targetMetadata.getAggregatedLabels());
        }
        Set<Label> labelsToRemove = new HashSet<>(labels);
        labelsToRemove.removeAll(targetLabels);
        for (Label label : labelsToRemove) {
            node.removeLabel(label);
        }
        Set<Label> labelsToAdd = new HashSet<>(targetLabels);
        labelsToAdd.removeAll(labels);
        for (Label label : labelsToAdd) {
            node.addLabel(label);
        }
        instanceManager.removeInstance(instance);
        M migratedInstance = instanceManager.getInstance(node);
        if (migrationHandler != null) {
            migrationHandler.migrate(instance, migratedInstance);
        }
        instanceManager.destroyInstance(instance);
        return migratedInstance;
    }

    @Override
    public <T> void delete(T instance) {
        Node node = instanceManager.getNode(instance);
        node.delete();
    }

    @Override
    public QueryResult executeQuery(String query) {
        return executeQuery(query, Collections.<String, Object>emptyMap());
    }

    @Override
    public QueryResult executeQuery(String query, Map<String, Object> parameters) {
        ExecutionResult result = executionEngine.execute(query, parameters);
        Iterable<QueryResult.Row> rowIterable = new RowIterable(result.columns(), result.iterator());
        return new QueryResultImpl(result.columns(), rowIterable);
    }

    @Override
    public void close() {
        instanceManager.close();
    }

    @Override
    public GraphDatabaseService getGraphDatabaseService() {
        return database;
    }

    private final class RowIterable implements Iterable<QueryResult.Row>, Closeable {

        private List<String> columns;
        private ResourceIterator<Map<String, Object>> iterator;

        private RowIterable(List<String> columns, ResourceIterator<Map<String, Object>> iterator) {
            this.columns = columns;
            this.iterator = iterator;
        }

        @Override
        public Iterator<QueryResult.Row> iterator() {

            return new Iterator<QueryResult.Row>() {

                @Override
                public boolean hasNext() {
                    return iterator.hasNext();
                }

                @Override
                public QueryResult.Row next() {
                    Map<String, Object> next = iterator.next();
                    Map<String, Object> row = new LinkedHashMap<>();
                    for (String column : columns) {
                        Object value = next.get(column);
                        Object decodedValue = decodeValue(value);
                        row.put(column, decodedValue);
                    }
                    return new QueryResult.Row(row);
                }

                @Override
                public void remove() {
                    iterator.remove();
                }

                private Object decodeValue(Object value) {
                    Object decodedValue;
                    if (value instanceof Node) {
                        Node node = (Node) value;
                        return instanceManager.getInstance(node);
                    } else if (value instanceof List<?>) {
                        List<?> listValue = (List<?>) value;
                        List<Object> decodedList = new ArrayList<>();
                        for (Object o : listValue) {
                            decodedList.add(decodeValue(o));
                        }
                        decodedValue = decodedList;
                    } else {
                        decodedValue = value;
                    }
                    return decodedValue;
                }
            };
        }

        @Override
        public void close() throws IOException {
            iterator.close();
        }
    }
}
