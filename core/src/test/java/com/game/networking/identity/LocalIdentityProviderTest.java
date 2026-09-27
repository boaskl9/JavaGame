package com.game.networking.identity;

import com.badlogic.gdx.Preferences;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class LocalIdentityProviderTest {

    /** Preferences backed by a map. */
    private static Preferences inMemoryPreferences() {
        Map<String, String> store = new HashMap<>();
        Preferences prefs = mock(Preferences.class);
        when(prefs.getString(anyString(), any())).thenAnswer(inv -> store.getOrDefault(inv.getArgument(0), inv.getArgument(1)));
        when(prefs.putString(anyString(), anyString())).thenAnswer(inv -> {
            store.put(inv.getArgument(0), inv.getArgument(1));
            return prefs;
        });
        return prefs;
    }

    @Test
    void theIdIsGeneratedOnceAndReused() {
        Preferences prefs = inMemoryPreferences();

        PlayerIdentity first = new LocalIdentityProvider(prefs).getIdentity("Bob");
        PlayerIdentity second = new LocalIdentityProvider(prefs).getIdentity("Robert");

        assertEquals("local", first.getProvider());
        assertEquals(first.getId(), second.getId(), "same install, same ID, whatever the name");
        assertEquals("Robert", second.getDisplayName());
        verify(prefs, times(1)).flush();
    }

    @Test
    void differentInstallsGetDifferentIds() {
        assertNotEquals(
            new LocalIdentityProvider(inMemoryPreferences()).getIdentity("A").getId(),
            new LocalIdentityProvider(inMemoryPreferences()).getIdentity("A").getId());
    }

    @Test
    void profilesUseSeparatePreferenceFiles() {
        assertEquals("javagame-identity", LocalIdentityProvider.preferencesName(null));
        assertEquals("javagame-identity-second", LocalIdentityProvider.preferencesName("second"));
    }

    @Test
    void keysCombineProviderAndId() {
        assertEquals("steam:7656119", new PlayerIdentity("steam", "7656119", "Bob").key());
        assertEquals("Player", new PlayerIdentity("local", "x", "  ").getDisplayName());
        assertThrows(IllegalArgumentException.class, () -> new PlayerIdentity("", "x", "Bob"));
    }
}
