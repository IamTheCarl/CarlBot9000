package net.artifactgaming.carlbot.modules.pelt;

import net.artifactgaming.carlbot.*;
import net.artifactgaming.carlbot.Module;
import net.artifactgaming.carlbot.modules.Echo;
import net.artifactgaming.carlbot.modules.authority.Authority;
import net.artifactgaming.carlbot.modules.authority.AuthorityRequiring;
import net.artifactgaming.carlbot.modules.persistence.Persistence;
import net.artifactgaming.carlbot.modules.persistence.PersistentModule;
import net.artifactgaming.carlbot.modules.persistence.Table;
import net.artifactgaming.carlbot.modules.selfdocumentation.Documented;
import net.dv8tion.jda.core.entities.Guild;
import net.dv8tion.jda.core.entities.User;
import net.dv8tion.jda.core.events.message.MessageReceivedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class Pelt implements Module, Documented, PersistentModule, AuthorityRequiring {

    /**
     * Used in the SQL Data table to represent the column name to store the pelted person's ID.
     */
    private static final String PELTED_PERSON_ID = "pelted_person_id";

    /**
     * Used in the SQL Data table to represent the column name to store the pelted person's name.
     */
    private static final String PELTED_PERSON_NAME = "pelted_person_name";

    private Logger logger = LoggerFactory.getLogger(Echo.class);
    private Persistence persistence;

    @Override
    public Authority[] getRequiredAuthority() {
        return new Authority[] {
                new SelfPelt(),
                new AllPelt()
        };
    }

    private class SelfPeltCommand implements Command, AuthorityRequiring {

        @Override
        public String getCallsign() {
            return "selfPelt";
        }

        @Override
        public void runCommand(MessageReceivedEvent event, String rawString, List<String> tokens) throws Exception {
            if (event.getGuild() == null){
                event.getChannel().sendMessage("This command can only be used in a server!").queue();
                return;
            }

            ToggleUserPeltStatus(event.getGuild(), event.getAuthor());
        }

        private void ToggleUserPeltStatus(Guild guildToPeltOn, User user) throws SQLException {
            Table guildPeltTable = getPeltTableByGuild(guildToPeltOn);

            ResultSet resultSet = guildPeltTable.select().where(PELTED_PERSON_ID, "=", user.getId()).execute();

            if (resultSet.next()){
                guildPeltTable.delete()
                        .where(PELTED_PERSON_ID, "=", user.getId()).execute();
            } else {
                guildPeltTable.insert()
                        .set(PELTED_PERSON_ID, user.getId())
                        .set(PELTED_PERSON_NAME, user.getName())
                        .execute();
            }
        }

        @Override
        public Module getParentModule() {
            return Pelt.this;
        }

        @Override
        public Authority[] getRequiredAuthority() {
            return new Authority[]{
                    new SelfPelt()
            };
        }
    }

    private class UnpeltCommand implements Command, AuthorityRequiring {

        @Override
        public String getCallsign() {
            return "unpelt";
        }

        @Override
        public void runCommand(MessageReceivedEvent event, String rawString, List<String> tokens) throws Exception {
            // TODO: Refactor with 'PeltCommand' (DRY principle)
            if (event.getGuild() == null){
                event.getChannel().sendMessage("This command can only be used in a server!").queue();
                return;
            }

            ObjectResult<List<User>> mentionedUsersResult = tryGetMentionedUsersFromMessage(event);

            if (!mentionedUsersResult.getResult()){
                event.getChannel().sendMessage("You need to mention a user to unpelt!").queue();
                return;
            }

            List<User> usersToUnpelt = mentionedUsersResult.getObject();

            removeUsersToPelt(event.getGuild(), usersToUnpelt);
        }

        private void removeUsersToPelt(Guild guildToPeltOn, List<User> users) throws SQLException {
            Table guildPeltTable = getPeltTableByGuild(guildToPeltOn);

            for (User userToPelt: users) {
                if (userToPelt.isBot()){ continue; }

                ResultSet resultSet = guildPeltTable.select().where(PELTED_PERSON_ID, "=", userToPelt.getId()).execute();

                if (resultSet.next()){
                    guildPeltTable.delete()
                            .where(PELTED_PERSON_ID, "=", userToPelt.getId()).execute();
                }
            }
        }

        @Override
        public Module getParentModule() {
            return Pelt.this;
        }

        @Override
        public Authority[] getRequiredAuthority() {
            return new Authority[] {
                    new AllPelt()
            };
        }
    }

    private class PeltCommand implements Command, AuthorityRequiring {

        @Override
        public String getCallsign() {
            return "pelt";
        }

        @Override
        public void runCommand(MessageReceivedEvent event, String rawString, List<String> tokens) throws Exception {
            if (event.getGuild() == null){
                event.getChannel().sendMessage("This command can only be used in a server!").queue();
                return;
            }

            ObjectResult<List<User>> mentionedUsersResult = tryGetMentionedUsersFromMessage(event);

            if (!mentionedUsersResult.getResult()){
                event.getChannel().sendMessage("You need to mention a user to pelt!").queue();
                return;
            }

            List<User> usersToPelt = mentionedUsersResult.getObject();

            addUsersToPelt(event.getGuild(), usersToPelt);
        }

        private void addUsersToPelt(Guild guildToPeltOn, List<User> users) throws SQLException {
            Table guildPeltTable = getPeltTableByGuild(guildToPeltOn);

            for (User userToPelt: users) {
                if (userToPelt.isBot()){ continue; }

                ResultSet resultSet = guildPeltTable.select().where(PELTED_PERSON_ID, "=", userToPelt.getId()).execute();

                if (!resultSet.next()){
                    guildPeltTable.insert()
                            .set(PELTED_PERSON_ID, userToPelt.getId())
                            .set(PELTED_PERSON_NAME, userToPelt.getName()).execute();
                }
            }
        }

        @Override
        public Module getParentModule() {
            return Pelt.this;
        }

        @Override
        public Authority[] getRequiredAuthority() {
            return new Authority[] {
                    new AllPelt()
            };
        }
    }

    private ObjectResult<List<User>> tryGetMentionedUsersFromMessage(MessageReceivedEvent event){
        List<User> mentionedUsers = event.getMessage().getMentionedUsers();

        if (mentionedUsers.size() == 0){
            return new ObjectResult<>(null);
        } else {
            return new ObjectResult<>(filterOutSameUsersInList(mentionedUsers));
        }
    }

    private List<User> filterOutSameUsersInList(List<User> input){
        List<User> copy = new ArrayList<>();

        for (User user: input) {
            if (!copy.contains(user)){
                copy.add(user);
            }
        }

        return copy;
    }

    private Table getPeltTableByGuild(Guild guild) throws SQLException {

        Table table = persistence.getGuildTable(guild, this);
        Table guildTable = new Table(table, "pelt");

        if (!guildTable.exists()) {
            guildTable.create();

            guildTable.alter().add()
                    .pushValue("pelted_person_id varchar").pushValue("pelted_person_name varchar")
                    .execute();
        }

        return guildTable;
    }

    @Override
    public void setup(CarlBot carlbot) {

    }

    @Override
    public Command[] getCommands(CarlBot carlbot) {
        return new Command[] {new PeltCommand()};
    }

    @Override
    public String getDocumentation() {
        return "This module allows you to pelt anyone with a peanut";
    }

    @Override
    public String getDocumentationCallsign() {
        return "pelt";
    }
}
