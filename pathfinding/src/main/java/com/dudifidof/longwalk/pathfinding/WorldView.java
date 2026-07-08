package com.dudifidof.longwalk.pathfinding;

/**
 * Read-only view of the world the pathfinder needs. All positions refer to
 * the block a standing player's feet would occupy, not the block below them.
 *
 * <p>Implemented against a live Minecraft world by the mod module; a fake
 * in-memory implementation is used in tests so the A* logic in
 * {@link PathfindingEngine} can be verified without a running game.
 */
public interface WorldView {

    /**
     * True if a player could stand with their feet at {@code pos}: the space
     * at {@code pos} and one block above it is passable, the block directly
     * below is solid, and none of those blocks are a hazard.
     */
    boolean isWalkable(BlockPos3 pos);

    /**
     * True if occupying {@code pos} is dangerous (lava, fire, or similar) and
     * must never be routed through, mined into, or bridged over.
     */
    boolean isHazard(BlockPos3 pos);

    /** True if {@code pos} is currently occupied by a solid, non-passable block. */
    boolean isSolid(BlockPos3 pos);

    /**
     * True if the solid block at {@code pos} can be broken with tools
     * currently available (excludes bedrock, obsidian without a suitable
     * tool, etc). Only meaningful when {@link #isSolid(BlockPos3)} is true.
     */
    boolean isBreakable(BlockPos3 pos);

    /**
     * Ticks (1/20 second) required to break the block at {@code pos} with the
     * best currently held/available tool. Only meaningful when
     * {@link #isBreakable(BlockPos3)} is true.
     */
    int breakTicks(BlockPos3 pos);

    /** True if at least one solid, placeable block is currently available to bridge/build with. */
    boolean hasPlaceableBlock();
}
