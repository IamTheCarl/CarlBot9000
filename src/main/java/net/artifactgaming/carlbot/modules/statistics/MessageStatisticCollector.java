package net.artifactgaming.carlbot.modules.statistics;

import net.artifactgaming.carlbot.listeners.MessageReader;
import net.dv8tion.jda.core.events.message.MessageReceivedEvent;

import java.util.HashSet;

public class MessageStatisticCollector implements MessageReader {

    /**
     * ID of guilds to collect data from.
     */
    private HashSet<String> targetGuildIDs;

    public MessageStatisticCollector(){
        targetGuildIDs = new HashSet<>();
    }

    @Override
    public void onMessageReceived(MessageReceivedEvent event) {
        if (messageFromBotOrNonGuild(event)){
            return;
        }

        if (isTrackingGuild(event.getGuild().getId())){

        }
    }

    private boolean messageFromBotOrNonGuild(MessageReceivedEvent event){
        return event.getAuthor().isBot() || event.getGuild() == null;
    }

    public boolean isTrackingGuild(String guildID){
        return targetGuildIDs.contains(guildID);
    }

    public void untrackGuildStatisticsByID(String guildID){
        targetGuildIDs.remove(guildID);
    }

    public void trackGuildStatisticsByID(String guildID){
        targetGuildIDs.add(guildID);
    }
}
