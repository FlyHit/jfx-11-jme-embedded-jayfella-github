package com.jayfella.jfx.embedded.jfx;

import com.jme3.renderer.RenderManager;
import com.jme3.renderer.Renderer;
import com.jme3.texture.FrameBuffer;
import com.jme3.texture.Image;
import com.jme3.util.BufferUtils;
import javafx.animation.AnimationTimer;
import javafx.application.Platform;
import javafx.scene.image.PixelFormat;
import javafx.scene.image.PixelWriter;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.FutureTask;

/**
 * The base implementation of a frame transfer.
 *
 * @param <T> the destination's type.
 * @author JavaSaBr
 */
public abstract class AbstractFrameTransfer<T> implements FrameTransfer {
    /**
     * The Frame buffer.
     */
    protected final FrameBuffer frameBuffer;

    /**
     * The Pixel writer.
     */
    protected final PixelWriter pixelWriter;

    /**
     * The Frame byte buffer.
     */
    protected final ByteBuffer frameByteBuffer;

    /**
     * The transfer mode.
     */
    protected final FrameTransferSceneProcessor.TransferMode transferMode;

    /**
     * The byte buffer.
     */
    protected final byte[] byteBuffer;

    /**
     * The image byte buffer.
     */
    protected final byte[] imageByteBuffer;

    /**
     * The prev image byte buffer.
     */
    protected final byte[] prevImageByteBuffer;

    /**
     * How many frames need to write else.
     */
    protected int frameCount;

    /**
     * The width.
     */
    private final int width;

    /**
     * The height.
     */
    private final int height;

    private static int numSamples = 1;
    private static Image.Format imageFormat;
    private static long frame;
    private static long lastLoad;
    private final AnimationTimer timer;
    private boolean finished = true;
    private FrameBuffer normalFrameBuffer;

    static {
        try {
            imageFormat = ImageUtil.getImageFormat();
        } catch (ExecutionException | InterruptedException e) {
            e.printStackTrace();
        }
    }

    private final ExecutorService pool = Executors.newSingleThreadExecutor();

    public AbstractFrameTransfer(T destination, int width, int height, FrameTransferSceneProcessor.TransferMode transferMode) {
        this(destination, transferMode, null, width, height);
    }

    public AbstractFrameTransfer(
            T destination,
            FrameTransferSceneProcessor.TransferMode transferMode,
            FrameBuffer frameBuffer,
            int width,
            int height
    ) {
        this.transferMode = transferMode;
        this.width = frameBuffer != null ? frameBuffer.getWidth() : width;
        this.height = frameBuffer != null ? frameBuffer.getHeight() : height;
        this.frameCount = 0;

        if (frameBuffer != null) {
            this.frameBuffer = frameBuffer;
        } else {
            this.frameBuffer = new FrameBuffer(width, height, numSamples);
            this.frameBuffer.setColorBuffer(imageFormat);
            this.frameBuffer.setDepthBuffer(Image.Format.Depth);
            this.frameBuffer.setSrgb(true);
        }

        if (numSamples > 1) {
            normalFrameBuffer = new FrameBuffer(width, height, 1);
            normalFrameBuffer.setColorBuffer(imageFormat);
            normalFrameBuffer.setDepthBuffer(Image.Format.Depth);
            normalFrameBuffer.setSrgb(true);
        }

        frameByteBuffer = BufferUtils.createByteBuffer(getWidth() * getHeight() * 4);
        byteBuffer = new byte[getWidth() * getHeight() * 4];
        prevImageByteBuffer = new byte[getWidth() * getHeight() * 4];
        imageByteBuffer = new byte[getWidth() * getHeight() * 4];
        pixelWriter = getPixelWriter(destination, this.frameBuffer, width, height);

        timer = new AnimationTimer() {
            @Override
            public void handle(long now) {
                frame++;
            }
        };
        timer.start();
    }

    @Override
    public void initFor(Renderer renderer, boolean main) {
        if (main) {
            renderer.setMainFrameBufferOverride(frameBuffer);
        }
    }

