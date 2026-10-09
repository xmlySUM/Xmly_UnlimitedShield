package com.xmly.unlimitshield.client;

import com.xmly.unlimitshield.shield.ShieldHealthView;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public final class ClientShieldRegistry {

    private static final Map<Integer, Entry> SHIELDED = new HashMap<>();

    private record Entry(boolean natural, float baseline, boolean exponentVisible) {
    }

    private ClientShieldRegistry() {
    }

    public static void setShielded(int entityId, boolean shielded, boolean natural, float baseline,
                                   boolean exponentVisible) {
        if (shielded) {
            SHIELDED.put(entityId, new Entry(natural, baseline, exponentVisible));
            ShieldHealthView.put(entityId, natural, baseline);
        } else {
            SHIELDED.remove(entityId);
            ShieldHealthView.remove(entityId);
        }
    }

    public static boolean exponentVisible(int entityId) {
        Entry entry = SHIELDED.get(entityId);
        return entry == null || entry.exponentVisible();
    }

    public static void clear() {
        SHIELDED.clear();
        ShieldHealthView.clear();
    }

    public static void tick() {
        if (SHIELDED.isEmpty()) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) {
            SHIELDED.clear();
            return;
        }

        Iterator<Map.Entry<Integer, Entry>> ids = SHIELDED.entrySet().iterator();
        while (ids.hasNext()) {
            Map.Entry<Integer, Entry> entry = ids.next();
            Entity entity = minecraft.level.getEntity(entry.getKey());
            if (entity == null) {

                ids.remove();
                continue;
            }
            if (!(entity instanceof LivingEntity living)) {
                continue;
            }
            if (living.deathTime > 0) {
                living.deathTime = 0;
            }

            Entry state = entry.getValue();
            float wanted = state.natural() ? living.getMaxHealth() : state.baseline();

            wanted = Math.min(wanted, living.getMaxHealth());
            if (wanted > 0.0F && living.getHealth() != wanted) {
                living.setHealth(wanted);
            }
        }
    }
}
