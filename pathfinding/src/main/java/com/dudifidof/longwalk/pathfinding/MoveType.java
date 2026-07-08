package com.dudifidof.longwalk.pathfinding;

/** The kind of action needed to reach a path node from its predecessor. */
public enum MoveType {
    /** Walk forward on the same level. */
    WALK,
    /** Walk forward while jumping up a single block. */
    JUMP_UP,
    /** Walk forward off a ledge, dropping down to lower ground. */
    DROP_DOWN,
    /** Jump across a single-block gap with no floor. */
    GAP_JUMP,
    /** Break the blocking block and walk through. Last resort. */
    MINE_THROUGH,
    /** Place a carried block to create footing, then walk onto it. Last resort. */
    BRIDGE_PLACE
}
