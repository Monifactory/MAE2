package stone.mae2.api.block;

import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Indicates this block can form trails
 * 
 * <b> !! Don't forget to tag your block with {@code mae2:blocks/cloud_chambers}
 * !! </b>
 * 
 * Without it, trails won't form at all. This interface is simply to allow
 * blocks to override the default
 */
public interface TrailForming {
  /**
   * Get the particle used to represent the given trail type
   * 
   * @param state
   * @param trail
   * @return
   */
  public ParticleOptions getTrailParticle(BlockState state, TrailType trail);

  /**
   * Combination particle type and trail style
   * 
   * In real cloud chambers, heavier particles like alpha particles have
   * thicker, more noticeable trails, but can't travel as far. Lighter particles
   * conversely, travel farther, but have less noticeable trails.
   * 
   * The in-game version is not as realistic, but these trail types provide an
   * approximation of them.
   */
  public enum TrailType {
    /**
     * A lighter particle, travels farther and is more common in the background,
     * but has a thinner trail
     */
    LIGHT(20, 0, 2, 0, appeng.client.render.effects.ParticleTypes.VIBRANT),
    /**
     * A heavier particle, doesn't travel as far, is less common in the
     * background, but has a thicker trail
     */
    HEAVY(2, .5, .5, .1, net.minecraft.core.particles.ParticleTypes.CLOUD);

    /**
     * Don't edit this please, only exists for performance reasons
     */
    public static final TrailType[] values = TrailType.values();

    /**
     * The mean length of this trail
     */
    public final double meanLength;
    /**
     * The standard deviation of the length of this trail
     */
    public final double stddevLength;

    /**
     * The mean chance of this trail appearing in the background
     */
    public final double meanChance;
    /**
     * The standard deviation of the chance of this trail appearing in the
     * background.
     */
    public final double stddevChance;

    /**
     * The particle to default to if the cloud chamber doesn't give one
     */
    public final ParticleOptions particle;

    private TrailType(double meanLength, double stddevLength, double meanChance,
      double stddevChance, ParticleOptions particle) {
      this.meanLength = meanLength;
      this.stddevLength = stddevLength;
      this.meanChance = meanChance;
      this.stddevChance = stddevChance;
      this.particle = particle;
    }

  }
}
