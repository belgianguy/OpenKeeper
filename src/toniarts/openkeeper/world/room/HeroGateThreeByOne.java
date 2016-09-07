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
import com.jme3.scene.BatchNode;
import com.jme3.scene.Spatial;
import java.awt.Point;
import toniarts.openkeeper.tools.convert.AssetsConverter;
import toniarts.openkeeper.tools.convert.map.Thing.Room.Direction;
import toniarts.openkeeper.world.MapLoader;
import toniarts.openkeeper.world.object.ObjectLoader;
import toniarts.openkeeper.world.room.WallSection.WallDirection;

/**
 *
 * @author ArchDemon
 */
public class HeroGateThreeByOne extends GenericRoom {

    public HeroGateThreeByOne(AssetManager assetManager, RoomInstance roomInstance, ObjectLoader objectLoader) {
        super(assetManager, roomInstance, objectLoader);
    }

    @Override
    protected BatchNode constructFloor() {
        BatchNode root = new BatchNode();
        String modelName = roomInstance.getRoom().getCompleteResource().getName();
        Point center = roomInstance.getCenter();
        // Contruct the tiles
        int j = 0;
        for (Point p : roomInstance.getCoordinates()) {
            int piece = (roomInstance.getDirection() == Direction.WEST || roomInstance.getDirection() == Direction.SOUTH) ? j + 3 : 5 - j;
            Spatial tile = assetManager.loadModel(AssetsConverter.MODELS_FOLDER + "/" + modelName + piece + ".j3o");
            j++;
            resetAndMoveSpatial(tile, center, new Point(center.x + p.x, center.y + p.y));
            root.attachChild(tile);

            // Set the transform and scale to our scale and 0 the transform
            switch (roomInstance.getDirection()) {
                case NORTH:
                    tile.rotate(0, -FastMath.HALF_PI, 0);
                    break;
                case EAST:
                    tile.rotate(0, FastMath.PI, 0);
                    break;
                case SOUTH:
                    tile.rotate(0, FastMath.HALF_PI, 0);
                    break;
            }
        }
        root.move(-MapLoader.TILE_WIDTH / 2, 0, -MapLoader.TILE_WIDTH / 2);
        // n.scale(MapLoader.TILE_WIDTH); // Squares anyway...
        return root;
    }

    @Override
    protected BatchNode constructWall() {
        BatchNode root = new BatchNode();
        // Get the wall points
        Point center = roomInstance.getCenter();
        String modelName = roomInstance.getRoom().getCompleteResource().getName();
        for (WallSection section : roomInstance.getWallSections()) {

            int i = 0;
            int sectionSize = section.getCoordinates().size();

            for (Point p : section.getCoordinates()) {

                int piece;
                if (sectionSize == 3) {
                    piece = (section.getDirection() == WallDirection.EAST
                            || section.getDirection() == WallDirection.NORTH) ? 2 - i : i;
                } else {
                    piece = 6;
                }
                i++;
                float yAngle = 0;
                if (section.getDirection() == WallDirection.NORTH) {
                    yAngle = -FastMath.HALF_PI;
                } else if (section.getDirection() == WallDirection.SOUTH) {
                    yAngle = FastMath.HALF_PI;
                } else if (section.getDirection() == WallDirection.EAST) {
                    yAngle = FastMath.PI;
                }
                //yAngle = -section.getDirection().ordinal() * FastMath.HALF_PI;

                Spatial tile = assetManager.loadModel(AssetsConverter.MODELS_FOLDER + "/" + modelName + piece + ".j3o");
                if (yAngle != 0) {
                    tile.rotate(0, yAngle, 0);
                }
                resetAndMoveSpatial(tile, center, new Point(center.x + p.x, center.y + p.y));
                tile.move(-MapLoader.TILE_WIDTH / 2, 0, -MapLoader.TILE_WIDTH / 2);
                root.attachChild(tile);
            }
        }
        return root;
    }

    @Override
    public Spatial getWallSpatial(Point p, WallSection.WallDirection direction) {
        return null;
    }
}
