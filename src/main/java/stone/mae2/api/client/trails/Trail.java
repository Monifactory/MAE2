package stone.mae2.api.client.trails;

import net.minecraft.core.particles.ParticleOptions;

/**
 * Combination particle type and trail style
 * 
 * In real cloud chambers, heavier particles like alpha particles have thicker,
 * more noticeable trails, but can't travel as far. Lighter particles
 * conversely, travel farther, but have less noticeable trails.
 * 
 * The in-game version is not as realistic, but these trail types provide a nice
 * approximation of them.
 */
public interface Trail {
  /**
   * The mean length of this trail
   */
  double getMeanLength();

  /**
   * The standard deviation of the length of this trail
   */
  double getStddevLength();

  /**
   * The particle to default to if the cloud chamber doesn't give one
   */
  ParticleOptions getParticle();
}