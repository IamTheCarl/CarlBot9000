package net.artifactgaming.carlbot.modules;

import net.artifactgaming.carlbot.*;
import net.artifactgaming.carlbot.Module;
import net.artifactgaming.carlbot.modules.persistence.Persistence;
import net.artifactgaming.carlbot.modules.persistence.PersistentModule;
import net.artifactgaming.carlbot.modules.persistence.Table;
import net.artifactgaming.carlbot.modules.selfdocumentation.Documented;
import net.dv8tion.jda.core.entities.Guild;
import net.dv8tion.jda.core.entities.TextChannel;
import net.dv8tion.jda.core.entities.User;
import net.dv8tion.jda.core.events.message.MessageReceivedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class Pelt implements Module, Documented, PersistentModule {

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

    private class PeltCommand implements Command {

        @Override
        public String getCallsign() {
            return "pelt";
        }

        @Override
        public void runCommand(MessageReceivedEvent event, String rawString, List<String> tokens) throws Exception {
            ObjectResult<List<User>> mentionedUsersResult = tryGetMentionedUsersFromMessage(event);

            if (!mentionedUsersResult.getResult()){
                event.getChannel().sendMessage("You need to mention a user to pelt!").queue();
                return;
            }

            List<User> usersToPelt = filterOutSameUsersInList(mentionedUsersResult.getObject());

            for (User user: usersToPelt){
                // TODO: Add to the table on the list of persons to pelt
            }
        }

        private ObjectResult<List<User>> tryGetMentionedUsersFromMessage(MessageReceivedEvent event){
            List<User> mentionedUsers = event.getMessage().getMentionedUsers();

            if (mentionedUsers.size() == 0){
                return new ObjectResult<>(null);
            } else {
                return new ObjectResult<>(mentionedUsers);
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

        @Override
        public Module getParentModule() {
            return Pelt.this;
        }
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
