package net.artifactgaming.carlbot.modules.listeners;

import net.dv8tion.jda.core.events.message.MessageReceivedEvent;

public interface MessageReader {
    void onMessageReceived(MessageReceivedEvent event);
}
