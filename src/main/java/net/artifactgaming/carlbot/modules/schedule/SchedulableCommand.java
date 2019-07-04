package net.artifactgaming.carlbot.modules.schedule;

import net.artifactgaming.carlbot.Command;
import net.dv8tion.jda.core.entities.Channel;
import net.dv8tion.jda.core.entities.TextChannel;

import java.util.List;

public interface SchedulableCommand extends Command {
    void InvokeCommand(TextChannel channel, List<String> tokens);
}
