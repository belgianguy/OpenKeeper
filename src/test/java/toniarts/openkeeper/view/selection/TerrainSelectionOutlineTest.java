/*
 * Copyright (C) 2014-2026 OpenKeeper
 *
 * OpenKeeper is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */
package toniarts.openkeeper.view.selection;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.jme3.math.Vector3f;
import com.jme3.scene.VertexBuffer;
import java.nio.FloatBuffer;
import org.junit.jupiter.api.Test;
import toniarts.openkeeper.utils.Point;
import toniarts.openkeeper.view.map.TerrainMeshDeformer;

class TerrainSelectionOutlineTest {

    private static final float EPSILON = 0.000001f;

    @Test
    void multiTileTopEdgeKeepsEveryPerturbedCorner() {
        TerrainSelectionOutline outline = new TerrainSelectionOutline();
        outline.update(new Point(0, 0), new Point(2, 1));

        assertEquals(36, outline.getVertexCount());
        FloatBuffer positions = (FloatBuffer) outline
                .getBuffer(VertexBuffer.Type.Position).getData();

        assertTopCorner(positions, 0, 0, 0);
        assertTopCorner(positions, 1, 1, 0);
        assertTopCorner(positions, 2, 1, 0);
        assertTopCorner(positions, 3, 2, 0);
        assertTopCorner(positions, 4, 2, 0);
        assertTopCorner(positions, 5, 3, 0);
    }

    private static void assertTopCorner(FloatBuffer positions, int vertexIndex,
            int latticeX, int latticeZ) {
        Vector3f expected = TerrainMeshDeformer.topCorner(latticeX, latticeZ)
                .addLocal(0, 0.01f, 0);
        int offset = vertexIndex * 3;
        assertEquals(expected.x, positions.get(offset), EPSILON);
        assertEquals(expected.y, positions.get(offset + 1), EPSILON);
        assertEquals(expected.z, positions.get(offset + 2), EPSILON);
    }
}
