package stone.mae2.api.client.trails;

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
  public ParticleOptions getTrailParticle(BlockState state, Trail trail);
}
