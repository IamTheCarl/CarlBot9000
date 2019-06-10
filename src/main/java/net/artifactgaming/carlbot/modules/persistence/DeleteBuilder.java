package net.artifactgaming.carlbot.modules.persistence;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class DeleteBuilder extends ca.krasnay.sqlbuilder.DeleteBuilder implements SQLBuilder {

    Table table;

    public DeleteBuilder(Table table) {
        super(table.tableName);
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
    public DeleteBuilder where(String expr) {
        return (DeleteBuilder) super.where(expr);
    }
}
