package net.artifactgaming.carlbot.modules.persistence;

import java.io.Serializable;
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
public class DeleteBuilder implements SQLBuilder, Serializable {

    private Table table;

    public DeleteBuilder(Table table) {
        this.table = table;
    }

    @Override
    public ResultSet execute() throws SQLException {
        Connection connection = table.server.getConnection();
        PreparedStatement statement = connection.prepareStatement(toString());

        table.logger.debug("Run sql: " + this.toString());

        int i = 1;
        for (String value : whereValues) {
            statement.setString(i, value);
            i++;
        }

        statement.execute();

        // Result is non-applicable.
        return null;
    }

    private static final long serialVersionUID = 1;

    private List<String> wheres = new ArrayList<>();

    private List<String> whereValues = new ArrayList<>();

    @Override
    public String toString() {
        StringBuilder sql = new StringBuilder("delete from ").append(table.getName());
        SQLBuilder.appendList(sql, wheres, " where ", " and ");
        return sql.toString();
    }

    public DeleteBuilder where(String expr, String cond, String value) {
        wheres.add(expr + " " + cond + "?");
        whereValues.add(value);
        return this;
    }
}
