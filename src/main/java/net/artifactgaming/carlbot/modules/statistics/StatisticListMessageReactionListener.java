package net.artifactgaming.carlbot.modules.statistics;

import net.artifactgaming.carlbot.ObjectResult;
import net.artifactgaming.carlbot.listeners.OnMessageReaction;
import net.artifactgaming.carlbot.modules.statistics.StatisticList.StatisticListMessage;
import net.dv8tion.jda.core.entities.Message;
import net.dv8tion.jda.core.events.message.MessageDeleteEvent;
import net.dv8tion.jda.core.events.message.react.MessageReactionAddEvent;
import net.dv8tion.jda.core.events.message.react.MessageReactionRemoveAllEvent;
import net.dv8tion.jda.core.events.message.react.MessageReactionRemoveEvent;
import net.dv8tion.jda.core.exceptions.InsufficientPermissionException;

import java.util.ArrayList;

// TODO: Refector with 'StatisticListMessageReactionHandler' in quotes module!
public class StatisticListMessageReactionListener implements OnMessageReaction {
    static final String NEXT_EMOTE_NAME = String.valueOf((char) 10145); // '➡' 10145
    static final String PREVIOUS_EMOTE_NAME = String.valueOf((char) 11013); // '⬅' 11013


    private enum ReactionType {
        NEXT,
        PREVIOUS,
        NULL
    }

    private ArrayList<StatisticListMessage> statisticListMessages;

    StatisticListMessageReactionListener(){
        statisticListMessages = new ArrayList<>();
    }

    public void handleMessageLeftIdle(StatisticListMessage idledMessage){
        statisticListMessages.remove(idledMessage);
    }

    @Override
    public void onMessageDelete(MessageDeleteEvent event) {
        ObjectResult<StatisticListMessage> resultOfFetchingStatisticListMessage = tryGetStatisticListMessageByMessageID(event.getMessageId());

        if (resultOfFetchingStatisticListMessage.getResult()){
            // Message has been deleted, remove from list of quoteListMessages to manage.
            statisticListMessages.remove(resultOfFetchingStatisticListMessage.getObject());
            resultOfFetchingStatisticListMessage.getObject().cancelAndPurgeIdleTimer();
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

        ObjectResult<StatisticListMessage> resultOfFetchingStatisticListMessage = tryGetStatisticListMessageByMessageID(event.getMessageId());

        if (resultOfFetchingStatisticListMessage.getResult()){
            StatisticListMessage targetStatisticMessage = resultOfFetchingStatisticListMessage.getObject();

            ReactionType reactionType = getReactionTypeFromReactionEvent.get();

            if (reactionType == ReactionType.NEXT){
                targetStatisticMessage.getNextPage();
            } else if (reactionType == ReactionType.PREVIOUS){
                targetStatisticMessage.getPreviousPage();
            }

            editEventMessageWithContent.accept(targetStatisticMessage.getCurrentPage());

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
        ObjectResult<StatisticListMessage> resultOfFetchingStatisticListMessage = tryGetStatisticListMessageByMessageID(event.getMessageId());

        if (resultOfFetchingStatisticListMessage.getResult()){
            StatisticListMessage targetStatisticMessage = resultOfFetchingStatisticListMessage.getObject();

            Message messageToEdit = event.getChannel().getMessageById(event.getMessageId()).complete();
            messageToEdit.addReaction(NEXT_EMOTE_NAME).complete();
            messageToEdit.addReaction(PREVIOUS_EMOTE_NAME).queue();
        }
    }


    public void addStatisticListMessageToListener(StatisticListMessage statisticListMessage){
        statisticListMessages.add(statisticListMessage);
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

    private ObjectResult<StatisticListMessage> tryGetStatisticListMessageByMessageID(String messageID){
        for (StatisticListMessage statisticListMessage : statisticListMessages){
            if (statisticListMessage.getMessageID().equals(messageID)){
                return new ObjectResult<>(statisticListMessage);
            }
        }

        return new ObjectResult<>(null);
    }
}
