package net.artifactgaming.carlbot;

import net.dv8tion.jda.core.events.message.MessageReceivedEvent;

public interface CommandPermissionChecker {
    /**
     * A callback that can be used by any module to check if a command should be allowed to run.
     * If you wish to send the user who issued the command a response message, this callback is responsible for that.
     * @param event The message event that issued the command.
     * @return true to let the command run, false to cancel it.
     */
    boolean checkPermission(MessageReceivedEvent event, Command command);
}
