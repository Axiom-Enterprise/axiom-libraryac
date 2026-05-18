package com.github.axiom.ac.packet;

import com.github.axiom.ac.api.PlayerData;
import com.github.axiom.ac.math.RollingBuffer;
import com.github.axiom.ac.math.Vec3;
import java.util.Objects;
import java.util.UUID;

/**
 * Concrete {@link PlayerData}. Mutable and thread-confined: it is
 * updated only from the owning player's netty thread, so it carries
 * no synchronisation.
 */
public final class PlayerDataImpl implements PlayerData {

    private static final Vec3 ORIGIN = new Vec3(0, 0, 0);
    private static final int HISTORY_CAPACITY = 20;

    private final UUID uuid;
    private final RollingBuffer<Vec3> positionHistory = new RollingBuffer<>(HISTORY_CAPACITY);

    private Vec3 position = ORIGIN;
    private Vec3 previousPosition = ORIGIN;
    private Vec3 velocity = ORIGIN;
    private float yaw;
    private float pitch;
    private boolean onGround;

    public PlayerDataImpl(UUID uuid) {
        this.uuid = Objects.requireNonNull(uuid, "uuid");
    }

    /**
     * Advances state from one decoded movement packet. A position
     * update shifts current to previous, recomputes velocity as the
     * delta, and appends to the position history. A movement packet
     * that carries no position means the player did not move that
     * tick, so velocity is reset to zero. A rotation update sets the
     * look angle. The ground flag is always applied.
     *
     * <p>The velocity after the very first position update is the
     * delta from the origin and is not meaningful — checks should
     * discard the first sample.
     */
    public void applyMovement(MovementUpdate update) {
        if (update.hasPosition()) {
            previousPosition = position;
            position = update.position();
            velocity = position.subtract(previousPosition);
            positionHistory.add(position);
        } else {
            velocity = ORIGIN;
        }
        if (update.hasRotation()) {
            yaw = update.yaw();
            pitch = update.pitch();
        }
        onGround = update.onGround();
    }

    @Override
    public UUID uuid() {
        return uuid;
    }

    @Override
    public Vec3 position() {
        return position;
    }

    @Override
    public Vec3 velocity() {
        return velocity;
    }

    @Override
    public float yaw() {
        return yaw;
    }

    @Override
    public float pitch() {
        return pitch;
    }

    @Override
    public boolean onGround() {
        return onGround;
    }

    /** Position recorded before the most recent position update. */
    public Vec3 previousPosition() {
        return previousPosition;
    }

    /** Bounded history of recent positions, oldest first. */
    public RollingBuffer<Vec3> positionHistory() {
        return positionHistory;
    }
}
