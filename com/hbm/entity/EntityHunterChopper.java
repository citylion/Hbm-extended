package com.hbm.entity;

import com.hbm.items.ModItems;
import com.hbm.lib.Library;
import com.hbm.lib.ModDamageSource;
import com.hbm.particles.EntitySmokeFX;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityFlying;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.ai.EntityAILookIdle;
import net.minecraft.entity.ai.EntityAINearestAttackableTarget;
import net.minecraft.entity.boss.IBossDisplayData;
import net.minecraft.entity.monster.IMob;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.projectile.EntityLargeFireball;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.stats.AchievementList;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.DamageSource;
import net.minecraft.util.MathHelper;
import net.minecraft.util.Vec3;
import net.minecraft.world.EnumDifficulty;
import net.minecraft.world.World;

public class EntityHunterChopper extends EntityFlying implements IMob, IBossDisplayData {
	public int courseChangeCooldown;
	public double waypointX;
	public double waypointY;
	public double waypointZ;
	private Entity targetedEntity;
	/** Cooldown time between target loss and new target aquirement. */
	private int aggroCooldown;
	public int prevAttackCounter;
	public int attackCounter;
	public int mineDropCounter;
	/** The explosion radius of spawned fireballs. */
	private int explosionStrength = 1;
	private static final String __OBFID = "CL_00001689";
	public boolean isDying = false;

	public EntityHunterChopper(World p_i1735_1_) {
		super(p_i1735_1_);
		this.setSize(8.25F, 3.0F);
		this.isImmuneToFire = true;
		this.experienceValue = 500;
	}

	@SideOnly(Side.CLIENT)
	public boolean func_110182_bF() {
		return this.dataWatcher.getWatchableObjectByte(16) != 0;
	}

	/**
	 * Called when the entity is attacked.
	 */
	public boolean attackEntityFrom(DamageSource source, float amount) {
		if (this.isEntityInvulnerable() || !(source.isExplosion()  || ModDamageSource.getIsTau(source) || (ModDamageSource.getIsEmplacer(source) && source.getEntity() != this))) {
			return false;
		} else if(amount >= this.getHealth()) {
			this.initDeath();
			return false;
		}
		
		if(rand.nextInt(20) == 0)
		{
			if(!worldObj.isRemote)
			{
				this.worldObj.createExplosion(this, this.posX, this.posY, this.posZ, 5F, true);
				this.dropDamageItem();
			}
		}

		for (int j = 0; j < 3; j++) {
			double d0 = rand.nextDouble() / 20 * rand.nextInt(2) == 0 ? -1 : 1;
			double d1 = rand.nextDouble() / 20 * rand.nextInt(2) == 0 ? -1 : 1;
			double d2 = rand.nextDouble() / 20 * rand.nextInt(2) == 0 ? -1 : 1;

			for (int i = 0; i < 8; i++)
				if(this.worldObj.isRemote)
					worldObj.spawnParticle("fireworksSpark", this.posX, this.posY, this.posZ, d0 * i, d1 * i, d2 * i);
		}

		return super.attackEntityFrom(source, amount);
	}

	protected void entityInit() {
		super.entityInit();
		this.dataWatcher.addObject(16, Byte.valueOf((byte) 0));
	}

	protected void applyEntityAttributes() {
		super.applyEntityAttributes();
		this.getEntityAttribute(SharedMonsterAttributes.maxHealth).setBaseValue(1000.0D);
	}

