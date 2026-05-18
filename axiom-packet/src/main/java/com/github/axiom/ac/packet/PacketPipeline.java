package com.github.axiom.ac.packet;

import com.github.axiom.ac.math.Vec3;
import com.github.retrooper.packetevents.event.PacketListenerAbstract;
import com.github.retrooper.packetevents.event.PacketListenerPriority;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.protocol.packettype.PacketTypeCommon;
import com.github.retrooper.packetevents.protocol.world.Location;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPlayerFlying;
import java.util.UUID;

/**
 * PacketEvents listener that feeds decoded client movement packets
 * into the {@link PlayerRegistry}. This is the only PacketEvents-aware
 * type in the module; all decoding logic it depends on
 * ({@link MovementUpdate}, {@link PlayerDataImpl}) is PacketEvents-free.
 */
public final class PacketPipeline extends PacketListenerAbstract {

    private final PlayerRegistry registry;

    public PacketPipeline(PlayerRegistry registry) {
        super(PacketListenerPriority.NORMAL);
        this.registry = registry;
    }

    @Override
    public void onPacketReceive(PacketReceiveEvent event) {
        if (!isMovementPacket(event.getPacketType())) {
            return;
        }
        UUID uuid = event.getUser().getUUID();
        if (uuid == null) {
            return;
        }
        PlayerDataImpl data = registry.get(uuid).orElse(null);
        if (data == null) {
            return;
        }
        data.applyMovement(decode(new WrapperPlayClientPlayerFlying(event)));
    }

    /** True when {@code type} is one of the client movement packets. */
    private static boolean isMovementPacket(PacketTypeCommon type) {
        return type == PacketType.Play.Client.PLAYER_POSITION
                || type == PacketType.Play.Client.PLAYER_POSITION_AND_ROTATION
                || type == PacketType.Play.Client.PLAYER_ROTATION
                || type == PacketType.Play.Client.PLAYER_FLYING;
    }

    /** Decodes a flying-packet wrapper into a {@link MovementUpdate}. */
    static MovementUpdate decode(WrapperPlayClientPlayerFlying wrapper) {
        boolean hasPosition = wrapper.hasPositionChanged();
        boolean hasRotation = wrapper.hasRotationChanged();
        Location location = wrapper.getLocation();
        Vec3 position = hasPosition
                ? new Vec3(location.getX(), location.getY(), location.getZ())
                : null;
        return new MovementUpdate(hasPosition, position, hasRotation,
                location.getYaw(), location.getPitch(), wrapper.isOnGround());
    }
}
