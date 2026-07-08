package com.dudifidof.longwalk.pathfinding;

/**
 * One step of a computed path: move to {@code position} using {@code moveType}.
 * {@code affectedBlock} is set for moves that mine or place a block (equal to
 * {@code position} in practice), and is {@code null} otherwise.
 */
public record PathStep(BlockPos3 position, MoveType moveType, BlockPos3 affectedBlock) {

    public PathStep(BlockPos3 position, MoveType moveType) {
        this(position, moveType, null);
    }
}
