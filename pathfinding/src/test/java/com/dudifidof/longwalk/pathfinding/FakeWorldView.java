package com.dudifidof.longwalk.pathfinding;

import java.util.HashMap;
import java.util.Map;

/** In-memory {@link WorldView} for tests: a sparse voxel grid, air by default. */
final class FakeWorldView implements WorldView {

    enum Block {
        AIR, SOLID, SOLID_UNBREAKABLE, LAVA
    }

    private final Map<BlockPos3, Block> blocks = new HashMap<>();
    private boolean placeableBlockAvailable = false;

    FakeWorldView floor(int x1, int x2, int z1, int z2, int y) {
        for (int x = Math.min(x1, x2); x <= Math.max(x1, x2); x++) {
            for (int z = Math.min(z1, z2); z <= Math.max(z1, z2); z++) {
                set(new BlockPos3(x, y, z), Block.SOLID);
            }
        }
        return this;
    }

    FakeWorldView set(BlockPos3 pos, Block block) {
        blocks.put(pos, block);
        return this;
    }

    FakeWorldView withPlaceableBlock(boolean available) {
        this.placeableBlockAvailable = available;
        return this;
    }

    private Block blockAt(BlockPos3 pos) {
        return blocks.getOrDefault(pos, Block.AIR);
    }

    @Override
    public boolean isWalkable(BlockPos3 pos) {
        return blockAt(pos) == Block.AIR
                && blockAt(pos.offset(0, 1, 0)) == Block.AIR
                && isFloorAt(pos.offset(0, -1, 0));
    }

    private boolean isFloorAt(BlockPos3 pos) {
        Block b = blockAt(pos);
        return b == Block.SOLID || b == Block.SOLID_UNBREAKABLE;
    }

    @Override
    public boolean isHazard(BlockPos3 pos) {
        return blockAt(pos) == Block.LAVA;
    }

    @Override
    public boolean isSolid(BlockPos3 pos) {
        Block b = blockAt(pos);
        return b == Block.SOLID || b == Block.SOLID_UNBREAKABLE;
    }

    @Override
    public boolean isBreakable(BlockPos3 pos) {
        return blockAt(pos) == Block.SOLID;
    }

    @Override
    public int breakTicks(BlockPos3 pos) {
        return 20;
    }

    @Override
    public boolean hasPlaceableBlock() {
        return placeableBlockAvailable;
    }
}
