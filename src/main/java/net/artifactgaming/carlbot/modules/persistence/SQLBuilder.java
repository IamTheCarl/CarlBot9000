package net.artifactgaming.carlbot.modules.persistence;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

public interface SQLBuilder {
    /**
     * Execute the generated sql statement.
     * @return the results of the statement, or null if there are no applicable results.
     */
    ResultSet execute() throws SQLException;

    /**
     * Constructs a list of items with given separators.
     *
     * @param sql
     *            StringBuilder to which the constructed string will be
     *            appended.
     * @param list
     *            List of objects (usually strings) to join.
     * @param init
     *            String to be added to the start of the list, before any of the
     *            items.
     * @param sep
     *            Separator string to be added between items in the list.
     */
    static void appendList(StringBuilder sql, List<?> list, String init, String sep) {

        boolean first = true;

        for (Object s : list) {
            if (first) {
                sql.append(init);
            } else {
                sql.append(sep);
            }
            sql.append(s);
            first = false;
        }
    }
}
