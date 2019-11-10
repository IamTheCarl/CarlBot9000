package net.artifactgaming.carlbot.listeners;

import net.dv8tion.jda.core.events.guild.member.*;

/**
 * The respective functions are invoked when the respective events are occured.
 * Make sure to add this event-listener to the CarlBot.
 */
public interface OnGuildMember {
    void onGuildMemberJoin(GuildMemberJoinEvent event);
    void onGuildMemberLeave(GuildMemberLeaveEvent event);
    void onGuildMemberRoleAdd(GuildMemberRoleAddEvent event);
    void onGuildMemberRoleRemove(GuildMemberRoleRemoveEvent event);
    void onGuildMemberNickChange(GuildMemberNickChangeEvent event);
}
