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
import java.util.List;

public class Persistence implements Module {

    JdbcDataSource server;
    Logger logger = LoggerFactory.getLogger(Persistence.class);

    // A list of all tables is kept in this table.
    Table tableOfTables;

    // A list of all columns and their tables is kept in this table.
    Table tableOfColumns;

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

        tableOfTables = new Table(this, "INFORMATION_SCHEMA.TABLES");
        tableOfColumns = new Table(this, "INFORMATION_SCHEMA.COLUMNS");
        users = new Table(this, "CARLBOT_USERS");
        guilds = new Table(this, "CARLBOT_GUILDS");
    }

    @Override
    public void setup(CarlBot carlbot) {
        List<Module> modules = carlbot.getModules();

        //columns.add(new TableColumn("discord_id", TableColumn.Type.CharString));

        for (Module module : modules) {
            if (module instanceof PersistentModule) {
                String name = module.getClass().getCanonicalName();
                logger.info("Detected persistent module: " + name);
            }
        }

        try {
            if (!users.exists()) {
                users.create();
                users.alter().add().pushValue("discord_id varchar").execute();
            }

            if (!guilds.exists()) {
                guilds.create();
                guilds.alter().add().pushValue("discord_id varchar").execute();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public Table getGuildTable(String guildID, PersistentModule module) throws SQLException {
        String moduleName = module.getClass().getCanonicalName();

        Table table = new Table(this, "\"GUILD_" + guildID + ":" + moduleName.toUpperCase() + "\"");
        // You don't have to create the table for it to be a parent.

        // We're going to add this server to our list of known servers though, but only if it's not already there.
        ResultSet resultSet = guilds.select().where("discord_id", "=", guildID).execute();
        if (!resultSet.next()) {
            guilds.insert().set("discord_id", guildID).execute();
        }

        return table;
    }

    public Table getGuildTable(Guild guild, PersistentModule module) throws SQLException {
        return getGuildTable(guild.getId(), module);
    }

    @Override
    public Command[] getCommands(CarlBot carlbot) {
        return new Command[] {};
    }
}
