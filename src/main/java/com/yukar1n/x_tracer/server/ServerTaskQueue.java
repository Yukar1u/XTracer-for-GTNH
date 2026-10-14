package com.yukar1n.x_tracer.server;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

public class ServerTaskQueue {
    private final Queue<Runnable> tasks = new ConcurrentLinkedQueue<>();

    public void enqueue(Runnable task) {
        if (task != null) tasks.offer(task);
    }

    @SubscribeEvent
    public void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        LocalizedNameService.tick();
        Runnable task;
        while ((task = tasks.poll()) != null) {
            try {
                task.run();
            } catch (Throwable error) {
                error.printStackTrace();
            }
        }
    }
}
