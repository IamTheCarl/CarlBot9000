package net.artifactgaming.carlbot.listeners;

import net.dv8tion.jda.core.events.message.MessageDeleteEvent;
import net.dv8tion.jda.core.events.message.react.MessageReactionAddEvent;
import net.dv8tion.jda.core.events.message.react.MessageReactionRemoveAllEvent;
import net.dv8tion.jda.core.events.message.react.MessageReactionRemoveEvent;

/**
 * The respective functions are invoked when the respective events are occured.
 * Make sure to add this event-listener to the CarlBot.
 */
public interface OnMessageReaction {
    void onMessageDelete(MessageDeleteEvent event);
    void onMessageReactionAdd(MessageReactionAddEvent event);
    void onMessageReactionRemove(MessageReactionRemoveEvent event) ;
    void onMessageReactionRemoveAll(MessageReactionRemoveAllEvent event);

}
