package net.artifactgaming.carlbot.modules.pelt;

import net.artifactgaming.carlbot.listeners.MessageReader;
import net.dv8tion.jda.core.events.message.MessageReceivedEvent;

import java.sql.SQLException;

public class PeltMessageReceivedListener implements MessageReader {
    private Pelt peltModule;

    PeltMessageReceivedListener(Pelt peltModule) {
        this.peltModule = peltModule;
    }

    @Override
    public void onMessageReceived(MessageReceivedEvent event) {
        if (event.getGuild() == null || event.getAuthor().isBot()){
            return;
        }

        try {
            if (peltModule.userIsPeltedInGuild(event.getAuthor().getId(), event.getGuild())) {
                // TODO: Actually pelt this user.
                event.getTextChannel().sendMessage("TODO: Actually pelt this user.").queue();
            }
        } catch (SQLException e){
            peltModule.logger.error(e.getMessage());
        }
    }
}
