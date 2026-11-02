package net.gasharaujo.justgashloot;

import java.util.*;
import java.util.regex.Pattern;

/** Persistent item IDs and #item-tag IDs. An empty list keeps all generated items. */
public final class VoidRules {
    public static final int LIMIT = 64, LENGTH = 256;
    private static final Pattern ID = Pattern.compile("[a-z0-9_.-]+:[a-z0-9/._-]+");
    private VoidRules() {}
    public static String normalize(String text) {
        if (text == null || text.length() > LENGTH) return "";
        String value = text.strip().toLowerCase(Locale.ROOT);
        boolean tag = value.startsWith("#");
        String id = tag ? value.substring(1) : value;
        if (!id.contains(":")) id = "minecraft:" + id;
        return ID.matcher(id).matches() && (tag ? id.length() < LENGTH : id.length() <= LENGTH) ? (tag ? "#" : "") + id : "";
    }
    public static List<String> restore(List<String> entries) {
        var result = new LinkedHashSet<String>();
        if (entries != null) for (String entry : entries) {
            String id = normalize(entry);
            if (!id.isEmpty()) result.add(id);
            if (result.size() == LIMIT) break;
        }
        return new ArrayList<>(result);
    }
}
