/*
 * Copyright (C) 2014-2026 OpenKeeper
 *
 * OpenKeeper is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */
package toniarts.openkeeper.view.map;

import com.jme3.math.FastMath;
import com.jme3.math.Transform;
import com.jme3.math.Vector3f;
import com.jme3.scene.Geometry;
import com.jme3.scene.Mesh;
import com.jme3.scene.Spatial;
import com.jme3.scene.VertexBuffer;
import com.jme3.util.BufferUtils;
import com.jme3.util.mikktspace.MikktspaceTangentGenerator;
import java.nio.FloatBuffer;
import java.util.Locale;
import toniarts.openkeeper.utils.Point;
import toniarts.openkeeper.utils.WorldUtils;

/**
 * Deforms private terrain instances against a shared world-grid control cage
 * before they are submitted to a {@code BatchNode}. Every tile evaluates the
 * same deterministic offset for a shared corner, so wall and top meshes remain
 * watertight while consecutive tile edges visibly change direction.
 */
public final class TerrainMeshDeformer {

    private static final float CAGE_DISPLACEMENT = 0.16f;
    private static final int CAGE_SEED = 0x444b32;
    private static final float HEIGHT_FACTOR = 0.3f;

    private long meshCount;
    private long vertexCount;
    private float maximumDisplacement;

    public void deform(Spatial spatial, Point tile, boolean wall) {
        if (spatial == null) {
            return;
        }

        CornerOffset northWest = cornerOffset(tile.x, tile.y,
                CAGE_DISPLACEMENT, CAGE_SEED);
        CornerOffset northEast = cornerOffset(tile.x + 1, tile.y,
                CAGE_DISPLACEMENT, CAGE_SEED);
        CornerOffset southWest = cornerOffset(tile.x, tile.y + 1,
                CAGE_DISPLACEMENT, CAGE_SEED);
        CornerOffset southEast = cornerOffset(tile.x + 1, tile.y + 1,
                CAGE_DISPLACEMENT, CAGE_SEED);

        spatial.updateGeometricState();
        spatial.depthFirstTraversal(candidate -> {
            if (candidate instanceof Geometry geometry) {
                deformGeometry(geometry, northWest, northEast, southWest, southEast, wall);
            }
        });
    }

    private void deformGeometry(Geometry geometry, CornerOffset northWest,
            CornerOffset northEast, CornerOffset southWest, CornerOffset southEast,
            boolean wall) {
        Mesh mesh = geometry.getMesh().deepClone();
        VertexBuffer positionBuffer = mesh.getBuffer(VertexBuffer.Type.Position);
        if (positionBuffer == null || !(positionBuffer.getData() instanceof FloatBuffer positions)) {
            return;
        }

        Transform transform = geometry.getWorldTransform();
        Vector3f local = new Vector3f();
        Vector3f tileLocal = new Vector3f();
        Vector3f deformed = new Vector3f();
        for (int vertexIndex = 0; vertexIndex < mesh.getVertexCount(); vertexIndex++) {
            int offset = vertexIndex * 3;
            local.set(positions.get(offset), positions.get(offset + 1), positions.get(offset + 2));
            transform.transformVector(local, tileLocal);

            float u = FastMath.clamp(tileLocal.x / WorldUtils.TILE_WIDTH + 0.5f, 0, 1);
            float v = FastMath.clamp(tileLocal.z / WorldUtils.TILE_WIDTH + 0.5f, 0, 1);
            CornerOffset cageOffset = interpolate(northWest, northEast,
                    southWest, southEast, u, v);
            float influence = wall
                    ? FastMath.clamp((tileLocal.y - WorldUtils.FLOOR_HEIGHT)
                            / (WorldUtils.TOP_HEIGHT - WorldUtils.FLOOR_HEIGHT), 0, 1)
                    : 1;

            deformed.set(tileLocal.x + cageOffset.x * influence,
                    tileLocal.y + cageOffset.y * influence,
                    tileLocal.z + cageOffset.z * influence);
            maximumDisplacement = Math.max(maximumDisplacement, deformed.distance(tileLocal));
            transform.transformInverseVector(deformed, local);
            positions.put(offset, local.x);
            positions.put(offset + 1, local.y);
            positions.put(offset + 2, local.z);
            vertexCount++;
        }

        positionBuffer.updateData(positions);
        recomputeNormals(mesh);
        mesh.clearCollisionData();
        mesh.updateBound();
        geometry.setMesh(mesh);
        MikktspaceTangentGenerator.generate(geometry);
        geometry.updateModelBound();
        meshCount++;
    }

    static CornerOffset cornerOffset(int latticeX, int latticeZ, float strength, int salt) {
        return new CornerOffset(
                signedHash(latticeX, latticeZ, salt, 0) * strength,
                signedHash(latticeX, latticeZ, salt, 1) * strength * HEIGHT_FACTOR,
                signedHash(latticeX, latticeZ, salt, 2) * strength);
    }

