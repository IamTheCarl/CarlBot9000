package net.artifactgaming.carlbot.modules.persistence;

import java.text.DecimalFormat;
import java.util.Objects;

public class TableColumn {

    public enum Type {
        Integer,
        SmallInt,
        Numeric,
        Real,
        DecimalFormatDoublePrecision,
        Float,
        Charecter,
        CharString,
        Bit,
        BitVarying,
        Date,
        Time,
        Timestamp,
        TimeWithTimeZone,
        TimestampWithTimeZone
    }

    private String name;
    private Type type;

    public String getName() {
        return name;
    }

    @Override
    public String toString() {
        return "TableColumn{" +
                "name='" + name + '\'' +
                ", type=" + type +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TableColumn that = (TableColumn) o;
        return name.equals(that.name) &&
                type == that.type;
    }

    @Override
    public int hashCode() {
        return Objects.hash(name);
    }

    public Type getType() {
        return type;
    }

    public void setName(String name) {
        this.name = Persistence.cleanSQL(name);
    }

    public TableColumn(String name, Type type) {
        setName(name);
        this.type = type;
    }

    String getTypeSQL() {
        String typeName;

        switch (type) {
            case Integer:
                typeName = "INTEGER";
                break;
            case SmallInt:
                typeName = "SMALLINT";
                break;
            case Numeric:
                typeName = "NUMERIC";
                break;
            case Real:
                typeName = "REAL";
                break;
            case DecimalFormatDoublePrecision:
                typeName = "DOUBLE PRECISION";
                break;
            case Float:
                typeName = "FLOAT";
                break;
            case Charecter:
                typeName = "CHAR";
                break;
            case CharString:
                typeName = "VARCHAR2";
                break;
            case Bit:
                typeName = "BIT";
                break;
            case BitVarying:
                typeName = "BIT VARYING";
                break;
            case Date:
                typeName = "DATE";
                break;
            case Time:
                typeName = "TIME";
                break;
            case Timestamp:
                typeName = "TIMESTAMP";
                break;
            case TimeWithTimeZone:
                typeName = "TIME WITH TIME ZONE";
                break;
            case TimestampWithTimeZone:
                typeName = "TIMESTAMP with TIME ZONE";
                break;
            default:
                typeName = "UNKNOWN";
                break;
        }

        return typeName;
    }

    String getSQL() {
        return "\"" + name + "\" " + getTypeSQL();
    }
}
