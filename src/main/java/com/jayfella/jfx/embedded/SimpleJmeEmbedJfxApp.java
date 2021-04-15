package com.jayfella.jfx.embedded;

import com.jayfella.jfx.embedded.jfx.FrameTransferSceneProcessor;
import com.jayfella.jfx.embedded.jfx.ImageViewFrameTransferSceneProcessor;
import com.jayfella.jfx.embedded.jfx.JfxMouseInput;
import com.jayfella.jfx.embedded.jfx.LazyResizeImageView;
import com.jayfella.jfx.embedded.jme.JmeOffscreenSurfaceContext;
import com.jme3.app.SimpleApplication;
import com.jme3.app.state.AppState;
import com.jme3.math.ColorRGBA;
import com.jme3.renderer.ViewPort;
import com.jme3.system.AppSettings;

import java.util.List;
import java.util.logging.Logger;

public abstract class SimpleJmeEmbedJfxApp extends SimpleApplication implements JmeEmbedJfxApp {

    private static final Logger log = Logger.getLogger(SimpleJmeEmbedJfxApp.class.getName());

    private final Thread jmeThread;

    private LazyResizeImageView imageView;
    private ImageViewFrameTransferSceneProcessor sceneProcessor;

    private boolean started = false;
    protected boolean initialized = false;

    public SimpleJmeEmbedJfxApp(AppState... initialStates) {
        super(initialStates);

        jmeThread = Thread.currentThread();

        AppSettings settings = new AppSettings(true);

        settings.setCustomRenderer(JmeOffscreenSurfaceContext.class);
        settings.setResizable(true);

        // setPauseOnLostFocus(false);

        setSettings(settings);

        createCanvas();

    }

    @Override
    public void start() {
        JmeOffscreenSurfaceContext canvasContext = (JmeOffscreenSurfaceContext) getContext();
        canvasContext.setApplication(this);
        canvasContext.setSystemListener(this);
        startCanvas(true);
    }

    private void initJavaFxImage() {

        imageView = new LazyResizeImageView();
        imageView.getProperties().put(JfxMouseInput.PROP_USE_LOCAL_COORDS, true);
        imageView.setFocusTraversable(true);

        List<ViewPort> vps = renderManager.getPostViews();
        ViewPort last = vps.get(vps.size()-1);

        sceneProcessor = new ImageViewFrameTransferSceneProcessor();
        sceneProcessor.bind(imageView, this, last);
        sceneProcessor.setEnabled(true);

        sceneProcessor.setTransferMode(FrameTransferSceneProcessor.TransferMode.ON_CHANGES);

    }

    @Override
    public void simpleInitApp() {
        initJavaFxImage();

        viewPort.setBackgroundColor(ColorRGBA.Black);

        started = true;

        log.info("jMonkeyEngine Started.");

        initApp();
    }

    protected abstract void initApp();

    @Override
    public LazyResizeImageView getCanvas() {
        return imageView;
    }

    @Override
    public boolean isStarted() {
        return started;
    }

    @Override
    public boolean isInitialized() {
        return initialized;
    }

    @Override
    public boolean isJmeThread() {
        return Thread.currentThread() == jmeThread;
    }

    public AppSettings getSettings() {
        return settings;
    }
}
