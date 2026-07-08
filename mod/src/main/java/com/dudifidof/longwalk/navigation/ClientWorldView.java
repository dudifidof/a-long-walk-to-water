package com.dudifidof.longwalk.navigation;

import com.dudifidof.longwalk.pathfinding.BlockPos3;
import com.dudifidof.longwalk.pathfinding.WorldView;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.fluid.FluidState;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.tag.FluidTags;
import net.minecraft.util.math.BlockPos;

/**
 * Bridges the Minecraft-independent {@link WorldView} contract to a live
 * client world/player, so {@link com.dudifidof.longwalk.pathfinding.PathfindingEngine}
 * never has to depend on Minecraft classes directly.
 */
public final class ClientWorldView implements WorldView {

    /** Anything that would take longer than this to break is treated as effectively unbreakable. */
    private static final int MAX_BREAK_TICKS = 20 * 30;

    private final ClientWorld world;
    private final ClientPlayerEntity player;

    public ClientWorldView(MinecraftClient client) {
        this.world = client.world;
        this.player = client.player;
    }

    private static BlockPos toBlockPos(BlockPos3 pos) {
        return new BlockPos(pos.x(), pos.y(), pos.z());
    }

    @Override
    public boolean isWalkable(BlockPos3 pos) {
        BlockPos feet = toBlockPos(pos);
        return isPassable(feet) && isPassable(feet.up()) && isSolidGround(feet.down()) && !isHazard(pos);
    }

    private boolean isPassable(BlockPos pos) {
        BlockState state = world.getBlockState(pos);
        return state.getCollisionShape(world, pos).isEmpty() && state.getFluidState().isEmpty();
    }

    private boolean isSolidGround(BlockPos pos) {
        BlockState state = world.getBlockState(pos);
        return !state.getCollisionShape(world, pos).isEmpty() && state.getFluidState().isEmpty();
    }

    @Override
    public boolean isHazard(BlockPos3 pos) {
        BlockPos blockPos = toBlockPos(pos);
        FluidState fluid = world.getFluidState(blockPos);
        if (fluid.isIn(FluidTags.LAVA)) {
            return true;
        }
        BlockState state = world.getBlockState(blockPos);
        return state.isOf(Blocks.FIRE) || state.isOf(Blocks.LAVA_CAULDRON) || state.isOf(Blocks.MAGMA_BLOCK);
    }

    @Override
    public boolean isSolid(BlockPos3 pos) {
        BlockPos blockPos = toBlockPos(pos);
        return !world.getBlockState(blockPos).getCollisionShape(world, blockPos).isEmpty();
    }

    @Override
    public boolean isBreakable(BlockPos3 pos) {
        BlockPos blockPos = toBlockPos(pos);
        return world.getBlockState(blockPos).getHardness(world, blockPos) >= 0;
    }

    @Override
    public int breakTicks(BlockPos3 pos) {
        BlockPos blockPos = toBlockPos(pos);
        BlockState state = world.getBlockState(blockPos);
        float hardness = state.getHardness(world, blockPos);
        if (hardness < 0) {
            return MAX_BREAK_TICKS;
        }

        ItemStack tool = player.getInventory().getMainHandStack();
        float speed = tool.getMiningSpeedMultiplier(state);
        if (speed <= 0) {
            return MAX_BREAK_TICKS;
        }

        float damageMultiplier = tool.isSuitableFor(state) ? 1.5f : 5.0f;
        float seconds = hardness * damageMultiplier / speed;
        return Math.min(MAX_BREAK_TICKS, Math.max(1, (int) (seconds * 20)));
    }

    @Override
    public boolean hasPlaceableBlock() {
        for (int slot = 0; slot < 9; slot++) {
            if (player.getInventory().getStack(slot).getItem() instanceof BlockItem) {
                return true;
            }
        }
        return false;
    }
}
