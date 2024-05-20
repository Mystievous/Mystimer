package com.starseekstudios.mystimer.exception;

public class TimerUnsetException extends Exception {

    public TimerUnsetException() {
        super("Timer is ended or unset.");
    }
}
