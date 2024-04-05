package com.hbm.blocks.network;

import api.hbm.block.IConveyorBelt;
import api.hbm.block.IConveyorItem;
import api.hbm.block.IConveyorPackage;
import api.hbm.block.IEnterableBlock;
import com.hbm.blocks.ModBlocks;
import com.hbm.entity.item.EntityMovingItem;
import com.hbm.modules.ModulePatternMatcher;
import com.hbm.tileentity.network.TileEntityCraneBase;
import com.hbm.tileentity.network.TileEntityCraneRouter;
import com.hbm.tileentity.network.TileEntityCraneUnboxer;
import net.minecraft.block.Block;
import net.minecraft.block.BlockContainer;
import net.minecraft.block.material.Material;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

import java.util.ArrayList;
import java.util.List;

public class CraneRouter extends BlockContainer implements IEnterableBlock {
    public CraneRouter(Material materialIn, String s) {
        super(materialIn);
        this.setUnlocalizedName(s);
        this.setRegistryName(s);
        ModBlocks.ALL_BLOCKS.add(this);
    }
    @Override
    public TileEntity createNewTileEntity(World world, int meta) {
        return new TileEntityCraneRouter();
    }

    @Override
    public boolean canItemEnter(World world, int x, int y, int z, EnumFacing dir, IConveyorItem entity) {
        return true;
    }

    @Override
    public void onItemEnter(World world, int x, int y, int z, EnumFacing dir, IConveyorItem entity) {
        TileEntityCraneRouter router = (TileEntityCraneRouter) world.getTileEntity(new BlockPos(x, y, z));
        ItemStack stack = entity.getItemStack();

        List<EnumFacing> validDirs = new ArrayList<>();

        //check filters for all sides
        for(EnumFacing side : EnumFacing.VALUES) {

            ModulePatternMatcher matcher = router.patterns[side.getIndex()];
            int mode = router.modes[side.getIndex()];

            //if the side is disabled or wildcard, skip
            if(mode == router.MODE_NONE || mode == router.MODE_WILDCARD)
                continue;

            boolean matchesFilter = false;

            for(int slot = 0; slot < 5; slot++) {
                ItemStack filter = router.inventory.getStackInSlot(side.getIndex() * 5 + slot);

                if(filter.isEmpty())
                    continue;

                //the filter kicks in so long as one entry matches
                if(matcher.isValidForFilter(filter, slot, stack)) {
                    matchesFilter = true;
                    break;
                }
            }

            //add dir if matches with whitelist on or doesn't match with blacklist on
            if((mode == router.MODE_WHITELIST && matchesFilter) || (mode == router.MODE_BLACKLIST && !matchesFilter)) {
                validDirs.add(side);
            }
        }

        //if no valid dirs have yet been found, use wildcard
        if(validDirs.isEmpty()) {
            for(EnumFacing side : EnumFacing.VALUES) {
                if(router.modes[side.getIndex()] == router.MODE_WILDCARD) {
                    validDirs.add(side);
                }
            }
        }

        if(validDirs.isEmpty()) {
            world.spawnEntity(new EntityItem(world, x + 0.5, y + 0.5, z + 0.5, stack.copy()));
            return;
        }

        int i = world.rand.nextInt(validDirs.size());
        sendOnRoute(world, x, y, z, entity, validDirs.get(i));
    }

    protected void sendOnRoute(World world, int x, int y, int z, IConveyorItem item, EnumFacing dir) {
        IConveyorBelt belt = null;
        BlockPos targetPos = new BlockPos(x + dir.getFrontOffsetX(), y + dir.getFrontOffsetY(), z + dir.getFrontOffsetZ());
        Block block = world.getBlockState(targetPos).getBlock();

        if (block instanceof IConveyorBelt) {
            belt = (IConveyorBelt) block;
        }

        if (belt != null) {
            EntityMovingItem moving = new EntityMovingItem(world);
            Vec3d pos = new Vec3d(x + 0.5 + dir.getFrontOffsetX() * 0.55, y + 0.5 + dir.getFrontOffsetY() * 0.55, z + 0.5 + dir.getFrontOffsetZ() * 0.55);
            Vec3d snap = belt.getClosestSnappingPosition(world, targetPos, pos);
            moving.setPosition(snap.x, snap.y, snap.z);
            moving.setItemStack(item.getItemStack());
            world.spawnEntity(moving);
        } else {
            world.spawnEntity(new EntityItem(world, x + 0.5 + dir.getFrontOffsetX() * 0.55, y + 0.5 + dir.getFrontOffsetY() * 0.55, z + 0.5 + dir.getFrontOffsetZ() * 0.55, item.getItemStack()));
        }
    }

    @Override public boolean canPackageEnter(World world, int x, int y, int z, EnumFacing dir, IConveyorPackage entity) { return false; }
    @Override public void onPackageEnter(World world, int x, int y, int z, EnumFacing dir, IConveyorPackage entity) { }

}
