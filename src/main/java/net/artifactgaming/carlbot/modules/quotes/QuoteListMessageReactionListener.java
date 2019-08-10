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

import java.util.ArrayList;

/**
 * Handles all the quote list messages, updating them as messages get reactions.
 */
public class QuoteListMessageReactionListener implements OnMessageReaction {

    static final String NEXT_EMOTE_NAME = "➡";
    static final String PREVIOUS_EMOTE_NAME = "⬅";

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

        // Ignore bots
        if (event.getUser().isBot()){
            return;
        }

        ObjectResult<QuoteListMessage> resultOfFetchingQuoteListMessage = tryGetQuoteListMessageByMessageID(event.getMessageId());

        if (resultOfFetchingQuoteListMessage.getResult()){
            QuoteListMessage targetQuoteMessage = resultOfFetchingQuoteListMessage.getObject();

            ReactionType reactionType = getReactionTypeFromEmoteName(event.getReaction().getReactionEmote().getName());

            if (reactionType == ReactionType.NEXT){
                targetQuoteMessage.getNextPage();
                Message messageToEdit = event.getChannel().getMessageById(event.getMessageId()).complete();
                messageToEdit.editMessage("```" + targetQuoteMessage.getCurrentPageAsReadableDiscordString() + "```").queue();
            } else if (reactionType == ReactionType.PREVIOUS){
                targetQuoteMessage.getPreviousPage();

                Message messageToEdit = event.getChannel().getMessageById(event.getMessageId()).complete();
                messageToEdit.editMessage("```" + targetQuoteMessage.getCurrentPageAsReadableDiscordString() + "```").queue();
            }
        }
    }

    @Override
    public void onMessageReactionRemove(MessageReactionRemoveEvent event) {
        // Ignore bots
        if (event.getUser().isBot()){
            return;
        }

        ObjectResult<QuoteListMessage> resultOfFetchingQuoteListMessage = tryGetQuoteListMessageByMessageID(event.getMessageId());

        if (resultOfFetchingQuoteListMessage.getResult()){
            QuoteListMessage targetQuoteMessage = resultOfFetchingQuoteListMessage.getObject();

            ReactionType reactionType = getReactionTypeFromEmoteName(event.getReactionEmote().getName());

            if (reactionType == ReactionType.NEXT){
                targetQuoteMessage.getNextPage();
                Message messageToEdit = event.getChannel().getMessageById(event.getMessageId()).complete();
                messageToEdit.editMessage("```" + targetQuoteMessage.getCurrentPageAsReadableDiscordString() + "```").queue();
            } else if (reactionType == ReactionType.PREVIOUS){
                targetQuoteMessage.getPreviousPage();
                Message messageToEdit = event.getChannel().getMessageById(event.getMessageId()).complete();
                messageToEdit.editMessage("```" + targetQuoteMessage.getCurrentPageAsReadableDiscordString() + "```").queue();
            }
        }
    }

    @Override
    public void onMessageReactionRemoveAll(MessageReactionRemoveAllEvent event) {
        ObjectResult<QuoteListMessage> resultOfFetchingQuoteListMessage = tryGetQuoteListMessageByMessageID(event.getMessageId());

        if (resultOfFetchingQuoteListMessage.getResult()){
            QuoteListMessage targetQuoteMessage = resultOfFetchingQuoteListMessage.getObject();

            Message messageToEdit = event.getChannel().getMessageById(event.getMessageId()).complete();
            messageToEdit.addReaction(NEXT_EMOTE_NAME);
            messageToEdit.addReaction(PREVIOUS_EMOTE_NAME);
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
