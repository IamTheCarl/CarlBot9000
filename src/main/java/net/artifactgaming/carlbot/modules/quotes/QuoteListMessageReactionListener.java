package net.artifactgaming.carlbot.modules.quotes;

import net.artifactgaming.carlbot.ObjectResult;
import net.artifactgaming.carlbot.Utils;
import net.artifactgaming.carlbot.listeners.OnMessageReaction;
import net.dv8tion.jda.bot.JDABot;
import net.dv8tion.jda.core.JDA;
import net.dv8tion.jda.core.entities.Emote;
import net.dv8tion.jda.core.entities.Message;
import net.dv8tion.jda.core.entities.MessageReaction;
import net.dv8tion.jda.core.events.message.MessageDeleteEvent;
import net.dv8tion.jda.core.events.message.react.MessageReactionAddEvent;
import net.dv8tion.jda.core.events.message.react.MessageReactionRemoveAllEvent;
import net.dv8tion.jda.core.events.message.react.MessageReactionRemoveEvent;
import net.dv8tion.jda.core.exceptions.InsufficientPermissionException;

import java.util.ArrayList;

/**
 * Handles all the quote list messages, updating them as messages get reactions.
 */
public class QuoteListMessageReactionListener implements OnMessageReaction {

    static final String NEXT_EMOTE_NAME = String.valueOf((char) 10145); // '➡' 10145
    static final String PREVIOUS_EMOTE_NAME = String.valueOf((char) 11013); // '⬅' 11013


    private enum ReactionType {
        NEXT,
        PREVIOUS,
        NULL
    }

    ArrayList<QuoteListMessage> quoteListMessages;

    QuoteListMessageReactionListener(){
        quoteListMessages = new ArrayList<>();
    }

    void handleMessageLeftIdle(QuoteListMessage idledMessage){
        quoteListMessages.remove(idledMessage);
    }

    @Override
    public void onMessageDelete(MessageDeleteEvent event) {
        ObjectResult<QuoteListMessage> resultOfFetchingQuoteListMessage = tryGetQuoteListMessageByMessageID(event.getMessageId());

        if (resultOfFetchingQuoteListMessage.getResult()){
            // Message has been deleted, remove from list of quoteListMessages to manage.
            quoteListMessages.remove(resultOfFetchingQuoteListMessage.getObject());
            resultOfFetchingQuoteListMessage.getObject().cancelAndPurgeIdleTimer();
        }
    }

    @Override
    public void onMessageReactionAdd(MessageReactionAddEvent event) {

        ///region Local_Function

        java.util.function.Supplier<ReactionType> getReactionTypeFromReactionEvent = () -> {
            return getReactionTypeFromEmoteName(event.getReaction().getReactionEmote().getName());
        };

        java.util.function.BooleanSupplier eventUserIsBot = () -> {
            return event.getUser().isBot();
        };

        java.util.function.Consumer<String> editEventMessageWithContent = (String content) -> {
            Message messageToEdit = event.getChannel().getMessageById(event.getMessageId()).complete();
            messageToEdit.editMessage(content).queue();
        };

        ///endregion

        // Ignore bots
        if (eventUserIsBot.getAsBoolean()){
            return;
        }

        ObjectResult<QuoteListMessage> resultOfFetchingQuoteListMessage = tryGetQuoteListMessageByMessageID(event.getMessageId());

        if (resultOfFetchingQuoteListMessage.getResult()){
            QuoteListMessage targetQuoteMessage = resultOfFetchingQuoteListMessage.getObject();

            ReactionType reactionType = getReactionTypeFromReactionEvent.get();

            if (reactionType == ReactionType.NEXT){
                targetQuoteMessage.getNextPage();
            } else if (reactionType == ReactionType.PREVIOUS){
                targetQuoteMessage.getPreviousPage();
            }

            editEventMessageWithContent.accept("```" + targetQuoteMessage.getCurrentPageAsReadableDiscordString() + "```");

            try {
                event.getReaction().removeReaction(event.getUser()).queue();
            } catch (InsufficientPermissionException e){
                event.getChannel().sendMessage("ERROR: I do not have enough permission to remove emotes from others!").queue();
            }
        }
    }

    @Override
    public void onMessageReactionRemove(MessageReactionRemoveEvent event) {
    }

    @Override
    public void onMessageReactionRemoveAll(MessageReactionRemoveAllEvent event) {
        ObjectResult<QuoteListMessage> resultOfFetchingQuoteListMessage = tryGetQuoteListMessageByMessageID(event.getMessageId());

        if (resultOfFetchingQuoteListMessage.getResult()){
            QuoteListMessage targetQuoteMessage = resultOfFetchingQuoteListMessage.getObject();

            Message messageToEdit = event.getChannel().getMessageById(event.getMessageId()).complete();
            messageToEdit.addReaction(NEXT_EMOTE_NAME).complete();
            messageToEdit.addReaction(PREVIOUS_EMOTE_NAME).queue();
        }
    }


    public void addQuoteListMessageToListener(QuoteListMessage quoteListMessage){
        quoteListMessages.add(quoteListMessage);
    }

    private static ReactionType getReactionTypeFromEmoteName(String emoteName){
        if (emoteName.equals(NEXT_EMOTE_NAME)){
            return ReactionType.NEXT;
        } else if (emoteName.equals(PREVIOUS_EMOTE_NAME)){
            return ReactionType.PREVIOUS;
        } else {
            return ReactionType.NULL;
        }
    }

    private ObjectResult<QuoteListMessage> tryGetQuoteListMessageByMessageID(String messageID){
        for (QuoteListMessage quoteListMessage : quoteListMessages){
            if (quoteListMessage.getMessageID().equals(messageID)){
                return new ObjectResult<>(quoteListMessage);
            }
        }

        return new ObjectResult<>(null);
    }
}
