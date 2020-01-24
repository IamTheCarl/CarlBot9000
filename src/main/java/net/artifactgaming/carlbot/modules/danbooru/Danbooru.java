package net.artifactgaming.carlbot.modules.danbooru;
import net.artifactgaming.carlbot.*;

import net.artifactgaming.carlbot.Module;
import net.artifactgaming.carlbot.modules.authority.Authority;
import net.artifactgaming.carlbot.modules.authority.AuthorityRequiring;
import net.artifactgaming.carlbot.modules.danbooru.Authority.ManageDanbooru;
import net.artifactgaming.carlbot.modules.danbooru.DanbooruDataModel.DanbooruChannel;
import net.artifactgaming.carlbot.modules.danbooru.DanbooruDataModel.Rating;
import net.artifactgaming.carlbot.modules.danbooru.DatabaseSQL.DanbooruDatabaseHandler;
import net.artifactgaming.carlbot.modules.persistence.Persistence;
import net.artifactgaming.carlbot.modules.persistence.PersistentModule;
import net.artifactgaming.carlbot.modules.selfdocumentation.Documented;
import net.artifactgaming.carlbot.modules.statistics.Statistics;
import net.dv8tion.jda.core.events.message.MessageReceivedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sun.nio.ch.Util;

import java.util.List;

public class Danbooru implements Module, Documented, PersistentModule {

    private Persistence persistence;

    private DanbooruDatabaseHandler danbooruDatabaseHandler;

    private Logger logger = LoggerFactory.getLogger(Danbooru.class);

    @Override
    public void setup(CarlBot carlbot) {
        // Get the persistence module.
        persistence = (Persistence) carlbot.getModule(Persistence.class);

        if (persistence == null) {
            logger.error("Persistence module is not loaded.");
            carlbot.crash();
        }

        danbooruDatabaseHandler = new DanbooruDatabaseHandler(persistence, this);
    }

    private class SetRatingCommand implements Command, Documented, AuthorityRequiring {

        @Override
        public String getCallsign() {
            return "rating";
        }

        @Override
        public void runCommand(MessageReceivedEvent event, String rawString, List<String> tokens) throws Exception {
            if (event.getGuild() == null){
                event.getTextChannel().sendMessage("This command can only be used in a guild!").queue();
                return;
            }

            if (tokens.size() == 0){
                event.getTextChannel().sendMessage("Set a rating for this channel's danbooru webhook! (S, Q, NSFW)").queue();
                return;
            }

            Rating givenRating = toRating(tokens.get(0));
            DanbooruChannel danbooruChannel = danbooruDatabaseHandler.getDanbooruChannel(event.getGuild(), event.getTextChannel());
            danbooruChannel.setMinAcceptableRating(givenRating);
            danbooruDatabaseHandler.updateDanbooruChannel(event.getGuild(), danbooruChannel);

            event.getTextChannel().sendMessage("This channel's minimum acceptable rating is set to " + givenRating + ".").queue();
        }

        @Override
        public Module getParentModule() {
            return Danbooru.this;
        }

        @Override
        public Authority[] getRequiredAuthority() {
            return new Authority[] {new ManageDanbooru()};
        }

        @Override
        public String getDocumentation() {
            return "Set the ratings which the webhook will fetch. (S, Q, NSFW)";
        }

        @Override
        public String getDocumentationCallsign() {
            return "rating";
        }

        private Rating toRating(String input){
            input = input.toUpperCase().trim();

            if (input.equals("QUESTIONABLE") || input.equals("Q")){
                return Rating.QUESTIONABLE;
            }
            if (input.equals("NSFW") || input.equals("NOT SAFE FOR WORK")){
                return Rating.NSFW;
            }

            return Rating.SAFE;
        }
    }

    private class SetTagsCommand implements Command, Documented, AuthorityRequiring {

        @Override
        public String getCallsign() {
            return "tags";
        }

        @Override
        public void runCommand(MessageReceivedEvent event, String rawString, List<String> tokens) throws Exception {
            if (event.getGuild() == null){
                event.getTextChannel().sendMessage("This command can only be used in a guild!").queue();
                return;
            }

            DanbooruChannel danbooruChannel = danbooruDatabaseHandler.getDanbooruChannel(event.getGuild(), event.getTextChannel());

            String tags = rawString.substring(Utils.CALLSIGN.length() + "danbooru tags ".length()).trim();

            danbooruChannel.setTags(tags);

            if (tags.isEmpty()){
                event.getTextChannel().sendMessage("Disabled danbooru webhook on this channel. (Tags was empty!)").queue();
                danbooruChannel.setActive(false);
            } else {
                event.getTextChannel().sendMessage("Tags set to: " + tags).queue();
            }

            danbooruDatabaseHandler.updateDanbooruChannel(event.getGuild(), danbooruChannel);
        }

