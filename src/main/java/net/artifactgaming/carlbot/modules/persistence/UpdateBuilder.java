package net.artifactgaming.carlbot.modules.persistence;

import java.io.Serializable;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class UpdateBuilder implements SQLBuilder, Serializable {

    private Table table;

    UpdateBuilder(Table table) {
        this.table = table;
    }

    @Override
    public ResultSet execute() throws SQLException {
        Connection connection = table.server.getConnection();
        PreparedStatement statement = connection.prepareStatement(toString());

        table.logger.debug("Run sql: " + this.toString());

        int i = 1;
        for (String value : setValues) {
            statement.setString(i, value);
            i++;
        }

        for (String value : whereValues) {
            statement.setString(i, value);
            i++;
        }

        statement.execute();

        // Result is non-applicable.
        return null;
    }

    private static final long serialVersionUID = 1;

    private List<String> sets = new ArrayList<>();

    private List<String> wheres = new ArrayList<>();

    private List<String> setValues = new ArrayList<>();

    private List<String> whereValues = new ArrayList<>();

    public UpdateBuilder set(String expr, String value) {
        sets.add(expr + "=?");
        setValues.add(value);
        return this;
    }

    @Override
    public String toString() {
        StringBuilder sql = new StringBuilder("update ").append(table.getName());
        SQLBuilder.appendList(sql, sets, " set ", ", ");
        SQLBuilder.appendList(sql, wheres, " where ", " and ");
        return sql.toString();
    }

    public UpdateBuilder where(String expr, String cond, String value) {
        wheres.add(expr + " " + cond + " ?");
        whereValues.add(value);
        return this;
    }

}
