package net.artifactgaming.carlbot;

import net.dv8tion.jda.core.events.message.MessageReceivedEvent;
import org.h2.jdbc.JdbcSQLException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

public class CommandHandler {

    private HashMap<String, Command> commands = new HashMap<>();
    private String subName = "Error";

    private Logger logger = LoggerFactory.getLogger(CommandHandler.class);
    private CarlBot carlbot;

    public void setSubName(String subName) {
        this.subName = "Command " + subName + " error";
    }

    public CommandHandler(CarlBot carlbot) {
        this.carlbot = carlbot;
    }

    public String addCommand(Command command) {
        String callsign = command.getCallsign();

        if (!commands.containsKey(callsign)) {
            commands.put(callsign, command);
        } else {
            logger.warn("Attempt to add command \"" + callsign + "\" to command handler. A command with the same " +
                        "name already exists in this command handler. The first command added has been left in place " +
                        "and the new one has been ignored.\n\n" +

                        "Command classes of interest: " + command.getClass().getCanonicalName() + ", " +
                        commands.get(callsign).getClass().getCanonicalName()
                    );
        }

        return callsign;
    }

    public void runCommand(MessageReceivedEvent event, String rawContent, List<String> tokens) {

        // Need to actually have a command.
        if (tokens.size() > 0) {
            String callsign = tokens.get(0);

            Command command = commands.get(callsign);

            // Find command.
            if (command != null) {
                for (CommandPermissionChecker checker : carlbot.permissionCheckers) {

                    // Not allowed? ABORT.
                    if (!checker.checkPermission(event, command)) {
                        return;
                    }
                }

                try {
                    command.runCommand(event, rawContent, tokens.subList(1, tokens.size()));
                } catch (Exception e) {
                    logger.error("Uncaught error in command: ",  e);
                    event.getChannel().sendMessage(
                            "Sorry, but there was a critical error processing that command.\n"
                                + "Please try again later.").queue();
                }
            } else {
                event.getChannel().sendMessage(subName + ": Unknown command \"" + callsign + "\".").queue();
            }
        } else {
            event.getChannel().sendMessage(subName + ": No command name given.").queue();
        }
    }

    public Set<String> getCallsigns() {
        return commands.keySet();
    }

    public Command getCommand(String callsign) {
        return commands.get(callsign);
    }

    public Collection<Command> getCommands() {
        return commands.values();
    }
}
