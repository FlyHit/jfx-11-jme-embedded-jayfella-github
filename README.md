jfx-11-jme-embedded
---

 [ ![Download](https://api.bintray.com/packages/jayfella/com.jayfella/jfx-11-jme-embedded/images/download.svg) ](https://bintray.com/jayfella/com.jayfella/jfx-11-jme-embedded/_latestVersion) 

Embed JME into JavaFX!

For scientific, industrial or game development use, embedding jMonkeyEngine into JavaFX allows you to make complete use
of JavaFX as a GUI whilst utilizing jMonkeyEngine to visualize your data in a JavaFx Component.

The output of jMonkeyEngine is rendered to a JavaFX ImageView component that you can put anywhere in your JavaFx scene,
and includes automatic resizing to fit the size of its parent.


#### Delayed Resizing

Since resizing the JME canvas is expensive, the ImageView utilizes a timer to reject continuous resize requests until
a certain time-frame has passed. In general terms it means that when you resize the window, instead of constantly
resizing with the JME viewport, it waits until the window has stopped resizing. This delay can seem unsightly, and can
be changed via the `getImageView().setResizeDelay(float delay)` method of the class that extends `SimpleJfxApplication`.


#### build.gradle

Add the JavaFX plugin to your `build.gradle` file, along with the jmonkeyengine dependencies. In addition - as per JavaFX
requirements - you must also notify JavaFX of the modules you require. 

```groovy
plugins {
    id 'org.openjfx.javafxplugin' version '0.0.8'
}

// choose which javafx modules you want to include
javafx {
    version = '11'
    modules = [
            'javafx.base',
            'javafx.controls',
            'javafx.fxml',
            'javafx.graphics',
            'javafx.swing'
    ]
}

repositories {
    jcenter()
}

dependencies {

    // jmonkeyengine dependencies
    implementation "org.jmonkeyengine:jme3-core:3.3.2-stable"
    implementation "org.jmonkeyengine:jme3-desktop:3.3.2-stable"
    implementation "org.jmonkeyengine:jme3-lwjgl3:3.3.2-stable"

    // this library
    implementation "com.jayfella:jfx-11-jme-embedded:1.0.0"

}

``` 

#### Jme Application Class

Below is a simple JME application that extends `SimpleJfxApplication` with a rotating box. This class will give you an
additional method `getImageView()` to obtain the JavaFX ImageView Component that you will add to your JavaFX scene.

```java
public class MyJmeGame extends SimpleJfxApplication {

    public MyJmeGame(AppState... initialStates) {
        super(initialStates);
    }

    private Geometry box;

    @Override
    public void initApp() {
        flyCam.setDragToRotate(true);
        flyCam.setMoveSpeed(6);

        DirectionalLight directionalLight = new DirectionalLight(
                new Vector3f(-1, -1, -1).normalizeLocal(),
                ColorRGBA.White.clone()
        );

        rootNode.addLight(directionalLight);

        Texture texture = assetManager.loadTexture("com/jme3/app/Monkey.png");

        box = new Geometry("Box", new Box(1,1,1));
        box.setMaterial(new Material(assetManager, Materials.PBR));
        box.getMaterial().setTexture("BaseColorMap", texture);
        box.getMaterial().setColor("BaseColor", ColorRGBA.White);
        box.getMaterial().setFloat("Roughness", 0.001f);
        box.getMaterial().setFloat("Metallic", 0.001f);

        rootNode.attachChild(box);

    }

    @Override
    public void simpleUpdate(float tpf) {
        box.rotate(tpf * .2f, tpf * .3f, tpf * .4f);
    }
    
}
```

### Integrating JME into JavaFX

Outlined below is an example of a JavaFX `Application` class. This is typically your point of entry for a JavaFX
application. Notes have been provided in the code below to explain each step of the process.

```java

public class MyJavaFxApplication extends Application {

    @Override
    public void start(Stage primaryStage) throws Exception {

        // We need to start JME on a new thread, not on the JFX thread.
        // We could do this a million ways, but let's just be as safe as possible.
        AtomicReference<SimpleJfxApplication> jfxApp = new AtomicReference<>();

        new Thread(new ThreadGroup("LWJGL"), () -> {

            // create a new instance of our game.
            // SimpleJfxApplication myJmeGame = new MyJmeGame();

            // or add some appstates..
            SimpleJfxApplication myJmeGame = new MyJmeGame(
                    new StatsAppState(),
                    new AudioListenerState(),
                    new FlyCamAppState()
            );

            // set our appSettings here
            AppSettings appSettings = myJmeGame.getSettings();
            appSettings.setUseJoysticks(true);
            appSettings.setGammaCorrection(true);
            appSettings.setSamples(16);


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
        SimpleJfxApplication app = jfxApp.get();

        primaryStage.setTitle("Test JME Embedded in JavaFx");

        StackPane root = new StackPane();

        // add the ImageView that Jme renders to...
        root.getChildren().add(app.getImageView());

        primaryStage.setScene(new Scene(root, 800, 600));
        primaryStage.show();

    }

}

```


#### Launching the JavaFx Application

Finally, to launch the JavaFX application, create a class with a `public static void main` method and call
`Application.launch(MyJavaFxApplication.class, args);`. There are some additional settings you may or may not wish to
set that may improve compatibility for your setup before you launch the JavaFX `Application` class.

```java

public class Main {

    public static void main(String... args) {

        // some general settings for JFX for maximum compatibility

        Configuration.GLFW_CHECK_THREAD0.set(false); // need to disable to work on macos
        Configuration.MEMORY_ALLOCATOR.set("jemalloc"); // use jemalloc
        System.setProperty("prism.lcdtext", "false"); // JavaFx

        System.setProperty(LWJGLBufferAllocator.PROPERTY_CONCURRENT_BUFFER_ALLOCATOR, "true");

        Application.launch(MyJavaFxApplication.class, args);

    }

}

```

### Working Example

For a complete working example of the classes outlined in this documentation see the `src/test` directory of this
repository.