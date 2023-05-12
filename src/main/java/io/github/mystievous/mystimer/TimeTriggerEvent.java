package io.github.mystievous.mystimer;

import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

public class TimeTriggerEvent extends Event {

    private static final HandlerList HANDLERS = new HandlerList();

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }

    @Override
    public @NotNull HandlerList getHandlers() {
        return HANDLERS;
    }

    private final Timer timer;
    private final String triggerId;

    public TimeTriggerEvent(Timer timer, String triggerId) {
        this.timer = timer;
        this.triggerId = triggerId;
    }

    public String getTriggerId() {
        return triggerId;
    }

    public Timer getTimer() {
        return timer;
    }
}
