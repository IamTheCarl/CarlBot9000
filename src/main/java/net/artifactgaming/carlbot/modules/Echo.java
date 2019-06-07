package net.artifactgaming.carlbot.modules;

import net.artifactgaming.carlbot.CarlBot;
import net.artifactgaming.carlbot.Command;
import net.artifactgaming.carlbot.Module;
import net.artifactgaming.carlbot.Utils;
import net.dv8tion.jda.core.events.message.MessageReceivedEvent;

import java.util.List;

public class Echo implements Module {

    private class EchoCommand implements Command {

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
    }

    @Override
    public void setup(CarlBot carbot) {

    }

    @Override
    public Command[] getCommands(CarlBot carlbot) {
        return new Command[] {new EchoCommand()};
    }
}
