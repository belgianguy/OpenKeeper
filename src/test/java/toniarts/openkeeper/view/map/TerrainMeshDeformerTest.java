/*
 * Copyright (C) 2014-2026 OpenKeeper
 *
 * OpenKeeper is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */
package toniarts.openkeeper.view.map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import org.junit.jupiter.api.Test;

class TerrainMeshDeformerTest {

    private static final float EPSILON = 0.000001f;

    @Test
    void worldGridCornerIsDeterministic() {
        TerrainMeshDeformer.CornerOffset first =
                TerrainMeshDeformer.cornerOffset(12, 24, 0.2f, 255);
        TerrainMeshDeformer.CornerOffset second =
                TerrainMeshDeformer.cornerOffset(12, 24, 0.2f, 255);

        assertEquals(first, second);
        assertNotEquals(new TerrainMeshDeformer.CornerOffset(0, 0, 0), first);
    }

    @Test
    void adjacentTileCagesProduceTheSameSharedEdge() {
        float strength = 0.2f;
        int salt = 255;
        TerrainMeshDeformer.CornerOffset aNorthWest =
                TerrainMeshDeformer.cornerOffset(4, 7, strength, salt);
        TerrainMeshDeformer.CornerOffset sharedNorth =
                TerrainMeshDeformer.cornerOffset(5, 7, strength, salt);
        TerrainMeshDeformer.CornerOffset aSouthWest =
                TerrainMeshDeformer.cornerOffset(4, 8, strength, salt);
        TerrainMeshDeformer.CornerOffset sharedSouth =
                TerrainMeshDeformer.cornerOffset(5, 8, strength, salt);
        TerrainMeshDeformer.CornerOffset bNorthEast =
                TerrainMeshDeformer.cornerOffset(6, 7, strength, salt);
        TerrainMeshDeformer.CornerOffset bSouthEast =
                TerrainMeshDeformer.cornerOffset(6, 8, strength, salt);

        for (float v : new float[]{0, 0.25f, 0.5f, 0.75f, 1}) {
            TerrainMeshDeformer.CornerOffset fromLeft = TerrainMeshDeformer.interpolate(
                    aNorthWest, sharedNorth, aSouthWest, sharedSouth, 1, v);
            TerrainMeshDeformer.CornerOffset fromRight = TerrainMeshDeformer.interpolate(
                    sharedNorth, bNorthEast, sharedSouth, bSouthEast, 0, v);
            assertOffsetEquals(fromLeft, fromRight);
        }
    }

    @Test
    void interpolationReturnsExactCageCorners() {
        TerrainMeshDeformer.CornerOffset northWest = offset(1, 2, 3);
        TerrainMeshDeformer.CornerOffset northEast = offset(4, 5, 6);
        TerrainMeshDeformer.CornerOffset southWest = offset(7, 8, 9);
        TerrainMeshDeformer.CornerOffset southEast = offset(10, 11, 12);

        assertEquals(northWest, TerrainMeshDeformer.interpolate(
                northWest, northEast, southWest, southEast, 0, 0));
        assertEquals(northEast, TerrainMeshDeformer.interpolate(
                northWest, northEast, southWest, southEast, 1, 0));
        assertEquals(southWest, TerrainMeshDeformer.interpolate(
                northWest, northEast, southWest, southEast, 0, 1));
        assertEquals(southEast, TerrainMeshDeformer.interpolate(
                northWest, northEast, southWest, southEast, 1, 1));
    }

    private static TerrainMeshDeformer.CornerOffset offset(float x, float y, float z) {
        return new TerrainMeshDeformer.CornerOffset(x, y, z);
    }

    private static void assertOffsetEquals(TerrainMeshDeformer.CornerOffset expected,
            TerrainMeshDeformer.CornerOffset actual) {
        assertEquals(expected.x(), actual.x(), EPSILON);
        assertEquals(expected.y(), actual.y(), EPSILON);
        assertEquals(expected.z(), actual.z(), EPSILON);
    }
}
