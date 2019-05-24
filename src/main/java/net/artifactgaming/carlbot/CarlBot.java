package net.artifactgaming.carlbot;

import net.artifactgaming.carlbot.modules.Echo;
import net.artifactgaming.carlbot.modules.listeners.MessageReader;
import net.dv8tion.jda.core.AccountType;
import net.dv8tion.jda.core.JDABuilder;
import net.dv8tion.jda.core.events.message.MessageReceivedEvent;
import net.dv8tion.jda.core.hooks.ListenerAdapter;

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

    private ArrayList<MessageReader> messageReaders = new ArrayList<>();
    private HashMap<String, Command> commands = new HashMap<>();

    private String callsign = "$>";

    public static void main(String[] args) throws IOException {
        CarlBot bot = new CarlBot();
        bot.getTokenFromFile("./botToken.txt");

        bot.addModule(new Echo());

        bot.run();
    }

    public void setToken(String token) {
        this.token = token;
    }

    public void addModule(Module module) {
        modules.add(module);
        System.out.println("Added module: " + module.getClass().getCanonicalName());

        for (Command command : module.getCommands()) {
            String callsign = command.getCallsign();
            commands.put(callsign, command);

            System.out.println("Added command with callsign: " + callsign);
        }

        if (module instanceof MessageReader) {
            messageReaders.add((MessageReader) module);
        }
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

    @Override
    public void run() {
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

                // Need to actually have a command.
                if (tokens.size() > 0) {
                    String callsign = tokens.get(0);

                    Command command = commands.get(callsign);

                    // Find command.
                    if (command != null) {
                        command.runCommand(event, rawContent, tokens);
                    } else {
                        event.getChannel().sendMessage("Error: Unknown command \"" + callsign + "\".").queue();
                    }

                } else {
                    event.getChannel().sendMessage("Error: No command name given.").queue();
                }
            }
        }
    }
}
