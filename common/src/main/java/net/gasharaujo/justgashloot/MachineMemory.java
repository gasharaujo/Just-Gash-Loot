package net.gasharaujo.justgashloot;

import com.google.gson.Gson;
import java.util.UUID;

/** Additive persistence: old machines simply have no generation data. */
final class MachineMemory {
    private static final Gson JSON = new Gson();
    String table = "", ownerName = "";
    UUID owner;
    GenerationCycle cycle = new GenerationCycle();
    String encode() { return JSON.toJson(this); }
    static MachineMemory decode(String value) {
        if (value.isEmpty()) return new MachineMemory();
        try {
            MachineMemory result = JSON.fromJson(value, MachineMemory.class);
            if (result == null) return new MachineMemory();
            if (result.table == null || result.table.length() > 256 || !LootRuntime.validId(result.table)) result.table = "";
            if (result.ownerName == null || result.ownerName.length() > 16) result.ownerName = "";
            if (result.cycle == null) result.cycle = new GenerationCycle();
            GenerationCycle c = result.cycle;
            if (c.work < 0 || c.work >= GenerationCycle.WORK || c.credit < 0 || c.credit > 31 || c.lavaThirds < 0 || c.lavaThirds > 2
                    || c.settings == null && c.credit != 0) result.cycle = new GenerationCycle();
            return result;
        } catch (RuntimeException error) { return new MachineMemory(); }
    }
}
