package com.xmly.unlimitshield.api;

import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.eventbus.api.Cancelable;

@Cancelable
public class ShieldGrantEvent extends LivingEvent {

    private double x;
    private long y;

    public ShieldGrantEvent(LivingEntity entity, double x, long y) {
        super(entity);
        this.x = x;
        this.y = y;
    }

    public double getX() {
        return this.x;
    }

    public void setX(double x) {
        this.x = x;
    }

    public long getY() {
        return this.y;
    }

    public void setY(long y) {
        this.y = y;
    }
}
