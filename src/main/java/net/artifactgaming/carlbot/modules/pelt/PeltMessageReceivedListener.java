package net.artifactgaming.carlbot.modules.pelt;

import net.artifactgaming.carlbot.listeners.MessageReader;
import net.dv8tion.jda.core.events.message.MessageReceivedEvent;

import java.sql.SQLException;

public class PeltMessageReceivedListener implements MessageReader {
    private static final String PEANUT_EMOTE_UNICODE = "\uD83E\uDD5C"; // https://emojiguide.org/peanuts

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
                event.getMessage().addReaction(PEANUT_EMOTE_UNICODE).queue();
            }
        } catch (SQLException e){
            peltModule.logger.error(e.getMessage());
        }
    }
}
