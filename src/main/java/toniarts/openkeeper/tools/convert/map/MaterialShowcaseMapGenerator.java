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
import java.util.List;
import toniarts.openkeeper.tools.convert.map.Thing.KeeperCreature;
import toniarts.openkeeper.tools.convert.map.Thing.Object;

/**
 * Builds a small map for visually checking KMF material flags and lava bloom.
 * The stock 48x48 "Small" map supplies the editor metadata and player setup;
 * only its fixed-size name/path fields, tile grid and thing list are changed.
 */
public final class MaterialShowcaseMapGenerator {

    static final String MAP_NAME = "MaterialShowcase";
    static final String TEMPLATE_NAME = "Small";
    static final List<String> FILE_SUFFIXES = List.of(
            ".kwd", "Map.kld", "Players.kld", "Things.kld", "Triggers.kld", "Variables.kld");
    private static final int MAP_HEADER_SIZE = 36;
    private static final int THINGS_HEADER_SIZE = 56;
    private static final int MAP_WIDTH = 48;
    private static final int MAP_HEIGHT = 48;

    private MaterialShowcaseMapGenerator() {
    }

    public static void main(String[] args) throws IOException {
        if (args.length < 1 || args.length > 2) {
            throw new IllegalArgumentException("Usage: MaterialShowcaseMapGenerator <Dungeon Keeper 2 folder> [--validate]");
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
        for (String suffix : FILE_SUFFIXES) {
            Path target = maps.resolve(MAP_NAME + suffix);
            if (Files.exists(target)) {
                throw new IOException("Refusing to overwrite existing showcase map file: " + target);
            }
        }

        for (String suffix : FILE_SUFFIXES) {
            Files.copy(maps.resolve(TEMPLATE_NAME + suffix), maps.resolve(MAP_NAME + suffix),
                    StandardCopyOption.COPY_ATTRIBUTES);
        }

        rewriteLevelFile(maps.resolve(MAP_NAME + ".kwd"), TEMPLATE_NAME, MAP_NAME);
        rewriteTiles(maps.resolve(MAP_NAME + "Map.kld"));
        appendShowcaseThings(maps.resolve(MAP_NAME + "Things.kld"));
        Files.writeString(maps.resolve(MAP_NAME + "-README.txt"), readme(), StandardCharsets.UTF_8);
    }

    static void validate(Path dk2Root) {
        Path levelFile = dk2Root.resolve("Data/editor/maps").resolve(MAP_NAME + ".kwd");
        KwdFile level = new KwdFile(dk2Root.toString(), levelFile);
        if (!MAP_NAME.equals(level.getGameLevel().getName())
                || level.getMap().getWidth() != MAP_WIDTH
                || level.getMap().getHeight() != MAP_HEIGHT) {
            throw new IllegalStateException("Generated level metadata is invalid");
        }
        assertTerrain(level, 16, 5, 5, "lava");
        assertTerrain(level, 16, 13, 4, "water");
        assertTerrain(level, 5, 17, 7, "gem wall");
        assertTerrain(level, 10, 18, 11, "lair");
        assertTerrain(level, 5, 5, 22, "temple");
        assertTerrain(level, 31, 33, 30, "blue keeper enclosure");
        assertTerrain(level, 15, 22, 9, "portal gem display wall");

        long candles = level.getThings(Object.class).stream().filter(thing -> thing.getObjectId() == 111).count();
        long gems = level.getThings(Object.class).stream()
                .filter(thing -> thing.getObjectId() == 28 && thing.getPlayerId() == 1)
                .count();
        boolean mistress = level.getThings(KeeperCreature.class).stream().anyMatch(thing -> thing.getCreatureId() == 4);
        boolean darkAngel = level.getThings(KeeperCreature.class).stream().anyMatch(thing -> thing.getCreatureId() == 23);
        if (candles < 4 || gems != 0 || !mistress || !darkAngel) {
            throw new IllegalStateException("Generated showcase things are incomplete");
        }
    }

    private static void assertTerrain(KwdFile level, int x, int y, int expected, String area) {
        int actual = level.getMap().getTile(x, y).getTerrainId();
        if (actual != expected) {
            throw new IllegalStateException("Invalid " + area + " terrain at " + x + "," + y + ": " + actual);
        }
    }

    static void rewriteLevelFile(Path levelFile, String oldName, String newName) throws IOException {
        byte[] data = Files.readAllBytes(levelFile);
        replaceFirst(data, utf16(oldName), utf16(newName));
        for (String suffix : FILE_SUFFIXES.subList(1, FILE_SUFFIXES.size())) {
            replaceFirst(data, ascii(oldName + suffix), ascii(newName + suffix));
        }
        Files.write(levelFile, data);
    }

    static void rewriteTiles(Path mapFile) throws IOException {
        byte[] data = Files.readAllBytes(mapFile);
        if (data.length != MAP_HEADER_SIZE + MAP_WIDTH * MAP_HEIGHT * 4) {
            throw new IOException("Unexpected SmallMap.kld layout: " + data.length + " bytes");
        }

        for (int y = 0; y < MAP_HEIGHT; y++) {
            for (int x = 0; x < MAP_WIDTH; x++) {
                setTile(data, x, y, 8, 3); // Claimed floor for keeper 1.
            }
        }

        // Impenetrable border.
        fill(data, 0, 0, MAP_WIDTH - 1, 1, 30, 2);
        fill(data, 0, MAP_HEIGHT - 2, MAP_WIDTH - 1, MAP_HEIGHT - 1, 30, 2);
        fill(data, 0, 0, 1, MAP_HEIGHT - 1, 30, 2);
        fill(data, MAP_WIDTH - 2, 0, MAP_WIDTH - 1, MAP_HEIGHT - 1, 30, 2);

        // Preserve both template dungeon hearts and their starting imps. Seal
        // the blue keeper inside an impenetrable ring for an undisturbed test.
        fill(data, 10, 10, 14, 14, 14, 3);
        fill(data, 33, 33, 37, 37, 14, 4);
        fill(data, 31, 31, 39, 31, 30, 2);
        fill(data, 31, 39, 39, 39, 30, 2);
        fill(data, 31, 31, 31, 39, 30, 2);
        fill(data, 39, 31, 39, 39, 30, 2);

        // A real Temple room distinguishes constructed candlesticks from the
        // otherwise identical bare candle models placed beside the lava.
        fill(data, 5, 5, 9, 9, 22, 3);

        // Side-by-side surface comparison, with candles along the lava edge.
        fill(data, 16, 5, 25, 11, 5, 2);
        fill(data, 16, 13, 25, 19, 4, 2);

        // Environment-mapped gem wall and a lair-material display pad.
        fill(data, 5, 17, 8, 24, 7, 2);
        fill(data, 10, 18, 14, 24, 11, 3);

        // Enclosed arena used by PortalGemCampaign's gem-carrying boss. The
        // reinforced walls also supply nearby torches for billboard comparison.
        fill(data, 15, 20, 22, 20, 9, 3);
        fill(data, 15, 24, 22, 24, 9, 3);
        fill(data, 15, 20, 15, 24, 9, 3);
        fill(data, 22, 20, 22, 24, 9, 3);

        // Small pens keep the two reflective creatures near their markers.
        makePen(data, 27, 6, 31, 10);
        makePen(data, 27, 13, 31, 17);

        Files.write(mapFile, data);
    }

    private static void makePen(byte[] data, int x1, int y1, int x2, int y2) {
        fill(data, x1, y1, x2, y1, 9, 3);
        fill(data, x1, y2, x2, y2, 9, 3);
        fill(data, x1, y1, x1, y2, 9, 3);
        fill(data, x2, y1, x2, y2, 9, 3);
    }

    static void appendShowcaseThings(Path thingsFile) throws IOException {
        byte[] original = Files.readAllBytes(thingsFile);
        ByteArrayOutputStream additions = new ByteArrayOutputStream();

        // Bare emissive candle models beside lava. DKII leaves these flames
        // unlit; compare them with the room-constructed Temple candlesticks.
        writeObject(additions, 15, 6, 111, 3);
        writeObject(additions, 15, 10, 111, 3);
        writeObject(additions, 26, 6, 111, 3);
        writeObject(additions, 26, 10, 111, 3);

        // Lair objects exercise the Mistress environment map and Dark Angel 0x10 material.
        writeObject(additions, 11, 21, 23, 3);
        writeObject(additions, 13, 21, 19, 3);

        // Live models make camera-relative environment mapping easy to inspect.
        writeKeeperCreature(additions, 29, 8, 4, 3);  // Mistress
        writeKeeperCreature(additions, 29, 15, 23, 3); // Dark Angel

        byte[] extra = additions.toByteArray();
        byte[] result = new byte[original.length + extra.length];
        System.arraycopy(original, 0, result, 0, original.length);
        System.arraycopy(extra, 0, result, original.length, extra.length);

        ByteBuffer header = ByteBuffer.wrap(result).order(ByteOrder.LITTLE_ENDIAN);
        header.putInt(8, result.length);
        header.putInt(20, header.getInt(20) + 8);
        header.putInt(52, result.length - THINGS_HEADER_SIZE);
        Files.write(thingsFile, result);
    }

    private static void writeObject(ByteArrayOutputStream out, int x, int y, int objectId, int playerId) {
        ByteBuffer record = littleEndian(32);
        record.putInt(194).putInt(24);
        record.putInt(x).putInt(y);
        record.putInt(0).putInt(0).putInt(0);
        record.putShort((short) 0).put((byte) objectId).put((byte) playerId);
        out.writeBytes(record.array());
    }

    private static void writeKeeperCreature(ByteArrayOutputStream out, int x, int y, int creatureId, int playerId) {
        ByteBuffer record = littleEndian(36);
        record.putInt(200).putInt(28);
        record.putInt(x).putInt(y).putInt(0);
        record.putShort((short) 0).put((byte) 1).put((byte) 9);
        record.putInt(100).putInt(0).putShort((short) 0);
        record.put((byte) creatureId).put((byte) playerId);
        out.writeBytes(record.array());
    }

    private static ByteBuffer littleEndian(int size) {
        return ByteBuffer.allocate(size).order(ByteOrder.LITTLE_ENDIAN);
    }

    private static void fill(byte[] data, int x1, int y1, int x2, int y2, int terrain, int player) {
        for (int y = y1; y <= y2; y++) {
            for (int x = x1; x <= x2; x++) {
                setTile(data, x, y, terrain, player);
            }
        }
    }

    private static void setTile(byte[] data, int x, int y, int terrain, int player) {
        int offset = MAP_HEADER_SIZE + (y * MAP_WIDTH + x) * 4;
        data[offset] = (byte) terrain;
        data[offset + 1] = (byte) player;
        // Stock editor maps use 1 as the normal/valid tile marker here.
        data[offset + 2] = 1;
        data[offset + 3] = 1;
    }

    private static void replaceFirst(byte[] data, byte[] oldValue, byte[] newValue) throws IOException {
        int offset = indexOf(data, oldValue);
        if (offset < 0) {
            throw new IOException("Template field not found: " + new String(oldValue, StandardCharsets.US_ASCII));
        }
        if (offset + newValue.length > data.length) {
            throw new IOException("Replacement does not fit template field");
        }
        System.arraycopy(newValue, 0, data, offset, newValue.length);
    }

    private static int indexOf(byte[] data, byte[] value) {
        outer:
        for (int i = 0; i <= data.length - value.length; i++) {
            for (int j = 0; j < value.length; j++) {
                if (data[i + j] != value[j]) {
                    continue outer;
                }
            }
            return i;
        }
        return -1;
    }

    private static byte[] ascii(String value) {
        return value.getBytes(StandardCharsets.US_ASCII);
    }

    private static byte[] utf16(String value) {
        return value.getBytes(StandardCharsets.UTF_16LE);
    }

    private static String readme() {
        return """
                OpenKeeper material showcase
                ============================

                Load MaterialShowcase from the custom-map menu.

                Areas:
                All exhibits form a compact ring around the player-one dungeon heart:
                - North-west: genuine Temple room with room-constructed candlesticks.
                - North-east: lava pool with four bare candle models; water lies below it.
                - East pens: Mistress above, Dark Angel below.
                - South-east: enclosed boss arena used by the companion PortalGemCampaign map.
                - South-west: gem terrain and a lair containing Mistress and Dark Angel beds.
                - Far south-east: blue keeper sealed inside impenetrable rock.

                Toggle Bloom in Graphics Options to compare the lava and candle glow.
                Compare Temple candle flames and wall-torch flames between DKII and OpenKeeper.
                The #TRANS25#icey material cannot be represented by a persistent stock map object;
                use iceicle.kmf in the model viewer for that isolated check.
                """;
    }
}
