package com.starseekstudios.mystimer;

import java.time.Duration;
import java.util.function.Consumer;

public class ScheduledAction {

    private final Duration time;
    private final Consumer<Timer> timeAction;

    public ScheduledAction(Duration time, Consumer<Timer> timeAction) {
        this.time = time;
        this.timeAction = timeAction;
    }

    public Duration getTime() {
        return time;
    }

    public void runTimeAction(Timer timer) {
        timeAction.accept(timer);
    }

}
