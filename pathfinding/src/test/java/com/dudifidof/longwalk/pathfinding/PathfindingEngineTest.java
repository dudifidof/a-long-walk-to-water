package com.dudifidof.longwalk.pathfinding;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PathfindingEngineTest {

    @Test
    void flatGroundGivesAStraightWalkedPath() {
        FakeWorldView world = new FakeWorldView().floor(0, 10, 0, 0, 0);
        PathfindingEngine engine = new PathfindingEngine(world);

        Optional<List<PathStep>> result = engine.findPath(new BlockPos3(0, 1, 0), new BlockPos3(10, 1, 0));

        assertTrue(result.isPresent());
        List<PathStep> path = result.get();
        assertEquals(new BlockPos3(10, 1, 0), path.get(path.size() - 1).position());
        assertTrue(path.stream().allMatch(step -> step.moveType() == MoveType.WALK));
    }

    @Test
    void singleBlockStepUsesJumpNotMining() {
        FakeWorldView world = new FakeWorldView()
                .floor(0, 3, 0, 0, 0)
                .floor(4, 10, 0, 0, 1); // ground raised by one block from x=4 onward

        PathfindingEngine engine = new PathfindingEngine(world);
        Optional<List<PathStep>> result = engine.findPath(new BlockPos3(0, 1, 0), new BlockPos3(10, 2, 0));

        assertTrue(result.isPresent());
        List<PathStep> path = result.get();
        assertTrue(path.stream().anyMatch(step -> step.moveType() == MoveType.JUMP_UP));
        assertTrue(path.stream().noneMatch(step -> step.moveType() == MoveType.MINE_THROUGH));
    }

    @Test
    void lavaInTheDirectLineIsRoutedAround() {
        FakeWorldView world = new FakeWorldView()
                .floor(0, 10, 0, 1, 0) // two parallel lanes, z=0 and z=1
                .set(new BlockPos3(5, 1, 0), FakeWorldView.Block.LAVA);

        PathfindingEngine engine = new PathfindingEngine(world);
        Optional<List<PathStep>> result = engine.findPath(new BlockPos3(0, 1, 0), new BlockPos3(10, 1, 0));

        assertTrue(result.isPresent());
        List<PathStep> path = result.get();
        assertTrue(path.stream().noneMatch(step -> step.position().equals(new BlockPos3(5, 1, 0))));
    }

    @Test
    void gapTooWideForJumpUsesBridgeWhenBlockIsAvailable() {
        FakeWorldView world = new FakeWorldView()
                .floor(0, 3, 0, 0, 0)
                .floor(6, 10, 0, 0, 0) // x=4,5 have no floor at all: a 2-wide gap
                .withPlaceableBlock(true);

        PathfindingEngine engine = new PathfindingEngine(world);
        Optional<List<PathStep>> result = engine.findPath(new BlockPos3(0, 1, 0), new BlockPos3(10, 1, 0));

        assertTrue(result.isPresent());
        List<PathStep> path = result.get();
        assertTrue(path.stream().anyMatch(step -> step.moveType() == MoveType.BRIDGE_PLACE));
        assertEquals(new BlockPos3(10, 1, 0), path.get(path.size() - 1).position());
    }

    @Test
    void fullyBlockedCorridorMinesThroughAsLastResort() {
        FakeWorldView world = new FakeWorldView()
                .floor(0, 10, 0, 0, 0)
                .set(new BlockPos3(5, 1, 0), FakeWorldView.Block.SOLID)
                .set(new BlockPos3(5, 2, 0), FakeWorldView.Block.SOLID_UNBREAKABLE) // blocks jumping over
                .withPlaceableBlock(false); // no bridging escape sideways either

        PathfindingEngine engine = new PathfindingEngine(world);
        Optional<List<PathStep>> result = engine.findPath(new BlockPos3(0, 1, 0), new BlockPos3(10, 1, 0));

        assertTrue(result.isPresent());
        List<PathStep> path = result.get();
        assertTrue(path.stream().anyMatch(
                step -> step.moveType() == MoveType.MINE_THROUGH && step.position().equals(new BlockPos3(5, 1, 0))));
    }

    @Test
    void unbreakableWallIsRoutedAroundNotMined() {
        FakeWorldView world = new FakeWorldView()
                .floor(0, 10, -1, 1, 0) // three parallel lanes: z=-1, 0, 1
                .set(new BlockPos3(5, 1, 0), FakeWorldView.Block.SOLID_UNBREAKABLE)
                .set(new BlockPos3(5, 2, 0), FakeWorldView.Block.SOLID_UNBREAKABLE);

        PathfindingEngine engine = new PathfindingEngine(world);
        Optional<List<PathStep>> result = engine.findPath(new BlockPos3(0, 1, 0), new BlockPos3(10, 1, 0));

        assertTrue(result.isPresent());
        List<PathStep> path = result.get();
        assertTrue(path.stream().noneMatch(step -> step.moveType() == MoveType.MINE_THROUGH));
        assertEquals(new BlockPos3(10, 1, 0), path.get(path.size() - 1).position());
    }

    @Test
    void wideGapWithNoBlocksAvailableIsUnreachable() {
        FakeWorldView world = new FakeWorldView()
                .floor(0, 3, 0, 0, 0)
                .floor(7, 10, 0, 0, 0) // x=4,5,6 missing: a 3-wide gap, too wide to jump
                .withPlaceableBlock(false);

        PathfindingEngine engine = new PathfindingEngine(world);
        Optional<List<PathStep>> result = engine.findPath(new BlockPos3(0, 1, 0), new BlockPos3(10, 1, 0));

        assertFalse(result.isPresent());
    }
}