	protected void updateEntityActionState() {
		if (!this.worldObj.isRemote && this.worldObj.difficultySetting == EnumDifficulty.PEACEFUL) {
			this.setDead();
		}

		if (!isDying) {
			if (this.ticksExisted % 2 == 0)
				this.worldObj.playSoundEffect(this.posX, this.posY, this.posZ, "fireworks.blast", 10.0F, 0.5F);

			this.despawnEntity();
			this.prevAttackCounter = this.attackCounter;
			double d0 = this.waypointX - this.posX;
			double d1 = this.waypointY - this.posY;
			double d2 = this.waypointZ - this.posZ;
			double d3 = d0 * d0 + d1 * d1 + d2 * d2;

			if (d3 < 1.0D || d3 > 3600.0D) {
				if (this.targetedEntity != null) {
					this.waypointX = targetedEntity.posX + (double) ((this.rand.nextFloat() * 2.0F - 1.0F) * 16.0F);
					this.waypointZ = targetedEntity.posZ + (double) ((this.rand.nextFloat() * 2.0F - 1.0F) * 16.0F);
					this.waypointY = this.worldObj.getHeightValue((int) waypointX, (int) waypointZ) + 10
							+ rand.nextInt(15);
				} else {
					this.waypointX = this.posX + (double) ((this.rand.nextFloat() * 2.0F - 1.0F) * 16.0F);
					this.waypointZ = this.posZ + (double) ((this.rand.nextFloat() * 2.0F - 1.0F) * 16.0F);
					this.waypointY = this.worldObj.getHeightValue((int) waypointX, (int) waypointZ) + 10
							+ rand.nextInt(15);
				}
			}

			if (this.courseChangeCooldown-- <= 0) {
				this.courseChangeCooldown += this.rand.nextInt(5) + 2;
				d3 = (double) MathHelper.sqrt_double(d3);

				if (this.isCourseTraversable(this.waypointX, this.waypointY, this.waypointZ, d3)) {
					this.motionX += d0 / d3 * 0.1D;
					this.motionY += d1 / d3 * 0.1D;
					this.motionZ += d2 / d3 * 0.1D;
				} else {
					this.waypointX = this.posX + (double) ((this.rand.nextFloat() * 2.0F - 1.0F) * 16.0F);
					this.waypointZ = this.posZ + (double) ((this.rand.nextFloat() * 2.0F - 1.0F) * 16.0F);
					this.waypointY = this.worldObj.getHeightValue((int) waypointX, (int) waypointZ) + 10
							+ rand.nextInt(15);
				}
			}

			if (this.targetedEntity != null && this.targetedEntity.isDead) {
				this.targetedEntity = null;
			}

			if (this.targetedEntity == null || this.attackCounter <= 0) {
				// this.targetedEntity =
				// this.worldObj.getClosestVulnerablePlayerToEntity(this,
				// 100.0D);
				this.targetedEntity = Library.getClosestEntityForChopper(worldObj, this.posX, this.posY, this.posZ,
						250);

				if (this.targetedEntity != null) {
					this.aggroCooldown = 20;
				}
			}

			double d4 = 64.0D;

			if (this.targetedEntity != null && this.targetedEntity.getDistanceSqToEntity(this) < d4 * d4) {
				double d8 = 2.0D;
				Vec3 vec3 = this.getLook(1.0F);
				double xStart = this.posX + vec3.xCoord * d8;
				double yStart = this.posY - 0.5;
				double zStart = this.posZ + vec3.zCoord * d8;
				double d5 = this.targetedEntity.posX - xStart;
				double d6 = this.targetedEntity.boundingBox.minY + (double) (this.targetedEntity.height / 2.0F)
						- yStart;
				double d7 = this.targetedEntity.posZ - zStart;

				++this.attackCounter;
				if (attackCounter >= 200) {
					attackCounter -= 200;
					System.out.println(this.targetedEntity.toString());
				}

				if (this.attackCounter % 2 == 0 && attackCounter >= 120) {
					worldObj.playSoundAtEntity(this, "random.explode", 10.0F, 3.0F);
					// EntityLargeFireball entitylargefireball = new
					// EntityLargeFireball(this.worldObj, this, d5, d6, d7);
					EntityBullet entityarrow = new EntityBullet(this.worldObj, this, 3.0F, 35, 45, false, "chopper");
					Vec3 vec2 = Vec3.createVectorHelper(d5 - 1 + rand.nextInt(3), d6 - 1 + rand.nextInt(3),
							d7 - 1 + rand.nextInt(3)).normalize();
					double motion = 3;
					entityarrow.motionX = vec2.xCoord * motion;
					entityarrow.motionY = vec2.yCoord * motion;
					entityarrow.motionZ = vec2.zCoord * motion;
					// entitylargefireball.field_92057_e =
					// this.explosionStrength;
					entityarrow.setDamage(3 + rand.nextInt(5));
					// entitylargefireball.posX = this.posX + vec3.xCoord * d8;
					// entitylargefireball.posY = this.posY +
					// (double)(this.height /
					// 2.0F) + 0.5D;
					// entitylargefireball.posZ = this.posZ + vec3.zCoord * d8;
					entityarrow.posX = xStart;
					entityarrow.posY = yStart;
					entityarrow.posZ = zStart;
					// this.worldObj.spawnEntityInWorld(entitylargefireball);
					this.worldObj.spawnEntityInWorld(entityarrow);
				}
				if (this.attackCounter >= 80 && this.attackCounter < 120) {
					worldObj.playSoundAtEntity(this, "random.click", 10.0F, 0.5F + ((attackCounter / 100) - 0.8F));
				}

				this.mineDropCounter++;
				if (mineDropCounter > 100 && rand.nextInt(15) == 0) {
					EntityChopperMine mine = new EntityChopperMine(worldObj, this.posX, this.posY - 0.5, this.posZ, 0, -0.3, 0, this);
					this.mineDropCounter = 0;
					this.worldObj.spawnEntityInWorld(mine);
					
					if(rand.nextInt(3) == 0)
					{
						EntityChopperMine mine1 = new EntityChopperMine(worldObj, this.posX, this.posY - 0.5, this.posZ, 1, -0.3, 0, this);
						EntityChopperMine mine2 = new EntityChopperMine(worldObj, this.posX, this.posY - 0.5, this.posZ, 0, -0.3, 1, this);
						EntityChopperMine mine3 = new EntityChopperMine(worldObj, this.posX, this.posY - 0.5, this.posZ, -1, -0.3, 0, this);
						EntityChopperMine mine4 = new EntityChopperMine(worldObj, this.posX, this.posY - 0.5, this.posZ, 0, -0.3, -1, this);
						this.worldObj.spawnEntityInWorld(mine1);
						this.worldObj.spawnEntityInWorld(mine2);
						this.worldObj.spawnEntityInWorld(mine3);
						this.worldObj.spawnEntityInWorld(mine4);
					}
				}

			} else {

				if (this.attackCounter > 0) {
					this.attackCounter = 0;
				}
			}

			if (!this.worldObj.isRemote) {
				byte b1 = this.dataWatcher.getWatchableObjectByte(16);
				byte b0 = (byte) (this.attackCounter > 10 ? 1 : 0);

				if (b1 != b0) {
					this.dataWatcher.updateObject(16, Byte.valueOf(b0));
				}
			}
		} else {
			motionY -= 0.08;
			if(Math.sqrt(Math.pow(this.motionX, 2) + Math.pow(this.motionZ, 2)) * 1.2 < 1.8)
			{
				this.motionX *= 1.2;
				this.motionZ *= 1.2;
			}
			
			if(rand.nextInt(30) == 0)
			{
		    	this.worldObj.createExplosion(this, this.posX, this.posY, this.posZ, 5F, true);
			}
			
			this.worldObj.spawnEntityInWorld(new EntitySmokeFX(worldObj, this.posX, this.posY, this.posZ, 0, 0, 0));
			
			if(this.onGround)
			{
		    	this.worldObj.createExplosion(this, this.posX, this.posY, this.posZ, 15F, true);
		    	this.dropItems();
		    	this.setDead();
			}
		}

		float f3 = MathHelper.sqrt_double(this.motionX * this.motionX + this.motionZ * this.motionZ);
		if(this.rotationYaw - (float) (Math.atan2(this.motionX, this.motionZ) * 180.0D / Math.PI) >= 10)
			this.prevRotationYaw = this.rotationYaw -= 10;
		if(this.rotationYaw - (float) (Math.atan2(this.motionX, this.motionZ) * 180.0D / Math.PI) <= -10)
			this.prevRotationYaw = this.rotationYaw += 10;
		if(this.rotationYaw - (float) (Math.atan2(this.motionX, this.motionZ) * 180.0D / Math.PI) < 10 && this.rotationYaw - (float) (Math.atan2(this.motionX, this.motionZ) * 180.0D / Math.PI) > 10)
			this.prevRotationYaw = (float) (Math.atan2(this.motionX, this.motionZ) * 180.0D / Math.PI);
		this.prevRotationPitch = this.rotationPitch = (float) (Math.atan2(this.motionY, f3) * 180.0D / Math.PI);
	}

