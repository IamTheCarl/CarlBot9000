package net.artifactgaming.carlbot.modules.persistence;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class InsertBuilder extends ca.krasnay.sqlbuilder.InsertBuilder implements SQLBuilder {

    Table table;

    /**
     * Constructor.
     *
     * @param table The table we will be inserting into.
     */
    public InsertBuilder(Table table) {
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
    public InsertBuilder set(String column, String value) {
        return (InsertBuilder) super.set(column, value);
    }
}
