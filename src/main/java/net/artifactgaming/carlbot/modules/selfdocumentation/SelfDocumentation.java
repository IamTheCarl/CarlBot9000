package net.artifactgaming.carlbot.modules.selfdocumentation;

import net.artifactgaming.carlbot.CarlBot;
import net.artifactgaming.carlbot.Command;
import net.artifactgaming.carlbot.CommandSet;
import net.artifactgaming.carlbot.Module;
import net.dv8tion.jda.core.events.message.MessageReceivedEvent;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SelfDocumentation implements Module, Documented {

    private Map<String, Documented> documentedModules = new HashMap<>();
    private CarlBot carlbot;

    @Override
    public void setup(CarlBot carlbot) {

        this.carlbot = carlbot;

        // Find all modules with documentation.
        for (Module module : carlbot.getModules()) {
            if (module instanceof Documented) {
                Documented document = (Documented) module;
                documentedModules.put(document.getDocumentationCallsign(), document);
            }
        }
    }

    @Override
    public String getDocumentation() {
        return "This module handles documentation that is built into Carl Bot 9000.\n\n"
                + "Type `$>help` to get a list of Carl Bot modules with documentation.\n"
                + "You can type `$>help [module name]` to get documentation on that specific module.\n"
                + "A module's documentation will have a list of documented commands. To get details on that command, "
                + "you can type `$>help [module name] [command name]`.\n\n"
                + "There's more documentation you can access with the help command. You can learn about it by typing "
                + "`$>help documentation help`.";
    }

    @Override
    public String getDocumentationCallsign() {
        return "documentation";
    }

    private class HelpCommand implements Command, Documented {

        @Override
        public String getCallsign() {
            return "help";
        }

        @Override
        public void runCommand(MessageReceivedEvent event, String rawString, List<String> tokens) throws Exception {
            if (tokens.size() > 0) {

                Documented moduleDocument = documentedModules.get(tokens.get(0));

                if (moduleDocument != null) {
                    if (tokens.size() == 1) {
                        event.getChannel().sendMessage(moduleDocument.getDocumentation()).queue();

                        int numCommands = 0;
                        String commandList = "Documented commands provided by this module:\n```\n";

                        for (Command command : carlbot.getCommands()) {
                            // Ensure that all shown documentated commands are from the requested command.
                            if (command.getParentModule() != moduleDocument) { continue; }

                            // For command sets (Like QuoteCommands)
                            if (command instanceof CommandSet){
                                CommandSet commandSet = (CommandSet) command;

                                for (Command commandInCommandSet : commandSet.getCommands()){
                                    if (commandInCommandSet instanceof Documented) {
                                        numCommands++;
                                        commandList += ((Documented) commandInCommandSet).getDocumentationCallsign();
                                        commandList += '\n';
                                    }
                                }
                            }
                            else if (command instanceof Documented) {
                                numCommands++;
                                commandList += ((Documented) command).getDocumentationCallsign();
                                commandList += '\n';
                            }
                        }

                        commandList += "```";

                        if (numCommands > 0) {
                            event.getChannel().sendMessage(commandList).queue();
                        } else {
                            event.getChannel().sendMessage("This module does not provide any sub-commands.").queue();
                        }
                    } else {

                        String documentation = "The module exists but the command could not be found in it.";

                        for (Command command : carlbot.getCommands()) {
                            boolean respectiveCommandFound = false;
                            if (command.getParentModule() != moduleDocument){ continue; }

                            // If this command has a command set, loop through the commandset.
                            if (command instanceof CommandSet){
                                CommandSet commandSet = (CommandSet) command;

                                for (Command commandInCommandSet : commandSet.getCommands()){
                                    // Search if any of the commands in this commandset matches the desired command.
                                    if (commandInCommandSet instanceof  Documented){
                                        Documented documentedCommand = (Documented) commandInCommandSet;
                                        if (documentedCommand.getDocumentationCallsign().equals(tokens.get(1))) {
                                            documentation = documentedCommand.getDocumentation();
                                            respectiveCommandFound = true;
                                        }
                                    }
                                }

                            } else if (command instanceof Documented) {
                                Documented commandDocument = (Documented)command;
                                if (commandDocument.getDocumentationCallsign().equals(tokens.get(1))) {
                                    documentation = ((Documented) command).getDocumentation();
                                    respectiveCommandFound = true;
                                }
                            }

                            if (respectiveCommandFound){
                                break;
                            }
                        }

                        event.getChannel().sendMessage(documentation).queue();
                    }

                } else {
                    event.getChannel().sendMessage("Could not find a module by this name. Check for typos.")
                            .queue();
                }
            } else {
                String message = "If you're just getting started with Carl Bot, type `$>help documentation`.\n"
                        + "Modules you can get documentation for:\n```\n";

                for (String moduleName : documentedModules.keySet()) {
                    message += moduleName;
                    message += "\n";
                }

                message += "```";

                event.getChannel().sendMessage(message).queue();
            }
        }

        @Override
        public Module getParentModule() {
            return SelfDocumentation.this;
        }

        @Override
        public String getDocumentation() {
            return "The help command provides documentation. Type `$>help documentation` for a basic tutorial.\n\n"
                    + "The fact that you found this means you likely already have the basics of this command though.\n"
                    + "Some commands contain sub-commands. A good example would be the authority module's authority "
                    + "command. To get documentation for a sub-command, you just need to type out your usual command "
                    + "to get documentation for the parent command and then you can add the sub-command to the end to "
                    + "get its documentation. For example `$>help authority authority set` would give you "
                    + "documentation for the command `$>authority set everyone UseQuotes give`.";
        }

        @Override
        public String getDocumentationCallsign() {
            return "help";
        }
    }

    @Override
    public Command[] getCommands(CarlBot carlbot) {
        return new Command[] { new HelpCommand() };
    }
}
