package net.artifactgaming.carlbot;

import net.dv8tion.jda.core.events.message.MessageReceivedEvent;

import java.util.HashMap;
import java.util.List;

public class CommandContainer {

    private HashMap<String, Command> commands = new HashMap<>();
    private String subName = "Error";

    public void setSubName(String subName) {
        this.subName = "Command " + subName + " error";
    }

    public String addCommand(Command command) {
        String callsign = command.getCallsign();
        commands.put(callsign, command);

        return callsign;
    }

    public void runCommand(MessageReceivedEvent event, String rawContent, List<String> tokens) {

        // Need to actually have a command.
        if (tokens.size() > 0) {
            String callsign = tokens.get(0);

            Command command = commands.get(callsign);

            // Find command.
            if (command != null) {
                command.runCommand(event, rawContent, tokens.subList(1, tokens.size()));
            } else {
                event.getChannel().sendMessage(subName + ": Unknown command \"" + callsign + "\".").queue();
            }
        } else {
            event.getChannel().sendMessage(subName + ": No command name given.").queue();
        }
    }
}
