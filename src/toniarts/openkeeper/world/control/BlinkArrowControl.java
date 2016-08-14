package toniarts.openkeeper.world.control;

import com.jme3.asset.AssetManager;
import com.jme3.asset.TextureKey;
import com.jme3.material.Material;
import com.jme3.renderer.RenderManager;
import com.jme3.renderer.ViewPort;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.SceneGraphVisitor;
import com.jme3.scene.Spatial;
import com.jme3.scene.control.AbstractControl;
import com.jme3.texture.Texture;
import toniarts.openkeeper.tools.convert.ConversionUtils;
import toniarts.openkeeper.tools.convert.KmfModelLoader;
import toniarts.openkeeper.utils.AssetUtils;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Created by wietse on 14/08/16.
 */
public class BlinkArrowControl extends AbstractControl {

    private static final Logger logger = Logger.getLogger(BlinkArrowControl.class.getName());

    private static String YELLOW_ARROW_TEXTURE = "Textures/&mask&3dfearrow1.png";
    private static String RED_ARROW_TEXTURE = "Textures/&mask&3dfearrow2.png";


    private AssetManager assetManager;

    private int time;
    private boolean unlimited = false;
    private float tick = 0;
    public final float PERIOD = 0.5f;
    private boolean flashed = false;

    public BlinkArrowControl(AssetManager assetManager) {
        this.assetManager = assetManager;
    }

    public BlinkArrowControl(AssetManager assetManager, int time) {
        this.assetManager = assetManager;
        this.time = time;
        if (this.time == 0) {
            unlimited = true;
        }
    }

    @Override
    public void setSpatial(Spatial spatial) {
        if (spatial == null) {
            enabled = false;
        }
        super.setSpatial(spatial);
    }

    @Override
    protected void controlUpdate(float tpf) {
        if (!enabled) {
            return;
        }

        if (tick >= PERIOD) {
            tick -= PERIOD;
            flashed = !flashed;
            swapTexture((Node) spatial, flashed);
        }

        if (time < 0) {
            if (flashed) {
                swapTexture((Node) spatial, false);
            }
            spatial.removeControl(this);
            return;
        }

        tick += tpf;
        if (!unlimited) {
            time -= tpf;
        }
    }

    private void swapTexture(Node node, boolean flashed) {
        if (node == null) {
            return;
        }

        node.depthFirstTraversal(new SceneGraphVisitor() {
            @Override
            public void visit(Spatial spatial) {
                if (spatial instanceof Geometry) {
                    Material material = ((Geometry) spatial).getMaterial();

                    Integer texCount = spatial.getUserData(KmfModelLoader.MATERIAL_ALTERNATIVE_TEXTURES_COUNT);
                    if(texCount > 0) {
                        final String diffuseTexture;
                        if (flashed) {
                            diffuseTexture = YELLOW_ARROW_TEXTURE;
                        } else {
                            diffuseTexture = RED_ARROW_TEXTURE;
                        }

                        try {
                            Texture texture = assetManager.loadTexture(new TextureKey(ConversionUtils.getCanonicalAssetKey(diffuseTexture), false));
                            material.setTexture("DiffuseMap", texture);

                            AssetUtils.assignMapsToMaterial(assetManager, material);
                        } catch (Exception e) {
                            logger.log(Level.WARNING, "Error applying texture: {0}: {1}", new Object[]{diffuseTexture, e.getMessage()});
                        }
                    }
                }
            }
        });
    }

    @Override
    protected void controlRender(RenderManager rm, ViewPort vp) {

    }


}
