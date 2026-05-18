package com.github.axiom.ac.packet;

import com.github.axiom.ac.api.PlayerData;
import com.github.axiom.ac.math.RollingBuffer;
import com.github.axiom.ac.math.Rotation;
import com.github.axiom.ac.math.Vec3;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * Concrete {@link PlayerData}. Mutable and thread-confined: it is
 * updated only from the owning player's netty thread, so it carries
 * no synchronisation.
 *
 * <p>It tracks both a position history and a rotation history, the
 * latter feeding reach and aim analysis: every packet that carries a
 * look angle appends to the rotation history and records the prior
 * angle, so a check can read the per-tick yaw and pitch deltas.
 */
public final class PlayerDataImpl implements PlayerData {

    private static final Vec3 ORIGIN = new Vec3(0, 0, 0);
    private static final int HISTORY_CAPACITY = 20;

    private final UUID uuid;
    private final RollingBuffer<Vec3> positionHistory = new RollingBuffer<>(HISTORY_CAPACITY);
    private final RollingBuffer<Rotation> rotationHistory =
            new RollingBuffer<>(HISTORY_CAPACITY);

    private Vec3 position = ORIGIN;
    private Vec3 previousPosition = ORIGIN;
    private Vec3 velocity = ORIGIN;
    private float yaw;
    private float pitch;
    private float previousYaw;
    private float previousPitch;
    private boolean onGround;

    public PlayerDataImpl(UUID uuid) {
        this.uuid = Objects.requireNonNull(uuid, "uuid");
    }

    /**
     * Advances state from one decoded movement packet. A position
     * update shifts current to previous, recomputes velocity as the
     * delta, and appends to the position history. A movement packet
     * that carries no position means the player did not move that
     * tick, so velocity is reset to zero. A rotation update shifts
     * the current look angle to previous, sets the new one, and
     * appends to the rotation history. The ground flag is always
     * applied.
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
            previousYaw = yaw;
            previousPitch = pitch;
            yaw = update.yaw();
            pitch = update.pitch();
            rotationHistory.add(new Rotation(yaw, pitch));
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

    @Override
    public Rotation rotation() {
        return new Rotation(yaw, pitch);
    }

    @Override
    public Rotation previousRotation() {
        return new Rotation(previousYaw, previousPitch);
    }

    /** Position recorded before the most recent position update. */
    public Vec3 previousPosition() {
        return previousPosition;
    }

    /** Bounded history of recent positions, oldest first. */
    public RollingBuffer<Vec3> positionHistory() {
        return positionHistory;
    }

    /**
     * Bounded history of recent look angles, oldest first. Empty
     * until the first rotation packet.
     */
    @Override
    public List<Rotation> rotationHistory() {
        return rotationHistory.toList();
    }

    /** Bounded rotation history as its backing buffer. */
    public RollingBuffer<Rotation> rotationBuffer() {
        return rotationHistory;
    }

    /**
     * Signed, wrapped yaw change applied by the most recent rotation
     * packet — zero before any rotation has been seen.
     */
    public double lastYawDelta() {
        return previousRotation().yawDelta(rotation());
    }

    /**
     * Signed pitch change applied by the most recent rotation
     * packet — zero before any rotation has been seen.
     */
    public double lastPitchDelta() {
        return previousRotation().pitchDelta(rotation());
    }
}
