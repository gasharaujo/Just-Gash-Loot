package net.gasharaujo.justgashloot;

import com.google.gson.Gson;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.*;

/** One collection per real player, shared by all tablets in this world. */
public final class LibraryBook {
    private static final Gson JSON = new Gson();
    private static final Codec<Entry> ENTRY_CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.STRING.listOf().fieldOf("Tables").forGetter(Entry::tables),
            Codec.STRING.optionalFieldOf("Active", "").forGetter(Entry::active)
    ).apply(instance, Entry::restore));
    public static final Codec<LibraryBook> CODEC = Codec.unboundedMap(Codec.STRING, ENTRY_CODEC).xmap(LibraryBook::restore, value -> value.players);
    final Map<String, Entry> players = new TreeMap<>();
    public static final class Entry {
        final SortedSet<String> tables = new TreeSet<>();
        String active = "";
        public List<String> tables() { return List.copyOf(tables); }
        public String active() { return active; }
        public void capture(String id) { tables.add(id); active = id; }
        public boolean select(String id) { if (!tables.contains(id)) return false; active = id; return true; }
        private static Entry restore(List<String> tables, String active) {
            Entry result = new Entry();
            for (String id : tables) if (id.length() <= 256 && LootRuntime.validId(id)) result.tables.add(id);
            if (result.tables.contains(active)) result.active = active;
            return result;
        }
    }
    private static LibraryBook restore(Map<String, Entry> players) {
        LibraryBook result = new LibraryBook();
        for (var entry : players.entrySet()) { UUID.fromString(entry.getKey()); result.players.put(entry.getKey(), entry.getValue()); }
        return result;
    }
    public Entry player(UUID uuid) { return players.computeIfAbsent(uuid.toString(), ignored -> new Entry()); }
    String encode() { return JSON.toJson(this); }
    static LibraryBook decode(String text) {
        if (text.isEmpty()) return new LibraryBook();
        LibraryBook book = JSON.fromJson(text, LibraryBook.class);
        if (book == null) throw new IllegalArgumentException("Missing tablet library");
        for (var entry : book.players.entrySet()) {
            UUID.fromString(entry.getKey());
            Entry data = entry.getValue();
            data.tables.removeIf(id -> id == null || id.length() > 256 || !LootRuntime.validId(id));
            if (!data.tables.contains(data.active)) data.active = "";
        }
        return book;
    }
}
