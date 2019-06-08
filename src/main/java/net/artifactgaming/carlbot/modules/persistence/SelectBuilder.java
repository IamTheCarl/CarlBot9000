package net.artifactgaming.carlbot.modules.persistence;

import ca.krasnay.sqlbuilder.SubSelectBuilder;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Wraps Krasnay's select builder to be more friendly with how we manage our database.
 */
public class SelectBuilder extends ca.krasnay.sqlbuilder.SelectBuilder implements SQLBuilder {

    Table table;

    SelectBuilder(Table table) {
        this.table = table;
        this.from(table.getName());
    }

    @Override
    public ResultSet execute() throws SQLException {
        Connection connection = table.server.getConnection();
        PreparedStatement statement = connection.prepareStatement(this.toString());

        table.logger.debug("Run sql: " + this.toString());
        ResultSet results = statement.executeQuery();

        return results;
    }

    @Override
    public SelectBuilder and(String expr) {
        return (SelectBuilder) super.and(expr);
    }

    @Override
    public SelectBuilder column(String name) {
        return (SelectBuilder) super.column(name);
    }

    @Override
    public SelectBuilder column(SubSelectBuilder subSelect) {
        return (SelectBuilder) super.column(subSelect);
    }

    @Override
    public SelectBuilder column(String name, boolean groupBy) {
        return (SelectBuilder) super.column(name, groupBy);
    }

    @Override
    public SelectBuilder clone() {
        return (SelectBuilder) super.clone();
    }

    @Override
    public SelectBuilder distinct() {
        return (SelectBuilder) super.distinct();
    }

    @Override
    public SelectBuilder forUpdate() {
        return (SelectBuilder) super.forUpdate();
    }

    @Override
    public SelectBuilder from(String table) {
        return (SelectBuilder) super.from(table);
    }

    @Override
    public SelectBuilder groupBy(String expr) {
        return (SelectBuilder) super.groupBy(expr);
    }

    @Override
    public SelectBuilder having(String expr) {
        return (SelectBuilder) super.having(expr);
    }

    @Override
    public SelectBuilder join(String join) {
        return (SelectBuilder) super.join(join);
    }

    @Override
    public SelectBuilder leftJoin(String join) {
        return (SelectBuilder) super.leftJoin(join);
    }

    @Override
    public SelectBuilder noWait() {
        return (SelectBuilder) super.noWait();
    }

    @Override
    public SelectBuilder orderBy(String name) {
        return (SelectBuilder) super.orderBy(name);
    }

    @Override
    public SelectBuilder orderBy(String name, boolean ascending) {
        return (SelectBuilder) super.orderBy(name, ascending);
    }

    @Override
    public SelectBuilder union(ca.krasnay.sqlbuilder.SelectBuilder unionBuilder) {
        return (SelectBuilder) super.union(unionBuilder);
    }

    @Override
    public SelectBuilder where(String expr) {
        return (SelectBuilder) super.where(expr);
    }
}
