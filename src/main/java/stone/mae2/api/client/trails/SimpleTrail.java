package stone.mae2.api.client.trails;

import net.minecraft.core.particles.ParticleOptions;

/**
 * A simple, static trail implementation
 */
public record SimpleTrail(double meanLength, double stddevLength,
  double meanChance, double stddevChance, ParticleOptions particle)
  implements BackgroundTrail {
  @Override
  public double getMeanLength() { return meanLength; }

  @Override
  public double getStddevLength() { return stddevLength; }

  @Override
  public double getMeanChance() { return meanChance; }

  @Override
  public double getStddevChance() { return stddevChance; }

  @Override
  public ParticleOptions getParticle() { return particle; }
}
