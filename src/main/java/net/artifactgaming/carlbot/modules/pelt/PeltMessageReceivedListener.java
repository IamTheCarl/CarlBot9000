package net.artifactgaming.carlbot.modules.pelt;

import net.artifactgaming.carlbot.listeners.MessageReader;
import net.dv8tion.jda.core.events.message.MessageReceivedEvent;

import java.sql.SQLException;
import java.util.Random;
import java.util.function.Supplier;

public class PeltMessageReceivedListener implements MessageReader {
    private static final String PEANUT_EMOTE_UNICODE = "\uD83E\uDD5C"; // https://emojiguide.org/peanuts

    // https://emojiguide.org
    private static final String[] OTHER_RANDOM_EMOTE_UNICODE = new String[]{
            "\uD83C\uDF30",
            "\uD83D\uDD29",
            "\uD83D\uDC4C",
            "\uD83C\uDF4C",
            "\uD83C\uDF59",
            "\uD83E\uDDE0",
            "\uD83D\uDCAF",
            "\uD83D\uDE4C",
            "\uD83D\uDD28",
            "\uD83D\uDC3F",
            "\uD83D\uDC96",
            "\uD83D\uDCA2",
            "\uD83D\uDCA3",
            "\uD83D\uDCA5",
            "\uD83D\uDD25",
            "\uD83E\uDD21",
            "\uD83D\uDC7B",
            "\uD83E\uDD16",
            "\uD83D\uDE48",
            "\uD83D\uDE49",
            "\uD83D\uDE4A"
    };

    private static final int CHANCE_TO_GENERATE_PEANUT = 299;

    private static final int GENERATE_CHANCE_BOUND = 301;

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

        int generatedValue = rand.nextInt(GENERATE_CHANCE_BOUND);
        if (CHANCE_TO_GENERATE_PEANUT > generatedValue){
            return PEANUT_EMOTE_UNICODE;
        } else {
            return getRandomEmoteUnicodeFromOtherRandomEmoteUnicode.get();
        }
    }

}
