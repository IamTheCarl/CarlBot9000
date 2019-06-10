package net.artifactgaming.carlbot.modules.persistence;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * A rework of Krasnay's insert builder to be more friendly with how we manage our database.
 * https://github.com/jkrasnay/sqlbuilder
 */
public class InsertBuilder implements SQLBuilder {

    Table table;

    /**
     * Constructor.
     *
     * @param table The table we will be inserting into.
     */
    InsertBuilder(Table table) {
        this.table = table;
    }

    @Override
    public ResultSet execute() throws SQLException {
        Connection connection = table.server.getConnection();
        PreparedStatement statement = connection.prepareStatement(toString());

        table.logger.debug("Run sql: " + this.toString());

        int i = 1;
        for (String value : values) {
            statement.setString(i, value);
            i++;
        }

        statement.execute();

        // Result is non-applicable.
        return null;
    }

    private static final long serialVersionUID = 1;

    private List<String> columns = new ArrayList<String>();

    private List<String> values = new ArrayList<String>();

    /**
     * Inserts a column name, value pair into the SQL.
     *
     * @param column
     *            Name of the table column.
     * @param value
     *            Value to substitute in.
     */
    public InsertBuilder set(String column, String value) {
        columns.add(column);
        values.add(value);
        return this;
    }

    @Override
    public String toString() {
        StringBuilder sql = new StringBuilder("insert into ").append(table.getName()).append(" (");
        SQLBuilder.appendList(sql, columns, "", ", ");
        sql.append(") values (");

        for (int i = 0; i < values.size(); i++) {
            sql.append("?,");
        }

        // Pop off that extra comma.
        sql.deleteCharAt(sql.length() - 1);

        sql.append(")");
        return sql.toString();
    }
}
