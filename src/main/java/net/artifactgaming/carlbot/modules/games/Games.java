package net.artifactgaming.carlbot.modules.games;

import net.artifactgaming.carlbot.*;
import net.artifactgaming.carlbot.Module;
import net.artifactgaming.carlbot.modules.selfdocumentation.Documented;
import net.dv8tion.jda.core.events.message.MessageReceivedEvent;

import java.util.Collection;
import java.util.List;

public class Games implements Module, Documented {

    @Override
    public void setup(CarlBot carlbot) {

    }

    private class GamesCommands implements Command {

        private CommandHandler commands;

        GamesCommands(CarlBot carlbot) {
            commands = new CommandHandler(carlbot);

            commands.setSubName(this.getCallsign());
        }

        @Override
        public String getCallsign() {
            return "games";
        }

        @Override
        public void runCommand(MessageReceivedEvent event, String rawString, List<String> tokens) throws Exception {
            commands.runCommand(event, rawString, tokens);
        }

        @Override
        public Module getParentModule() {
            return Games.this;
        }
    }

    @Override
    public Command[] getCommands(CarlBot carlbot) {
        return new Command[]{new GamesCommands(carlbot)};
    }

    @Override
    public String getDocumentation() {
        return "Modules that contain some fun and games.";
    }

    @Override
    public String getDocumentationCallsign() {
        return "games";
    }
}
