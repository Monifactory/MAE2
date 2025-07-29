package stone.mae2.api;

import appeng.api.networking.ticking.TickRateModulation;
import appeng.api.networking.ticking.TickingRequest;

import stone.mae2.bootstrap.MAE2Config.TickRates.TickRate;

// TODO: find a better package for this
public interface Tickable {
  TickingRequest getTickingRequest();

  TickRateModulation tick();

  public static TickingRequest toTickingRequest(TickRate rate,
    boolean isSleeping, boolean canBeAlerted) {
    return new TickingRequest(rate.minRate(), rate.maxRate(), isSleeping,
      canBeAlerted);
  }

  public static class TickingEntry implements Comparable<TickingEntry> {
    private final TickingRequest request;
    private final Tickable tickable;
    private long nextTick;
    private int updateRate;

    private static final int FASTER_SPEED_UP = 1;
    private static final int SLOWER_SPEED_DOWN = 2;

    public TickingEntry(TickingRequest request, Tickable tickable,
      long currentTick) {
      this.request = request;
      this.tickable = tickable;
      this.nextTick = currentTick + request.initialTickRate();
      this.updateRate = request.initialTickRate();
    }

    @Override
    public int compareTo(TickingEntry other) {
      return Long.compare(this.nextTick, other.nextTick);
    }

    public void update(TickRateModulation change) {
      switch (change) {
      case FASTER:
        this.updateRate -= FASTER_SPEED_UP;
        this.updateRate = Math.max(updateRate, this.request.minTickRate());
        break;
      case IDLE:
        this.updateRate = this.request.maxTickRate();
        break;
      case SAME:
        break;
      case SLEEP:
        break;
      case SLOWER:
        this.updateRate += SLOWER_SPEED_DOWN;
        this.updateRate = Math.min(updateRate, this.request.maxTickRate());
        break;
      case URGENT:
        this.updateRate = this.request.minTickRate();
        break;
      default:
        break;
      }
      this.nextTick += this.updateRate;
    }

    public long getNextTick() { return this.nextTick; }

    public Tickable getTickable() { return this.tickable; }
  }
}
