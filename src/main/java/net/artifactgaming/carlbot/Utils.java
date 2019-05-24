package net.artifactgaming.carlbot;

import net.dv8tion.jda.core.entities.Message;
import net.dv8tion.jda.core.entities.User;

public class Utils {
    public static String cleanMessage(User sender, String message) {

        message = message.replace("@everyone", "@.everyone");
        message = message.replace("@here","@.here");

        return message;
    }
}
