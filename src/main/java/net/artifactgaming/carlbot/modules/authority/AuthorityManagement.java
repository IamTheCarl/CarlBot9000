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
import java.sql.ResultSetMetaData;
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

            // Are they asking to list te authority of a user/role or do they just want to know what authorities
            // there are in general?
            if (tokens.isEmpty()) {
                // Just liste authority in general.
                String message = "Authorities you can assign to users and roles:\n```";

                for (Map.Entry<String, Authority> entry : authorities.entrySet()) {
                    message += entry.getKey() + " [" + entry.getValue().getClass().getCanonicalName() + "]\n";
                }

                message += "```";

                event.getChannel().sendMessage(message).queue();
            } else {
                // We are listing the authority of a user/role.
                String discordId = Utils.getMemberOrRoleFromMessage(event, tokens.get(0));

                // Okay so we found our target.
                if (discordId != null) {
                    Table table = getAuthorityTable(event.getGuild());

                    List<Authority> giveAuthority = new ArrayList<>();
                    List<Authority> denyAuthority = new ArrayList<>();

                    // Role or member, we need to get the immediate authority.
                    listAuthorities(discordId, giveAuthority, denyAuthority, table);

                    Member member = event.getGuild().getMemberById(discordId);
                    if (member != null) {
                        // So it's a member. We need to get all the additional authority added by the roles.
                        for (Role role : member.getRoles()) {
                            listAuthorities(role.getId(), giveAuthority, denyAuthority, table);
                        }

                        // Don't forget the everyone role.
                        listAuthorities(member.getGuild().getPublicRole().getId(), giveAuthority, denyAuthority, table);
                    }

                    String message = "Authority given:\n```\n";

                    if (giveAuthority.isEmpty()) {
                        message += "None\n";
                    } else {
                        for (Authority authority : giveAuthority) {
                            message += authority.getName() + " [" + authority.getClass().getCanonicalName() + "]\n";
                        }
                    }

                    message += "```\nAuthority denied:\n```\n";

                    if (denyAuthority.isEmpty()) {
                        message += "None\n";
                    } else {
                        for (Authority authority : denyAuthority) {
                            message += authority.getName() + " [" + authority.getClass().getCanonicalName() + "]\n";
                        }
                    }

                    message += "```";

                    event.getChannel().sendMessage(message).queue();

                } else {
                    event.getChannel().sendMessage("Could not find member or role.").queue();
                }
            }
        }
    }

    private void listAuthorities(String discordId, List<Authority> giveAuthority, List<Authority> denyAuthority, Table table)
            throws SQLException {
        // Start by getting the immediate authority of the object.
        ResultSet resultSet = table.select().column("*").where("discord_id", "=", discordId).execute();
        ResultSetMetaData rsmd = resultSet.getMetaData();

        while (resultSet.next()) {
            for (int i = 1; i <= rsmd.getColumnCount(); i++) {
                String column = rsmd.getColumnName(i);

                // Don't count the discord_id.
                if (column.equals("DISCORD_ID")) {
                    continue;
                }

                Boolean setting = resultSet.getBoolean(column);
                if (resultSet.wasNull()) {
                    setting = null;
                }

                if (setting != null) {
                    Authority authority = getAuthorityByName(column);

                    // Add authority to one of the lists.
                    if (setting) {
                        // Don't add duplicates.
                        if (!giveAuthority.contains(authority) && !denyAuthority.contains(authority)) {
                            giveAuthority.add(authority);
                        }
                    } else {
                        // Don't add duplicates.
                        if (!giveAuthority.contains(authority) && !denyAuthority.contains(authority)) {
                            denyAuthority.add(authority);
                        }
                    }
                }
            }
        }
    }

    class SetAuthorityCommand implements Command, AuthorityRequiring {

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
                            value = "1";
                            break;
                        case "deny":
                            value = "0";
                            break;
                        case "ignore":
                            value = null;
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
                    ResultSet resultSet = table.select().column("*").where("discord_id", "=", discordId)
                            .execute();
                    if (!resultSet.next()) {
                        table.insert().set("discord_id", discordId).execute();
                    }

                    table.update().set("\"" + authorityName + "\"", value)
                            .where("discord_id", "=", discordId).execute();
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

    class TestCommand implements Command {

        @Override
        public String getCallsign() {
            return "test";
        }

        @Override
        public void runCommand(MessageReceivedEvent event, String rawString, List<String> tokens) throws Exception {
            if (tokens.size() == 1) {
                Authority authority = getAuthorityByName(tokens.get(0));
                boolean has = checkHasAuthority(event.getMember(), authority, true);

                if (has) {
                    event.getChannel().sendMessage("You have this authority.").queue();
                } else {
                    event.getChannel().sendMessage("You do not have this authority.").queue();
                }
            } else {
                event.getChannel().sendMessage("Wrong number of arguments.").queue();
            }
        }
    };

    class AuthorityCommand implements AuthorityRequiring, Command {

        private CommandHandler commands;

        AuthorityCommand(CarlBot carlbot) {
            commands = new CommandHandler(carlbot);

            commands.setSubName("Authority");
            commands.addCommand(new ListAuthorityCommand());
            commands.addCommand(new SetAuthorityCommand());
            commands.addCommand(new TestCommand());
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

    private enum AuthorityState {
        Give,
        Deny,
        Ignore
    }

    private AuthorityState checkAuthorityRaw(String id, Guild guild, Authority authority) throws SQLException {
        boolean hasAuthority = false;
        String authorityName = authority.getClass().getCanonicalName();

        Table table = getAuthorityTable(guild);
        ResultSet resultSet = table.select().column("*").where("discord_id", "=", id).execute();

        while (resultSet.next()) {
            Boolean permission = resultSet.getBoolean(authorityName);
            if (resultSet.wasNull()) {
                permission = null;
            }

            if (permission != null) {
                if (permission) {
                    hasAuthority = true;
                    break;
                } else {
                    return AuthorityState.Deny;
                }
            }
        }

        resultSet.close();

        return (hasAuthority) ? AuthorityState.Give:AuthorityState.Ignore;
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

    /**
     * Determines if a user has an authority. Note that role authority is included here.
     * @param member The member we are checking the authority of.
     * @param authority The authority that the user or role needs to have.
     * @param ignoreOwner if false, this will always return true for members who are the owner of the guild.
     * @return true if they have the authority, directly or by a role; false if otherwise.
     */
    public boolean checkHasAuthority(Member member, Authority authority, boolean ignoreOwner) throws SQLException {
        // Do we even need to check?
        if (!ignoreOwner && member.isOwner()) {
            return true;
        } else {

            Guild guild = member.getGuild();

            // First check if the member has directly been given authority.
            if (checkAuthorityRaw(member.getUser().getId(), guild, authority) == AuthorityState.Give) {
                return true; // We have the authority. Don't need to check anything else.
            } else {
                // If any of the roles have the authority, we can do it.
                for (Role role : member.getRoles()) {
                    AuthorityState state = checkAuthorityRaw(role.getId(), guild, authority);

                    if (state == AuthorityState.Give) {
                        return true;
                    }

                    if (state == AuthorityState.Deny) {
                        return false;
                    }
                }

                // Check if the everyone role has it.
                AuthorityState state = checkAuthorityRaw(member.getGuild().getPublicRole().getId(), guild, authority);
                if (state == AuthorityState.Give) {
                    return true;
                }

                // Nothing could give the authority. Assume they don't have it.
                return false;
            }
        }
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
        return checkAuthorityRaw(role.getId(), role.getGuild(), authority) == AuthorityState.Give;
    }

    /**
     * Gets authority object from a string.
     * @param name String of the authority name. This could be the string that the authority object returs when you call
     *             getName(), or it can be the canonical name of the authority class.
     * @return The authority, or null if it does not exist.
     */
    public Authority getAuthorityByName(String name) {
        Authority authority = authorities_absolute.get(name);
        if (authority == null) {
            authority = authorities.get(name);
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

        // This enables us to cancel commands that the user does not have authority to use.
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
            } catch (Exception e) {
                logger.error("Error while checking authority.", e);
                event.getChannel().sendMessage("An error happened while checking your authority.\n"
                        + "The error has been logged. Please try again later.").queue();

                return false;
            }

            // We passed all checks.
            return true;
        });
    }

    @Override
    public Command[] getCommands(CarlBot carlbot) {
        return new Command[] {new AuthorityCommand(carlbot)};
    }
}
