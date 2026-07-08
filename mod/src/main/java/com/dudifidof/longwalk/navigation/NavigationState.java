package com.dudifidof.longwalk.navigation;

import com.dudifidof.longwalk.pathfinding.BlockPos3;
import com.dudifidof.longwalk.pathfinding.PathStep;

import java.util.List;

/** Holds the currently active `/run`, if any. Read/written from the client thread only. */
public final class NavigationState {

    private static List<PathStep> activePath;
    private static int activeIndex;
    private static BlockPos3 target;
    private static boolean cancelRequested;

    private NavigationState() {
    }

    public static void start(BlockPos3 newTarget, List<PathStep> path) {
        target = newTarget;
        activePath = path;
        activeIndex = 0;
        cancelRequested = false;
    }

    public static void stop() {
        activePath = null;
        activeIndex = 0;
        target = null;
        cancelRequested = false;
    }

    public static void requestCancel() {
        cancelRequested = true;
    }

    public static boolean isCancelRequested() {
        return cancelRequested;
    }

    public static boolean isActive() {
        return activePath != null;
    }

    public static List<PathStep> path() {
        return activePath;
    }

    public static int index() {
        return activeIndex;
    }

    public static void advance() {
        activeIndex++;
    }

    public static BlockPos3 target() {
        return target;
    }
}
