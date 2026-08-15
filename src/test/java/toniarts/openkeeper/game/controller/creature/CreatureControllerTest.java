/*
 * Copyright (C) 2014-2026 OpenKeeper
 *
 * OpenKeeper is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */
package toniarts.openkeeper.game.controller.creature;

import com.jme3.math.Vector3f;
import com.simsilica.es.EntityData;
import com.simsilica.es.EntityId;
import com.simsilica.es.base.DefaultEntityData;
import java.lang.reflect.Proxy;
import org.junit.jupiter.api.Test;
import toniarts.openkeeper.game.component.Mobile;
import toniarts.openkeeper.game.component.Position;
import toniarts.openkeeper.game.controller.IMapController;

import static org.junit.jupiter.api.Assertions.assertFalse;

class CreatureControllerTest {

    @Test
    void standingInWaterDoesNotGrantWaterTraversal() {
        CreatureController creature = createCreatureInLiquid();

        assertFalse(creature.canWalkOnWater());
    }

    @Test
    void standingInLavaDoesNotGrantLavaTraversal() {
        CreatureController creature = createCreatureInLiquid();

        assertFalse(creature.canWalkOnLava());
    }

    private static CreatureController createCreatureInLiquid() {
        EntityData entityData = new DefaultEntityData();
        EntityId entityId = entityData.createEntity();
        entityData.setComponents(entityId,
                new Mobile(false, false, false, 1f),
                new Position(0f, new Vector3f(1f, 1f, 1f)));
        IMapController mapController = (IMapController) Proxy.newProxyInstance(
                IMapController.class.getClassLoader(),
                new Class<?>[]{IMapController.class},
                (proxy, method, args) -> switch (method.getName()) {
                    case "isWater", "isLava" -> true;
                    default -> defaultValue(method.getReturnType());
                });

        return new CreatureController(entityId, entityData, null, null, null, null, null,
                null, null, mapController, null, null, null);
    }

    private static Object defaultValue(Class<?> type) {
        if (!type.isPrimitive()) {
            return null;
        }
        if (type == boolean.class) {
            return false;
        }
        if (type == char.class) {
            return '\0';
        }
        return 0;
    }
}
