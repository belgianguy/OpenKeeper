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
package toniarts.openkeeper.tools.convert.kmf;

import java.util.EnumSet;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import toniarts.openkeeper.tools.convert.IFlagEnum;

/**
 * KMF Material wrapper
 *
 * @author Toni Helenius <helenius.toni@gmail.com>
 */
public final class Material {

    public enum MaterialFlag implements IFlagEnum {

        HAS_ALPHA(0x0001),
        // DKII disables culling globally; OpenKeeper applies the equivalent per material.
        DOUBLE_SIDED(0x0002),
        ALPHA_ADDITIVE(0x0004),
        // Used by the single #TRANS25#icey material in the DK2 asset corpus.
        TRANSLUCENT(0x0008),
        // DKII maps these two values into its internal transparency-mode family.
        UNKNOWN10(0x0010), // only used for 'Angel Bed RibsCreature Bed_Dark Angel2'
        UNKNOWN20(0x0020), // never used?
        // Enables sphere environment mapping; the following scalar controls its strength.
        HAS_SPECULAR(0x0040),
        // The preceding scalar is a constant self-illumination value.
        HAS_EMISSIVE(0x0080),
        // Used by unused/placeholder materials such as InvisibleKnightBack.
        INVISIBLE(0x0100);
        private final long flagValue;

        private MaterialFlag(long flagValue) {
            this.flagValue = flagValue;
        }

        @Override
        public long getFlagValue() {
            return flagValue;
        }
    };
    private String name;
    private List<String> textures;
    private EnumSet<MaterialFlag> flag;
    private float emissive;
    private float specular;
    private String environmentMapTexture;

    public String getName() {
        return name;
    }

    protected void setName(String name) {
        this.name = name;
    }

    public List<String> getTextures() {
        return textures;
    }

    protected void setTextures(List<String> textures) {
        this.textures = textures;
    }

    public EnumSet<MaterialFlag> getFlag() {
        return flag;
    }

    protected void setFlag(EnumSet<MaterialFlag> flag) {
        this.flag = flag;
    }

    public float getEmissive() {
        return emissive;
    }

    protected void setEmissive(float emissive) {
        this.emissive = emissive;
    }

    public float getSpecular() {
        return specular;
    }

    protected void setSpecular(float specular) {
        this.specular = specular;
    }

    public String getEnvironmentMapTexture() {
        return environmentMapTexture;
    }

    protected void setEnvironmentMapTexture(String environmentMapTexture) {
        this.environmentMapTexture = environmentMapTexture;
    }

    public boolean equalsIgnoreCase(List<String> l1, List<String> l2) {

        // Null check
        if (l1 == null && l2 == null) {
            return true;
        }
        if ((l1 == null && l2 != null) || (l2 == null && l1 != null)) {
            return false;
        }

        // Size check
        if (l1.size() != l2.size()) {
            return false;
        }

        // Go through the individual items
        Iterator<String> i1 = l1.iterator();
        Iterator<String> i2 = l2.iterator();
        while (i1.hasNext()) {
            if (!i1.next().equalsIgnoreCase(i2.next())) {
                return false;
            }
        }
        return true;
    }

    @Override
    public String toString() {
        return name;
    }

    @Override
    public int hashCode() {
        int hash = 5;
        hash = 13 * hash + Objects.hashCode(this.name);
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final Material other = (Material) obj;
        if (Float.floatToIntBits(this.emissive) != Float.floatToIntBits(other.emissive)) {
            return false;
        }
        if (Float.floatToIntBits(this.specular) != Float.floatToIntBits(other.specular)) {
            return false;
        }
        if (!Objects.equals(this.name, other.name)) {
            return false;
        }
        if (!Objects.equals(this.environmentMapTexture, other.environmentMapTexture)) {
            return false;
        }
        if (!Objects.equals(this.textures, other.textures)) {
            return false;
        }
        if (!Objects.equals(this.flag, other.flag)) {
            return false;
        }
        return true;
    }

}
