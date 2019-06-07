package net.artifactgaming.carlbot.modules;

import net.artifactgaming.carlbot.*;
import net.dv8tion.jda.core.events.message.MessageReceivedEvent;

import java.util.List;

public class Quotes implements Module {

    private class AddCommand implements Command {

        @Override
        public String getCallsign() {
            return "add";
        }

        @Override
        public void runCommand(MessageReceivedEvent event, String rawString, List<String> tokens) throws Exception {
            event.getChannel().sendMessage("Add command issued.").queue();
        }
    }

    private class RemoveCommand implements Command {

        @Override
        public String getCallsign() {
            return "remove";
        }

        @Override
        public void runCommand(MessageReceivedEvent event, String rawString, List<String> tokens) throws Exception {
            event.getChannel().sendMessage("Remove command issued.").queue();
        }
    }

    private class SetupCommand implements Command {

        @Override
        public String getCallsign() {
            return "setup";
        }

        @Override
        public void runCommand(MessageReceivedEvent event, String rawString, List<String> tokens) throws Exception {
            event.getChannel().sendMessage("setup command issued.").queue();
        }
    }

    private class DeleteAllCommand implements Command {

        @Override
        public String getCallsign() {
            return "delete_all";
        }

        @Override
        public void runCommand(MessageReceivedEvent event, String rawString, List<String> tokens) throws Exception {
            event.getChannel().sendMessage("Delete all command issued.").queue();
        }
    }

    private class RandomCommand implements Command {

        @Override
        public String getCallsign() {
            return "random";
        }

        @Override
        public void runCommand(MessageReceivedEvent event, String rawString, List<String> tokens) throws Exception {
            event.getChannel().sendMessage("Random command issued.").queue();
        }
    }

    private class QuoteCommand implements Command {

        private CommandHandler commands;

        QuoteCommand(CarlBot carlbot) {
            commands = new CommandHandler(carlbot);

            commands.setSubName(this.getCallsign());
            commands.addCommand(new AddCommand());
            commands.addCommand(new RemoveCommand());
            commands.addCommand(new SetupCommand());
            commands.addCommand(new DeleteAllCommand());
            commands.addCommand(new RandomCommand());
        }

        @Override
        public String getCallsign() {
            return "quotes";
        }

        @Override
        public void runCommand(MessageReceivedEvent event, String rawString, List<String> tokens) throws Exception {

            // First check if its a number.
            if (tokens.size() > 0) {
                String callsign = tokens.get(0);

                try {
                    // Get quote number, if it's a quote number.
                    int quoteIndex = Integer.parseInt(callsign);
                    String message = "Index quote " + quoteIndex;
                    message = Utils.cleanMessage(event.getAuthor(), message);

                    event.getChannel().sendMessage(message).queue();
                } catch (NumberFormatException e) {
                    // Wasn't a number? Maybe it's a command.
                    commands.runCommand(event, rawString, tokens);
                }
            }
        }
    }

    @Override
    public void setup(CarlBot carbot) {

    }

    @Override
    public Command[] getCommands(CarlBot carlbot) {
        return new Command[] { new QuoteCommand(carlbot) };
    }
}
