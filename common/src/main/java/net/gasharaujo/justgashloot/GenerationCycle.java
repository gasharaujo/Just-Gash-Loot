package net.gasharaujo.justgashloot;

/** Integer-only server clock. Resource access and loot rolls belong to the caller. */
public final class GenerationCycle {
    public static final int WORK = 2400;
    public static final int[] LAVA_TICKS = {800, 400, 200, 100, 60, 40};
    public static final int[] FE_TICKS = {400, 200, 100, 60, 20, 4};
    public static final int[] ROLLS = {1, 2, 3, 5, 6, 8};
    public static final int[] LUCK = {0, 1, 5, 15, 50, 100};
    public record Settings(int energy, int speed, int quantity, int quality) {
        public Settings {
            if (energy < 0 || energy > 5 || speed < 0 || speed > 5 || quantity < 0 || quantity > 5 || quality < 0 || quality > 5)
                throw new IllegalArgumentException("Invalid upgrade tier");
        }
        public int interval() { return 1 << energy; }
    }
    public interface Fuel {
        boolean takeEnergy(int amount);
        boolean takeLava(int amount);
    }
    public Settings settings;
    public int work, credit, lavaThirds;
    public boolean paidWithEnergy;

    /** Returns true exactly once for a completed cycle. No fuel means no progress. */
    public boolean tick(Settings installed, Fuel fuel) {
        if (settings == null || work == 0 && credit == 0) settings = installed;
        if (credit == 0) {
            int fe = 24000 / FE_TICKS[settings.speed];
            if (fuel.takeEnergy(fe)) paidWithEnergy = true;
            else {
                int thirds = lavaThirds + 2400 / LAVA_TICKS[settings.speed];
                if (!fuel.takeLava(thirds / 3)) return false;
                lavaThirds = thirds % 3;
                paidWithEnergy = false;
            }
            credit = settings.interval();
        }
        credit--;
        work += WORK / (paidWithEnergy ? FE_TICKS[settings.speed] : LAVA_TICKS[settings.speed]);
        if (work < WORK) return false;
        work -= WORK;
        return true;
    }
    /** Call after consuming this cycle's settings for the loot roll. */
    public void next(Settings installed) {
        if (!installed.equals(settings)) { credit = 0; settings = installed; }
    }
    public void reset() { work = credit = 0; settings = null; }
}
