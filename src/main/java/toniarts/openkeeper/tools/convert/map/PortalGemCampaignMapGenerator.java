/*
 * Copyright (C) 2014-2026 OpenKeeper
 *
 * OpenKeeper is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */
package toniarts.openkeeper.tools.convert.map;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import toniarts.openkeeper.tools.convert.map.Thing.GoodCreature;

/**
 * Builds a campaign-style companion to the material showcase. A weak Lord of
 * the Land carries a Portal Gem using the same CREATURE_CREATED to
 * ATTACH_PORTAL_GEM trigger pair used by Bullfrog's campaign maps. DKII drops
 * the diamond-shaped gem through its native creature-death path.
 */
public final class PortalGemCampaignMapGenerator {

    static final String MAP_NAME = "PortalGemCampaign";
    private static final int THINGS_HEADER_SIZE = 56;
    private static final int TRIGGERS_HEADER_SIZE = 60;
    private static final int BOSS_TRIGGER_ID = 23;
    private static final int ATTACH_GEM_TRIGGER_ID = 65502;

    private PortalGemCampaignMapGenerator() {
    }

    public static void main(String[] args) throws IOException {
        if (args.length < 1 || args.length > 2) {
            throw new IllegalArgumentException("Usage: PortalGemCampaignMapGenerator <Dungeon Keeper 2 folder> [--validate]");
        }

        Path dk2Root = Path.of(args[0]).toAbsolutePath().normalize();
        Path maps = dk2Root.resolve("Data/editor/maps");
        if (args.length == 2 && "--validate".equals(args[1])) {
            validate(dk2Root);
            System.out.println("Validated " + maps.resolve(MAP_NAME + ".kwd"));
        } else {
            generate(maps);
            validate(dk2Root);
            System.out.println("Generated and validated " + maps.resolve(MAP_NAME + ".kwd"));
        }
    }

    static void generate(Path maps) throws IOException {
        for (String suffix : MaterialShowcaseMapGenerator.FILE_SUFFIXES) {
            Path target = maps.resolve(MAP_NAME + suffix);
            if (Files.exists(target)) {
                throw new IOException("Refusing to overwrite existing campaign test map file: " + target);
            }
        }

        for (String suffix : MaterialShowcaseMapGenerator.FILE_SUFFIXES) {
            Files.copy(maps.resolve(MaterialShowcaseMapGenerator.TEMPLATE_NAME + suffix),
                    maps.resolve(MAP_NAME + suffix), StandardCopyOption.COPY_ATTRIBUTES);
        }

        MaterialShowcaseMapGenerator.rewriteLevelFile(maps.resolve(MAP_NAME + ".kwd"),
                MaterialShowcaseMapGenerator.TEMPLATE_NAME, MAP_NAME);
        MaterialShowcaseMapGenerator.rewriteTiles(maps.resolve(MAP_NAME + "Map.kld"));
        MaterialShowcaseMapGenerator.appendShowcaseThings(maps.resolve(MAP_NAME + "Things.kld"));
        appendGemBoss(maps.resolve(MAP_NAME + "Things.kld"));
        writeNativeGemTriggerPair(maps.resolve(MAP_NAME + "Triggers.kld"));
        Files.writeString(maps.resolve(MAP_NAME + "-README.txt"), readme(), StandardCharsets.UTF_8);
    }

    static void validate(Path dk2Root) {
        Path levelFile = dk2Root.resolve("Data/editor/maps").resolve(MAP_NAME + ".kwd");
        KwdFile level = new KwdFile(dk2Root.toString(), levelFile);
        GoodCreature boss = level.getThings(GoodCreature.class).stream()
                .filter(creature -> creature.getCreatureId() == 29 && creature.getTriggerId() == BOSS_TRIGGER_ID)
                .findFirst().orElseThrow(() -> new IllegalStateException("Portal Gem boss is missing"));
        if (boss.getPosX() != 18 || boss.getPosY() != 22) {
            throw new IllegalStateException("Portal Gem boss is outside its arena");
        }
        Trigger root = level.getTrigger(BOSS_TRIGGER_ID);
        Trigger attach = level.getTrigger(ATTACH_GEM_TRIGGER_ID);
        if (!(root instanceof TriggerGeneric generic)
                || generic.getType() != TriggerGeneric.TargetType.CREATURE_CREATED
                || root.getIdChild() != ATTACH_GEM_TRIGGER_ID
                || !(attach instanceof TriggerAction action)
                || action.getType() != TriggerAction.ActionType.ATTACH_PORTAL_GEM) {
            throw new IllegalStateException("Native Portal Gem trigger pair is invalid");
        }
    }

