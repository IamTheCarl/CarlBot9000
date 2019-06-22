package net.artifactgaming.carlbot;

public class ObjectResult<T> {
    private T object;

    private boolean result;

    private String resultMessage;

    public ObjectResult(T object){
        this.object = object;
        resultMessage = Utils.STRING_EMPTY;
    }

    public ObjectResult(T object, String resultMessage){
        this.object = object;
        this.resultMessage = resultMessage;
    }

    public T getObject() {
        return object;
    }

    public boolean getResult() {
        return object != null;
    }

    public String getResultMessage(){
        return resultMessage;
    }
}
