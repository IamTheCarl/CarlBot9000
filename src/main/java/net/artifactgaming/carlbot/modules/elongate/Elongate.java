package net.artifactgaming.carlbot.modules.elongate;

import net.artifactgaming.carlbot.CarlBot;
import net.artifactgaming.carlbot.Command;
import net.artifactgaming.carlbot.Module;
import net.artifactgaming.carlbot.modules.selfdocumentation.Documented;
import net.dv8tion.jda.core.events.message.MessageReceivedEvent;
import net.dv8tion.jda.core.requests.restaction.MessageAction;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import net.sf.json.JSONSerializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;

public class Elongate implements Module, Documented {

    private Logger logger = LoggerFactory.getLogger(Elongate.class);

    private ArrayList<String> soundEffects = new ArrayList<>();
    private HashMap<String, Elongatable> elongables = new HashMap<>();

    @Override
    public void setup(CarlBot carlbot) {
        try {
            loadConfig(new File("./assets/elongate/config.json"));
        } catch (IOException e) {
            logger.error("Fatal error during setup: ", e);
            e.printStackTrace();
        }
    }

    private static String readFile(String path)
            throws IOException
    {
        byte[] encoded = Files.readAllBytes(Paths.get(path));
        return new String(encoded, StandardCharsets.UTF_8);
    }

    private void loadConfig(File file) throws IOException {
        String rawJson = readFile(file.getPath());
        JSONObject json = (JSONObject) JSONSerializer.toJSON(rawJson);

        JSONArray elongables = json.getJSONArray("elongables");
        if (elongables != null) {
            for (int i = 0; i < elongables.size(); i++) {
                JSONObject elongable = elongables.getJSONObject(i);

                if (elongable != null) {
                    String name = elongable.getString("name");
                    String creator = elongable.getString("creator");
                    String nickname = elongable.getString("creator-nickname");
                    String website = elongable.getString("creator-website");

                    ArrayList<File> images = new ArrayList<>();
                    JSONArray imageArray = elongable.getJSONArray("images");

                    for (int j = 0; j < imageArray.size(); j++) {
                        String pathString = imageArray.getString(j);
                        if (pathString != null) {
                            File path = new File("./assets/elongate/" + pathString);
                            if (path.exists()) {
                                images.add(path);
                                logger.info("Added elongation image: " + path);
                            } else {
                                logger.warn("Could not find elongation image: " + path);
                            }
                        } else {
                            logger.warn("Failed to load path strings because one was not a string.");
                            images = null; // This will make us fail gracefully.
                            break;
                        }
                    }

                    if (name != null && creator != null && website != null && images != null) {
                        this.elongables.put(name, new Elongatable(creator, nickname, website, images));
                    } else {
                        logger.error("Not all required data for elongation was provided.");
                    }
                }
            }
        } else {
            logger.error("Failed to load elongatables. Entree does not exist in json file.");
        }

        JSONArray soundEffects = json.getJSONArray("sound-effects");
        if (soundEffects != null) {
            for (int i = 0; i < soundEffects.size(); i++) {
                String effect = soundEffects.getString(i);
                if (effect != null) {
                    this.soundEffects.add(effect);
                    logger.info("Loaded sound effect: " + effect);
                } else {
                    logger.error("Failed to load sound effect " + i + " because it was not a string.");
                }
            }
        } else {
            logger.error("Failed to load sound effects. Did not find json array \"sound-effects\".");
        }
    }

    @Override
    public Command[] getCommands(CarlBot carlbot) {
        return new Command[] {new ElongateCommand()};
    }

    @Override
    public String getDocumentation() {

        StringBuilder elongatableNames = new StringBuilder();

        for (String name : this.elongables.keySet()) {
            elongatableNames.append(name).append("\n");
        }

        return "Elongate your favorite 2hus. Just type `$>elongate [2hu name here]` and it'll do it.\n" +
                "Contact @IamtheCarl2 on Twitter if you want to contribute more elongatable 2hus. Remember that " +
                "permission from the elongation artwork's creator must be given for it to be used.\n" +
                "List of elongatable 2hus:\n```" +
                elongatableNames +
                "```";
    }

    @Override
    public String getDocumentationCallsign() {
        return "elongate";
    }

    private class ElongateCommand implements Command {

        @Override
        public String getCallsign() {
            return "elongate";
        }

        @Override
        public void runCommand(MessageReceivedEvent event, String rawString, List<String> tokens) throws Exception {
            // event.getChannel().sendMessage("BWAAAANG!").queue();

            String name = tokens.get(0);
            Elongatable elongatable = null;
            if (name != null) {
                elongatable = Elongate.this.elongables.get(name);
            }

            if (elongatable != null) {
                MessageAction message = event.getChannel().sendMessage(soundEffects.get((int)(Math.random() * soundEffects.size())));

                for (File file : elongatable.images) {
                    message = message.addFile(file);
                    message.queue();
                    message = event.getChannel().sendMessage(" ");
                }

                String credit = "Thank you " + elongatable.creator;
                if (elongatable.nickname != null) {
                    credit += " (" + elongatable.nickname + ")";
                }
                credit += " from " + elongatable.website + " for contributing this elongation.";

                event.getChannel().sendMessage(credit).queue();
            } else {
                event.getChannel().sendMessage("Provide a name for a 2hu to be elongated. " +
                        "Type `$>help elongate` to get a list of elongatable 2hus.").queue();
            }
        }

        @Override
        public Module getParentModule() {
            return Elongate.this;
        }
    }

    private static class Elongatable {
        String creator;
        String nickname;
        String website;

        ArrayList<File> images;

        Elongatable(
                String creator,
                String nickname,
                String website,
                ArrayList<File> images) {

            this.creator = creator;
            this.nickname = nickname;
            this.website = website;

            this.images = images;

        }
    }
}
