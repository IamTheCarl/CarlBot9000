package net.artifactgaming.carlbot.modules.authority;

import net.artifactgaming.carlbot.*;
import net.artifactgaming.carlbot.modules.persistence.*;
import net.dv8tion.jda.core.entities.Guild;
import net.dv8tion.jda.core.entities.Member;
import net.dv8tion.jda.core.entities.Role;
import net.dv8tion.jda.core.events.message.MessageReceivedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

public class AuthorityManagement implements AuthorityRequiring, Module, PersistentModule {

    private Logger logger = LoggerFactory.getLogger(AuthorityManagement.class);

    private HashMap<String, Authority> authorities = new HashMap<>();
    private HashMap<String, Authority> authorities_absolute = new HashMap<>();

    private Persistence persistence;
    private AuthorityManagement manager = this;

    @Override
    public Authority[] getRequiredAuthority() {
        return new Authority[] { new AuthorityToManipulate() };
    }

    class ListAuthorityCommand implements Command {

        @Override
        public String getCallsign() {
            return "list";
        }

        @Override
        public void runCommand(MessageReceivedEvent event, String rawString, List<String> tokens) throws Exception {

            if (tokens.isEmpty()) {
                String message = "Authorities you can assign to users and roles:\n```";

                for (Map.Entry<String, Authority> entry : authorities.entrySet()) {
                    message += entry.getKey() + " [" + entry.getValue().getClass().getCanonicalName() + "]\n";
                }

                message += "```";

                event.getChannel().sendMessage(message).queue();
            } else {
                String discordId = Utils.getMemberOrRoleFromMessage(event, tokens.get(0));

                if (discordId != null) {
                    event.getChannel().sendMessage("Discord ID: " + discordId).queue();
                } else {
                    event.getChannel().sendMessage("Could not find member or role.").queue();
                }
            }
        }
    }

    private Table getAuthorityTable(Guild guild) throws SQLException {
        Table table = persistence.getGuildTable(guild, manager);
        Table authorityTable = new Table(table, "authority_bindings");

        if (!authorityTable.exists()) {
            authorityTable.create(); // Make sure it exists.

            authorityTable.alter().add().pushValue("discord_id varchar").execute();
        }

        return authorityTable;
    }

    class AddAuthorityCommand implements Command, AuthorityRequiring {

        @Override
        public String getCallsign() {
            return "set";
        }

        @Override
        public void runCommand(MessageReceivedEvent event, String rawString, List<String> tokens) throws Exception {
            if (tokens.size() == 3) {
                String discordId = Utils.getMemberOrRoleFromMessage(event, tokens.get(0));

                if (discordId != null) {
                    Authority authority = getAuthorityByName(tokens.get(1));

                    if (authority == null) {
                        event.getChannel().sendMessage("Could not find requested authority.").queue();
                        return;
                    }

                    String value = tokens.get(2);

                    switch (tokens.get(2)) {
                        // No problem, these are all valid authority modes.
                        case "give":
                            value = "true";
                            break;
                        case "deny":
                            value = "false";
                            break;
                        case "ignore":
                            value = "null";
                            break;
                        default:
                            // Problem, what is this thing?
                            event.getChannel()
                                    .sendMessage("Authority status must be give, deny, or ignore.").queue();
                            return;
                    }

                    Table table = getAuthorityTable(event.getGuild());
                    String authorityName = authority.getClass().getCanonicalName();

                    // Make sure the column exists.
                    if (!table.columnExists(authorityName)) {
                        table.alter().add().pushValue("\"" + authorityName + "\" varchar").execute();
                    }

                    // Check if our row exists. If not, make it exist.
                    ResultSet resultSet = table.select().column("*").where("discord_id=" + discordId).execute();
                    if (!resultSet.next()) {
                        table.insert().set("discord_id", discordId).execute();
                    }

                    table.update().set("\"" + authorityName + "\"=" + value).where("discord_id=" + discordId).execute();
                    event.getChannel().sendMessage("Authority status set.").queue();

                } else {
                    event.getChannel().sendMessage("Could not find member or role.").queue();
                }
            } else {
                event.getChannel().sendMessage("Wrong number of arguments given.").queue();
            }
        }

        @Override
        public Authority[] getRequiredAuthority() {
            return new Authority[] { new AuthorityToManipulate() };
        }
    }

    class ManipulateAuthorityCommand implements AuthorityRequiring, Command {

        private CommandHandler commands;

        ManipulateAuthorityCommand(CarlBot carlbot) {
            commands = new CommandHandler(carlbot);

            commands.setSubName("Authority");
            commands.addCommand(new ListAuthorityCommand());
            commands.addCommand(new AddAuthorityCommand());
        }

