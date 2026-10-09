package com.xmly.unlimitshield.client;

import com.xmly.unlimitshield.network.ShieldNetwork;
import com.xmly.unlimitshield.network.ShieldRequestPacket;
import com.xmly.unlimitshield.network.ShieldResponsePacket;
import com.xmly.unlimitshield.shield.ShieldManager;
import com.xmly.unlimitshield.shield.ShieldValue;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.EntityHitResult;

public final class ClientShieldState {

    private static final int REFRESH_TICKS = 4;

    private static final int PENDING_TICKS = 40;

    private static boolean armed;
    private static int targetId = -1;
    private static boolean locked;
    private static boolean targetShielded;
    private static boolean exponentVisible = true;
    private static ShieldValue value = new ShieldValue(0.0, 0L);
    private static int refreshCooldown;
    private static int pendingTicks;

    private ClientShieldState() {
    }

    public static boolean toggle() {
        armed = !armed;
        if (!armed) {
            clearTarget();
        }
        return armed;
    }

    public static boolean isArmed() {
        return armed;
    }

    public static boolean hasTarget() {
        return targetId >= 0;
    }

    public static int targetId() {
        return targetId;
    }

    public static boolean targetShielded() {
        return targetShielded;
    }

    public static boolean exponentVisible() {
        return exponentVisible;
    }

    public static ShieldValue value() {
        return value;
    }

    public static void tick(Minecraft minecraft) {
        if (!armed || minecraft.level == null || minecraft.player == null) {
            return;
        }

        Entity looked = minecraft.hitResult instanceof EntityHitResult hit
                ? ShieldManager.resolveParts(hit.getEntity())
                : null;
        if (looked != null) {
            if (looked.getId() != targetId) {

                targetId = looked.getId();
                locked = true;
                targetShielded = false;
                value = new ShieldValue(0.0, 0L);
                pendingTicks = PENDING_TICKS;
                refreshCooldown = 0;
            }
        } else if (!locked) {
            clearTarget();
            return;
        }

        if (targetId < 0) {
            return;
        }
        if (pendingTicks > 0 && --pendingTicks == 0) {

            clearTarget();
            return;
        }
        if (--refreshCooldown <= 0) {
            refreshCooldown = REFRESH_TICKS;
            ShieldNetwork.sendToServer(new ShieldRequestPacket(targetId));
        }
    }

    public static void accept(ShieldResponsePacket packet) {
        if (packet.entityId() != targetId) {
            return;
        }
        pendingTicks = 0;
        targetShielded = packet.active();
        exponentVisible = packet.exponentVisible();
        value = new ShieldValue(packet.x(), packet.y());

        locked = packet.active();
    }

    public static void clear() {
        clearTarget();
        armed = false;
    }

    private static void clearTarget() {
        targetId = -1;
        locked = false;
        targetShielded = false;
        exponentVisible = true;
        pendingTicks = 0;
        refreshCooldown = 0;
    }
}
