package stone.mae2.api.client.trails;

import appeng.core.AppEngClient;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import stone.mae2.MAE2;
import stone.mae2.bootstrap.MAE2Tags;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public final class CloudChamberUtil {
  public static final int AREA = 32;
  public static final double PARTICLES_PER_BLOCK = 8;
  private static long lastTick = -1;

  public static final Set<BackgroundTrail> backgroundTrails = new HashSet<>();
  public static final Map<ResourceLocation, Trail> REGISTRY = new HashMap<>();

  /**
   * A heavier particle, doesn't travel as far, is less common in the
   * background, but has a thicker trail
   */
  public static final BackgroundTrail ALPHA = registerBackgroundTrail(
    MAE2.toKey("alpha"), new SimpleTrail(2, 1, .5, 1,
      net.minecraft.core.particles.ParticleTypes.CLOUD));
  /**
   * A lighter particle, travels farther and is more common in the background,
   * but has a thinner trail
   */
  public static final BackgroundTrail BETA = registerBackgroundTrail(
    MAE2.toKey("beta"), new SimpleTrail(20, 0, 1.5, 1,
      appeng.client.render.effects.ParticleTypes.VIBRANT));

  /**
   * Registers a trail to spawn as part of the background radiation effect
   * 
   * @param trail
   */
  public static synchronized BackgroundTrail registerBackgroundTrail(
    ResourceLocation location, BackgroundTrail trail) {
    backgroundTrails.add(trail);
    registerTrail(location, trail);
    return trail;
  }

  public static synchronized Trail registerTrail(ResourceLocation location, Trail trail) {
    REGISTRY.put(location, trail);
    return trail;
  }

  /**
   * Attempts to draw background radiation trails if it hasn't before
   * 
   * <b> !!! This should be called every tick while trails should be drawn !!!
   * </b> Simple method is via calling inside
   * {@link Block#animateTick(net.minecraft.world.level.block.state.BlockState, Level, BlockPos, RandomSource)}
   * 
   * Can be called multiple times per tick fine
   * 
   * @param level
   * @param random
   * @param currentTick
   */
  public static void tryBackgroundRadiation(Level level, RandomSource random) {
    long currentTick = level.getGameTime();
    if (currentTick == lastTick)
      return;
    lastTick = currentTick;
    for (BackgroundTrail trail : backgroundTrails) {
      double chance = trail.getMeanChance()
        + random.nextGaussian() * trail.getStddevChance();
      for (int i = 0; i < (int) chance
        + (random.nextFloat() < chance - (int) chance ? 1 : 0); i++) {
        if (!AppEngClient.instance().shouldAddParticles(random))
          continue;
        Vec3 surface = randomPoint(random, trail);
        @SuppressWarnings("resource")
        Vec3 playerPos = Minecraft.getInstance().player.position();
        Vec3 offset = playerPos.offsetRandom(random, AREA);
        CloudChamberUtil
          .drawTrail(level, surface.add(offset), surface.scale(-1).add(offset),
            random, trail);
      }
    }
  }

  /**
   * Generates a normal vector facing a random angle
   * 
   * @param random
   * @return
   */
  public static Vec3 randomNormal(RandomSource random) {
    double polar = random.nextDouble() * 2 * Math.PI;
    double azimuthal = random.nextDouble() * 2 * Math.PI;
    Vec3 normal = new Vec3(Math.sin(polar) * Math.cos(azimuthal),
      Math.cos(polar), Math.sin(polar) * Math.sin(azimuthal));
    return normal;
  }

  /**
   * Picks a random point on a sphere with a radius according to the mean length
   * of the given trail
   * 
   * @param random
   * @param trail
   * @return
   */
  public static Vec3 randomPoint(RandomSource random, Trail trail) {
    Vec3 normal = randomNormal(random);
    double length = trail.getMeanLength()
      + random.nextGaussian() * trail.getStddevLength();
    return normal.scale(length);
  }

  /**
   * Draws a cloud chamber trail from start to end using the given particle
   * 
   * See {@link CloudChamberUtil#drawTrail(Level, Vec3, Vec3, TrailType)} for a
   * variant that respects cloud chamber particle types
   * 
   * @param level    level to draw the trail in
   * @param start    starting point
   * @param end      ending point
   * @param particle particle to draw
   */
  public static void drawTrail(Level level, Vec3 start, Vec3 end,
    RandomSource random, ParticleOptions particle) {
    double step = (1d / (PARTICLES_PER_BLOCK * start.distanceTo(end)));
    Vec3i lastBlock = new Vec3i((int) Math.floor(start.x),
      (int) Math.floor(start.y), (int) Math.floor(start.z));
    boolean inChamber = level
      .getBlockState(
        new BlockPos(lastBlock.getX(), lastBlock.getY(), lastBlock.getZ()))
      .is(MAE2Tags.CLOUD_CHAMBERS);
    ;
    for (double i = 0; i < 1; i += step) {
      Vec3 lerped = start.lerp(end, i);
      if ((int) Math.floor(lerped.x) != lastBlock.getX()
        || (int) Math.floor(lerped.y) != lastBlock.getY()
        || (int) Math.floor(lerped.z) != lastBlock.getZ()) {
        inChamber = level
          .getBlockState(new BlockPos((int) Math.floor(lerped.x),
            (int) Math.floor(lerped.y), (int) Math.floor(lerped.z)))
          .is(MAE2Tags.CLOUD_CHAMBERS);
        lastBlock = new Vec3i((int) Math.floor(lerped.x),
          (int) Math.floor(lerped.y), (int) Math.floor(lerped.z));
      }
      if (inChamber) {
        level
          .addParticle(particle, lerped.x(), lerped.y(), lerped.z(), 0d, 0d,
            0d);
      } else {
        i += step;
      }

    }
  }

  /**
   * Draws a cloud chamber trail from start to end using cloud chamber particles
   * 
   * See {@link CloudChamberUtil#drawTrail(Level, Vec3, Vec3, ParticleOptions)}
   * for a variant that doesn't respect cloud chamber particle types
   * 
   * @param level level to draw the trail in
   * @param start starting point
   * @param end   ending point
   * @param trail trail type to draw
   */
  public static void drawTrail(Level level, Vec3 start, Vec3 end,
    RandomSource random, Trail trail) {
    double step = (1d / (PARTICLES_PER_BLOCK * start.distanceTo(end)));
    Vec3i lastBlock = new Vec3i((int) Math.floor(start.x),
      (int) Math.floor(start.y), (int) Math.floor(start.z));

    BlockState state = level
      .getBlockState(
        new BlockPos(lastBlock.getX(), lastBlock.getY(), lastBlock.getZ()));
    boolean inChamber = state.is(MAE2Tags.CLOUD_CHAMBERS);
    ParticleOptions particle;
    if (state.getBlock() instanceof TrailForming former)
      particle = former.getTrailParticle(state, trail);
    else
      particle = trail.getParticle();

    for (double i = 0; i < 1; i += step) {
      Vec3 lerped = start.lerp(end, i);
      if ((int) Math.floor(lerped.x) != lastBlock.getX()
        || (int) Math.floor(lerped.y) != lastBlock.getY()
        || (int) Math.floor(lerped.z) != lastBlock.getZ()) {
        state = level
          .getBlockState(new BlockPos((int) Math.floor(lerped.x),
            (int) Math.floor(lerped.y), (int) Math.floor(lerped.z)));
        inChamber = state.is(MAE2Tags.CLOUD_CHAMBERS);
        if (inChamber) {
          if (state.getBlock() instanceof TrailForming former)
            particle = former.getTrailParticle(state, trail);
          else
            particle = trail.getParticle();
        }
        lastBlock = new Vec3i((int) Math.floor(lerped.x),
          (int) Math.floor(lerped.y), (int) Math.floor(lerped.z));
      }
      if (inChamber) {
        level
          .addParticle(particle, lerped.x(), lerped.y(), lerped.z(), 0, 0, 0);
      } else {
        i += step;
      }

    }
  }

}
