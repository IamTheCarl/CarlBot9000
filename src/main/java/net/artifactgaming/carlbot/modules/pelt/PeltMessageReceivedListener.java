package net.artifactgaming.carlbot.modules.pelt;

import net.artifactgaming.carlbot.listeners.MessageReader;
import net.dv8tion.jda.core.events.message.MessageReceivedEvent;

import java.sql.SQLException;
import java.util.Random;
import java.util.function.Supplier;

public class PeltMessageReceivedListener implements MessageReader {
    private static final String PEANUT_EMOTE_UNICODE = "\uD83E\uDD5C"; // https://emojiguide.org/peanuts

    private static final String[] OTHER_RANDOM_EMOTE_UNICODE = new String[]{
            "\uD83C\uDF30", //ChestNut https://emojiguide.org/chestnut
            "\uD83D\uDD29", // Nut and Bolt https://emojiguide.org/nut-and-bolt
            "\uD83D\uDC4C", // Ok Hand https://emojiguide.org/ok-hand
            "\uD83D\uDCA6" // Sweat droplets https://emojiguide.org/sweat-droplets
    };

    /**
     * Out of 100;
     * Putting more than 100 always guarantees a peanut.
     * Putting non-positive guarantees a random emote chosen.
     */
    private static final int CHANCE_TO_GENERATE_PEANUT = 95;

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
                event.getMessage().addReaction(randomlyPickEmoteUnicodeToPeltWith()).queue();
            }
        } catch (SQLException e){
            peltModule.logger.error(e.getMessage());
        }
    }

    private String randomlyPickEmoteUnicodeToPeltWith(){
        Random rand = new Random();
        ///region Local_Function
        Supplier<String> getRandomEmoteUnicodeFromOtherRandomEmoteUnicode = () -> {
            int rnd = rand.nextInt(OTHER_RANDOM_EMOTE_UNICODE.length);
            return OTHER_RANDOM_EMOTE_UNICODE[rnd];
        };
        ///endregion

        int generatedValue = rand.nextInt(101);
        if (CHANCE_TO_GENERATE_PEANUT > generatedValue){
            return PEANUT_EMOTE_UNICODE;
        } else {
            return getRandomEmoteUnicodeFromOtherRandomEmoteUnicode.get();
        }
    }

}
