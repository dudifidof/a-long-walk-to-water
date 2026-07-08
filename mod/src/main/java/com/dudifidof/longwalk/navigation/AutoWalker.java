package com.dudifidof.longwalk.navigation;

import com.dudifidof.longwalk.LongWalkMod;
import com.dudifidof.longwalk.pathfinding.BlockPos3;
import com.dudifidof.longwalk.pathfinding.MoveType;
import com.dudifidof.longwalk.pathfinding.PathStep;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

import java.util.List;

/**
 * Drives the local player each client tick to follow the path computed by
 * {@link com.dudifidof.longwalk.pathfinding.PathfindingEngine}: turns and
 * walks/jumps toward the next node, mines through blocking nodes, and
 * places blocks to bridge gaps, by simulating key presses and interactions
 * exactly as a real player would.
 *
 * <p>Cancels itself when requested, on reaching the goal, if the player
 * takes unexpected damage, or if position stops changing (stuck).
 */
public final class AutoWalker {

    private static final double NODE_ARRIVAL_DISTANCE = 0.35;
    private static final int STUCK_TICKS = 100; // ~5s with no meaningful movement
    private static final int MINE_TIMEOUT_TICKS = 200; // ~10s max spent on a single block

    private static Vec3d lastProgressPos;
    private static int ticksSinceProgress;
    private static float lastHealth = -1;
    private static int mineTicksOnCurrentNode;

    private AutoWalker() {
    }

    public static void onClientTick(MinecraftClient client) {
        if (!NavigationState.isActive()) {
            return;
        }

        ClientPlayerEntity player = client.player;
        if (player == null || client.world == null) {
            NavigationState.stop();
            return;
        }

        if (NavigationState.isCancelRequested()) {
            stopAndClearInput(client, "Stopped.");
            return;
        }

        if (tookUnexpectedDamage(player)) {
            stopAndClearInput(client, "Auto-walk aborted: took damage.");
            return;
        }

        List<PathStep> path = NavigationState.path();
        int index = NavigationState.index();
        if (index >= path.size()) {
            stopAndClearInput(client, "Arrived.");
            return;
        }

        if (isStuck(player)) {
            stopAndClearInput(client, "Auto-walk aborted: stuck.");
            return;
        }

        if (driveTowards(client, player, path.get(index))) {
            mineTicksOnCurrentNode = 0;
            NavigationState.advance();
        }
    }

    private static boolean tookUnexpectedDamage(ClientPlayerEntity player) {
        float health = player.getHealth();
        boolean damaged = lastHealth >= 0 && health < lastHealth - 0.01f;
        lastHealth = health;
        return damaged;
    }

    private static boolean isStuck(ClientPlayerEntity player) {
        Vec3d pos = player.getPos();
        if (lastProgressPos == null || lastProgressPos.distanceTo(pos) > 0.3) {
            lastProgressPos = pos;
            ticksSinceProgress = 0;
            return false;
        }
        return ++ticksSinceProgress > STUCK_TICKS;
    }

    private static boolean driveTowards(MinecraftClient client, ClientPlayerEntity player, PathStep step) {
        return switch (step.moveType()) {
            case WALK, JUMP_UP, DROP_DOWN, GAP_JUMP -> walkToward(client, player, step.position(),
                    step.moveType() == MoveType.JUMP_UP || step.moveType() == MoveType.GAP_JUMP);
            case MINE_THROUGH -> mineBlock(client, player, step.affectedBlock());
            case BRIDGE_PLACE -> placeBlock(client, player, step.affectedBlock());
        };
    }

    private static boolean walkToward(MinecraftClient client, ClientPlayerEntity player, BlockPos3 target,
                                       boolean shouldJump) {
        Vec3d targetCenter = new Vec3d(target.x() + 0.5, target.y(), target.z() + 0.5);
        Vec3d playerPos = player.getPos();
        double dx = targetCenter.x - playerPos.x;
        double dz = targetCenter.z - playerPos.z;
        double horizontalDistance = Math.sqrt(dx * dx + dz * dz);

        player.setYaw(yawTowards(dx, dz));

        boolean atNode = horizontalDistance < NODE_ARRIVAL_DISTANCE && Math.abs(player.getY() - target.y()) < 1.1;
        client.options.forwardKey.setPressed(!atNode);
        client.options.jumpKey.setPressed(!atNode && shouldJump && player.isOnGround());
        return atNode;
    }

    private static boolean mineBlock(MinecraftClient client, ClientPlayerEntity player, BlockPos3 target) {
        client.options.forwardKey.setPressed(false);
        client.options.jumpKey.setPressed(false);

        BlockPos pos = new BlockPos(target.x(), target.y(), target.z());
        if (client.world.getBlockState(pos).isAir()) {
            return true;
        }

        Vec3d center = Vec3d.ofCenter(pos);
        Vec3d playerPos = player.getPos();
        player.setYaw(yawTowards(center.x - playerPos.x, center.z - playerPos.z));

        Direction side = Direction.getFacing(center.x - playerPos.x, 0, center.z - playerPos.z);
        if (client.interactionManager != null) {
            client.interactionManager.attackBlock(pos, side);
        }

        if (++mineTicksOnCurrentNode > MINE_TIMEOUT_TICKS) {
            mineTicksOnCurrentNode = 0;
            return true; // give up on this block; stuck-detection catches genuine dead ends
        }
        return false;
    }

    private static boolean placeBlock(MinecraftClient client, ClientPlayerEntity player, BlockPos3 target) {
        client.options.forwardKey.setPressed(false);
        client.options.jumpKey.setPressed(false);

        int blockSlot = findHotbarBlockSlot(player);
        if (blockSlot < 0) {
            return true; // nothing to place with; NavigationState will need a fresh /run to route another way
        }
        player.getInventory().selectedSlot = blockSlot;

        BlockPos below = new BlockPos(target.x(), target.y() - 1, target.z());
        BlockHitResult hit = new BlockHitResult(Vec3d.ofCenter(below).add(0, 0.5, 0), Direction.UP, below, false);
        if (client.interactionManager != null) {
            client.interactionManager.interactBlock(player, Hand.MAIN_HAND, hit);
        }
        return true;
    }

    private static int findHotbarBlockSlot(ClientPlayerEntity player) {
        for (int slot = 0; slot < 9; slot++) {
            ItemStack stack = player.getInventory().getStack(slot);
            if (stack.getItem() instanceof BlockItem) {
                return slot;
            }
        }
        return -1;
    }

    private static float yawTowards(double dx, double dz) {
        return (float) (MathHelper.atan2(dz, dx) * (180.0 / Math.PI)) - 90.0f;
    }

    private static void stopAndClearInput(MinecraftClient client, String message) {
        client.options.forwardKey.setPressed(false);
        client.options.jumpKey.setPressed(false);
        client.options.sprintKey.setPressed(false);
        NavigationState.stop();
        if (client.player != null) {
            client.player.sendMessage(Text.literal("[LongWalk] " + message), false);
        }
        LongWalkMod.LOGGER.info("Auto-walk ended: {}", message);
    }
}
