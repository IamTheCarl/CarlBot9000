package net.artifactgaming.carlbot.modules.persistence;

import ca.krasnay.sqlbuilder.InsertBuilder;
import ca.krasnay.sqlbuilder.SelectBuilder;
import net.artifactgaming.carlbot.Utils;
import org.h2.jdbcx.JdbcDataSource;
import org.slf4j.Logger;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Table {

    Persistence database;
    JdbcDataSource server;
    Logger logger;

    String tableName;
    private Map<String, TableColumn> columns = null;

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

        this.tableName = parent.tableName + "/" + tableName;
    }

    public void setColumns(List<TableColumn> columns) {
        this.columns = new HashMap<String, TableColumn>();

        for (TableColumn column : columns) {
            this.columns.put(column.getName(), column);
        }
    }

    public boolean checkTableExists() throws SQLException {
        Connection connection = server.getConnection();
        PreparedStatement statement = connection.prepareStatement(
                "SELECT * FROM information_schema.tables WHERE table_name='?'");

        statement.setString(1, tableName);
        ResultSet results = statement.executeQuery();

        boolean exists = results.next();
        connection.close();

        return exists;
    }

    public void createTable() throws SQLException {
        Connection connection = server.getConnection();

        PreparedStatement statement = connection.prepareStatement(
                "SELECT * FROM information_schema.tables WHERE table_name=?");

        statement.setString(1, tableName);
        ResultSet results = statement.executeQuery();

        String cleanTableName = Persistence.cleanSQL(tableName);

        // Table does not exist. We must create.
        if (!results.next()) {

            String columnsString = "";

            if (columns != null) {
                for (TableColumn column : columns.values()) {
                    columnsString += column.getSQL() + ", ";
                }

                columnsString = columnsString.substring(0, columnsString.length() - 2);
            }

            statement = connection.prepareStatement("CREATE TABLE \"" + cleanTableName + "\" (" + columnsString + ")");
            statement.execute();
            connection.commit();
            logger.debug("Created table: " + cleanTableName);
        } else {
            logger.debug("Did not create table because it already exists: " + cleanTableName);
            addColumns();
        }

        connection.close();
    }

    public boolean checkColumnExists(String name) throws SQLException {
        Connection connection = server.getConnection();
        PreparedStatement statement = connection.prepareStatement(
                "SELECT * FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_NAME=? and COLUMN_NAME=?");

        statement.setString(1, tableName);
        statement.setString(2, name);
        ResultSet results = statement.executeQuery();

        boolean exists = results.next();
        connection.close();

        return exists;
    }

    public void addColumns() throws SQLException {
        Connection connection = server.getConnection();

        for (TableColumn column : columns.values()) {
            // Column does not exist. We need to add it.
            if (!checkColumnExists(column.getName())) {
                logger.debug("Added column " + column.getName() + " to table: " + tableName);
                PreparedStatement statement = connection.prepareStatement(
                        "ALTER TABLE \"" + tableName + "\" ADD " + column.getSQL());
                statement.execute();
            }
        }

        connection.close();
    }

    public void addRow(List<RowColumn> values) throws SQLException {

        logger.debug("ADD ROW");

        Connection connection = server.getConnection();

        String columnNames = "";
        String valuesString = "";

        for (RowColumn column : values) {

            Object value = column.value;
            if (!this.columns.containsKey(column.columnName)) {
                throw new SQLException("Table does not contain column.");
            }

            columnNames += "\"" + column.columnName + "\", ";

            if (value instanceof Boolean) {
                valuesString += ((boolean)value) ? "1":"0";
                valuesString += ", ";
                continue;
            }

            if (value instanceof Number) {
                valuesString += value.toString();
                valuesString += ", ";
                continue;
            }

            if (value instanceof String) {
                valuesString += value;
                valuesString += ", ";
                continue;
            }

            // FIXME needs to throw an error.
        }

        columnNames = columnNames.substring(0, columnNames.length() - 2);
        valuesString = valuesString.substring(0, valuesString.length() - 2);

        PreparedStatement statement = connection.prepareStatement(
                "INSERT INTO \"" + tableName + "\" (" + columnNames + ") VALUES (" + valuesString + ")");

        statement.execute();
        connection.commit();
        connection.close();
    }

    public ResultSet select(SelectBuilder selector) throws SQLException {
        Connection connection = server.getConnection();
        PreparedStatement statement = connection.prepareStatement(selector.clone()
                                                                 .from('"' + tableName + '"').toString());

        ResultSet results = statement.executeQuery();

        return results;
    }

    public void insert(InsertBuilder insert) throws SQLException {
        Connection connection = server.getConnection();
        PreparedStatement statement = connection.prepareStatement(insert.toString());

        logger.debug("Quiry: " + insert.toString());

        statement.execute();
        connection.close();
    }

    public String getSQL() {
        return '"' + tableName + '"';
    }
}
