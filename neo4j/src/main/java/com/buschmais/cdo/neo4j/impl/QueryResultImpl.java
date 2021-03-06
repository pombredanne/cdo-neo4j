package com.buschmais.cdo.neo4j.impl;

import com.buschmais.cdo.api.QueryResult;
import org.apache.commons.io.IOUtils;

import java.io.Closeable;
import java.util.List;
import java.util.Map;

/**
 * Represents the result of a query.
 */
public class QueryResultImpl implements Closeable, QueryResult {

    /**
     * The column names returned by the query.
     */
    private final List<String> columns;
    /**
     * The Iterable which can be used to scroll through the rows returned by the
     * query.
     * <p>
     * Where applicable the values of a row are transformed to instances of the
     * corresponding classes.
     * </p>
     */
    private final Iterable<Row> rows;

    /**
     * Constructor.
     *
     * @param columns A list containing the names of the returned columns.
     * @param rows    The rows.
     */
    public QueryResultImpl(List<String> columns, Iterable<Row> rows) {
        this.columns = columns;
        this.rows = rows;
    }

    /**
     * Return the column names.
     *
     * @return The column names.
     */
    @Override
    public List<String> getColumns() {
        return columns;
    }

    /**
     * Return the {@link Iterable} to be used to scroll through the rows.
     *
     * @return The {@link Iterable} to be used to scroll through the rows.
     */
    @Override
    public Iterable<Row> getRows() {
        return rows;
    }

    @Override
    public void close() {
        if (this.rows instanceof Closeable) {
            IOUtils.closeQuietly((Closeable) rows);
        }
    }

}
