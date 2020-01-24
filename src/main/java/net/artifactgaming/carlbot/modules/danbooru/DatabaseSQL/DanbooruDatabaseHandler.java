package net.artifactgaming.carlbot.modules.danbooru.DatabaseSQL;

import net.artifactgaming.carlbot.modules.danbooru.Danbooru;
import net.artifactgaming.carlbot.modules.persistence.Persistence;
import net.artifactgaming.carlbot.modules.persistence.PersistentModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DanbooruDatabaseHandler {

    private Logger logger = LoggerFactory.getLogger(Danbooru.class);

    private Persistence persistenceRef;

    private PersistentModule persistentModuleRef;

    public DanbooruDatabaseHandler(Persistence _persistenceRef, PersistentModule _persistentModuleRef) {
        persistenceRef = _persistenceRef;
        persistentModuleRef = _persistentModuleRef;
    }
}
