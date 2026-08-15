/*
 * Copyright (C) 2014-2026 OpenKeeper
 *
 * OpenKeeper is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */
package toniarts.openkeeper.game.logic;

import org.junit.jupiter.api.Test;
import toniarts.openkeeper.game.component.Mobile;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MovementSystemTest {

    private static final float MAX_SPEED = 4f;

    @Test
    void groundedCreatureMovesAtHalfSpeedInWater() {
        Mobile mobile = new Mobile(false, true, false, MAX_SPEED);

        assertEquals(2f, MovementSystem.getEffectiveMaxSpeed(mobile, true));
    }

    @Test
    void flyingCreatureIsNotSlowedByWater() {
        Mobile mobile = new Mobile(true, false, false, MAX_SPEED);

        assertEquals(MAX_SPEED, MovementSystem.getEffectiveMaxSpeed(mobile, true));
    }

    @Test
    void groundedCreatureMovesAtFullSpeedOnLand() {
        Mobile mobile = new Mobile(false, true, false, MAX_SPEED);

        assertEquals(MAX_SPEED, MovementSystem.getEffectiveMaxSpeed(mobile, false));
    }

    @Test
    void waterSlowdownAlsoAppliesWhenCreatureCannotTraverseWater() {
        Mobile mobile = new Mobile(false, false, false, MAX_SPEED);

        assertEquals(2f, MovementSystem.getEffectiveMaxSpeed(mobile, true));
    }
}
