package net.artifactgaming.carlbot.modules.statistics;

import net.artifactgaming.carlbot.CarlBot;
import net.artifactgaming.carlbot.Command;
import net.artifactgaming.carlbot.Module;
import net.artifactgaming.carlbot.modules.authority.Authority;
import net.artifactgaming.carlbot.modules.authority.AuthorityManagement;
import net.artifactgaming.carlbot.modules.authority.AuthorityRequiring;
import net.artifactgaming.carlbot.modules.persistence.Persistence;
import net.artifactgaming.carlbot.modules.persistence.PersistentModule;
import net.artifactgaming.carlbot.modules.quotes.Quotes;
import net.artifactgaming.carlbot.modules.selfdocumentation.Documented;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;

public class Statistics implements Module, AuthorityRequiring, PersistentModule, Documented {

    private static final Path STATISTICS_PATH = Paths.get(new File(".").getAbsolutePath(), "etc", "Statistics");

    private Persistence persistence;

    private MessageStatisticCollector messageStatisticCollector;

    private Logger logger = LoggerFactory.getLogger(Quotes.class);

    @Override
    public Authority[] getRequiredAuthority() {
        return new Authority[0];
    }

    @Override
    public String getDocumentation() {
        return "Commands relating to collecting and displaying statistics on your server!";
    }

    @Override
    public String getDocumentationCallsign() {
        return "stats";
    }

    @Override
    public void setup(CarlBot carlbot) {
        // Get the persistence module.
        persistence = (Persistence) carlbot.getModule(Persistence.class);

        if (persistence == null) {
            logger.error("Persistence module is not loaded.");
            carlbot.crash();
        }

        messageStatisticCollector = new MessageStatisticCollector();
        // TODO: Start tracking guilds that want their data to be tracked.

        carlbot.addOnMessageReceivedListener(messageStatisticCollector);
    }

    @Override
    public Command[] getCommands(CarlBot carlbot) {
        return new Command[0];
    }
}