        @Override
        public Authority[] getRequiredAuthority() {
            return new Authority[] { new AuthorityToManipulate() };
        }

        @Override
        public String getCallsign() {
            return "authority";
        }

        @Override
        public void runCommand(MessageReceivedEvent event, String rawString, List<String> tokens) throws Exception {
            commands.runCommand(event, rawString, tokens);
        }
    }

    private boolean checkAuthorityRaw(String id, Guild guild, Authority authority) throws SQLException {
        boolean hasAuthority = false;
        String authorityName = authority.getClass().getCanonicalName();

        Table table = getAuthorityTable(guild);

        SelectBuilder builder = table.select();
        builder.column("*").where("\"discord_id\"='" + id +"'");
        ResultSet resultSet = builder.execute();

        while (resultSet.next()) {
            boolean permission = resultSet.getBoolean(authorityName);
            if (permission) {
                hasAuthority = true;
                break;
            }
        }

        resultSet.close();

        return hasAuthority;
    }

    /**
     * Determines if a user has an authority. Note that role authority is included here.
     * @param member The member we are checking the authority of.
     * @param authority The authority that the user or role needs to have.
     * @param ignoreOwner if false, this will always return true for members who are the owner of the guild.
     * @return true if they have the authority, directly or by a role; false if otherwise.
     */
    public boolean checkHasAuthority(Member member, Authority authority, boolean ignoreOwner) throws SQLException {
        // Do we even need to check?
        if (ignoreOwner || !member.isOwner()) {
            // First check if the member has directly been given authority.
            return checkAuthorityRaw(member.getUser().getId(), member.getGuild(), authority);
        }

        return true;
    }

    /**
     * Determines if a user has an authority. Note that role authority is included here.
     * @param member The member we are checking the authority of.
     * @param authority The authority that the user or role needs to have.
     * @return true if they have the authority, directly or by a role; false if otherwise.
     */
    public boolean checkHasAuthority(Member member, Authority authority) throws SQLException {
        return checkHasAuthority(member, authority, false);
    }

    /**
     * Determines if a role has an authority.
     * @param role The role we are checking the authority of.
     * @param authority The authority that the user or role needs to have.
     * @return true if they have the authority, directly or by a role; false if otherwise.
     */
    public boolean checkHasAuthority(Role role, Authority authority) throws SQLException {
        return checkAuthorityRaw(role.getId(), role.getGuild(), authority);
    }

    public Authority getAuthorityByName(String name) {
        Authority authority = authorities.get(name);
        if (authority == null) {
            authority = authorities_absolute.get(name);
        }

        return authority;
    }

    @Override
    public void setup(CarlBot carlbot) {

        // Get the persistence module.
        persistence = (Persistence) carlbot.getModule(Persistence.class);

        if (persistence == null) {
            logger.error("Persistence module is not loaded.");
            carlbot.crash();
        }

        List<Module> modules = carlbot.getModules();

        for (Module module : modules) {
            if (module instanceof AuthorityRequiring) {
                AuthorityRequiring authorityModule = (AuthorityRequiring)module;
                Authority[] authorities = authorityModule.getRequiredAuthority();

                for (Authority authority : authorities) {
                    this.authorities.put(authority.getName(), authority);
                    this.authorities_absolute.put(authority.getClass().getCanonicalName(), authority);

                    logger.info("Registered authority: " + authority.getClass().getCanonicalName());
                }
            }
        }

        carlbot.addCommandPermissionChecker((MessageReceivedEvent event, Command command)->{
            try {
                // Does this command actually require authority?
                if (command instanceof AuthorityRequiring) {
                    // Check for all authorities.
                    for (Authority authority : ((AuthorityRequiring) command).getRequiredAuthority()) {

                        // Don't have it? Fail.
                        if (!checkHasAuthority(event.getMember(), authority)) {

                            String message = "You lack the autority needed to use this command.\n"
                                    + "Authority required:\n```\n";

                            for (Authority authorityToList : ((AuthorityRequiring) command).getRequiredAuthority()) {
                                message += authorityToList.getName()
                                        + " [" + authorityToList.getClass().getCanonicalName() + "]\n";
                            }

                            message += "```";

                            event.getChannel().sendMessage(message).queue();
                            return false;
                        }
                    }
                }
            } catch (SQLException e) {
                logger.error("Error while checking authority.", e);
                event.getChannel().sendMessage("A database error happened while checking your authority.\n"
                        + "The error has been logged. Please try again later.").queue();

                return false;
            }

            // We passed all checks.
            return true;
        });
    }

    @Override
    public Command[] getCommands(CarlBot carlbot) {
        return new Command[] {new ManipulateAuthorityCommand(carlbot)};
    }
}
