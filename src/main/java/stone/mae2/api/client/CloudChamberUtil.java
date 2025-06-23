package stone.mae2.api.client;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import stone.mae2.api.block.TrailForming;
import stone.mae2.api.block.TrailForming.TrailType;
import stone.mae2.bootstrap.MAE2Tags;

public final class CloudChamberUtil {

  public static final int AREA = 32;
  private static final double PARTICLES_PER_BLOCK = 8;
  private static long lastTick = -1;

  /**
   * Attempts to draw background radiation trails if it hasn't before
   * 
   * <b> !!! This should be called every tick while trails should be drawn !!!
   * </b> Simple method is via calling inside
   * {@link Block#animateTick(net.minecraft.world.level.block.state.BlockState, Level, BlockPos, RandomSource)}
   * 
   * @param level
   * @param random
   * @param currentTick
   */
  public static void tryBackgroundRadiation(Level level, RandomSource random) {
    long currentTick = level.getGameTime();
    if (currentTick > lastTick) {
      lastTick = currentTick;
      for (TrailType trail : TrailType.values) {
        double chance = trail.meanChance
          + random.nextGaussian() * trail.stddevChance;
        for (int i = 0; i < (int) chance
          + (random.nextFloat() < chance - (int) chance ? 1 : 0); i++) {
          Vec3 surface = randomPoint(random, trail);
          @SuppressWarnings("resource")
          Vec3 playerPos = Minecraft.getInstance().player.position();
          Vec3 offset = playerPos.offsetRandom(random, AREA);
          CloudChamberUtil
            .drawTrail(level, surface.add(offset),
              surface.scale(-1).add(offset), trail);
        }
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
  public static Vec3 randomPoint(RandomSource random, TrailType trail) {
    Vec3 normal = randomNormal(random);
    double length = trail.meanLength
      + random.nextGaussian() * trail.stddevLength;
    return normal.scale(length);
  }

  /**
   * Draws a cloud chamber trail from start to end using the given particle
   * 
   * See {@link CloudChamberUtil#drawTrail(Level, Vec3, Vec3, TrailType)} for a
   * variant that respects cloud chamber particle types
   * 
   * @param level
   * @param start
   * @param end
   * @param particle
   */
  public static void drawTrail(Level level, Vec3 start, Vec3 end,
    ParticleOptions particle) {
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
          .addParticle(particle, lerped.x(), lerped.y(), lerped.z(), 0, 0, 0);
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
   * @param level
   * @param start
   * @param end
   * @param trail
   */
  public static void drawTrail(Level level, Vec3 start, Vec3 end,
    TrailType trail) {
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
      particle = trail.particle;

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
            particle = trail.particle;
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