	/**
	 * True if the ghast has an unobstructed line of travel to the waypoint.
	 */
	private boolean isCourseTraversable(double p_70790_1_, double p_70790_3_, double p_70790_5_, double p_70790_7_) {
		double d4 = (this.waypointX - this.posX) / p_70790_7_;
		double d5 = (this.waypointY - this.posY) / p_70790_7_;
		double d6 = (this.waypointZ - this.posZ) / p_70790_7_;
		AxisAlignedBB axisalignedbb = this.boundingBox.copy();

		for (int i = 1; (double) i < p_70790_7_; ++i) {
			axisalignedbb.offset(d4, d5, d6);

			if (!this.worldObj.getCollidingBoundingBoxes(this, axisalignedbb).isEmpty()) {
				return false;
			}
		}

		return true;
	}

	protected String getHurtSound() {
		return "none";
	}

	protected String getDeathSound() {
		return "none";
	}

	/**
	 * Drop 0-2 items of this living's type. @param par1 - Whether this entity
	 * has recently been hit by a player. @param par2 - Level of Looting used to
	 * kill this mob.
	 */
	protected void dropItems() {

		if(rand.nextInt(2) == 0)
			this.dropItem(ModItems.chopper_head, 1);
		if(rand.nextInt(2) == 0)
			this.dropItem(ModItems.chopper_torso, 1);
		if(rand.nextInt(2) == 0)
			this.dropItem(ModItems.chopper_wing, 1);
		if(rand.nextInt(3) == 0)
			this.dropItem(ModItems.chopper_tail, 1);
		if(rand.nextInt(3) == 0)
			this.dropItem(ModItems.chopper_gun, 1);
		if(rand.nextInt(3) == 0)
			this.dropItem(ModItems.chopper_blades, 1);

		this.dropItem(ModItems.combine_scrap, rand.nextInt(8) + 1);
		this.dropItem(ModItems.plate_combine_steel, rand.nextInt(5) + 1);
		this.dropItem(ModItems.wire_magnetized_tungsten, rand.nextInt(3) + 1);
	}

