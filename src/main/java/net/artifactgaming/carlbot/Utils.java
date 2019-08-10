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

    public final static String NEWLINE = "\r\n";

    ///region https://gist.github.com/jamietech/7c0b01be2ff6439c97fbec55e82daad5
    private final static String[] emojiLetters = new String[] { "\uD83C\uDDE6", "\uD83C\uDDE7", "\uD83C\uDDE8", "\uD83C\uDDE9", "\uD83C\uDDEA", "\uD83C\uDDEB", "\uD83C\uDDEC", "\uD83C\uDDED", "\uD83C\uDDEE", "\uD83C\uDDEF", "\uD83C\uDDF0", "\uD83C\uDDF1", "\uD83C\uDDF2", "\uD83C\uDDF3", "\uD83C\uDDF4", "\uD83C\uDDF5", "\uD83C\uDDF6", "\uD83C\uDDF7", "\uD83C\uDDF8", "\uD83C\uDDF9", "\uD83C\uDDFA", "\uD83C\uDDFB", "\uD83C\uDDFC", "\uD83C\uDDFD", "\uD83C\uDDFE", "\uD83C\uDDFF" };

    public static void reactMessageWithEmoji(Message message, String emojiName) {
        for (final char c : emojiName.toUpperCase().toCharArray()) {
            message.addReaction(emojiLetters[c - 64 - 1]).queue(); // 64 is char padding, 1 is array starting at 0
        }
    }

    ///endregion


    /**
     * Removes @everyone and @here pings from messages.
     * @param sender User who sent the message.
     * @param message The raw content of the message itself.
     * @return The cleaned version of the message.
     */
    @Deprecated
    public static String cleanMessage(User sender, String message) {
        message = message.replace("@everyone", "@.everyone");
        message = message.replace("@here","@.here");

        return message;
    }

    /**
     * Removes @everyone and @here pings from messages.
     * @param message The raw content of the message itself.
     * @return The cleaned version of the message.
     */
    public static String cleanMessage(String message){
        message = message.replace("@everyone", "@.everyone");
        message = message.replace("@here","@.here");

        return message;
    }

    public static String makeStringSQLFriendly(String stringVal){

        return stringVal.replace("\"", "''''");
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
