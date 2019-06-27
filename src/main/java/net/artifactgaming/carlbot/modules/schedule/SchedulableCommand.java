package net.artifactgaming.carlbot.modules.schedule;

import net.artifactgaming.carlbot.Command;

public interface SchedulableCommand extends Command {
    void InvokeCommand(String guildID, String channelID, String inputRawString);
}
