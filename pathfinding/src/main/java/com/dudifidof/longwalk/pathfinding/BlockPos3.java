package com.dudifidof.longwalk.pathfinding;

/** A plain integer block coordinate, independent of any Minecraft types. */
public record BlockPos3(int x, int y, int z) {

    public BlockPos3 offset(int dx, int dy, int dz) {
        return new BlockPos3(x + dx, y + dy, z + dz);
    }

    public double distanceTo(BlockPos3 other) {
        double dx = x - other.x;
        double dy = y - other.y;
        double dz = z - other.z;
        return Math.sqrt(dx * dx + dy * dy + dz * dz);
    }
}
