package net.gasharaujo.justgashloot;

/** Resource conservation, timing and interruption checks independent of Minecraft. */
public final class GenerationChecks {
    private static int checks;
    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
        checks++;
    }
    private static final class Fuel implements GenerationCycle.Fuel {
        int energy, lava;
        long feSpent, lavaSpent;
        Fuel(int energy, int lava) { this.energy = energy; this.lava = lava; }
        public boolean takeEnergy(int amount) { if (energy < amount) return false; energy -= amount; feSpent += amount; return true; }
        public boolean takeLava(int amount) { if (lava < amount) return false; lava -= amount; lavaSpent += amount; return true; }
    }
    public static void main(String[] args) {
        int[] lavaTimes = {800, 400, 200, 100, 60, 40}, feTimes = {400, 200, 100, 60, 20, 4};
        for (boolean energy : new boolean[]{false, true}) {
            for (int speed = 0; speed <= 5; speed++) {
                var settings = new GenerationCycle.Settings(0, speed, 0, 0);
                var cycle = new GenerationCycle(); var fuel = new Fuel(energy ? 24000 : 0, energy ? 0 : 800);
                int ticks = energy ? feTimes[speed] : lavaTimes[speed];
                for (int tick = 1; tick <= ticks; tick++) check(cycle.tick(settings, fuel) == (tick == ticks), "Exact duration: " + energy + "/" + speed + "/" + tick);
                check((energy ? fuel.feSpent : fuel.lavaSpent) == (energy ? 24000 : 800), "Speed conserves resource cost per cycle");
                check(fuel.energy == 0 && fuel.lava == 0, "No resource overdraft");
                for (int upgrade = 1; upgrade <= 5; upgrade++) {
                    cycle = new GenerationCycle(); fuel = new Fuel(energy ? 1000000 : 0, energy ? 0 : 1000000);
                    settings = new GenerationCycle.Settings(upgrade, speed, 0, 0);
                    int completed = 0;
                    for (int tick = 0; tick < ticks * 32; tick++) if (cycle.tick(settings, fuel)) { completed++; cycle.next(settings); }
                    check(completed == 32, "Energy never alters speed");
                    check((energy ? fuel.feSpent : fuel.lavaSpent) == (energy ? 24000L : 800L) * 32 / (1 << upgrade), "Energy savings and prepaid credit survive cycle boundaries");
                    check(cycle.credit == 0 && cycle.work == 0, "No residual work after complete payment intervals");
                }
            }
        }
        var cycle = new GenerationCycle(); var settings = new GenerationCycle.Settings(0, 0, 0, 0);
        var fuel = new Fuel(0, 400);
        for (int i = 0; i < 400; i++) check(!cycle.tick(settings, fuel), "Half lava cycle remains incomplete");
        int paused = cycle.work;
        for (int i = 0; i < 1000; i++) check(!cycle.tick(settings, fuel) && cycle.work == paused, "No free progress while starved");
        fuel.energy = 12000;
        for (int i = 0; i < 200; i++) check(cycle.tick(settings, fuel) == (i == 199), "Mixed source preserves completed fraction");
        check(fuel.lavaSpent == 400 && fuel.feSpent == 12000, "Mixed cycle pays proportionate work in each source");
        cycle = new GenerationCycle(); settings = new GenerationCycle.Settings(0, 4, 0, 0); fuel = new Fuel(0, 100);
        cycle.tick(settings, fuel); check(fuel.lavaSpent == 13 && cycle.lavaThirds == 1, "First fractional lava charge");
        cycle.tick(settings, fuel); check(fuel.lavaSpent == 26 && cycle.lavaThirds == 2, "Second fractional lava charge");
        cycle.tick(settings, fuel); check(fuel.lavaSpent == 40 && cycle.lavaThirds == 0, "Fractional charge is conserved without float drift");
        cycle = new GenerationCycle(); settings = new GenerationCycle.Settings(5, 5, 0, 0); fuel = new Fuel(6000, 0);
        int completed = 0;
        for (int i = 0; i < 32; i++) if (cycle.tick(settings, fuel)) { completed++; cycle.next(settings); }
        check(completed == 8 && fuel.feSpent == 6000, "Fast cycles share one Energy T5 payment");
        check(!cycle.tick(settings, fuel), "Paid work cannot repeat after credit expires");
        cycle = new GenerationCycle(); settings = new GenerationCycle.Settings(0, 5, 0, 0); fuel = new Fuel(5999, 20);
        cycle.tick(settings, fuel); check(fuel.energy == 5999 && fuel.lava == 0 && !cycle.paidWithEnergy, "Insufficient full FE charge falls back to lava without partial withdrawal");
        check(checks > 3000, "Clock exercised across resource and tier combinations");
        System.out.println("PASS: " + checks + " generation checks");
    }
}
