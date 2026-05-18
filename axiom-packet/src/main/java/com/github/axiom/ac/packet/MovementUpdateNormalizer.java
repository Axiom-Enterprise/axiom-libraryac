package com.github.axiom.ac.packet;

import com.github.axiom.ac.math.Rotation;
import com.github.axiom.ac.math.Vec3;
import java.util.Objects;

/**
 * Normalizes the partial movement packets a client sends into a
 * stream of complete ones.
 *
 * <p>A Minecraft movement packet may carry a position, a rotation,
 * both, or only a ground flag. A check that wants the player's full
 * state every tick would otherwise have to track the carried-forward
 * fields itself. This normalizer does it once: it remembers the last
 * known position and look angle and fills whatever an incoming packet
 * omits, so every result has both a position and a rotation.
 *
 * <p>It also canonicalises the look angle — wrapping yaw into
 * {@code [-180, 180)} and clamping pitch to {@code [-90, 90]} — so a
 * client cannot smuggle out-of-range angles past rotation checks.
 *
 * <p>One instance belongs to one connection and is updated only from
 * that connection's netty thread; it carries no synchronisation.
 */
public final class MovementUpdateNormalizer {

    private Vec3 position;
    private float yaw;
    private float pitch;
    private boolean seenPosition;
    private boolean seenRotation;

    /**
     * A normalizer with no prior state. Until the first packet that
     * carries each field, an absent position reads back as the origin
     * and an absent rotation as a zero angle.
     */
    public MovementUpdateNormalizer() {
        this.position = new Vec3(0, 0, 0);
    }

    /**
     * Returns {@code raw} as a complete update: any position or
     * rotation it omits is filled from the last one seen, and the
     * rotation is canonicalised. The carried-forward state is then
     * advanced from the (filled) result.
     *
     * @param raw a decoded, possibly partial movement packet
     * @return a packet with both {@code hasPosition} and
     *         {@code hasRotation} set
     */
    public MovementUpdate normalize(MovementUpdate raw) {
        Objects.requireNonNull(raw, "raw");

        if (raw.hasPosition()) {
            position = raw.position();
            seenPosition = true;
        }
        if (raw.hasRotation()) {
            Rotation canonical = new Rotation(raw.yaw(), raw.pitch()).normalized();
            yaw = canonical.yaw();
            pitch = canonical.pitch();
            seenRotation = true;
        }
        return new MovementUpdate(true, position, true, yaw, pitch, raw.onGround());
    }

    /** Whether a position has been seen since construction. */
    public boolean hasPosition() {
        return seenPosition;
    }

    /** Whether a rotation has been seen since construction. */
    public boolean hasRotation() {
        return seenRotation;
    }

    /** The last known position; the origin before the first position packet. */
    public Vec3 position() {
        return position;
    }

    /** The last known look angle; a zero angle before the first rotation packet. */
    public Rotation rotation() {
        return new Rotation(yaw, pitch);
    }
}
