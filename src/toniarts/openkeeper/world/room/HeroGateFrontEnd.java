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

import com.jme3.animation.AnimChannel;
import com.jme3.animation.AnimControl;
import com.jme3.animation.LoopMode;
import com.jme3.asset.AssetManager;
import com.jme3.math.FastMath;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import com.jme3.scene.BatchNode;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import java.awt.Point;

import toniarts.openkeeper.game.data.Settings;
import toniarts.openkeeper.utils.FullMoon;
import toniarts.openkeeper.game.data.Level;
import toniarts.openkeeper.game.data.Level.LevelType;
import toniarts.openkeeper.tools.convert.AssetsConverter;
import toniarts.openkeeper.tools.convert.ConversionUtils;
import toniarts.openkeeper.tools.convert.map.Thing;
import toniarts.openkeeper.world.MapLoader;
import toniarts.openkeeper.world.room.control.FrontEndLevelControl;

/**
 * Loads up a hero gate, front end edition. Main menu. Most of the objects are
 * listed in the objects, but I don't see how they help<br>
 * TODO: Effect on the gem holder & lightning, controls for the level selection
 *
 * @author Toni Helenius <helenius.toni@gmail.com>
 */
public class HeroGateFrontEnd extends GenericRoom {

    public HeroGateFrontEnd(AssetManager assetManager, RoomInstance roomInstance, Thing.Room.Direction direction) {
        super(assetManager, roomInstance, direction);
    }

    /**
     * Load an object asset
     *
     * @param model the model string
     * @param assetManager the asset manager instance
     * @param start starting point
     * @param p the current point
     * @param randomizeAnimation randomize object animation (speed and start
     * time)
     * @return reseted and ready to go model (also animated if there is a such
     * option)
     */
    private Spatial loadObject(String model, AssetManager assetManager, Point start, Point p, boolean randomizeAnimation) {
        Node object = (Node) assetManager.loadModel(ConversionUtils.getCanonicalAssetKey(AssetsConverter.MODELS_FOLDER + "/" + model + ".j3o"));

        // Reset
        resetAndMoveSpatial(object, start, p);
        object.move(0, 1f, 0);

        // Animate
        AnimControl animControl = (AnimControl) object.getChild(0).getControl(AnimControl.class);
        if (animControl != null) {
            AnimChannel channel = animControl.createChannel();
            channel.setAnim("anim");
            channel.setLoopMode(LoopMode.Loop);
            if (randomizeAnimation) {
                channel.setSpeed(FastMath.nextRandomInt(6, 10) / 10f);
                channel.setTime(FastMath.nextRandomFloat() * channel.getAnimMaxTime());
            }

            // Don't batch animated objects, seems not to work
            object.setBatchHint(Spatial.BatchHint.Never);
        }

        return object;
    }

    /**
     * Adds two candles to the tile, one to each side
     *
     * @param n node to attach to
     * @param assetManager the asset manager instance
     * @param start starting point for the room
     * @param p this tile coordinate
     */
    private void addCandles(Node n, AssetManager assetManager, Point start, Point p) {

        // The "candles"
        n.attachChild(loadObject("chain_swing", assetManager, start, p, true).move(-1f, 0, 0));
        Quaternion quat = new Quaternion();
        quat.fromAngleAxis(FastMath.PI, new Vector3f(0, -1, 0));
        n.attachChild(loadObject("chain_swing", assetManager, start, p, true).rotate(quat).move(1f, 0, 1f));
    }

    /**
     * Creates and attach the level node, creates a control for it
     *
     * @param map node to attach to
     * @param type level type
     * @param levelnumber level number
     * @param variation variation, like level "a" etc.
     * @param assetManager the asset manager instance
     * @param start starting point for the room
     * @param p this tile coordinate
     * @param randomizeAnimation randomize object animation (speed and start
     * time)
     */
    private void attachAndCreateLevel(Node map, LevelType type, int levelnumber, String variation, AssetManager assetManager, Point start, Point p, boolean randomizeAnimation) {
        String objName = "3dmap_level";
        if (type.equals(LevelType.Secret)) {
            objName = "Secret_Level";
        }

        Spatial lvl = loadObject(objName + levelnumber + (variation == null ? "" : variation), assetManager, start, p, randomizeAnimation);
        lvl.addControl(new FrontEndLevelControl(new Level(type, levelnumber, variation), assetManager));
        lvl.setBatchHint(Spatial.BatchHint.Never);
        map.attachChild(lvl);
    }

