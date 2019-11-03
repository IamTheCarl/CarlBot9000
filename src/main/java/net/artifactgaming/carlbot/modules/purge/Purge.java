package net.artifactgaming.carlbot.modules.purge;

import net.artifactgaming.carlbot.*;
import net.artifactgaming.carlbot.Module;
import net.artifactgaming.carlbot.modules.authority.Authority;
import net.artifactgaming.carlbot.modules.authority.AuthorityManagement;
import net.artifactgaming.carlbot.modules.authority.AuthorityRequiring;
import net.artifactgaming.carlbot.modules.selfdocumentation.Documented;
import net.dv8tion.jda.core.entities.Message;
import net.dv8tion.jda.core.events.message.MessageReceivedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public class Purge implements Module, AuthorityRequiring, Documented {

    private Logger logger = LoggerFactory.getLogger(Purge.class);

    private AuthorityManagement authorityManagement;

    @Override
    public void setup(CarlBot carlbot) {
        authorityManagement = (AuthorityManagement) carlbot.getModule(AuthorityManagement.class);

        if (authorityManagement == null) {
            logger.error("Authority module is not loaded.");
            carlbot.crash();
        }
    }

    @Override
    public Authority[] getRequiredAuthority() {
        return new Authority[]{
            new PurgeAuthority()
        };
    }

    private class PurgeCommand implements Command, Documented, AuthorityRequiring {

        @Override
        public String getCallsign() {
            return "purge";
        }

        @Override
        public void runCommand(MessageReceivedEvent event, String rawString, List<String> tokens) throws Exception {

            // Get first positive number
            int numberOfMessagesToDelete = Utils.getFirstOrDefaultNumber(tokens, -1, (x) -> x > 0);

            if (numberOfMessagesToDelete < 0){
                List<Message> messagesToDelete =  event.getChannel().getHistoryBefore(event.getMessageId(), numberOfMessagesToDelete).complete().getRetrievedHistory();
                // Delete the command message too.
                event.getMessage().delete().queue();

                event.getTextChannel().deleteMessages(messagesToDelete).queue((e)->{
                    event.getChannel().sendMessage(numberOfMessagesToDelete + " messages has been deleted!").queue();
                });
            } else {
                event.getChannel().sendMessage("You need to specify a positive number of messages to delete!").queue();
            }
        }

        @Override
        public Module getParentModule() {
            return Purge.this;
        }

        @Override
        public String getDocumentation() {
            return "Purge messages by a given number";
        }

        @Override
        public String getDocumentationCallsign() {
            return "purge";
        }

        @Override
        public Authority[] getRequiredAuthority() {
            return new Authority[]{
                    new PurgeAuthority()
            };
        }
    }

    @Override
    public Command[] getCommands(CarlBot carlbot) {
        return new Command[] {
                new PurgeCommand()
        };
    }

    @Override
    public String getDocumentation() {
        return "Module for purging messages.";
    }

    @Override
    public String getDocumentationCallsign() {
        return "Purge";
    }
}