    /**
     * Gets the displaced world-space position of a terrain top corner. Lattice
     * coordinate {@code (x, z)} is the north-west corner of tile
     * {@code (x, z)}, whose undisplaced position is half a tile before the
     * tile center on both horizontal axes.
     *
     * @param latticeX corner coordinate on the terrain grid
     * @param latticeZ corner coordinate on the terrain grid
     * @return a new vector at the same position used by deformed top meshes
     */
    public static Vector3f topCorner(int latticeX, int latticeZ) {
        CornerOffset offset = cornerOffset(latticeX, latticeZ,
                CAGE_DISPLACEMENT, CAGE_SEED);
        return new Vector3f(
                (latticeX - 0.5f) * WorldUtils.TILE_WIDTH + offset.x,
                WorldUtils.TOP_HEIGHT + offset.y,
                (latticeZ - 0.5f) * WorldUtils.TILE_WIDTH + offset.z);
    }

    static CornerOffset interpolate(CornerOffset northWest, CornerOffset northEast,
            CornerOffset southWest, CornerOffset southEast, float u, float v) {
        float northX = FastMath.interpolateLinear(u, northWest.x, northEast.x);
        float northY = FastMath.interpolateLinear(u, northWest.y, northEast.y);
        float northZ = FastMath.interpolateLinear(u, northWest.z, northEast.z);
        float southX = FastMath.interpolateLinear(u, southWest.x, southEast.x);
        float southY = FastMath.interpolateLinear(u, southWest.y, southEast.y);
        float southZ = FastMath.interpolateLinear(u, southWest.z, southEast.z);
        return new CornerOffset(FastMath.interpolateLinear(v, northX, southX),
                FastMath.interpolateLinear(v, northY, southY),
                FastMath.interpolateLinear(v, northZ, southZ));
    }

    public String summary() {
        return String.format(Locale.ROOT,
                "%d private meshes, %d vertices, maximum %.3f world units",
                meshCount, vertexCount, maximumDisplacement);
    }

    private static float signedHash(int x, int z, int salt, int component) {
        long value = 0x9e3779b97f4a7c15L;
        value ^= Integer.toUnsignedLong(x) * 0x632be59bd9b4e019L;
        value ^= Integer.toUnsignedLong(z) * 0xc2b2ae3d27d4eb4fL;
        value ^= Integer.toUnsignedLong(salt) * 0x165667b19e3779f9L;
        value ^= Integer.toUnsignedLong(component) * 0x85ebca77c2b2ae63L;
        value ^= value >>> 30;
        value *= 0xbf58476d1ce4e5b9L;
        value ^= value >>> 27;
        value *= 0x94d049bb133111ebL;
        value ^= value >>> 31;
        return ((value >>> 40) & 0xffffffL) / 8388607.5f - 1f;
    }

    private static void recomputeNormals(Mesh mesh) {
        VertexBuffer positionBuffer = mesh.getBuffer(VertexBuffer.Type.Position);
        if (positionBuffer == null || !(positionBuffer.getData() instanceof FloatBuffer positions)) {
            return;
        }
        float[] normals = new float[mesh.getVertexCount() * 3];
        int[] triangle = new int[3];
        Vector3f first = new Vector3f();
        Vector3f second = new Vector3f();
        Vector3f third = new Vector3f();
        for (int triangleIndex = 0; triangleIndex < mesh.getTriangleCount(); triangleIndex++) {
            mesh.getTriangle(triangleIndex, triangle);
            readPosition(positions, triangle[0], first);
            readPosition(positions, triangle[1], second);
            readPosition(positions, triangle[2], third);
            Vector3f normal = second.subtract(first).crossLocal(third.subtract(first));
            addNormal(normals, triangle[0], normal);
            addNormal(normals, triangle[1], normal);
            addNormal(normals, triangle[2], normal);
        }
        Vector3f normal = new Vector3f();
        for (int vertexIndex = 0; vertexIndex < mesh.getVertexCount(); vertexIndex++) {
            int offset = vertexIndex * 3;
            normal.set(normals[offset], normals[offset + 1], normals[offset + 2]);
            if (normal.lengthSquared() != 0) {
                normal.normalizeLocal();
            }
            normals[offset] = normal.x;
            normals[offset + 1] = normal.y;
            normals[offset + 2] = normal.z;
        }
        mesh.setBuffer(VertexBuffer.Type.Normal, 3, BufferUtils.createFloatBuffer(normals));
    }

    private static void readPosition(FloatBuffer positions, int vertexIndex, Vector3f store) {
        int offset = vertexIndex * 3;
        store.set(positions.get(offset), positions.get(offset + 1), positions.get(offset + 2));
    }

    private static void addNormal(float[] normals, int vertexIndex, Vector3f normal) {
        int offset = vertexIndex * 3;
        normals[offset] += normal.x;
        normals[offset + 1] += normal.y;
        normals[offset + 2] += normal.z;
    }

    record CornerOffset(float x, float y, float z) {
    }
}
