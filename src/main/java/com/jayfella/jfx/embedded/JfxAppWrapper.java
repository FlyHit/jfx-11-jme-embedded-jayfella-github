package com.jayfella.jfx.embedded;

import com.jayfella.jfx.embedded.jfx.FrameTransferSceneProcessor;
import com.jayfella.jfx.embedded.jfx.ImageViewFrameTransferSceneProcessor;
import com.jayfella.jfx.embedded.jfx.JfxMouseInput;
import com.jayfella.jfx.embedded.jfx.LazyResizeImageView;
import com.jayfella.jfx.embedded.jme.JmeOffscreenSurfaceContext;
import com.jme3.app.LegacyApplication;
import com.jme3.app.LostFocusBehavior;
import com.jme3.app.state.AppStateManager;
import com.jme3.asset.AssetManager;
import com.jme3.audio.AudioRenderer;
import com.jme3.audio.Listener;
import com.jme3.input.InputManager;
import com.jme3.profile.AppProfiler;
import com.jme3.renderer.Camera;
import com.jme3.renderer.RenderManager;
import com.jme3.renderer.Renderer;
import com.jme3.renderer.ViewPort;
import com.jme3.system.AppSettings;
import com.jme3.system.JmeContext;
import com.jme3.system.Timer;

import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;

/**
 * @author Chen Jiongyu
 */
public class JfxAppWrapper implements JmeEmbedJfxApp {
    private final LegacyApplication application;
    private final Thread jmeThread;
    protected boolean initialized = false;
    private LazyResizeImageView imageView;
    private final ImageViewFrameTransferSceneProcessor sceneProcessor;
    private boolean started = false;

    public JfxAppWrapper(AppEmbedded application) {
        if (!(application instanceof LegacyApplication)) {
            throw new IllegalArgumentException();
        }

        this.application = (LegacyApplication) application;
        application.getSettings().setCustomRenderer(JmeOffscreenSurfaceContext.class);
        this.application.createCanvas();
        jmeThread = Thread.currentThread();

        application.setBeforeInitialization(unused -> {
            initJavaFxImage();
            started = true;
        });
        application.setAfterInitialization(unused -> initialized = true);

        sceneProcessor = new ImageViewFrameTransferSceneProcessor();
        sceneProcessor.setNumSamples(application.getSettings().getSamples());
    }

    private void initJavaFxImage() {
        imageView = new LazyResizeImageView();
        imageView.getProperties().put(JfxMouseInput.PROP_USE_LOCAL_COORDS, true);
        imageView.setFocusTraversable(true);
        List<ViewPort> vps = application.getRenderManager().getPostViews();
        ViewPort last = vps.get(vps.size() - 1);
        sceneProcessor.bind(imageView, this, last);
        sceneProcessor.setEnabled(true);
        sceneProcessor.setTransferMode(FrameTransferSceneProcessor.TransferMode.ON_CHANGES);
    }

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

    @Override
    public LostFocusBehavior getLostFocusBehavior() {
        return application.getLostFocusBehavior();
    }

    @Override
    public void setLostFocusBehavior(LostFocusBehavior lostFocusBehavior) {
        application.setLostFocusBehavior(lostFocusBehavior);
    }

    @Override
    public boolean isPauseOnLostFocus() {
        return application.isPauseOnLostFocus();
    }

    @Override
    public void setPauseOnLostFocus(boolean pauseOnLostFocus) {
        application.setPauseOnLostFocus(pauseOnLostFocus);
    }

    @Override
    public void setSettings(AppSettings settings) {
        application.setSettings(settings);
    }

    @Override
    public Timer getTimer() {
        return application.getTimer();
    }

    @Override
    public void setTimer(Timer timer) {
        application.setTimer(timer);
    }

    @Override
    public AssetManager getAssetManager() {
        return application.getAssetManager();
    }

    @Override
    public InputManager getInputManager() {
        return application.getInputManager();
    }

    @Override
    public AppStateManager getStateManager() {
        return application.getStateManager();
    }

    @Override
    public RenderManager getRenderManager() {
        return application.getRenderManager();
    }

    @Override
    public Renderer getRenderer() {
        return application.getRenderer();
    }

    @Override
    public AudioRenderer getAudioRenderer() {
        return application.getAudioRenderer();
    }

    @Override
    public Listener getListener() {
        return application.getListener();
    }

    @Override
    public JmeContext getContext() {
        return application.getContext();
    }

    @Override
    public Camera getCamera() {
        return application.getCamera();
    }

    @Override
    public void start() {
        JmeOffscreenSurfaceContext canvasContext = (JmeOffscreenSurfaceContext) getContext();
        canvasContext.setApplication(this);
        canvasContext.setSystemListener(application);
        application.startCanvas(true);
    }

    @Override
    public void start(boolean waitFor) {
        throw new UnsupportedOperationException();
    }

    @Override
    public AppProfiler getAppProfiler() {
        return application.getAppProfiler();
    }

    @Override
    public void setAppProfiler(AppProfiler prof) {
        application.setAppProfiler(prof);
    }

    @Override
    public void restart() {
        application.restart();
    }

    @Override
    public void stop() {
        application.stop();
    }

    @Override
    public void stop(boolean waitFor) {
        application.stop(waitFor);
    }

    @Override
    public <V> Future<V> enqueue(Callable<V> callable) {
        return application.enqueue(callable);
    }

    @Override
    public void enqueue(Runnable runnable) {
        application.enqueue(runnable);
    }

    @Override
    public ViewPort getGuiViewPort() {
        return application.getGuiViewPort();
    }

    @Override
    public ViewPort getViewPort() {
        return application.getViewPort();
    }
}