        @Override
        public Module getParentModule() {
            return Danbooru.this;
        }

        @Override
        public Authority[] getRequiredAuthority() {
            return new Authority[] {new ManageDanbooru()};
        }

        @Override
        public String getDocumentation() {
            return "Set the danbooru tags for this channel";
        }

        @Override
        public String getDocumentationCallsign() {
            return "tags";
        }
    }

    private class ToggleCommand implements Command, Documented, AuthorityRequiring {

        @Override
        public String getCallsign() {
            return "toggle";
        }

        @Override
        public void runCommand(MessageReceivedEvent event, String rawString, List<String> tokens) throws Exception {
            if (event.getGuild() == null){
                event.getTextChannel().sendMessage("This command can only be used in a guild!").queue();
                return;
            }

            DanbooruChannel danbooruChannel = danbooruDatabaseHandler.getDanbooruChannel(event.getGuild(), event.getTextChannel());

            if (danbooruChannel.emptyTag()){
                event.getTextChannel().sendMessage("You need to configure a tag to search first!").queue();
                return;
            }

            boolean isActive = !danbooruChannel.isActive();
            danbooruChannel.setActive(isActive);

            danbooruDatabaseHandler.updateDanbooruChannel(event.getGuild(), danbooruChannel);
            event.getTextChannel().sendMessage("Danbooru webhook for this channel is now " + (isActive ? "enabled." : "disabled.")).queue();
        }

        @Override
        public Module getParentModule() {
            return Danbooru.this;
        }

        @Override
        public Authority[] getRequiredAuthority() {
            return new Authority[] {new ManageDanbooru()};
        }

        @Override
        public String getDocumentation() {
            return "toggle";
        }

        @Override
        public String getDocumentationCallsign() {
            return "Turn on/off the danbooru webhook for this channel!";
        }
    }

    private class InfoCommand implements Command, Documented {
        @Override
        public String getCallsign() {
            return "info";
        }

        @Override
        public void runCommand(MessageReceivedEvent event, String rawString, List<String> tokens) throws Exception {

            if (event.getGuild() == null){
                event.getTextChannel().sendMessage("This command can only be used in a guild!").queue();
                return;
            }

            DanbooruChannel danbooruChannel = danbooruDatabaseHandler.getDanbooruChannel(event.getGuild(), event.getTextChannel());

            if (danbooruChannel.emptyTag()){
                event.getTextChannel().sendMessage("This channel is not configured to have a danbooru webhook.").queue();
            } else {
                StringBuilder replyString = new StringBuilder();
                replyString.append("```" + Utils.NEWLINE);

                replyString.append("TAGS: \"").append(danbooruChannel.getTags()).append("\"").append(Utils.NEWLINE);
                replyString.append("MIN ACCEPTABLE RATING: \"").append(danbooruChannel.getMinAcceptableRating()).append("\"").append(Utils.NEWLINE);
                replyString.append("Is Active: ").append(danbooruChannel.isActive() ? "YES" : "NO").append(Utils.NEWLINE);

                replyString.append("```");

                event.getTextChannel().sendMessage(replyString.toString()).queue();
            }
        }

        @Override
        public Module getParentModule() {
            return Danbooru.this;
        }

        @Override
        public String getDocumentation() {
            return "Get information about danbooru webhook for this channel.";
        }

        @Override
        public String getDocumentationCallsign() {
            return "info";
        }

    }

    private class DanbooruCommands implements Command {
        private CommandHandler commands;

        DanbooruCommands(CarlBot carlbot) {
            commands = new CommandHandler(carlbot);

        }

        @Override
        public String getCallsign() {
            return "danbooru";
        }

        @Override
        public void runCommand(MessageReceivedEvent event, String rawString, List<String> tokens) throws Exception {
            commands.runCommand(event, rawString, tokens);
        }

        @Override
        public Module getParentModule() {
            return Danbooru.this;
        }

    }

    @Override
    public Command[] getCommands(CarlBot carlbot) {
        return new Command[0];
    }

    @Override
    public String getDocumentation() {
        return "danbooru";
    }

    @Override
    public String getDocumentationCallsign() {
        return "danbooru";
    }
}
