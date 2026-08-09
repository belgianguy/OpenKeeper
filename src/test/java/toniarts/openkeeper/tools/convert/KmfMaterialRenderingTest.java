package toniarts.openkeeper.tools.convert;

import com.jme3.asset.AssetKey;
import com.jme3.asset.DesktopAssetManager;
import com.jme3.asset.plugins.FileLocator;
import com.jme3.material.Material;
import com.jme3.material.RenderState;
import com.jme3.material.RenderState.FaceCullMode;
import com.jme3.math.ColorRGBA;
import com.jme3.shader.VarType;
import java.util.EnumSet;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import toniarts.openkeeper.tools.convert.kmf.Material.MaterialFlag;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class KmfMaterialRenderingTest {

    private static DesktopAssetManager assetManager;

    @BeforeAll
    static void createAssetManager() {
        assetManager = new DesktopAssetManager(true);
        assetManager.registerLocator("assets", FileLocator.class);
    }

    @Test
    void materialDefinitionSupportsEmissiveAndTwoDimensionalEnvironmentMaps() {
        Material material = createMaterial();

        assertEquals(VarType.Vector4, material.getMaterialDef().getMaterialParam("Emissive").getVarType());
        assertEquals(VarType.Texture2D, material.getMaterialDef().getMaterialParam("EnvMap").getVarType());
        assertNotNull(assetManager.locateAsset(new AssetKey<>("Shaders/KmfLighting.frag")));
        assertNotNull(assetManager.locateAsset(new AssetKey<>("Shaders/KmfSPLighting.frag")));
    }

    @Test
    void appliesTranslucencyDoubleSidedRenderingAndEmission() {
        var source = KmfTestData.material(flags(MaterialFlag.DOUBLE_SIDED, MaterialFlag.TRANSLUCENT,
                MaterialFlag.HAS_EMISSIVE), 0.6f, 0f, "DefaultEnvMap");
        Material material = createMaterial();

        KmfModelLoader.setMaterialFlags(material, source);

        assertTrue(material.isTransparent());
        assertEquals(RenderState.BlendMode.Alpha, material.getAdditionalRenderState().getBlendMode());
        assertFalse(material.getAdditionalRenderState().isDepthWrite());
        assertEquals(FaceCullMode.Off, material.getAdditionalRenderState().getFaceCullMode());
        assertEquals(new ColorRGBA(0.6f, 0.6f, 0.6f, 1f), material.getParam("Emissive").getValue());
        assertEquals(new ColorRGBA(0.6f, 0.6f, 0.6f, 1f), material.getParam("GlowColor").getValue());
    }

    @Test
    void additiveMaterialsDoNotWriteDepthOrReceiveShadows() {
        var source = KmfTestData.material(flags(MaterialFlag.ALPHA_ADDITIVE), 0f, 0f, "DefaultEnvMap");
        Material material = createMaterial();

        KmfModelLoader.setMaterialFlags(material, source);

        assertEquals(RenderState.BlendMode.AlphaAdditive, material.getAdditionalRenderState().getBlendMode());
        assertFalse(material.getAdditionalRenderState().isDepthWrite());
        assertFalse(material.isReceivesShadows());
    }

    @Test
    void invisibleMaterialsWriteNeitherColorNorDepth() {
        var source = KmfTestData.material(flags(MaterialFlag.INVISIBLE), 0f, 0f, "DefaultEnvMap");
        Material material = createMaterial();

        KmfModelLoader.setMaterialFlags(material, source);

        assertTrue(material.isTransparent());
        assertFalse(material.getAdditionalRenderState().isColorWrite());
        assertFalse(material.getAdditionalRenderState().isDepthWrite());
        assertFalse(material.isReceivesShadows());
    }

    private static Material createMaterial() {
        return new Material(assetManager, "MatDefs/KmfLighting.j3md");
    }

    private static int flags(MaterialFlag... flags) {
        return EnumSet.copyOf(java.util.List.of(flags)).stream()
                .mapToInt(flag -> (int) flag.getFlagValue())
                .reduce(0, (left, right) -> left | right);
    }
}
