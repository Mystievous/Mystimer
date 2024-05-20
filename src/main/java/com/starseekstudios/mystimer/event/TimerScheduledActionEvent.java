package com.starseekstudios.mystimer.event;

import com.starseekstudios.mystimer.ScheduledAction;
import com.starseekstudios.mystimer.Timer;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

public class TimerScheduledActionEvent extends Event implements Cancellable {

    private static final HandlerList HANDLERS = new HandlerList();

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }

    @Override
    public @NotNull HandlerList getHandlers() {
        return HANDLERS;
    }

    private final Timer timer;
    private final ScheduledAction scheduledAction;
    private boolean cancelled;

    public TimerScheduledActionEvent(Timer timer, ScheduledAction scheduledAction) {
        this.timer = timer;
        this.scheduledAction = scheduledAction;
    }

    public ScheduledAction getScheduledAction() {
        return scheduledAction;
    }

    public Timer getTimer() {
        return timer;
    }

    @Override
    public boolean isCancelled() {
        return cancelled;
    }

    @Override
    public void setCancelled(boolean cancelled) {
        this.cancelled = cancelled;
    }
}
