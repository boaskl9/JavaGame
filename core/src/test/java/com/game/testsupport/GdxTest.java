package com.game.testsupport;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.backends.headless.HeadlessApplication;
import com.badlogic.gdx.backends.headless.HeadlessApplicationConfiguration;
import com.badlogic.gdx.graphics.GL20;
import org.junit.jupiter.api.BeforeAll;
import org.mockito.Mockito;

/**
 * Base class for tests that need libGDX (files, textures, audio).
 * Boots a headless application once per JVM, with a mocked GL so textures "load" without a GPU.
 */
public abstract class GdxTest {
    private static boolean booted = false;

    @BeforeAll
    static void bootGdx() {
        synchronized (GdxTest.class) {
            if (booted) return;
            HeadlessApplicationConfiguration config = new HeadlessApplicationConfiguration();
            config.updatesPerSecond = -1; // Don't run the app loop; tests drive everything
            new HeadlessApplication(new ApplicationAdapter() {}, config);
            Gdx.gl = Gdx.gl20 = Mockito.mock(GL20.class);
            booted = true;
        }
    }
}
