package com.dudifidof.longwalk.pathfinding;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.PriorityQueue;

/**
 * A* pathfinder over a {@link WorldView}.
 *
 * <p>Walking, jumping a single step, dropping down, and jumping a one-block
 * gap are all cheap. Mining through a block or bridging a gap with a placed
 * block carry large fixed penalties on top of their real cost, so A* only
 * chooses them when no walkable/jumpable route exists at all — they behave
 * as a last resort without needing a separate search pass.
 */
public final class PathfindingEngine {

    private static final double WALK_COST = 1.0;
    private static final double JUMP_COST = 1.2;
    private static final double DROP_COST = 1.1;
    private static final double GAP_JUMP_COST = 1.5;
    private static final double MINE_LAST_RESORT_PENALTY = 50.0;
    private static final double BRIDGE_LAST_RESORT_PENALTY = 30.0;
    private static final int MAX_DROP_HEIGHT = 3;
    private static final int MAX_EXPANDED_NODES = 20_000;

    private static final int[][] CARDINALS = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};

    private final WorldView world;

    public PathfindingEngine(WorldView world) {
        this.world = world;
    }

    public Optional<List<PathStep>> findPath(BlockPos3 start, BlockPos3 goal) {
        Map<BlockPos3, Double> bestCost = new HashMap<>();
        Map<BlockPos3, PathStep> stepInto = new HashMap<>();
        Map<BlockPos3, BlockPos3> predecessor = new HashMap<>();

        PriorityQueue<Node> open = new PriorityQueue<>(Comparator.comparingDouble(n -> n.f));
        bestCost.put(start, 0.0);
        open.add(new Node(start, 0.0, heuristic(start, goal)));

        int expanded = 0;
        while (!open.isEmpty()) {
            if (++expanded > MAX_EXPANDED_NODES) {
                return Optional.empty();
            }
            Node current = open.poll();
            if (current.g > bestCost.getOrDefault(current.pos, Double.MAX_VALUE)) {
                continue;
            }
            if (current.pos.equals(goal)) {
                return Optional.of(reconstruct(stepInto, predecessor, start, goal));
            }
            for (Edge edge : neighbors(current.pos)) {
                double tentative = current.g + edge.cost;
                if (tentative < bestCost.getOrDefault(edge.to, Double.MAX_VALUE)) {
                    bestCost.put(edge.to, tentative);
                    predecessor.put(edge.to, current.pos);
                    stepInto.put(edge.to, new PathStep(edge.to, edge.moveType, edge.affectedBlock));
                    open.add(new Node(edge.to, tentative, tentative + heuristic(edge.to, goal)));
                }
            }
        }
        return Optional.empty();
    }

    private List<PathStep> reconstruct(Map<BlockPos3, PathStep> stepInto, Map<BlockPos3, BlockPos3> predecessor,
                                        BlockPos3 start, BlockPos3 goal) {
        LinkedList<PathStep> steps = new LinkedList<>();
        BlockPos3 cur = goal;
        while (!cur.equals(start)) {
            steps.addFirst(stepInto.get(cur));
            cur = predecessor.get(cur);
        }
        return steps;
    }

    private double heuristic(BlockPos3 a, BlockPos3 b) {
        return a.distanceTo(b);
    }

    private List<Edge> neighbors(BlockPos3 pos) {
        List<Edge> edges = new ArrayList<>();
        for (int[] d : CARDINALS) {
            addEdgesTowards(edges, pos, d[0], d[1]);
        }
        return edges;
    }

    private void addEdgesTowards(List<Edge> edges, BlockPos3 from, int dx, int dz) {
        BlockPos3 flat = from.offset(dx, 0, dz);

        if (world.isWalkable(flat) && !world.isHazard(flat)) {
            edges.add(new Edge(flat, MoveType.WALK, WALK_COST, null));
        }

        BlockPos3 up = flat.offset(0, 1, 0);
        if (world.isWalkable(up) && !world.isHazard(up)) {
            edges.add(new Edge(up, MoveType.JUMP_UP, JUMP_COST, null));
        }

        for (int fall = 1; fall <= MAX_DROP_HEIGHT; fall++) {
            BlockPos3 down = flat.offset(0, -fall, 0);
            if (world.isHazard(down)) {
                break;
            }
            if (world.isWalkable(down)) {
                edges.add(new Edge(down, MoveType.DROP_DOWN, DROP_COST + fall * 0.2, null));
                break;
            }
        }

        boolean flatIsOpenGap = !world.isWalkable(flat) && !world.isSolid(flat) && !world.isHazard(flat);
        if (flatIsOpenGap) {
            BlockPos3 far = from.offset(dx * 2, 0, dz * 2);
            if (world.isWalkable(far) && !world.isHazard(far)) {
                edges.add(new Edge(far, MoveType.GAP_JUMP, GAP_JUMP_COST, null));
            } else if (world.hasPlaceableBlock()) {
                edges.add(new Edge(flat, MoveType.BRIDGE_PLACE, WALK_COST + BRIDGE_LAST_RESORT_PENALTY, flat));
            }
        }

        boolean flatIsBlockingWall = !world.isWalkable(flat) && world.isSolid(flat) && !world.isHazard(flat);
        if (flatIsBlockingWall && world.isBreakable(flat)) {
            double cost = WALK_COST + MINE_LAST_RESORT_PENALTY + world.breakTicks(flat) / 20.0;
            edges.add(new Edge(flat, MoveType.MINE_THROUGH, cost, flat));
        }
    }

    private record Node(BlockPos3 pos, double g, double f) {
    }

    private record Edge(BlockPos3 to, MoveType moveType, double cost, BlockPos3 affectedBlock) {
    }
}
