package net.artifactgaming.carlbot.modules.persistence;

import net.artifactgaming.carlbot.CarlBot;
import net.artifactgaming.carlbot.Command;
import net.artifactgaming.carlbot.Module;

import net.dv8tion.jda.core.entities.Guild;
import org.h2.jdbcx.JdbcDataSource;
import org.h2.tools.Server;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class Persistence implements Module {

    JdbcDataSource server;
    Logger logger = LoggerFactory.getLogger(Persistence.class);

    private Table users;
    private Table guilds;

    public Persistence() throws SQLException {
        server = new JdbcDataSource();
        server.setURL("jdbc:h2:./database");

        // If debug is enabled, fire up the debug webpage and host it locally.
        if (logger.isDebugEnabled()) {
            Server.createWebServer("-web", "-webAllowOthers", "-webPort", "8082").start();
            logger.warn("Enabled database interface webserver.");
        }
        users = new Table(this, "carlbot_users");
        guilds = new Table(this, "carlbot_guilds");
    }

    @Override
    public void setup(CarlBot carlbot) {
        List<Module> modules = carlbot.getModules();

        ArrayList<TableColumn> columns = new ArrayList<>();

        columns.add(new TableColumn("discord_id", TableColumn.Type.CharString));

        for (Module module : modules) {
            if (module instanceof PersistentModule) {
                String name = module.getClass().getCanonicalName();
                logger.info("Detected persistent module: " + name);
            }
        }

        try {
            users.setColumns(columns);
            users.createTable();

            guilds.setColumns(columns);
            guilds.createTable();

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public Table getGuildTable(String guildID, PersistentModule module) throws SQLException {
        String moduleName = module.getClass().getCanonicalName();

        Table table = new Table(this, "GUILD_" + guildID + ":" + moduleName);
        // You don't have to create the table for it to be a parent.

        // We're going to add this server to our list of known servers though.

        List<RowColumn> row = new ArrayList<>();
        row.add(new RowColumn("discord_id", guildID));

        guilds.addRow(row);

        return table;
    }

    public Table getGuildTable(Guild guild, PersistentModule module) throws SQLException {
        return getGuildTable(guild.getId(), module);
    }

    static String cleanSQL(String input) {
        return input.replaceAll("\\^[a-zA-Z_\\-]+$", "");
    }

    @Override
    public Command[] getCommands(CarlBot carlbot) {
        return new Command[] {};
    }
}
