package com.jayfella.jfx.embedded.jfx;

import com.jme3.texture.Image;
import javafx.application.Platform;
import javafx.scene.image.PixelFormat;
import javafx.scene.image.WritableImage;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;

/**
 * @author Chen Jiongyu
 */
public class ImageUtil {
    public static Image.Format getImageFormat() throws ExecutionException, InterruptedException {
        final FutureTask<Image.Format> platformFormat = new FutureTask<>(() -> {
            PixelFormat pixelFormat = new WritableImage(1, 1).getPixelWriter().getPixelFormat();
            switch (pixelFormat.getType()) {
                case INT_ARGB_PRE:
                case INT_ARGB:
                    return Image.Format.ARGB8;
                case BYTE_RGB:
                    return Image.Format.RGB8;
                default:
                    return Image.Format.BGRA8;
            }
        });
        Platform.runLater(platformFormat);
        return platformFormat.get();
    }

    public static void premultiply(ByteBuffer buffer, Image.Format format) {
        buffer.rewind();
        IntBuffer intBuffer = buffer.asIntBuffer();
        if (format == Image.Format.ARGB8) {
            for (int i = 0; i < intBuffer.capacity(); i++) {
                int aInt = intBuffer.get(i);
                int a = aInt >>> 24;
                int r = (int) (((aInt & 0x00FF0000) >>> 16) * (double) a / 0xFF) << 16;
                int g = (int) (((aInt & 0x0000FF00) >>> 8) * (double) a / 0xFF) << 8;
                int b = (int) ((aInt & 0X000000FF) * (double) a / 0xFF);
                a <<= 24;
                aInt = a + r + g + b;
                intBuffer.put(i, aInt);
            }
        } else if (format == Image.Format.BGRA8) {
//            for (int i = 0; i < intBuffer.capacity(); i++) {
//                int pixel = intBuffer.get(i);
//                int a = pixel & 0xff;
//                int b = (pixel >> 24) & 0xff;
//                b = ((int) (b * a / 255.0)) << 24;
//                int g = (pixel >> 16) & 0xff;
//                g = ((int) (g * a / 255.0)) << 16;
//                int r = (pixel >> 8) & 0xff;
//                r = ((int) (r * a / 255.0)) << 8;
//                pixel = a + r + g + b;
//                intBuffer.put(i, pixel);
//            }
        }
        buffer.rewind();
    }

//    public static void main(String[] args) {
//        int aInt = 0x80123456;
//        int a = aInt & 0X000000FF;
//        int b = (int) ((aInt >>> 24) * (double) a / 0xFF) << 24;
//        int g = (int) (((aInt & 0x00FF0000) >>> 16) * (double) a / 0xFF) << 16;
//        int r = (int) (((aInt & 0x0000FF00) >>> 8) * (double) a / 0xFF) << 8;
//        aInt = a + r + g + b;
//        System.out.println(Integer.toBinaryString(aInt));
//    }
}
