import com.jayfella.jfx.embedded.SimpleJmeEmbedJfxApp;
import com.jayfella.jfx.embedded.jfx.LazyResizeImageView;
import com.jme3.app.FlyCamAppState;
import com.jme3.app.StatsAppState;
import com.jme3.app.state.ScreenshotAppState;
import com.jme3.audio.AudioListenerState;
import com.jme3.system.AppSettings;
import javafx.application.Application;
import javafx.beans.binding.Bindings;
import javafx.geometry.Dimension2D;
import javafx.scene.Scene;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

import java.util.concurrent.atomic.AtomicReference;

/**
 * @author Chen Jiongyu
 */
public class MyJavaFxApplication extends Application {

    @Override
    public void start(Stage primaryStage) throws Exception {

        // We need to start JME on a new thread, not on the JFX thread.
        // We could do this a million ways, but let's just be as safe as possible.
        AtomicReference<SimpleJmeEmbedJfxApp> jfxApp = new AtomicReference<>();

        new Thread(new ThreadGroup("LWJGL"), () -> {

            // create a new instance of our game.
            // SimpleJfxApplication myJmeGame = new MyJmeGame();

            // or add some appstates..
            SimpleJmeEmbedJfxApp myJmeGame = new MyJmeGame(
                    new StatsAppState(),
                    new AudioListenerState(),
                    new FlyCamAppState(),
                    new ScreenshotAppState("C:\\Users\\FlyWay\\Desktop\\New folder\\", "we")
            );

            // set our appSettings here
            AppSettings appSettings = myJmeGame.getSettings();
            appSettings.setUseJoysticks(true);
            appSettings.setGammaCorrection(true);
            appSettings.setSamples(16);
            appSettings.setFrameRate(60);
            appSettings.setVSync(true);

            jfxApp.set(myJmeGame);

            // we have a specific "start" method because due to LWJGL3 behavior this method will never return.
            // If we called this method in the constructor, it would never get constructed, so we have seperated
            // the blocking line of code in a method that gets called after construction.
            jfxApp.get().start();

        }, "LWJGL Render").start();

        // wait for the engine to initialize...
        // You can show some kind of indeterminate progress bar in a splash screen while you wait if you like...
        while (jfxApp.get() == null || !jfxApp.get().isInitialized()) {
            Thread.sleep(10);
        }

        // The application is never going to change from hereon in, so we can just reference the actual value.
        // Just remember that any calls to JME need to be enqueued from app.enqueue(Runnable) if you are not on the JME
        // thread (e.g. you're on the JavaFX thread). Any calls to JavaFx need to be done on the JavaFX thread, or via
        // Plaform.runLater(Runnable).
        SimpleJmeEmbedJfxApp app = jfxApp.get();

        primaryStage.setTitle("Test JME Embedded in JavaFx");

        LazyResizeImageView imageView = app.getCanvas();
        StackPane root = new StackPane();
        imageView.lazySizeProperty().bind(Bindings.createObjectBinding(() ->
                new Dimension2D(root.getWidth(), root.getHeight()), root.widthProperty(), root.heightProperty()));
        // add the ImageView that Jme renders to...
        root.getChildren().add(imageView);

        primaryStage.setScene(new Scene(root, 800, 600));
        primaryStage.setOnCloseRequest(event -> System.exit(0));
        primaryStage.show();
    }

}
