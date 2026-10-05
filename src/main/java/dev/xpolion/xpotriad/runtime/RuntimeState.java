package dev.xpolion.xpotriad.runtime;

public interface RuntimeState {

    void tick();

    boolean isFinished();

    void stop();
}
