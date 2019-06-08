package net.artifactgaming.carlbot.modules.persistence;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class UpdateBuilder extends ca.krasnay.sqlbuilder.UpdateBuilder implements SQLBuilder {

    Table table;

    UpdateBuilder(Table table) {
        super(table.getName());
        this.table = table;
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

    @Override
    public UpdateBuilder set(String expr) {
        return (UpdateBuilder) super.set(expr);
    }

    @Override
    public UpdateBuilder where(String expr) {
        return (UpdateBuilder) super.where(expr);
    }
}
