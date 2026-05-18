package com.github.axiom.ac.plugin;

import com.github.axiom.ac.core.AxiomProvider;
import com.github.axiom.ac.core.AxiomRuntime;
import com.github.axiom.ac.core.MemoryStorageProvider;
import com.github.axiom.ac.packet.PacketPipeline;
import com.github.retrooper.packetevents.PacketEvents;
import io.github.retrooper.packetevents.factory.spigot.SpigotPacketEventsBuilder;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Paper plugin entry point. Bootstraps {@link AxiomRuntime} as a
 * shared singleton, wires the PacketEvents pipeline, and bridges
 * Bukkit player join/quit into the runtime.
 */
public final class AxiomPlugin extends JavaPlugin implements Listener {

    private AxiomRuntime runtime;

    @Override
    public void onLoad() {
        PacketEvents.setAPI(SpigotPacketEventsBuilder.build(this));
        PacketEvents.getAPI().load();
    }

    @Override
    public void onEnable() {
        PacketEvents.getAPI().init();

        runtime = new AxiomRuntime(new MemoryStorageProvider());
        AxiomProvider.set(runtime);

        PacketEvents.getAPI().getEventManager()
                .registerListener(new PacketPipeline(runtime.players()));
        getServer().getPluginManager().registerEvents(this, this);

        getLogger().info("Axiom AC enabled.");
    }

    @Override
    public void onDisable() {
        AxiomProvider.clear();
        if (PacketEvents.getAPI() != null) {
            PacketEvents.getAPI().terminate();
        }
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        runtime.handlePlayerJoin(event.getPlayer().getUniqueId());
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        runtime.handlePlayerQuit(event.getPlayer().getUniqueId());
    }
}
