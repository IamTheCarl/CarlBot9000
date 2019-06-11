package net.artifactgaming.carlbot;

import net.artifactgaming.carlbot.modules.Echo;
import net.artifactgaming.carlbot.listeners.MessageReader;
import net.artifactgaming.carlbot.modules.Quotes.Quotes;
import net.artifactgaming.carlbot.modules.authority.AuthorityManagement;
import net.artifactgaming.carlbot.modules.persistence.Persistence;
import net.dv8tion.jda.core.AccountType;
import net.dv8tion.jda.core.JDABuilder;
import net.dv8tion.jda.core.entities.User;
import net.dv8tion.jda.core.events.message.MessageReceivedEvent;
import net.dv8tion.jda.core.hooks.ListenerAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.sf.json.JSONObject;
import net.sf.json.JSONSerializer;
import net.sf.json.JSONArray;

import javax.security.auth.login.LoginException;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class CarlBot extends ListenerAdapter implements Runnable {

    private String token = null;
    private ArrayList<Module> modules = new ArrayList<>();
    private HashMap<Class, Module> moduleLookup = new HashMap<>();

    private ArrayList<MessageReader> messageReaders = new ArrayList<>();
    ArrayList<CommandPermissionChecker> permissionCheckers = new ArrayList<>();

    private CommandHandler commands = new CommandHandler(this);

    private Logger logger = LoggerFactory.getLogger(CarlBot.class);

    private String callsign = "$>";
    private List<String> ownerIDs = new ArrayList<>();

    public static void main(String[] args) throws Exception {

        CarlBot bot = new CarlBot();
        bot.loadConfig(new File("./main_config.json"));

        bot.addModule(new Echo());
        bot.addModule(new Quotes());
        bot.addModule(new AuthorityManagement());
        bot.addModule(new Persistence());

        bot.run();
    }

    public void loadConfig(File file) throws IOException {
        String rawJson = readFile(file.getPath());
        JSONObject json = (JSONObject) JSONSerializer.toJSON(rawJson);

        token = json.getString("token");

        JSONArray owners = json.getJSONArray("owners");

        // There's no map feature, so we gotta unroll this ourselves.
        for (int i = 0; i < owners.size(); i++) {
            ownerIDs.add(owners.getString(i));
        }
    }

    public void addModule(Module module) {
        modules.add(module);
        moduleLookup.put(module.getClass(), module);

        logger.info("Added module: " + module.getClass().getCanonicalName());

        for (Command command : module.getCommands(this)) {
            String callsign = commands.addCommand(command);
            logger.info("Added command with callsign: " + callsign);
        }

        if (module instanceof MessageReader) {
            messageReaders.add((MessageReader) module);
        }
    }

    public boolean checkIsOwner(User user) {
        return ownerIDs.contains(user.getId());
    }

    public final List<Module> getModules() {
        return modules;
    }

    public Module getModule(Class moduleClass) {
        return moduleLookup.get(moduleClass);
    }

    private static String readFile(String path)
            throws IOException
    {
        byte[] encoded = Files.readAllBytes(Paths.get(path));
        return new String(encoded, StandardCharsets.UTF_8);

        // We have to remove the newline at the end of the file.
        //String string = new String(encoded, encoding);
        //return string.substring(0, string.length() - 1);
    }

    public void addCommandPermissionChecker(CommandPermissionChecker checker) {
        permissionCheckers.add(checker);
    }

    private void postSetupModules() {
        for (Module module : modules) {
            module.setup(this);
        }
    }

    @Override
    public void run() {

        postSetupModules();

        JDABuilder builder = new JDABuilder(AccountType.BOT);
        builder.setToken(token);

        builder.addEventListener(this);

        try {
            builder.buildAsync();
        } catch (LoginException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onMessageReceived(MessageReceivedEvent event) {
        // Always ignore messages from bots.
        if (!event.getAuthor().isBot()) {
            for (MessageReader reader : messageReaders) {
                reader.onMessageReceived(event);
            }

            String rawContent = event.getMessage().getContentRaw();
            if (rawContent.startsWith(callsign)) {
                // Cut off the callsign.
                String substring = rawContent.substring(callsign.length());
                List<String> tokens = ShellSplitter.shellSplit(substring);

                commands.runCommand(event, rawContent, tokens);
            }
        }
    }

    public void crash() {
        System.exit(-1);
    }
}
