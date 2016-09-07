/*
 * Copyright (C) 2014-2015 OpenKeeper
 *
 * OpenKeeper is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * OpenKeeper is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with OpenKeeper.  If not, see <http://www.gnu.org/licenses/>.
 */
package toniarts.openkeeper.world.room;

import com.jme3.asset.AssetManager;
import com.jme3.math.FastMath;
import com.jme3.math.Vector3f;
import com.jme3.renderer.queue.RenderQueue;
import com.jme3.scene.BatchNode;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import java.awt.Point;
import java.util.HashMap;
import java.util.Map;
import java.util.ResourceBundle;
import toniarts.openkeeper.Main;
import toniarts.openkeeper.tools.convert.AssetsConverter;
import toniarts.openkeeper.tools.convert.map.Room;
import toniarts.openkeeper.utils.AssetUtils;
import toniarts.openkeeper.world.MapLoader;
import toniarts.openkeeper.world.effect.EffectManagerState;
import toniarts.openkeeper.world.object.ObjectLoader;
import toniarts.openkeeper.world.room.control.RoomObjectControl;

/**
 * Base class for all rooms
 *
 * @author Toni Helenius <helenius.toni@gmail.com>
 */
public abstract class GenericRoom {

    public enum ObjectType {

        GOLD, LAIR;

    };

    /**
     * How the objects are laid out, there is always a 1 tile margin from the
     * sides. I don't know where this information really is, so I hardcoded it.
     */
    public enum RoomObjectLayout {

        /**
         * Allow side-to-side, every tile
         */
        ALLOW_NEIGHBOUR,
        /**
         * Allow a neighbouring object diagonally
         */
        ALLOW_DIAGONAL_NEIGHBOUR_ONLY,
        /**
         * No touching!
         */
        ISOLATED;
    }

    protected final AssetManager assetManager;
    protected final RoomInstance roomInstance;
    private final static int[] wallIndexes = new int[]{7, 8};
    private Node root;
    private final String tooltip;
    private static String notOwnedTooltip = null;
    protected final EffectManagerState effectManager;
    private ObjectType defaultObjectType;
    private final Map<ObjectType, RoomObjectControl> objectControls = new HashMap<>();
    private boolean destroyed = false;
    protected boolean[][] map;
    protected Point start;
    protected final ObjectLoader objectLoader;

    public GenericRoom(AssetManager assetManager, EffectManagerState effectManager,
            RoomInstance roomInstance, ObjectLoader objectLoader) {
        this.assetManager = assetManager;
        this.roomInstance = roomInstance;
        this.effectManager = effectManager;
        this.objectLoader = objectLoader;

        // Strings
        ResourceBundle bundle = Main.getResourceBundle("Interface/Texts/Text");
        tooltip = bundle.getString(Integer.toString(roomInstance.getRoom().getTooltipStringId()));
        if (notOwnedTooltip == null) {
            notOwnedTooltip = bundle.getString(Integer.toString(2471));
        }
    }

    public GenericRoom(AssetManager assetManager, RoomInstance roomInstance, ObjectLoader objectLoader) {
        this(assetManager, null, roomInstance, objectLoader);
    }

    protected void setupCoordinates() {
        map = roomInstance.getCoordinatesAsMatrix();
        start = roomInstance.getMatrixStartPoint();
    }

    public Spatial construct() {
        setupCoordinates();

        // Add the floor
        getRootNode().detachAllChildren();
        BatchNode floorNode = constructFloor();
        if (floorNode != null) {
            floorNode.setName("Floor");
            floorNode.setShadowMode(getFloorShadowMode());
            floorNode.batch();
            getRootNode().attachChild(floorNode);
        }

        // Custom wall
        BatchNode wallNode = constructWall();
        if (wallNode != null) {
            wallNode.setName("Wall");
            wallNode.setShadowMode(getWallShadowMode());
            wallNode.batch();
            getRootNode().attachChild(wallNode);
        }

        // The objects on the floor
        Node objectsNode = constructObjects();
        if (objectsNode != null) {
            objectsNode.setName("Objects");
            objectsNode.setShadowMode(RenderQueue.ShadowMode.CastAndReceive);
            getRootNode().attachChild(objectsNode);
        }
        return getRootNode();
    }

