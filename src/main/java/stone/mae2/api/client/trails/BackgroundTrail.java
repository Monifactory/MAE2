package stone.mae2.api.client.trails;

public interface BackgroundTrail extends Trail {
  /**
   * The mean chance of this trail appearing in the background
   */
  double getMeanChance();

  /**
   * The standard deviation of the chance of this trail appearing in the
   * background.
   */
  double getStddevChance();
}