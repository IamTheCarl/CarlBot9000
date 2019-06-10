package net.artifactgaming.carlbot.modules.persistence;

import org.h2.jdbcx.JdbcDataSource;
import org.slf4j.Logger;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class Table {

    Persistence database;

    JdbcDataSource server;
    Logger logger;

    String tableName;

    Table(Persistence database, String tableName) {
        this.database = database;
        this.server = database.server;
        this.logger = database.logger;

        this.tableName = tableName;
    }

    public Table(Table parent, String tableName) {
        this.database = parent.database;
        this.server = parent.server;
        this.logger = parent.logger;

        boolean useQuotes = parent.tableName.endsWith("\"");

        if (useQuotes) {
            this.tableName = parent.tableName.substring(0, parent.tableName.length() - 2)
                    + "/" + tableName + "\"";
        } else {
            this.tableName = parent.tableName + "/" + tableName;
        }

        this.tableName = this.tableName.toUpperCase();
    }

    String getNameSQLForm() {
        // Do we need to shave off the quotes?
        if (tableName.endsWith("\"")) {
            return tableName.substring(1, tableName.length() - 1);
        } else {
            return tableName;
        }
    }

    public boolean exists() throws SQLException {

        ResultSet results = database.tableOfTables.select()
                .column("*").where("table_name", "=", getNameSQLForm()).execute();

        boolean exists = results.next();
        results.close();

        return exists;
    }

    public boolean columnExists(String columnName) throws SQLException {

        ResultSet results = database.tableOfColumns.select().column("*")
                .where("TABLE_NAME", "=",getNameSQLForm())
                .where("COLUMN_NAME", "=",columnName).execute();

        boolean exists = results.next();
        results.close();

        return exists;
    }

    public void create() throws SQLException {
        Connection connection = server.getConnection();

        PreparedStatement statement = connection.prepareStatement("create table " + tableName);
        statement.execute();
        connection.commit();
        logger.debug("Created table: " + tableName);
    }

    public SelectBuilder select() { return new SelectBuilder(this); }

    public InsertBuilder insert() { return new InsertBuilder(this); }

    public AlterBuilder alter() { return new AlterBuilder(this); }

    public UpdateBuilder update() {
        return new UpdateBuilder(this);
    }

    public DeleteBuilder delete() { return new DeleteBuilder(this); }

    public String getName() {
        return tableName;
    }
}