    public Node getRootNode() {
        if (root == null) {
            root = new Node(roomInstance.getRoom().getName());
        }
        return root;
    }

    protected abstract BatchNode constructFloor();

    /**
     * Rooms typically don't contruct walls themselves, instead they are asked
     * for the wall spatials by the map loader in normal map drawing situation
     *
     * @see #getWallSpatial(java.awt.Point,
     * toniarts.openkeeper.world.room.WallSection.WallDirection)
     * @return contructed wall
     */
    protected BatchNode constructWall() {
        return null;
    }

    /**
     * Construct room objects
     *
     * @return node with all the room objects
     */
    protected Node constructObjects() {

        // Floor objects 0-2
        Room room = roomInstance.getRoom();
        int index = -1;
        Node objects = new Node();
        if (room.getObjects().get(0) > 0 || room.getObjects().get(1) > 0 || room.getObjects().get(2) > 0) {

            // Object map
            boolean[][] objectMap = new boolean[map.length][map[0].length];

            for (int x = 0; x < map.length; x++) {
                for (int y = 0; y < map[x].length; y++) {

                    // Skip non-room tiles
                    if (!map[x][y]) {
                        continue;
                    }

                    // See neighbouring tiles
                    boolean N = hasSameTile(map, x, y - 1);
                    boolean NE = hasSameTile(map, x + 1, y - 1);
                    boolean E = hasSameTile(map, x + 1, y);
                    boolean SE = hasSameTile(map, x + 1, y + 1);
                    boolean S = hasSameTile(map, x, y + 1);
                    boolean SW = hasSameTile(map, x - 1, y + 1);
                    boolean W = hasSameTile(map, x - 1, y);
                    boolean NW = hasSameTile(map, x - 1, y - 1);

                    if (N && NE && E && SE && S && SW && W && NW) {

                        // Building options
                        N = hasSameTile(objectMap, x, y - 1);
                        NE = hasSameTile(objectMap, x + 1, y - 1);
                        E = hasSameTile(objectMap, x + 1, y);
                        SE = hasSameTile(objectMap, x + 1, y + 1);
                        S = hasSameTile(objectMap, x, y + 1);
                        SW = hasSameTile(objectMap, x - 1, y + 1);
                        W = hasSameTile(objectMap, x - 1, y);
                        NW = hasSameTile(objectMap, x - 1, y - 1);
                        if (getRoomObjectLayout() == RoomObjectLayout.ALLOW_DIAGONAL_NEIGHBOUR_ONLY && (N || E || S || W)) {
                            continue;
                        }
                        if (getRoomObjectLayout() == RoomObjectLayout.ISOLATED && (N || E || S || W || NE || SE || SW || NW)) {
                            continue;
                        }
                        do {
                            if (index > 1) {
                                index = -1;
                            }
                            index++;
                        } while (room.getObjects().get(index) == 0);

                        // Add object
                        objectMap[x][y] = true;
                        objects.attachChild(objectLoader.load(assetManager, start.x + x, start.y + y, 0, 0, 0, room.getObjects().get(index), roomInstance.getOwnerId()));
                    }
                }
            }
        }
        return objects;
    }

    protected boolean hasSameTile(boolean[][] map, int x, int y) {

        // Check for out of bounds
        if (x < 0 || x >= map.length || y < 0 || y >= map[x].length) {
            return false;
        }
        return map[x][y];
    }

    protected RoomObjectLayout getRoomObjectLayout() {
        return RoomObjectLayout.ALLOW_NEIGHBOUR;
    }

    protected RenderQueue.ShadowMode getFloorShadowMode() {
        return RenderQueue.ShadowMode.Receive;
    }

    protected RenderQueue.ShadowMode getWallShadowMode() {
        return RenderQueue.ShadowMode.CastAndReceive;
    }

