package com.github.axiom.ac.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.github.axiom.ac.math.Vec3;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class CheckTest {

    private record FakePlayerData(UUID uuid, Vec3 position, Vec3 velocity,
                                  float yaw, float pitch, boolean onGround)
            implements PlayerData {
    }

    /** Flags when horizontal speed exceeds a fixed threshold. */
    private static final class SpeedCheck implements Check {
        @Override
        public String id() {
            return "speed";
        }

        @Override
        public Optional<Violation> inspect(PlayerData data) {
            double horizontal = Math.hypot(data.velocity().x(), data.velocity().z());
            if (horizontal > 1.0) {
                return Optional.of(new Violation(id(), "too fast", horizontal, 1.0));
            }
            return Optional.empty();
        }
    }

    private PlayerData withVelocity(Vec3 velocity) {
        return new FakePlayerData(UUID.randomUUID(), new Vec3(0, 0, 0),
                velocity, 0.0f, 0.0f, true);
    }

    @Test
    void checkExposesItsId() {
        assertEquals("speed", new SpeedCheck().id());
    }

    @Test
    void checkFlagsExcessiveSpeed() {
        Optional<Violation> result = new SpeedCheck().inspect(withVelocity(new Vec3(2, 0, 0)));
        assertTrue(result.isPresent());
        assertEquals("speed", result.get().checkId());
    }

    @Test
    void checkPassesNormalSpeed() {
        Optional<Violation> result = new SpeedCheck().inspect(withVelocity(new Vec3(0.1, 0, 0)));
        assertTrue(result.isEmpty());
    }
}
