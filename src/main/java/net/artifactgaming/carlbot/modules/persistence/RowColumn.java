package net.artifactgaming.carlbot.modules.persistence;

public class RowColumn {
    String columnName;
    Object value;

    public RowColumn(String columnName, Object value) {
        this.columnName = columnName;
        this.value = value;
    }

    public String getSQLValue() {
        if (value instanceof String) {
            return "\"" + value.toString() + "\"";
        } else {
            return value.toString();
        }
    }
}
