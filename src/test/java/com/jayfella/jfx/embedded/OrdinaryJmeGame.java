package com.jayfella.jfx.embedded;

import com.jme3.app.SimpleApplication;
import com.jme3.light.DirectionalLight;
import com.jme3.material.Material;
import com.jme3.material.Materials;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Vector3f;
import com.jme3.scene.Geometry;
import com.jme3.scene.shape.Box;
import com.jme3.system.AppSettings;
import com.jme3.texture.Texture;

import java.util.function.Consumer;

/**
 * @author Chen Jiongyu
 */
public class OrdinaryJmeGame extends SimpleApplication implements AppEmbedded {
    private Geometry box;
    private Consumer<Void> beforeInitialization;
    private Consumer<Void> afterInitialization;

    public static void main(String[] args) {
        OrdinaryJmeGame game = new OrdinaryJmeGame();
        AppSettings appSettings = new AppSettings(true);
        appSettings.setUseJoysticks(true);
        appSettings.setGammaCorrection(true);
        appSettings.setSamples(16);
        appSettings.setFrameRate(-1);
//        appSettings.setVSync(true);
        appSettings.setResizable(true);
        game.setSettings(appSettings);
        game.setShowSettings(false);
        game.start();
    }

    @Override
    public void simpleInitApp() {
        if (beforeInitialization != null) {
            beforeInitialization.accept(null);
        }

        flyCam.setDragToRotate(true);
        flyCam.setMoveSpeed(6);

        DirectionalLight directionalLight = new DirectionalLight(
                new Vector3f(-1, -1, -1).normalizeLocal(),
                ColorRGBA.White.clone()
        );

        rootNode.addLight(directionalLight);

        Texture texture = assetManager.loadTexture("com/jme3/app/Monkey.png");

        box = new Geometry("Box", new Box(1, 1, 1));
        box.setMaterial(new Material(assetManager, Materials.PBR));
        box.getMaterial().setTexture("BaseColorMap", texture);
        box.getMaterial().setColor("BaseColor", ColorRGBA.White);
        box.getMaterial().setFloat("Roughness", 0.001f);
        box.getMaterial().setFloat("Metallic", 0.001f);

        rootNode.attachChild(box);

        viewPort.setBackgroundColor(ColorRGBA.White);

//        GLUtil.setupDebugMessageCallback();
        if (afterInitialization != null) {
            afterInitialization.accept(null);
        }
    }

    @Override
    public void simpleUpdate(float tpf) {
        box.rotate(tpf * .2f, tpf * .3f, tpf * .4f);
    }

    @Override
    public void setBeforeInitialization(Consumer<Void> beforeInitialization) {
        this.beforeInitialization = beforeInitialization;
    }

    @Override
    public void setAfterInitialization(Consumer<Void> afterInitialization) {
        this.afterInitialization = afterInitialization;
    }

    @Override
    public AppSettings getSettings() {
        return settings;
    }
}
