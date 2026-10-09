package com.xmly.unlimitshield.shield;

import java.util.HashMap;
import java.util.Map;

public final class ShieldHealthView {

    public record Entry(boolean natural, float baseline) {
    }

    private static final Map<Integer, Entry> ENTRIES = new HashMap<>();

    private ShieldHealthView() {
    }

    public static void put(int entityId, boolean natural, float baseline) {
        ENTRIES.put(entityId, new Entry(natural, baseline));
    }

    public static void remove(int entityId) {
        ENTRIES.remove(entityId);
    }

    public static void clear() {
        ENTRIES.clear();
    }

    public static float baselineFor(int entityId, float maxHealth) {
        Entry entry = ENTRIES.get(entityId);
        if (entry == null) {
            return 0.0F;
        }

        float wanted = entry.natural() ? maxHealth : entry.baseline();
        return Math.min(wanted, maxHealth);
    }
}
