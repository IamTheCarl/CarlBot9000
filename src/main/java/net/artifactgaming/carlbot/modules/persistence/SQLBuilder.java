package net.artifactgaming.carlbot.modules.persistence;

import java.sql.ResultSet;
import java.sql.SQLException;

public interface SQLBuilder {
    /**
     * Execute the generated sql statement.
     * @return the results of the statement, or null if there are no applicable results.
     */
    ResultSet execute() throws SQLException;
}
