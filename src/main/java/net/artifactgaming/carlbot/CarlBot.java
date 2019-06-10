package net.artifactgaming.carlbot;

import net.artifactgaming.carlbot.modules.Echo;
import net.artifactgaming.carlbot.listeners.MessageReader;
import net.artifactgaming.carlbot.modules.Quotes.Quotes;
import net.artifactgaming.carlbot.modules.authority.AuthorityManagement;
import net.artifactgaming.carlbot.modules.persistence.Persistence;
import net.dv8tion.jda.core.AccountType;
import net.dv8tion.jda.core.JDABuilder;
import net.dv8tion.jda.core.events.message.MessageReceivedEvent;
import net.dv8tion.jda.core.hooks.ListenerAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.security.auth.login.LoginException;
import java.io.IOException;
import java.nio.charset.Charset;
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

    private final String callsign = "$>";

    public static void main(String[] args) throws Exception {

        CarlBot bot = new CarlBot();
        bot.getTokenFromFile("./botToken.txt");

        bot.addModule(new Echo());
        bot.addModule(new Quotes());
        bot.addModule(new AuthorityManagement());
        bot.addModule(new Persistence());

        bot.run();
    }

    public void setToken(String token) {
        this.token = token;
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

    public final List<Module> getModules() {
        return modules;
    }

    public Module getModule(Class moduleClass) {
        return moduleLookup.get(moduleClass);
    }

    private static String readFile(String path, Charset encoding)
            throws IOException
    {
        byte[] encoded = Files.readAllBytes(Paths.get(path));

        // We have to remove the newline at the end of the file.
        String string = new String(encoded, encoding);
        return string.substring(0, string.length() - 1);
    }

    public void getTokenFromFile(String path) throws IOException {
        token = readFile(path, StandardCharsets.UTF_8);
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
