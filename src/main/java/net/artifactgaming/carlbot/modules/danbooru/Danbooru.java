package net.artifactgaming.carlbot.modules.danbooru;
import net.artifactgaming.carlbot.CarlBot;
import net.artifactgaming.carlbot.Command;
import net.artifactgaming.carlbot.CommandHandler;
import net.artifactgaming.carlbot.Module;

import net.artifactgaming.carlbot.modules.selfdocumentation.Documented;
import net.dv8tion.jda.core.events.message.MessageReceivedEvent;

import java.util.List;

public class Danbooru implements Module, Documented {

    @Override
    public void setup(CarlBot carlbot) {

    }

    private class DanbooruCommands implements Command {
        private CommandHandler commands;

        DanbooruCommands(CarlBot carlbot) {
            commands = new CommandHandler(carlbot);

        }

        @Override
        public String getCallsign() {
            return "danbooru";
        }

        @Override
        public void runCommand(MessageReceivedEvent event, String rawString, List<String> tokens) throws Exception {
            commands.runCommand(event, rawString, tokens);
        }

        @Override
        public Module getParentModule() {
            return Danbooru.this;
        }

    }

    @Override
    public Command[] getCommands(CarlBot carlbot) {
        return new Command[0];
    }

    @Override
    public String getDocumentation() {
        return "danbooru";
    }

    @Override
    public String getDocumentationCallsign() {
        return "danbooru";
    }
}
