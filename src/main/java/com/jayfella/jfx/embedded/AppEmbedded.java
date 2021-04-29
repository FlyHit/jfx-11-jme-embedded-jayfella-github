package com.jayfella.jfx.embedded;

import com.jme3.system.AppSettings;

import java.util.function.Consumer;

/**
 * @author Chen Jiongyu
 */
public interface AppEmbedded {
    void setBeforeInitialization(Consumer<Void> beforeInitialization);

    void setAfterInitialization(Consumer<Void> afterInitialization);

    AppSettings getSettings();
}
