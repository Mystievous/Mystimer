package io.github.mystievous.mystimer;

import org.bukkit.Bukkit;

import java.time.Duration;
import java.util.function.Consumer;

public class TimeTrigger {

    private final String id;
    private final Duration time;
    private final Consumer<Timer> consumer;
    private boolean triggered;

    public TimeTrigger(String id, Duration time, Consumer<Timer> consumer) {
        this.id = id;
        this.time = time;
        this.consumer = consumer;
        this.triggered = false;
    }

    public void disable() {
        this.triggered = false;
    }

    public Duration getTime() {
        return time;
    }

    public void check(Timer timer) {
        if (!triggered && timer.getTimeLeft().compareTo(time) <= 0) {
            TimeTriggerEvent event = new TimeTriggerEvent(timer, id);
            Bukkit.getPluginManager().callEvent(event);
            consumer.accept(timer);
            triggered = true;
        }
    }

    public void reset() {
        triggered = false;
    }

}
