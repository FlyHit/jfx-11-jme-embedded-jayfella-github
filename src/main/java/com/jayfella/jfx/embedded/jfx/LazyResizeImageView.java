package com.jayfella.jfx.embedded.jfx;

import javafx.application.Platform;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.geometry.Dimension2D;
import javafx.scene.image.ImageView;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * @author Chen Jiongyu
 */
public class LazyResizeImageView extends ImageView {
    private final ObjectProperty<Dimension2D> lazySize = new SimpleObjectProperty<>();

    private final ExecutorService pool = Executors.newSingleThreadExecutor();
    private int delay = 500;
    private boolean finished = true;

    public LazyResizeImageView() {
        lazySize.addListener((observable, oldValue, newValue) -> {
            if (finished) {
                pool.submit(() -> {
                    finished = false;
                    try {
                        Thread.sleep(delay);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    Platform.runLater(() -> {
                        setFitWidth(getLazySize().getWidth());
                        setFitHeight(getLazySize().getHeight());
                        finished = true;
                    });
                });
            }
        });
    }

    public int getDelay() {
        return delay;
    }

    public void setDelay(int delay) {
        this.delay = delay;
    }

    public Dimension2D getLazySize() {
        return lazySize.get();
    }

    public ObjectProperty<Dimension2D> lazySizeProperty() {
        return lazySize;
    }
}
