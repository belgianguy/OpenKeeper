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
    private static final String TEMPLATE_NAME = "Small";
    private static final List<String> FILE_SUFFIXES = List.of(
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

        rewriteLevelFile(maps.resolve(MAP_NAME + ".kwd"));
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
        assertTerrain(level, 18, 6, 5, "lava");
        assertTerrain(level, 18, 16, 4, "water");
        assertTerrain(level, 5, 27, 7, "gem wall");
        assertTerrain(level, 19, 28, 11, "lair");

        long candles = level.getThings(Object.class).stream().filter(thing -> thing.getObjectId() == 111).count();
        long gems = level.getThings(Object.class).stream().filter(thing -> thing.getObjectId() == 28).count();
        boolean mistress = level.getThings(KeeperCreature.class).stream().anyMatch(thing -> thing.getCreatureId() == 4);
        boolean darkAngel = level.getThings(KeeperCreature.class).stream().anyMatch(thing -> thing.getCreatureId() == 23);
        if (candles < 4 || gems < 2 || !mistress || !darkAngel) {
            throw new IllegalStateException("Generated showcase things are incomplete");
        }
    }

    private static void assertTerrain(KwdFile level, int x, int y, int expected, String area) {
        int actual = level.getMap().getTile(x, y).getTerrainId();
        if (actual != expected) {
            throw new IllegalStateException("Invalid " + area + " terrain at " + x + "," + y + ": " + actual);
        }
    }

    private static void rewriteLevelFile(Path levelFile) throws IOException {
        byte[] data = Files.readAllBytes(levelFile);
        replaceFirst(data, utf16(TEMPLATE_NAME), utf16(MAP_NAME));
        for (String suffix : FILE_SUFFIXES.subList(1, FILE_SUFFIXES.size())) {
            replaceFirst(data, ascii(TEMPLATE_NAME + suffix), ascii(MAP_NAME + suffix));
        }
        Files.write(levelFile, data);
    }

    private static void rewriteTiles(Path mapFile) throws IOException {
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

        // Preserve both template dungeon hearts and their starting imps.
        fill(data, 10, 10, 14, 14, 14, 3);
        fill(data, 33, 33, 37, 37, 14, 4);

        // Side-by-side surface comparison, with candles along the lava edge.
        fill(data, 18, 6, 29, 13, 5, 2);
        fill(data, 18, 16, 29, 23, 4, 2);

        // Environment-mapped gem wall and a lair-material display pad.
        fill(data, 5, 27, 14, 30, 7, 2);
        fill(data, 19, 28, 25, 34, 11, 3);

        // Small pens keep the two reflective creatures near their markers.
        makePen(data, 31, 18, 35, 22);
        makePen(data, 37, 18, 41, 22);

        Files.write(mapFile, data);
    }

    private static void makePen(byte[] data, int x1, int y1, int x2, int y2) {
        fill(data, x1, y1, x2, y1, 9, 3);
        fill(data, x1, y2, x2, y2, 9, 3);
        fill(data, x1, y1, x1, y2, 9, 3);
        fill(data, x2, y1, x2, y2, 9, 3);
    }

    private static void appendShowcaseThings(Path thingsFile) throws IOException {
        byte[] original = Files.readAllBytes(thingsFile);
        ByteArrayOutputStream additions = new ByteArrayOutputStream();

        // Emissive candles beside lava.
        writeObject(additions, 17, 7, 111, 3);
        writeObject(additions, 17, 10, 111, 3);
        writeObject(additions, 30, 7, 111, 3);
        writeObject(additions, 30, 10, 111, 3);

        // Sphere-mapped portal gems.
        writeObject(additions, 32, 8, 28, 3);
        writeObject(additions, 35, 8, 28, 3);

        // Lair objects exercise the Mistress environment map and Dark Angel 0x10 material.
        writeObject(additions, 21, 31, 23, 3);
        writeObject(additions, 23, 31, 19, 3);

        // Live models make camera-relative environment mapping easy to inspect.
        writeKeeperCreature(additions, 33, 20, 4, 3);  // Mistress
        writeKeeperCreature(additions, 39, 20, 23, 3); // Dark Angel

        byte[] extra = additions.toByteArray();
        byte[] result = new byte[original.length + extra.length];
        System.arraycopy(original, 0, result, 0, original.length);
        System.arraycopy(extra, 0, result, original.length, extra.length);

        ByteBuffer header = ByteBuffer.wrap(result).order(ByteOrder.LITTLE_ENDIAN);
        header.putInt(8, result.length);
        header.putInt(20, header.getInt(20) + 10);
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
                - North centre: lava and water comparison pools; candles test emissive glow.
                - North-east: two portal gems test environment mapping while rotating the camera.
                - East centre pens: Mistress and Dark Angel test camera-relative reflections.
                - South-west: gem terrain provides another environment-mapping reference.
                - South centre lair: Mistress and Dark Angel beds exercise their special materials.

                Toggle Bloom in Graphics Options to compare the lava and candle glow.
                The #TRANS25#icey material cannot be represented by a persistent stock map object;
                use iceicle.kmf in the model viewer for that isolated check.
                """;
    }
}
