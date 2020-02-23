package net.artifactgaming.carlbot.modules.danbooru.WebHandler;

public class EmptyResultException extends Exception {
    public EmptyResultException() { super(); }
    public EmptyResultException(String message) { super(message); }
    public EmptyResultException(String message, Throwable cause) { super(message, cause); }
    public EmptyResultException(Throwable cause) { super(cause); }
}
