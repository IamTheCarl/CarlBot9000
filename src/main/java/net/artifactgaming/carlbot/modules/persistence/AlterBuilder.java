package net.artifactgaming.carlbot.modules.persistence;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class AlterBuilder implements SQLBuilder {

    private Table table;
    private String mode = "";

    private List<String> values = new ArrayList<>();

    AlterBuilder(Table table) {
        this.table = table;
    }

    public AlterBuilder add() {
        mode = "add";
        return this;
    }

    public AlterBuilder modify() {
        mode = "modify";
        return this;
    }

    public AlterBuilder drop() {
        mode = "drop column";
        return this;
    }

    public AlterBuilder rename() {
        mode = "rename column";
        return this;
    }

    public AlterBuilder pushValue(String value) {
        values.add(value);
        return this;
    }

    public String toString() {

        StringBuilder sql = new StringBuilder("alter table ").append(table.getName())
                .append(" ").append(mode).append(" ");

        boolean needBrackets = !mode.endsWith("column");

        if (needBrackets) {
            sql.append("(");
        }

        SQLBuilder.appendList(sql, values, "", ", ");

        if (needBrackets) {
            sql.append(")");
        }

        return sql.toString();
    }

    @Override
    public ResultSet execute() throws SQLException {
        Connection connection = table.server.getConnection();
        PreparedStatement statement = connection.prepareStatement(toString());

        table.logger.debug("Run sql: " + this.toString());
        statement.execute();

        // Result is non-applicable.
        return null;
    }
}
