package net.artifactgaming.carlbot;

import net.dv8tion.jda.core.events.message.MessageReceivedEvent;

import java.util.List;

public interface Command {
    String getCallsign();
    void runCommand(MessageReceivedEvent event, String rawString, List<String> tokens) throws Exception;
    Module getParentModule();
}
