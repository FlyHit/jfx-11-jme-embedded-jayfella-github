package com.jayfella.jfx.embedded.jfx;

import com.jme3.renderer.RenderManager;
import com.jme3.renderer.Renderer;
import com.jme3.texture.FrameBuffer;
import com.jme3.texture.Image;
import com.jme3.texture.Texture2D;
import com.jme3.util.BufferUtils;
import javafx.animation.AnimationTimer;
import javafx.application.Platform;
import javafx.geometry.Rectangle2D;
import javafx.scene.image.PixelFormat;
import javafx.scene.image.PixelWriter;

import java.nio.ByteBuffer;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL21.GL_PIXEL_PACK_BUFFER;
import static org.lwjgl.opengl.GL30.GL_COLOR_ATTACHMENT0;

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

    private final int[] pbos = new int[2];

    /**
     * The transfer mode.
     */
    protected final FrameTransferSceneProcessor.TransferMode transferMode;

    /**
     * The byte buffer.
     */
    protected final byte[] currentImage;

    /**
     * The prev image byte buffer.
     */
    protected final byte[] previousImage;

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
    /**
     * The Frame byte buffer.
     */
    protected ByteBuffer frameByteBuffer;
    private long frame;
    private final AnimationTimer timer;
    private volatile boolean finished = true;
    private FrameBuffer normalFrameBuffer;
    private long lastLoad;
    private int lastReadIdx = 0;

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
            glGenBuffers(pbos);
            glBindBuffer(GL_PIXEL_PACK_BUFFER, pbos[0]);
            int size = width * height * 4;
            glBufferData(GL_PIXEL_PACK_BUFFER, size, GL_STREAM_READ);
            glBindBuffer(GL_PIXEL_PACK_BUFFER, pbos[1]);
            glBufferData(GL_PIXEL_PACK_BUFFER, size, GL_STREAM_READ);
            glBindBuffer(GL_PIXEL_PACK_BUFFER, 0);

            this.frameBuffer = new FrameBuffer(width, height, numSamples);
            // texture is faster
            Texture2D msColor = new Texture2D(width, height, numSamples, imageFormat);
            Texture2D msDepth = new Texture2D(width, height, numSamples, Image.Format.Depth);
            this.frameBuffer.setColorTexture(msColor);
            this.frameBuffer.setDepthTexture(msDepth);
//            this.frameBuffer.setColorBuffer(imageFormat);
//            this.frameBuffer.setDepthBuffer(Image.Format.Depth);
            this.frameBuffer.setSrgb(true);
        }

        if (numSamples > 1) {
            normalFrameBuffer = new FrameBuffer(width, height, 1);
//            Texture2D texture = new Texture2D(width, height, 1, imageFormat);
//            normalFrameBuffer.setColorTexture(texture);
            normalFrameBuffer.setColorBuffer(imageFormat);
            normalFrameBuffer.setDepthBuffer(Image.Format.Depth);
            normalFrameBuffer.setSrgb(true);
        }

        frameByteBuffer = BufferUtils.createByteBuffer(getWidth() * getHeight() * 4);
        currentImage = new byte[getWidth() * getHeight() * 4];
        previousImage = new byte[getWidth() * getHeight() * 4];
        pixelWriter = getPixelWriter(destination, null, width, height);

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

        if (!finished) {
            return;
        }
        finished = false;

        lastReadIdx = lastReadIdx == 0 ? 1 : 0;
        int index = lastReadIdx == 0 ? 1 : 0;

        // Convert screenshot.
        frameByteBuffer.clear();
        Renderer renderer = renderManager.getRenderer();
        if (numSamples > 1) {
            renderer.setFrameBuffer(normalFrameBuffer);
            renderer.copyFrameBuffer(frameBuffer, normalFrameBuffer, false);
        }
        //  For framebuffer objects, the default read buffer is GL_COLOR_ATTACHMENT0
        glReadBuffer(GL_COLOR_ATTACHMENT0);
        glBindBuffer(GL_PIXEL_PACK_BUFFER, pbos[index]);
        glReadPixels(0, 0, width, height, GL_BGRA, GL_UNSIGNED_BYTE, 0);
        glBindBuffer(GL_PIXEL_PACK_BUFFER, pbos[lastReadIdx]);
        frameByteBuffer = glMapBuffer(GL_PIXEL_PACK_BUFFER, GL_READ_ONLY);
        glUnmapBuffer(GL_PIXEL_PACK_BUFFER);
        glBindBuffer(GL_PIXEL_PACK_BUFFER, 0);
        if (frameByteBuffer != null) {
            pool.submit(() -> {
                try {
                    frameByteBuffer.get(currentImage);
                    Rectangle2D dirtyRectangle = FrameUtil.getDirtyRectangle(currentImage, previousImage, width, height);
                    if (dirtyRectangle == null) {
                        finished = true;
                        return;
                    }
                    Platform.runLater(() -> writeFrame(dirtyRectangle));
                    System.arraycopy(currentImage, 0, previousImage, 0, currentImage.length);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });
        }
    }

    /**
     * Get the prev image byte buffer.
     *
     * @return the prev image byte buffer.
     */
    protected byte[] getPreviousImage() {
        return previousImage;
    }

    /**
     * Write content to image.
     */
    protected void writeFrame(Rectangle2D dirtyRectangle) {
        int minX = (int) dirtyRectangle.getMinX();
        int minY = (int) dirtyRectangle.getMinY();
        frameByteBuffer.position(4 * (width * minY + minX));
        PixelFormat pixelFormat = pixelWriter.getPixelFormat();
        // TODO premultiply not work and weaker performance
        switch (pixelFormat.getType()) {
            case INT_ARGB:
            case INT_ARGB_PRE:
                pixelWriter.setPixels((int) dirtyRectangle.getMinX(), (int) dirtyRectangle.getMinY(),
                        (int) dirtyRectangle.getWidth(), (int) dirtyRectangle.getHeight(),
                        PixelFormat.getIntArgbInstance(), frameByteBuffer.asIntBuffer(), width * 4);
                break;
            case BYTE_BGRA:
            case BYTE_BGRA_PRE:
                pixelWriter.setPixels((int) dirtyRectangle.getMinX(), (int) dirtyRectangle.getMinY(),
                        (int) dirtyRectangle.getWidth(), (int) dirtyRectangle.getHeight(),
                        PixelFormat.getByteBgraInstance(), frameByteBuffer, width * 4);
                break;
            default:
                break;
        }
        frameByteBuffer.position(0);
        finished = true;
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
        glDeleteBuffers(pbos);
        frameBuffer.dispose();
        // BufferUtils.destroyDirectBuffer(frameByteBuffer);
    }

    @Override
    public FrameBuffer getFrameBuffer() {
        return frameBuffer;
    }
}
