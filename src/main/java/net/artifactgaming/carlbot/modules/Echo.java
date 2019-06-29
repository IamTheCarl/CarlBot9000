package net.artifactgaming.carlbot.modules;

import net.artifactgaming.carlbot.CarlBot;
import net.artifactgaming.carlbot.Command;
import net.artifactgaming.carlbot.Module;
import net.artifactgaming.carlbot.Utils;
import net.artifactgaming.carlbot.modules.schedule.SchedulableCommand;
import net.artifactgaming.carlbot.modules.selfdocumentation.Documented;
import net.dv8tion.jda.core.events.message.MessageReceivedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class Echo implements Module, Documented {

    private Logger logger = LoggerFactory.getLogger(Echo.class);

    private class EchoCommand implements Command, SchedulableCommand {

        @Override
        public String getCallsign() {
            return "echo";
        }

        @Override
        public void runCommand(MessageReceivedEvent event, String rawString, List<String> tokens) throws Exception {
            String message = "[";

            for (String token : tokens) {
                message += token + " ";
            }

            if (message.length() > 1) {
                message = message.substring(0, message.length() - 1);
            }

            message += "]";

            // Clean the message up so it can't ping @everyone.
            message = Utils.cleanMessage(event.getAuthor(), message);

            event.getChannel().sendMessage(message).queue();
        }

        @Override
        public Module getParentModule() {
            return Echo.this;
        }

        @Override
        public void InvokeCommand(String guildID, String channelID, String inputRawString) {
            logger.error("Echoed! With raw string: " + inputRawString);
        }
    }

    @Override
    public void setup(CarlBot carbot) {

    }

    @Override
    public Command[] getCommands(CarlBot carlbot) {
        return new Command[] {new EchoCommand()};
    }

    //region DOCUMENTATION

    @Override
    public String getDocumentation() {
        return "This module allows the bot to repeat what you said!\n (It does not ping everyone though.)";
    }

    @Override
    public String getDocumentationCallsign() {
        return "echo";
    }

    //endregion
}
