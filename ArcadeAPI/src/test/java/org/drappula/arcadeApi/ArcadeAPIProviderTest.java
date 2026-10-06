package org.drappula.arcadeApi;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

class ArcadeAPIProviderTest {

    @AfterEach
    void reset() {
        try {
            ArcadeAPIProvider.unregister();
        } catch (Exception ignored) {
        }
    }

    @Test
    void getBeforeRegisterThrows() {
        assertThrows(IllegalStateException.class, ArcadeAPIProvider::get);
    }

    @Test
    void registerPublishesInstance() {
        ArcadeAPI api = mock(ArcadeAPI.class);

        ArcadeAPIProvider.register(api);

        assertSame(api, ArcadeAPIProvider.get());
    }

    @Test
    void doubleRegisterThrows() {
        ArcadeAPIProvider.register(mock(ArcadeAPI.class));

        assertThrows(IllegalStateException.class, () -> ArcadeAPIProvider.register(mock(ArcadeAPI.class)));
    }

    @Test
    void unregisterClearsInstance() {
        ArcadeAPIProvider.register(mock(ArcadeAPI.class));

        ArcadeAPIProvider.unregister();

        assertThrows(IllegalStateException.class, ArcadeAPIProvider::get);
    }
}
