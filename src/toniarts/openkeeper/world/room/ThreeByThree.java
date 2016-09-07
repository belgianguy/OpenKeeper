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
import com.jme3.scene.BatchNode;
import com.jme3.scene.Spatial;
import java.awt.Point;
import toniarts.openkeeper.tools.convert.AssetsConverter;
import toniarts.openkeeper.tools.convert.ConversionUtils;
import toniarts.openkeeper.world.MapLoader;
import toniarts.openkeeper.world.object.ObjectLoader;

/**
 * Portal is the only one I think
 *
 * @author Toni Helenius <helenius.toni@gmail.com>
 */
public class ThreeByThree extends GenericRoom implements ICreatureEntrance {

    public ThreeByThree(AssetManager assetManager, RoomInstance roomInstance, ObjectLoader objectLoader) {
        super(assetManager, roomInstance, objectLoader);
    }

    @Override
    protected BatchNode constructFloor() {
        BatchNode root = new BatchNode();
        // 3 by 3, a simple case
        int i = 0;
        Point start = roomInstance.getCoordinates().get(0);
        for (Point p : roomInstance.getCoordinates()) {
            Spatial tile = (Spatial) assetManager.loadModel(ConversionUtils.getCanonicalAssetKey(AssetsConverter.MODELS_FOLDER + "/" + roomInstance.getRoom().getCompleteResource().getName() + i + ".j3o"));

            resetAndMoveSpatial(tile, start, p);

            root.attachChild(tile);
            i++;
        }

        // Set the transform and scale to our scale and 0 the transform
        root.move(start.x * MapLoader.TILE_WIDTH - MapLoader.TILE_WIDTH / 2, 0, start.y * MapLoader.TILE_HEIGHT - MapLoader.TILE_HEIGHT / 2);
        root.scale(MapLoader.TILE_WIDTH); // Squares anyway...

        return root;
    }

    @Override
    public boolean isTileAccessible(int x, int y) {

        // The center tile is not accessible
        Point roomPoint = roomInstance.worldCoordinateToLocalCoordinate(x, y);
        return !(roomPoint.x == 1 && roomPoint.y == 1);
    }

    @Override
    public Point getEntranceCoordinate() {
        return roomInstance.getCoordinates().get(0);
    }
}
