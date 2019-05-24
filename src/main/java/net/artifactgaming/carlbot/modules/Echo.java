package net.artifactgaming.carlbot.modules;

import net.artifactgaming.carlbot.Command;
import net.artifactgaming.carlbot.Module;
import net.dv8tion.jda.core.events.message.MessageReceivedEvent;

import java.util.List;

public class Echo implements Module {

    private class EchoCommand implements Command {

        @Override
        public String getCallsign() {
            return "echo";
        }

        @Override
        public void runCommand(MessageReceivedEvent event, String rawString, List<String> tokens) {
            String message = "[";
            boolean isCallsign = true;

            for (String token : tokens) {

                // We don't want to include the callsign.
                if (!isCallsign) {
                    message += token + " ";
                } else {
                    isCallsign = false;
                }
            }

            if (message.length() > 1) {
                message = message.substring(0, message.length() - 1);
            }

            message += "]";

            event.getChannel().sendMessage(message).queue();
        }
    }

    @Override
    public Command[] getCommands() {
        return new Command[] {new EchoCommand()};
    }
}