    private static void appendGemBoss(Path thingsFile) throws IOException {
        byte[] original = Files.readAllBytes(thingsFile);
        ByteBuffer boss = ByteBuffer.allocate(40).order(ByteOrder.LITTLE_ENDIAN);
        boss.putInt(199).putInt(32);       // Bullfrog GoodCreature record.
        boss.putInt(18).putInt(22).putInt(0);
        boss.putShort((short) 0);          // No carried gold.
        boss.put((byte) 1).put((byte) 9); // Level 1, campaign boss flags.
        boss.putInt(0).putInt(25);         // No AP target, 25% initial health.
        boss.putShort((short) BOSS_TRIGGER_ID);
        boss.put((byte) 3).put((byte) 22); // Target red keeper, kill objective.
        boss.put((byte) 29);               // Lord of the Land.
        boss.put((byte) 0).put((byte) 0xff).put((byte) 4);

        byte[] result = new byte[original.length + boss.capacity()];
        System.arraycopy(original, 0, result, 0, original.length);
        System.arraycopy(boss.array(), 0, result, original.length, boss.capacity());
        ByteBuffer header = ByteBuffer.wrap(result).order(ByteOrder.LITTLE_ENDIAN);
        header.putInt(8, result.length);
        header.putInt(20, header.getInt(20) + 1);
        header.putInt(52, result.length - THINGS_HEADER_SIZE);
        Files.write(thingsFile, result);
    }

    private static void writeNativeGemTriggerPair(Path triggersFile) throws IOException {
        byte[] template = Files.readAllBytes(triggersFile);
        if (template.length != TRIGGERS_HEADER_SIZE) {
            throw new IOException("Unexpected SmallTriggers.kld layout: " + template.length + " bytes");
        }

        ByteArrayOutputStream records = new ByteArrayOutputStream(48);
        writeTrigger(records, 213, BOSS_TRIGGER_ID, 0, ATTACH_GEM_TRIGGER_ID, 3); // CREATURE_CREATED
        writeTrigger(records, 214, ATTACH_GEM_TRIGGER_ID, 0, 0, 20);             // ATTACH_PORTAL_GEM
        byte[] result = new byte[template.length + records.size()];
        System.arraycopy(template, 0, result, 0, template.length);
        System.arraycopy(records.toByteArray(), 0, result, template.length, records.size());

        ByteBuffer header = ByteBuffer.wrap(result).order(ByteOrder.LITTLE_ENDIAN);
        header.putInt(8, result.length);
        header.putInt(20, 1); // One generic trigger.
        header.putInt(24, 1); // One action trigger.
        header.putInt(28, 1);
        header.putInt(56, 16); // Bullfrog stores generic count * payload size here.
        Files.write(triggersFile, result);
    }

    private static void writeTrigger(ByteArrayOutputStream out, int tag, int id, int next, int child, int type) {
        ByteBuffer record = ByteBuffer.allocate(24).order(ByteOrder.LITTLE_ENDIAN);
        record.putInt(tag).putInt(16);
        record.putLong(0);
        record.putShort((short) id).putShort((short) next).putShort((short) child);
        record.put((byte) type).put((byte) 1);
        out.writeBytes(record.array());
    }

    private static String readme() {
        return """
                Portal Gem campaign test
                ========================

                The Lord of the Land is enclosed in the reinforced-wall arena south-east of the red Dungeon Heart.
                Drop creatures into the arena and kill him. He starts at level 1 with 25% health.
                DKII should drop the native diamond-shaped Portal Gem from the boss when he dies.

                This uses the same two native trigger records as Bullfrog's campaign:
                CREATURE_CREATED -> ATTACH_PORTAL_GEM. It does not place object 28 or a gem-holder prop.
                The rest of the map mirrors MaterialShowcase for material and flame comparisons.
                """;
    }
}
