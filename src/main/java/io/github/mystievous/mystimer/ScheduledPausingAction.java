package io.github.mystievous.mystimer;

import java.time.Duration;
import java.util.function.Consumer;

public class ScheduledPausingAction extends ScheduledAction {

    private Consumer<Timer> resumeAction;

    public ScheduledPausingAction(Duration time, Consumer<Timer> timeAction) {
        super(time, timeAction);
    }

    public void setResumeAction(Consumer<Timer> resumeAction) {
        this.resumeAction = resumeAction;
    }

    public void runResumeAction(Timer timer) {
        resumeAction.accept(timer);
    }

    @Override
    public void runTimeAction(Timer timer) {
        timer.pauseTimer();
        super.runTimeAction(timer);
    }
}