    public Spatial getWallSpatial(Point p, WallSection.WallDirection direction) {
        float yAngle = FastMath.PI;
        String resource = AssetsConverter.MODELS_FOLDER + "/" + roomInstance.getRoom().getCompleteResource().getName();

        for (WallSection section : roomInstance.getWallSections()) {

            if (section.getDirection() != direction) {
                continue;
            }

            int sectionSize = section.getCoordinates().size();
            for (int i = 0; i < sectionSize; i++) {
                // skip others
                if (!p.equals(section.getCoordinates().get(i))) {
                    continue;
                }

                Spatial spatial;
                if (i == 0 || i == (sectionSize - 1)) {
                    Vector3f moveFirst;
                    Vector3f moveSecond;
                    if (section.getDirection() == WallSection.WallDirection.WEST
                            || section.getDirection() == WallSection.WallDirection.SOUTH) {
                        moveFirst = new Vector3f(-0.25f, 0, -0.25f);
                        moveSecond = new Vector3f(-0.75f, 0, -0.25f);
                    } else { // NORTH, EAST
                        moveFirst = new Vector3f(-0.75f, 0, -0.25f);
                        moveSecond = new Vector3f(-0.25f, 0, -0.25f);
                    }

                    spatial = new BatchNode();
                    int firstPiece = (i == 0 ? 4 : 6);
                    if (firstPiece == 4 && (section.getDirection() == WallSection.WallDirection.EAST
                            || section.getDirection() == WallSection.WallDirection.NORTH)) {
                        firstPiece = 5; // The sorting direction forces us to do this
                    }

                    // Load the piece
                    Spatial part = AssetUtils.loadModel(assetManager, resource + firstPiece + ".j3o", false);
                    resetSpatial(part);
                    part.move(moveFirst);
                    part.rotate(0, yAngle, 0);
                    ((BatchNode) spatial).attachChild(part);

                    // Second
                    int secondPiece = (i == (sectionSize - 1) ? 5 : 6);
                    if (secondPiece == 5 && (section.getDirection() == WallSection.WallDirection.EAST
                            || section.getDirection() == WallSection.WallDirection.NORTH)) {
                        secondPiece = 4; // The sorting direction forces us to do this
                    }

                    part = AssetUtils.loadModel(assetManager, resource + secondPiece + ".j3o", false);
                    resetSpatial(part);
                    part.move(moveSecond);
                    part.rotate(0, yAngle, 0);
                    ((BatchNode) spatial).attachChild(part);

                    ((BatchNode) spatial).batch();
                } else {
                    // Complete walls, 8, 7, 8, 7 and so forth
                    spatial = AssetUtils.loadModel(assetManager, resource + getWallIndex(i) + ".j3o", false);
                    resetSpatial(spatial);
                    spatial.rotate(0, yAngle, 0);

                    if (section.getDirection() == WallSection.WallDirection.WEST) {
                        spatial.move(-MapLoader.TILE_WIDTH / 2, 0, MapLoader.TILE_WIDTH / 2);
                    } else if (section.getDirection() == WallSection.WallDirection.SOUTH) {
                        spatial.move(MapLoader.TILE_WIDTH / 2, 0, MapLoader.TILE_WIDTH / 2);
                    } else if (section.getDirection() == WallSection.WallDirection.EAST) {
                        spatial.move(MapLoader.TILE_WIDTH / 2, 0, -MapLoader.TILE_WIDTH / 2);
                    } else { // NORTH
                        spatial.move(-MapLoader.TILE_WIDTH / 2, 0, -MapLoader.TILE_WIDTH / 2);
                    }
                }

                return spatial;

            }
        }
        return null;
    }

    /**
     * Resets (scale & translation) and moves the spatial to the point. The
     * point is relative to the start point
     *
     * @param tile the tile, spatial
     * @param start start point
     * @param p the tile point
     */
    protected void resetAndMoveSpatial(Spatial tile, Point start, Point p) {

        // Reset, really, the size is 1 after this...
        if (tile instanceof Node) {
            for (Spatial subSpat : ((Node) tile).getChildren()) {
                subSpat.setLocalScale(1);
                subSpat.setLocalTranslation(0, 0, 0);
            }
        } else {
            tile.setLocalScale(1);
            tile.setLocalTranslation(0, 0, 0);
        }
        tile.move(p.x - start.x, -MapLoader.TILE_HEIGHT, p.y - start.y);
    }

