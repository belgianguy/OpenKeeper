/*
 * Copyright (C) 2014-2026 OpenKeeper
 *
 * OpenKeeper is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */
package toniarts.openkeeper.view.selection;

import com.jme3.math.Vector3f;
import com.jme3.scene.Mesh;
import com.jme3.scene.VertexBuffer;
import com.jme3.util.BufferUtils;
import java.util.ArrayList;
import java.util.List;
import toniarts.openkeeper.utils.Point;
import toniarts.openkeeper.utils.WorldUtils;
import toniarts.openkeeper.view.map.TerrainMeshDeformer;

/**
 * Selection box whose top perimeter follows the shared terrain deformation
 * cage. A vertex is emitted at every tile corner so a multi-tile selection
 * retains all bends along its boundary.
 */
final class TerrainSelectionOutline extends Mesh {

    private static final float SURFACE_BIAS = 0.01f;
    private static final float BOTTOM_HEIGHT = -SURFACE_BIAS;

    TerrainSelectionOutline() {
        setMode(Mode.Lines);
        update(new Point(), new Point());
        setDynamic();
    }

    void update(Point first, Point second) {
        int minX = Math.min(first.x, second.x);
        int maxX = Math.max(first.x, second.x) + 1;
        int minZ = Math.min(first.y, second.y);
        int maxZ = Math.max(first.y, second.y) + 1;

        List<Vector3f> vertices = new ArrayList<>();
        appendTopSide(vertices, minX, minZ, maxX, minZ, 1, 0);
        appendTopSide(vertices, maxX, minZ, maxX, maxZ, 0, 1);
        appendTopSide(vertices, maxX, maxZ, minX, maxZ, -1, 0);
        appendTopSide(vertices, minX, maxZ, minX, minZ, 0, -1);

        Vector3f northWest = bottomCorner(minX, minZ);
        Vector3f northEast = bottomCorner(maxX, minZ);
        Vector3f southEast = bottomCorner(maxX, maxZ);
        Vector3f southWest = bottomCorner(minX, maxZ);
        appendLine(vertices, northWest, northEast);
        appendLine(vertices, northEast, southEast);
        appendLine(vertices, southEast, southWest);
        appendLine(vertices, southWest, northWest);

        appendLine(vertices, northWest, topCorner(minX, minZ));
        appendLine(vertices, northEast, topCorner(maxX, minZ));
        appendLine(vertices, southEast, topCorner(maxX, maxZ));
        appendLine(vertices, southWest, topCorner(minX, maxZ));

        clearBuffer(VertexBuffer.Type.Position);
        setBuffer(VertexBuffer.Type.Position, 3,
                BufferUtils.createFloatBuffer(vertices.toArray(Vector3f[]::new)));
        updateBound();
    }

    private static void appendTopSide(List<Vector3f> vertices, int startX,
            int startZ, int endX, int endZ, int stepX, int stepZ) {
        int x = startX;
        int z = startZ;
        while (x != endX || z != endZ) {
            int nextX = x + stepX;
            int nextZ = z + stepZ;
            appendLine(vertices, topCorner(x, z), topCorner(nextX, nextZ));
            x = nextX;
            z = nextZ;
        }
    }

    private static Vector3f topCorner(int x, int z) {
        return TerrainMeshDeformer.topCorner(x, z).addLocal(0, SURFACE_BIAS, 0);
    }

    private static Vector3f bottomCorner(int x, int z) {
        return new Vector3f((x - 0.5f) * WorldUtils.TILE_WIDTH,
                BOTTOM_HEIGHT, (z - 0.5f) * WorldUtils.TILE_WIDTH);
    }

    private static void appendLine(List<Vector3f> vertices, Vector3f start,
            Vector3f end) {
        vertices.add(start);
        vertices.add(end);
    }
}
