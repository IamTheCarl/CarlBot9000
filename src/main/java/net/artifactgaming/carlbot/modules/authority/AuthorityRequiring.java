package net.artifactgaming.carlbot.modules.authority;

public interface AuthorityRequiring {
    /**
     * Used to indicate which authorities are required to use this command or module.
     * @return A list of classes that implement the Authority interface.
     */
    Authority[] getRequiredAuthority();
}
