package net.artifactgaming.carlbot;

import net.dv8tion.jda.core.entities.Member;
import net.dv8tion.jda.core.entities.Message;
import net.dv8tion.jda.core.entities.Role;
import net.dv8tion.jda.core.entities.User;
import net.dv8tion.jda.core.events.message.MessageReceivedEvent;

import java.util.List;

public class Utils {

    public final static String STRING_EMPTY = "";

    public final static String CALLSIGN = "$>";

    /**
     * Removes @everyone and @here pings from messages.
     * @param sender User who sent the message.
     * @param message The raw content of the message itself.
     * @return The cleaned version of the message.
     */
    public static String cleanMessage(User sender, String message) {

        message = message.replace("@everyone", "@.everyone");
        message = message.replace("@here","@.here");

        return message;
    }

    /**
     * Gets a member object from the mention or name with discriminator tag.
     * @param event The event containing the full message.
     * @param memberId The token containing the mention or name with discriminator tag.
     * @return The member or null if the member does not exist or the format is invalid.
     */
    public static Member getMemberFromMessage(MessageReceivedEvent event, String memberId) {

        List<Member> mentionedMembers = event.getMessage().getMentionedMembers();

        Member member = null;

        // Find by direct mention.
        for (Member memberToCheck : mentionedMembers) {
            if (memberId.contains(memberToCheck.getUser().getId())) {
                member = memberToCheck;
                break;
            }
        }

        // Find by same name.
        if (member == null) {
            for (Member memberToCheck : event.getGuild().getMembers()) {
                String nameToCheck =
                        memberToCheck.getUser().getName() + "#" + memberToCheck.getUser().getDiscriminator();

                if (memberId.equals(nameToCheck)) {
                    member = memberToCheck;
                    break;
                }
            }
        }

        return member;
    }

    public static ObjectResult<Integer> tryParseInteger(String value){
        try {
            int result = Integer.parseInt(value);
            return new ObjectResult<>(result);
        } catch (NumberFormatException e) {
            return new ObjectResult<>(null);
        }
    }

    /**
     * Gets a role object from the mention or name.
     * @param event The event containing the full message.
     * @param roleId The token containing the mention or role name.
     * @return The role or null if the role does not exist or the format is invalid.
     */
    public static Role getRoleFromMessage(MessageReceivedEvent event, String roleId) {
        Role role = null;

        for (Role roleToCheck : event.getGuild().getRoles()) {
            if (roleId.equals(roleToCheck.getAsMention())
             || roleId.equals(roleToCheck.getName())) {
                role = roleToCheck;
                break;
            }
        }

        // Is it the everyone role?
        if (role == null && roleId.equals("everyone")) {
            role = event.getGuild().getPublicRole();
        }

        return role;
    }

    /**
     * Gets the member or role snowflake from a message.
     * @param event The message event that references the member or role.
     * @param token The token in the message containing the mention or role.
     * @return The snowflake of the member or role, or null if it could not be determined.
     */
    public static String getMemberOrRoleFromMessage(MessageReceivedEvent event, String token) {
        String discordId = null;
        Member member = Utils.getMemberFromMessage(event, token);

        if (member != null) {
            discordId = member.getUser().getId();
        } else {
            Role role = Utils.getRoleFromMessage(event, token);

            if (role != null) {
                discordId = role.getId();
            }
        }

        return discordId;
    }
}
