package net.artifactgaming.carlbot.modules.danbooru.WebHandler;

public class InvalidTagsException extends Exception {
    public InvalidTagsException() { super(); }
    public InvalidTagsException(String message) { super(message); }
    public InvalidTagsException(String message, Throwable cause) { super(message, cause); }
    public InvalidTagsException(Throwable cause) { super(cause); }
}
