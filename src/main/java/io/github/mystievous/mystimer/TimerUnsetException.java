package io.github.mystievous.mystimer;

public class TimerUnsetException extends Exception {

    public TimerUnsetException() {
        super("Timer is ended or unset.");
    }
}