    @Override
    protected BatchNode constructFloor() {
        BatchNode root = new BatchNode();
        // The front end hero gate

        // Contruct the tiles
        int i = 1;
        Point start = roomInstance.getCoordinates().get(0);
        for (Point p : roomInstance.getCoordinates()) {
            Spatial tile = assetManager.loadModel(ConversionUtils.getCanonicalAssetKey(AssetsConverter.MODELS_FOLDER + "/" + roomInstance.getRoom().getCompleteResource().getName() + i + ".j3o"));

            // Reset
            resetAndMoveSpatial(tile, start, p);

            root.attachChild(tile);

            // Add some objects according to the tile number
            switch (i) {
                case 2:
                    root.attachChild(loadObject("3DFE_GemHolder", assetManager, start, p, false));
                    // The light beams, I dunno, there are maybe several of these here
                    Quaternion quat = new Quaternion();
                    quat.fromAngleAxis(FastMath.PI, new Vector3f(0, -1, 0));
                    root.attachChild(loadObject("3dfe_beams", assetManager, start, p, true).rotate(quat).move(0, 0.4f, 0.1f));
                    // TODO: Add a point/spot light here
                    
                    // Banners
                    root.attachChild(loadObject("banner1_swing", assetManager, start, p, true));
                    root.attachChild(loadObject("banner2_swing", assetManager, start, p, true));
                    // The "candles"
                    addCandles(root, assetManager, start, p);
                    break;
                case 5:
                    // Banners
                    root.attachChild(loadObject("banner3_swing", assetManager, start, p, true));
                    root.attachChild(loadObject("banner4_swing", assetManager, start, p, true));
                    // The "candles"
                    addCandles(root, assetManager, start, p);
                    break;
                case 8:
                    // Banners
                    root.attachChild(loadObject("banner1_swing", assetManager, start, p, true));
                    root.attachChild(loadObject("banner2_swing", assetManager, start, p, true));
                    // The "candles"
                    addCandles(root, assetManager, start, p);
                    break;
                case 11:
                    // Banners
                    root.attachChild(loadObject("banner1_swing", assetManager, start, p, true));
                    root.attachChild(loadObject("banner2_swing", assetManager, start, p, true));
                    // The "candles"
                    addCandles(root, assetManager, start, p);
                    // Map
                    Node map = new Node("Map");
                    final int playerLevelReached = Settings.getInstance().getSettingInteger(Settings.Setting.LEVEL_NUMBER);
                    for (int currentLevel = 1; currentLevel < 21; currentLevel++) {
                        switch (currentLevel) {
                            case 6:
                                attachAndCreateLevel(map, LevelType.Level, currentLevel, "a", assetManager, start, p, false);
                                //attachArrows(start, p, map, currentLevel, "3dmaplevel" + currentLevel + "a" + "_arrows", playerLevelReached);
                                attachAndCreateLevel(map, LevelType.Level, currentLevel, "b", assetManager, start, p, false);
                                //attachArrows(start, p, map, currentLevel, "3dmaplevel" + currentLevel + "b" + "_arrows", playerLevelReached);
                                break;
                            case 11:
                                attachAndCreateLevel(map, LevelType.Level, currentLevel, "a", assetManager, start, p, false);
                                //attachArrows(start, p, map, currentLevel, "3dmaplevel" + currentLevel + "a" + "_arrows", playerLevelReached);
                                attachAndCreateLevel(map, LevelType.Level, currentLevel, "b", assetManager, start, p, false);
                                //attachArrows(start, p, map, currentLevel, "3dmaplevel" + currentLevel + "b" + "_arrows", playerLevelReached);
                                attachAndCreateLevel(map, LevelType.Level, currentLevel, "c", assetManager, start, p, false);
                                //attachArrows(start, p, map, currentLevel, "3dmaplevel" + currentLevel + "c" + "_arrows", playerLevelReached);
                                break;
                            case 15:
                                attachAndCreateLevel(map, LevelType.Level, currentLevel, "a", assetManager, start, p, false);
                                //attachArrows(start, p, map, currentLevel, "3dmaplevel" + currentLevel + "a" + "_arrows", playerLevelReached);
                                attachAndCreateLevel(map, LevelType.Level, currentLevel, "b", assetManager, start, p, false);
                                //attachArrows(start, p, map, currentLevel, "3dmaplevel" + currentLevel + "b" + "_arrows", playerLevelReached);
                                break;
                            default:
                                attachAndCreateLevel(map, LevelType.Level, currentLevel, null, assetManager, start, p, false);
                                map.attachChild(loadObject("3dmaplevel" + currentLevel + "_arrows", assetManager, start, p, false));
                                //attachArrows(start, p, map, currentLevel, "3dmaplevel" + currentLevel + "_arrows", playerLevelReached);
                        }
                    }   // Secret levels
                    for (int x = 1; x < 6; x++) {
                        if (x == 5 && !FullMoon.isFullMoon())
                        {
                            // don't show full moon level
                            continue;
                        }
                        attachAndCreateLevel(map, LevelType.Secret, x, null, assetManager, start, p, false);
                    }   // The map base
                    map.attachChild(loadObject("3dmap_level21", assetManager, start, p, false));
                    // Add the map node
                    root.attachChild(map);
                    break;
                default:
                    break;
            }

            i++;
        }

        // Set the transform and scale to our scale and 0 the transform
        root.move(start.x * MapLoader.TILE_WIDTH - MapLoader.TILE_WIDTH / 2, 0, start.y * MapLoader.TILE_HEIGHT - MapLoader.TILE_HEIGHT / 2);
        root.scale(MapLoader.TILE_WIDTH); // Squares anyway...

        return root;
    }

    private void attachArrows(Point start, Point p, Node map, int currentLevel, String prefix, int playerLevelReached) {
        if(playerLevelReached >= currentLevel) {
            map.attachChild(loadObject(prefix, assetManager, start, p, false));
        }
    }

    @Override
    public Spatial getWallSpatial(Point start, WallSection.WallDirection direction) {
        return null;
    }

    @Override
    protected BatchNode constructWall() {
        return null;
    }
}
