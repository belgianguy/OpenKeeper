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
import toniarts.openkeeper.tools.convert.AssetsConverter;
import toniarts.openkeeper.tools.convert.ConversionUtils;
import toniarts.openkeeper.world.object.ObjectLoader;

/**
 * The graveyard
 *
 * @author Toni Helenius <helenius.toni@gmail.com>
 */
public class Graveyard extends Normal {

    public Graveyard(AssetManager assetManager, RoomInstance roomInstance, ObjectLoader objectLoader) {
        super(assetManager, roomInstance, objectLoader);
    }

    @Override
    protected String getPillarResource() {
        return ConversionUtils.getCanonicalAssetKey(AssetsConverter.MODELS_FOLDER + "/Graveyard_Pillar.j3o");
    }
}