	/**
	 * Returns the volume for the sounds this mob makes.
	 */
	protected float getSoundVolume() {
		return 10.0F;
	}

	/**
	 * Checks if the entity's current position is a valid location to spawn this
	 * entity.
	 */
	public boolean getCanSpawnHere() {
		return this.rand.nextInt(20) == 0 && super.getCanSpawnHere()
				&& this.worldObj.difficultySetting != EnumDifficulty.PEACEFUL;
	}

	/**
	 * Will return how many at most can spawn in a chunk at once.
	 */
	public int getMaxSpawnedInChunk() {
		return 1;
	}

	/**
	 * (abstract) Protected helper method to write subclass entity data to NBT.
	 */
	public void writeEntityToNBT(NBTTagCompound p_70014_1_) {
		super.writeEntityToNBT(p_70014_1_);
		p_70014_1_.setInteger("ExplosionPower", this.explosionStrength);
	}

	/**
	 * (abstract) Protected helper method to read subclass entity data from NBT.
	 */
	public void readEntityFromNBT(NBTTagCompound p_70037_1_) {
		super.readEntityFromNBT(p_70037_1_);

		if (p_70037_1_.hasKey("ExplosionPower", 99)) {
			this.explosionStrength = p_70037_1_.getInteger("ExplosionPower");
		}
	}
	
    @Override
	@SideOnly(Side.CLIENT)
    public boolean isInRangeToRenderDist(double distance)
    {
        return distance < 25000;
    }
    
    public void initDeath() {
    	this.worldObj.createExplosion(this, this.posX, this.posY, this.posZ, 10F, true);
    	isDying = true;
    }
    
    public void dropDamageItem() {
    	int i = rand.nextInt(10);

    	if(i < 6)
			this.dropItem(ModItems.combine_scrap, 1);
    	else if(i > 7)
			this.dropItem(ModItems.plate_combine_steel, 1);
    	else
			this.dropItem(ModItems.wire_magnetized_tungsten, 1);
    }
}