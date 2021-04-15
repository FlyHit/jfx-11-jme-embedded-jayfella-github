package com.jayfella.jfx.embedded;

import com.jme3.app.Application;
import javafx.scene.Node;

/**
 * @author Chen Jiongyu
 */
public interface JmeEmbedJfxApp extends Application {
    /**
     * get the JavaFx canvas for Jme to embed
     *
     * @return canvas
     */
    Node getCanvas();

    /**
     * Indicates that the first of two phases of the engine startup sequence is complete. It has started.
     * The engine is ready for input, but has not yet been initialized.
     * You must set initialized = true in your initApp method.
     *
     * @return whether or not the engine has started.
     */
    boolean isStarted();

    /**
     * Indicates that the second of two phases are complete. The engine is initialized.
     * The engine is waiting for the class to set initialized = true.
     * This usually occurs in the users initApp() method.
     *
     * @return whether or not the engine is initialized.
     */
    boolean isInitialized();

    boolean isJmeThread();
}
