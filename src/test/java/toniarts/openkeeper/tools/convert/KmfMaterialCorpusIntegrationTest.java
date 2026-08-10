package toniarts.openkeeper.tools.convert;

import com.jme3.asset.DesktopAssetManager;
import com.jme3.asset.ModelKey;
import com.jme3.asset.plugins.FileLocator;
import com.jme3.material.Material;
import com.jme3.material.RenderState;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Vector3f;
import com.jme3.scene.Geometry;
import com.jme3.scene.Spatial;
import com.jme3.texture.Texture;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import toniarts.openkeeper.tools.convert.kmf.KmfFile;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@EnabledIfSystemProperty(named = "openkeeper.dk2.root", matches = ".+")
class KmfMaterialCorpusIntegrationTest {

    private static DesktopAssetManager assetManager;
    private static Path meshes;

    @BeforeAll
    static void createAssetManager() {
        assetManager = new DesktopAssetManager(true);
        assetManager.registerLocator("assets", FileLocator.class);
        assetManager.registerLocator("assets/Converted", FileLocator.class);
        meshes = Path.of(System.getProperty("openkeeper.dk2.root"), "Data", "Meshes");
    }

    @Test
    void loadsTheTranslucentIceMaterial() throws IOException {
        Material material = materials("iceicle.kmf").getFirst();

        assertTrue(material.isTransparent());
        assertEquals(RenderState.BlendMode.Alpha, material.getAdditionalRenderState().getBlendMode());
        assertFalse(material.getAdditionalRenderState().isDepthWrite());
        assertEquals(RenderState.FaceCullMode.Off, material.getAdditionalRenderState().getFaceCullMode());
    }

    @Test
    void loadsTheEmissiveTempleBrazierMaterial() throws IOException {
        Material material = materials("3dfe_candle.kmf").stream()
                .filter(candidate -> candidate.getParam("Emissive") != null)
                .findFirst()
                .orElseThrow();

        assertEquals(ColorRGBA.White, material.getParam("Emissive").getValue());
        assertFalse(material.getParams().stream().anyMatch(param -> "GlowColor".equals(param.getName())));
    }

    @Test
    void loadsTheGemSphereEnvironmentMap() throws IOException {
        List<Material> gemMaterials = materials("gem.kmf");
        Material material = gemMaterials.stream()
                .filter(candidate -> candidate.getParam("EnvMap") != null)
                .findFirst()
                .orElseThrow();

        Texture environmentMap = (Texture) material.getParam("EnvMap").getValue();
        assertNotNull(environmentMap);
        assertTrue(environmentMap.getKey().getName().endsWith("EnvmapK.png"));
        assertTrue((Boolean) material.getParam("EnvMapAsSphereMap").getValue());
        assertEquals(new Vector3f(1f, 0f, 1f), material.getParam("FresnelParams").getValue());
        assertEquals(RenderState.FaceCullMode.Off, material.getAdditionalRenderState().getFaceCullMode());

        Material rays = gemMaterials.stream()
                .filter(candidate -> candidate.getAdditionalRenderState().getBlendMode()
                        == RenderState.BlendMode.Additive)
                .findFirst()
                .orElseThrow();
        assertTrue(rays.isTransparent());
        assertFalse(rays.getAdditionalRenderState().isDepthWrite());
        assertEquals(RenderState.FaceCullMode.Off, rays.getAdditionalRenderState().getFaceCullMode());
        assertEquals(ColorRGBA.White, rays.getParam("Emissive").getValue());
    }

    private static List<Material> materials(String fileName) throws IOException {
        Path file = meshes.resolve(fileName);
        KmfFile kmfFile = new KmfFile(file);
        KmfAssetInfo assetInfo = new KmfAssetInfo(assetManager, new ModelKey(fileName), kmfFile, false);
        Spatial model = (Spatial) new KmfModelLoader().load(assetInfo);
        List<Material> materials = new ArrayList<>();
        model.depthFirstTraversal(spatial -> {
            if (spatial instanceof Geometry geometry) {
                materials.add(geometry.getMaterial());
            }
        });
        return materials;
    }
}
