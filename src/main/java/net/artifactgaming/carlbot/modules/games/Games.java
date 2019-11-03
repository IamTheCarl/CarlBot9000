package net.artifactgaming.carlbot.modules.games;

import net.artifactgaming.carlbot.*;
import net.artifactgaming.carlbot.Module;
import net.artifactgaming.carlbot.modules.games.minesweeper.MinesweeperGenerator;
import net.artifactgaming.carlbot.modules.games.minesweeper.MinesweeperTile;
import net.artifactgaming.carlbot.modules.selfdocumentation.Documented;
import net.dv8tion.jda.core.events.message.MessageReceivedEvent;
import java.util.List;

public class Games implements Module, Documented {

    @Override
    public void setup(CarlBot carlbot) {

    }

    private class MinesweeperCommand implements Command, Documented {


        @Override
        public String getCallsign() {
            return "minesweeper";
        }

        @Override
        public void runCommand(MessageReceivedEvent event, java.lang.String rawString, List<java.lang.String> tokens) throws Exception {
            MinesweeperTile[][] playField = MinesweeperGenerator.generateField();

            event.getChannel().sendMessage(MinesweeperGenerator.toReadableString(playField)).queue();
        }

        @Override
        public Module getParentModule() {
            return Games.this;
        }

        @Override
        public String getDocumentation() {
            return "Create a playing field for minesweeper (10 x 10)";
        }

        @Override
        public String getDocumentationCallsign() {
            return "minesweeper";
        }
    }

    private class GamesCommands implements Command {

        private CommandHandler commands;

        GamesCommands(CarlBot carlbot) {
            commands = new CommandHandler(carlbot);
            commands.setSubName(this.getCallsign());

            commands.addCommand(new MinesweeperCommand());
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
