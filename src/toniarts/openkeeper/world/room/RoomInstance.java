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

import java.awt.Point;
import java.util.List;
import toniarts.openkeeper.tools.convert.map.Room;
import toniarts.openkeeper.tools.convert.map.Terrain;
import toniarts.openkeeper.tools.convert.map.Thing;
import toniarts.openkeeper.world.EntityInstance;
import toniarts.openkeeper.world.MapData;
import toniarts.openkeeper.world.TileData;

/**
 * Holds a room instance, series of coordinates that together form a room
 *
 * @author Toni Helenius <helenius.toni@gmail.com>
 */
public class RoomInstance extends EntityInstance<Room> {

    private List<WallSection> wallSections;
    private final MapData mapData;
    private final Thing.Room.Direction direction;

    public RoomInstance(Room room, MapData mapData) {
        this(room, mapData, null);
    }

    public RoomInstance(Room room, MapData mapData, Thing.Room.Direction direction) {
        super(room);
        this.mapData = mapData;
        this.direction = direction;
    }

    public Room getRoom() {
        return super.getEntity();
    }

    public void setWallSections(List<WallSection> wallSections) {
        this.wallSections = wallSections;
    }

    public List<WallSection> getWallSections() {
        return wallSections;
    }

    /**
     * Get the room owner id
     *
     * @return
     */
    public short getOwnerId() {
        return mapData.getTile(getCoordinates().get(0)).getPlayerId();
    }

    /**
     * Is the room attackable
     *
     * @return is attackable
     */
    boolean isAttackable() {
        return mapData.getTile(getCoordinates().get(0)).getTerrain().getFlags().contains(Terrain.TerrainFlag.ATTACKABLE);
    }

    /**
     * Get the instance health percentage
     *
     * @return health percentage
     */
    public int getHealthPercentage() {
        int health = 0;
        int maxHealth = 0;
        for (Point p : getCoordinates()) {
            TileData tile = mapData.getTile(p);
            health += tile.getHealth();
            maxHealth += tile.getTerrain().getMaxHealth();
        }
        return (int) (health / (float) maxHealth * 100.0);
    }

    /**
     * Get the room instance direction. Some fixed rooms may have this.
     *
     * @return room direction
     */
    public Thing.Room.Direction getDirection() {
        return direction;
    }

}