    /**
     * Resets (scale & translation) and moves the spatial to the point. The
     * point is relative to the start point
     *
     * @param tile the tile, spatial
     * @param start start point
     */
    protected void resetAndMoveSpatial(Node tile, Point start) {

        // Reset, really, the size is 1 after this...
        for (Spatial subSpat : tile.getChildren()) {
            subSpat.setLocalScale(MapLoader.TILE_WIDTH);
            subSpat.setLocalTranslation(0, 0, 0);
        }
        tile.move(start.x, -MapLoader.TILE_HEIGHT, start.y);
    }

    protected void resetSpatial(Spatial tile) {
        if (tile instanceof Node) {
            for (Spatial subSpat : ((Node) tile).getChildren()) {
                subSpat.setLocalScale(MapLoader.TILE_WIDTH);
                subSpat.setLocalTranslation(0, 0, 0);
            }
        } else {
            tile.setLocalScale(MapLoader.TILE_WIDTH);
            tile.setLocalTranslation(0, 0, 0);
        }
        tile.move(0, -MapLoader.TILE_HEIGHT, 0);
    }

    public int getWallIndex(int index) {
        int pointer = index % wallIndexes.length;
        return wallIndexes[pointer];
    }

    public boolean isTileAccessible(int x, int y) {
        return true;
    }

    public boolean isTileAccessible(Point p) {
        return isTileAccessible(p.x, p.y);
    }

    public String getTooltip(short playerId) {
        if (roomInstance.getOwnerId() != playerId && roomInstance.isAttackable()) {
            return notOwnedTooltip;
        }
        return tooltip.replaceAll("%37", Integer.toString(roomInstance.getHealthPercentage())).replaceAll("%38", Integer.toString(getUsedCapacity())).replaceAll("%39", Integer.toString(getMaxCapacity()));
    }

    protected final Spatial loadModel(String model) {
        return AssetUtils.loadModel(assetManager, AssetsConverter.MODELS_FOLDER
                + "/" + model + ".j3o", false);
    }

    protected void addObjectControl(RoomObjectControl control) {
        objectControls.put(control.getObjectType(), control);
        if (defaultObjectType == null) {
            defaultObjectType = control.getObjectType();
        }
    }

    public boolean hasObjectControl(ObjectType objectType) {
        return objectControls.containsKey(objectType);
    }

    public <T extends RoomObjectControl> T getObjectControl(ObjectType objectType) {
        return (T) objectControls.get(objectType);
    }

    /**
     * Destroy the room, marks the room as destroyed and releases all the
     * controls. The room <strong>should not</strong> be used after this.
     */
    public void destroy() {
        destroyed = true;

        // Destroy the controls
        for (RoomObjectControl control : objectControls.values()) {
            control.destroy();
        }
    }

    /**
     * Is this room instance destroyed? Not in the world anymore.
     *
     * @see #destroy()
     * @return is the room destroyed
     */
    public boolean isDestroyed() {
        return destroyed;
    }

    /**
     * Can store gold to the room?
     *
     * @return can store gold
     */
    public boolean canStoreGold() {
        return hasObjectControl(ObjectType.GOLD);
    }

    /**
     * Get room max capacity
     *
     * @return room max capacity
     */
    protected int getMaxCapacity() {
        RoomObjectControl control = getDefaultRoomObjectControl();
        if (control != null) {
            return control.getMaxCapacity();
        }
        return 0;
    }

    /**
     * Get used capacity
     *
     * @return the used capacity of the room
     */
    protected int getUsedCapacity() {
        RoomObjectControl control = getDefaultRoomObjectControl();
        if (control != null) {
            return control.getCurrentCapacity();
        }
        return 0;
    }

    private RoomObjectControl getDefaultRoomObjectControl() {
        if (defaultObjectType != null) {
            return objectControls.get(defaultObjectType);
        }
        return null;
    }

    public RoomInstance getRoomInstance() {
        return roomInstance;
    }

    /**
     * Is the room at full capacity
     *
     * @return max capacity used
     */
    public boolean isFullCapacity() {
        return getUsedCapacity() >= getMaxCapacity();
    }

    /**
     * Get the room type
     *
     * @return the room type
     */
    public Room getRoom() {
        return roomInstance.getRoom();
    }

    /**
     * Are we the dungeon heart?
     *
     * @return are we?
     */
    public boolean isDungeonHeart() {
        return false;
    }

}
