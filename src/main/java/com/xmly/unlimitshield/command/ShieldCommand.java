package com.xmly.unlimitshield.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.LongArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.xmly.unlimitshield.shield.ShieldData;
import com.xmly.unlimitshield.shield.ShieldManager;
import com.xmly.unlimitshield.shield.ShieldValue;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public final class ShieldCommand {

    private static final SimpleCommandExceptionType NO_LIVING_TARGETS =
            new SimpleCommandExceptionType(Component.translatable("commands.xmly_unlimitshield.no_targets"));

    private ShieldCommand() {
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        LiteralArgumentBuilder<CommandSourceStack> root = Commands.literal("shield")
                .requires(source -> source.hasPermission(2));

        root.then(Commands.literal("add")
                .then(Commands.argument("targets", EntityArgument.entities())
                        .then(Commands.argument("x", DoubleArgumentType.doubleArg(0.0))
                                .then(Commands.argument("y", LongArgumentType.longArg(0L))

                                        .executes(context -> addToTargets(context,
                                                EntityArgument.getEntities(context, "targets"),
                                                DoubleArgumentType.getDouble(context, "x"),
                                                LongArgumentType.getLong(context, "y"),
                                                null))
                                        .then(Commands.argument("hitsY", IntegerArgumentType.integer(-128, 127))
                                                .executes(context -> addToTargets(context,
                                                        EntityArgument.getEntities(context, "targets"),
                                                        DoubleArgumentType.getDouble(context, "x"),
                                                        LongArgumentType.getLong(context, "y"),
                                                        (byte) IntegerArgumentType.getInteger(context, "hitsY"))))))));

        root.then(Commands.literal("remove")
                .then(Commands.argument("targets", EntityArgument.entities())
                        .executes(context -> removeFromTargets(context,
                                EntityArgument.getEntities(context, "targets")))));

        root.then(Commands.literal("query")
                .then(Commands.argument("targets", EntityArgument.entities())
                        .executes(context -> query(context,
                                EntityArgument.getEntities(context, "targets")))));

        root.then(Commands.literal("addnear")
                .then(Commands.argument("x", DoubleArgumentType.doubleArg(0.0))
                        .then(Commands.argument("y", LongArgumentType.longArg(0L))
                                .then(Commands.argument("radius", DoubleArgumentType.doubleArg(0.0))
                                        .executes(context -> addToTargets(context,
                                                nearby(context),
                                                DoubleArgumentType.getDouble(context, "x"),
                                                LongArgumentType.getLong(context, "y"),
                                                null))))));

        root.then(Commands.literal("removenear")
                .then(Commands.argument("radius", DoubleArgumentType.doubleArg(0.0))
                        .executes(context -> removeFromTargets(context, nearby(context)))));

        dispatcher.register(root);
    }

    private static Collection<? extends Entity> nearby(CommandContext<CommandSourceStack> context)
            throws CommandSyntaxException {
        CommandSourceStack source = context.getSource();
        ServerLevel level = source.getLevel();
        double radius = DoubleArgumentType.getDouble(context, "radius");
        Vec3 origin = source.getPosition();
        AABB box = new AABB(origin, origin).inflate(radius);

        List<Entity> found = new ArrayList<>(level.getEntities(source.getEntity(), box));
        Entity executor = source.getEntity();
        if (executor != null && !found.contains(executor)) {
            found.add(executor);
        }
        return found;
    }

    private static int addToTargets(CommandContext<CommandSourceStack> context, Collection<? extends Entity> targets,
                                    double x, long y, Byte hitsY) throws CommandSyntaxException {
        List<LivingEntity> applied = new ArrayList<>();
        for (Entity target : targets) {
            if (target instanceof LivingEntity living) {
                ShieldManager.grant(living, x, y);
                if (hitsY != null) {
                    ShieldData.setHitsY(living, hitsY);
                }
                applied.add(living);
            }
        }
        if (applied.isEmpty()) {
            throw NO_LIVING_TARGETS.create();
        }

        final double fx = x;
        final long fy = y;
        int count = applied.size();
        context.getSource().sendSuccess(() -> Component.translatable("commands.xmly_unlimitshield.added",
                count, new ShieldValue(fx, fy).describe()), true);
        return count;
    }

    private static int removeFromTargets(CommandContext<CommandSourceStack> context,
                                         Collection<? extends Entity> targets) throws CommandSyntaxException {
        int count = 0;
        for (Entity target : targets) {
            if (target instanceof LivingEntity living && ShieldManager.hasShield(living)) {
                ShieldManager.revoke(living, "removed by command");
                count++;
            }
        }
        if (count == 0) {
            throw NO_LIVING_TARGETS.create();
        }

        int removed = count;
        context.getSource().sendSuccess(() -> Component.translatable("commands.xmly_unlimitshield.removed", removed), true);
        return removed;
    }

    private static int query(CommandContext<CommandSourceStack> context,
                             Collection<? extends Entity> targets) throws CommandSyntaxException {
        if (targets.isEmpty()) {
            throw NO_LIVING_TARGETS.create();
        }
        for (Entity target : targets) {
            if (target instanceof LivingEntity living) {
                String rendered = ShieldManager.describe(living);
                context.getSource().sendSuccess(() -> Component.translatable("commands.xmly_unlimitshield.query",
                        target.getDisplayName(), rendered), false);
            }
        }
        return targets.size();
    }
}
