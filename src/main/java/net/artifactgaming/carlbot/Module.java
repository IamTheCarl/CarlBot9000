package net.artifactgaming.carlbot;

public interface Module {
    void setup(CarlBot carlbot);
    Command[] getCommands(CarlBot carlbot);
}
