package com.github.axiom.ac.world;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Server-side cache of known block collision states. Uncached blocks
 * read back as {@link BlockState#UNKNOWN}. Populated by callers from
 * block updates and chunk data.
 *
 * <p>Thread-safe: block updates arrive on the server thread while
 * lookups happen on netty threads.
 */
public final class WorldCache {

    private final Map<BlockPos, BlockState> blocks = new ConcurrentHashMap<>();

    /**
     * Records the collision state of {@code pos}. Setting
     * {@link BlockState#UNKNOWN} forgets the block.
     */
    public void setBlock(BlockPos pos, BlockState state) {
        Objects.requireNonNull(pos, "pos");
        Objects.requireNonNull(state, "state");
        if (state.isUnknown()) {
            blocks.remove(pos);
        } else {
            blocks.put(pos, state);
        }
    }

    /** The cached state of {@code pos}, or {@code UNKNOWN} if absent. */
    public BlockState blockAt(BlockPos pos) {
        return blocks.getOrDefault(pos, BlockState.UNKNOWN);
    }

    /**
     * True when {@code pos} is cached and carries collision. The name
     * is kept for compatibility; with shaped blocks it now means
     * "collidable", not strictly a full cube.
     */
    public boolean isSolid(BlockPos pos) {
        return blockAt(pos).hasCollision();
    }

    /** Forgets every cached block. */
    public void clear() {
        blocks.clear();
    }

    /** Number of cached (non-{@code UNKNOWN}) blocks. */
    public int size() {
        return blocks.size();
    }
}
