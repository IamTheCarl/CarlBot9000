package net.artifactgaming.carlbot.modules.persistence;

import java.io.Serializable;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * A rework of Krasnay's select builder to be more friendly with how we manage our database.
 * https://github.com/jkrasnay/sqlbuilder
 */
public class SelectBuilder implements SQLBuilder, Cloneable, Serializable {

    private Table table;

    SelectBuilder(Table table) {
        this.table = table;
        this.from(table.getName());
    }

    @Override
    public ResultSet execute() throws SQLException {
        Connection connection = table.server.getConnection();
        PreparedStatement statement = connection.prepareStatement(this.toString());

        table.logger.debug("Run sql: " + this.toString());

        int i = 1;
        for (String column : columns) {
            statement.setString(i, column);
            i++;
        }

        for (String value : selectValues) {
            statement.setString(i, value);
            i++;
        }

        return statement.executeQuery();
    }

    private static final long serialVersionUID = 1;

    private boolean distinct;

    private List<String> columns = new ArrayList<>();

    private List<String> tables = new ArrayList<>();

    private List<String> joins = new ArrayList<>();

    private List<String> leftJoins = new ArrayList<>();

    private List<String> wheres = new ArrayList<>();

    private List<String> groupBys = new ArrayList<>();

    private List<String> havings = new ArrayList<>();

    private List<SelectBuilder> unions = new ArrayList<>();

    private List<String> orderBys = new ArrayList<>();

    private List<String> selectValues = new ArrayList<>();

    private int limit = 0;

    private int offset = 0;

    private boolean forUpdate;

    private boolean noWait;

    /**
     * Copy constructor. Used by {@link #clone()}.
     *
     * @param other
     *            SelectBuilder being cloned.
     */
    protected SelectBuilder(SelectBuilder other) {

        this.distinct = other.distinct;
        this.forUpdate = other.forUpdate;
        this.noWait = other.noWait;

        for (String column : other.columns) {
            this.columns.add(column);
        }

        this.tables.addAll(other.tables);
        this.joins.addAll(other.joins);
        this.leftJoins.addAll(other.leftJoins);
        this.wheres.addAll(other.wheres);
        this.groupBys.addAll(other.groupBys);
        this.havings.addAll(other.havings);

        for (SelectBuilder sb : other.unions) {
            this.unions.add(sb.clone());
        }

        this.orderBys.addAll(other.orderBys);
    }

    /**
     * Alias for {@link #where(String,String,String)}.
     */
    public SelectBuilder and(String expr, String op, String value) {
        return where(expr, op, value);
    }

    public SelectBuilder column(String name) {
        columns.add(name);
        return this;
    }

    public SelectBuilder column(String name, boolean groupBy) {
        columns.add(name);
        if (groupBy) {
            groupBys.add(name);
        }
        return this;
    }

    public SelectBuilder limit(int limit, int offset) {
        this.limit = limit;
        this.offset = offset;
        return this;
    }

    public SelectBuilder limit(int limit) {
        return limit(limit, 0);
    }

    @Override
    public SelectBuilder clone() {
        return new SelectBuilder(this);
    }

    public SelectBuilder distinct() {
        this.distinct = true;
        return this;
    }

    public SelectBuilder forUpdate() {
        forUpdate = true;
        return this;
    }

    public SelectBuilder from(String table) {
        tables.add(table);
        return this;
    }

    public List<SelectBuilder> getUnions() {
        return unions;
    }

    public SelectBuilder groupBy(String expr) {
        groupBys.add(expr);
        return this;
    }

    public SelectBuilder having(String expr) {
        havings.add(expr);
        return this;
    }

    public SelectBuilder join(String join) {
        joins.add(join);
        return this;
    }

    public SelectBuilder leftJoin(String join) {
        leftJoins.add(join);
        return this;
    }

    public SelectBuilder noWait() {
        if (!forUpdate) {
            throw new RuntimeException("noWait without forUpdate cannot be called");
        }
        noWait = true;
        return this;
    }

    public SelectBuilder orderBy(String name) {
        orderBys.add(name);
        return this;
    }

    /**
     * Adds an ORDER BY item with a direction indicator.
     *
     * @param name
     *            Name of the column by which to sort.
     * @param ascending
     *            If true, specifies the direction "asc", otherwise, specifies
     *            the direction "desc".
     */
    public SelectBuilder orderBy(String name, boolean ascending) {
        if (ascending) {
            orderBys.add(name + " asc");
        } else {
            orderBys.add(name + " desc");
        }
        return this;
    }

    @Override
    public String toString() {

        StringBuilder sql = new StringBuilder("select ");

        if (distinct) {
            sql.append("distinct ");
        }

        if (columns.size() == 0) {
            sql.append("*");
        } else {
            for (int i = 0; i < columns.size(); i++) {
                if (i != 0) {
                    sql.append(", ");
                }
                sql.append("?");
            }
        }

        SQLBuilder.appendList(sql, tables, " from ", ", ");
        SQLBuilder.appendList(sql, joins, " join ", " join ");
        SQLBuilder.appendList(sql, leftJoins, " left join ", " left join ");
        SQLBuilder.appendList(sql, wheres, " where ", " and ");
        SQLBuilder.appendList(sql, groupBys, " group by ", ", ");
        SQLBuilder.appendList(sql, havings, " having ", " and ");
        SQLBuilder.appendList(sql, unions, " union ", " union ");
        SQLBuilder.appendList(sql, orderBys, " order by ", ", ");

        if (forUpdate) {
            sql.append(" for update");
            if (noWait) {
                sql.append(" nowait");
            }
        }

        if(limit > 0) {
            sql.append(" limit ");
            sql.append(limit);
        }

        if(offset > 0) {
            sql.append(", ");
            sql.append(offset);
        }

        return sql.toString();
    }

    /**
     * Adds a "union" select builder. The generated SQL will union this query
     * with the result of the main query. The provided builder must have the
     * same columns as the parent select builder and must not use "order by" or
     * "for update".
     */
    public SelectBuilder union(SelectBuilder unionBuilder) {
        unions.add(unionBuilder);
        return this;
    }

    public SelectBuilder where(String expr, String op, String value) {
        wheres.add(expr + op + "?");
        selectValues.add(value);
        return this;
    }
}