    /**
     * Get the pixel writer.
     *
     * @param destination the destination.
     * @param frameBuffer the frame buffer.
     * @param width       the width.
     * @param height      the height.
     * @return the pixel writer.
     */
    protected PixelWriter getPixelWriter(T destination, FrameBuffer frameBuffer, int width, int height) {
        throw new UnsupportedOperationException();
    }

    @Override
    public int getWidth() {
        return width;
    }

    @Override
    public int getHeight() {
        return height;
    }

    public static void setNumSamples(int numSamples) {
        AbstractFrameTransfer.numSamples = numSamples;
    }

    @Override
    public void copyFrameBufferToImage(RenderManager renderManager) {
        if (frame <= lastLoad + 1) {
            return;
        }
        lastLoad = frame;

        synchronized (this) {
            if (!finished) {
                return;
            }
            finished = false;
        }

        // Convert screenshot.
        frameByteBuffer.clear();
        Renderer renderer = renderManager.getRenderer();
        FrameBuffer frameBufferToRead = frameBuffer;
        if (numSamples > 1) {
            renderer.setFrameBuffer(normalFrameBuffer);
            renderer.copyFrameBuffer(frameBuffer, normalFrameBuffer, false);
            frameBufferToRead = normalFrameBuffer;
        }
        renderer.readFrameBufferWithFormat(frameBufferToRead, frameByteBuffer, imageFormat);
        renderer.setFrameBuffer(null);

        pool.submit(() -> {
            frameByteBuffer.get(byteBuffer);
            if (transferMode == FrameTransferSceneProcessor.TransferMode.ON_CHANGES) {
                final byte[] prevBuffer = getPrevImageByteBuffer();
                if (Arrays.equals(prevBuffer, byteBuffer)) {
                    if (frameCount == 0) {
                        synchronized (this) {
                            finished = true;
                        }
                        return;
                    }
                } else {
                    frameCount = 2;
                    System.arraycopy(byteBuffer, 0, prevBuffer, 0, byteBuffer.length);
                }

                frameByteBuffer.position(0);
                frameCount--;
            }

            FutureTask<Boolean> writeFrame = new FutureTask<>(() -> {
                writeFrame();
                return true;
            });
            Platform.runLater(writeFrame);
            try {
                if (writeFrame.get()) {
                    synchronized (this) {
                        finished = true;
                    }
                }
            } catch (InterruptedException | ExecutionException e) {
                e.printStackTrace();
            }
        });
    }

    /**
     * Get the image byte buffer.
     *
     * @return the image byte buffer.
     */
    protected byte[] getImageByteBuffer() {
        return imageByteBuffer;
    }

    /**
     * Get the prev image byte buffer.
     *
     * @return the prev image byte buffer.
     */
    protected byte[] getPrevImageByteBuffer() {
        return prevImageByteBuffer;
    }

    /**
     * Write content to image.
     */
    protected void writeFrame() {
        frameByteBuffer.position(0);
        PixelFormat pixelFormat = pixelWriter.getPixelFormat();
        // TODO premultiply not work and weaker performance
        switch (pixelFormat.getType()) {
            case INT_ARGB:
            case INT_ARGB_PRE:
                pixelWriter.setPixels(0, 0, width, height,
                        PixelFormat.getIntArgbInstance(), frameByteBuffer.asIntBuffer(), width * 4);
                break;
            case BYTE_BGRA:
            case BYTE_BGRA_PRE:
                pixelWriter.setPixels(0, 0, width, height,
                        PixelFormat.getByteBgraInstance(), frameByteBuffer, width * 4);
                break;
            default:
                break;
        }
    }

    @Override
    public void dispose() {
        timer.stop();
        disposeImpl();
    }

    /**
     * Dispose.
     */
    protected void disposeImpl() {
        if (numSamples > 1) {
            normalFrameBuffer.dispose();
        }
        frameBuffer.dispose();
        // BufferUtils.destroyDirectBuffer(frameByteBuffer);
    }

    @Override
    public FrameBuffer getFrameBuffer() {
        return frameBuffer;
    }
}
