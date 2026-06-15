package com.github.axiom.ac.plugin;

import com.github.axiom.ac.core.AxiomProvider;
import com.github.axiom.ac.core.AxiomRuntime;
import com.github.axiom.ac.core.JsonStorageProvider;
import com.github.axiom.ac.packet.PacketPipeline;
import com.github.retrooper.packetevents.PacketEvents;
import io.github.retrooper.packetevents.factory.spigot.SpigotPacketEventsBuilder;
import java.nio.file.Path;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Paper plugin entry point. Bootstraps {@link AxiomRuntime} as a
 * shared singleton, wires the PacketEvents pipeline, bridges Bukkit
 * player join/quit into the runtime, and drives the inspection pass.
 *
 * <p>Inspection runs on the netty thread, immediately after each
 * tracked player's movement packet is decoded — the thread on which
 * {@code Check}s are contractually safe to run. Violations are
 * persisted to {@code violations.json} in the plugin data folder.
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

        Path storageFile = getDataFolder().toPath().resolve("violations.json");
        getDataFolder().mkdirs();
        runtime = new AxiomRuntime(new JsonStorageProvider(storageFile));
        AxiomProvider.set(runtime);

        // Inspection runs per movement packet, on the netty thread.
        PacketEvents.getAPI().getEventManager()
                .registerListener(new PacketPipeline(runtime.players(), runtime::inspect));
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
        if (runtime == null) {
            return;
        }
        runtime.handlePlayerJoin(event.getPlayer().getUniqueId());
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        if (runtime == null) {
            return;
        }
        runtime.handlePlayerQuit(event.getPlayer().getUniqueId());
    }
}
