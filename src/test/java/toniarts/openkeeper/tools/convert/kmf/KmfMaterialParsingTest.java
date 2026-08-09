package toniarts.openkeeper.tools.convert.kmf;

import java.util.EnumSet;
import org.junit.jupiter.api.Test;
import toniarts.openkeeper.tools.convert.KmfTestData;
import toniarts.openkeeper.tools.convert.kmf.Material.MaterialFlag;

import static org.junit.jupiter.api.Assertions.assertEquals;

class KmfMaterialParsingTest {

    @Test
    void decodesEveryKnownMaterialFlagAndScalar() {
        Material material = KmfTestData.material(0x01ff, 0.75f, 0.4f, "MistressEnvMap");

        assertEquals(EnumSet.allOf(MaterialFlag.class), material.getFlag());
        assertEquals(0.75f, material.getEmissive());
        assertEquals(0.4f, material.getSpecular());
        assertEquals("MistressEnvMap", material.getEnvironmentMapTexture());
    }

    @Test
    void preservesRawScalarsWhenTheirMarkerFlagsAreAbsent() {
        Material material = KmfTestData.material(0, 0.3f, 0.2f, "DefaultEnvMap");

        assertEquals(0.3f, material.getEmissive());
        assertEquals(0.2f, material.getSpecular());
    }
}
