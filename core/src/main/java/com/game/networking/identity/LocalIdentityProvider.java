package com.game.networking.identity;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Preferences;

import java.util.UUID;

/**
 * A random ID generated once per install and kept in the game's local preferences.
 *
 * Run with -Dgame.profile=NAME to use a separate ID (e.g. to join your own game from a second
 * copy on the same computer as a different character).
 */
public class LocalIdentityProvider implements IdentityProvider {
    public static final String PROVIDER = "local";
    private static final String PREFERENCES_NAME = "javagame-identity";
    private static final String ID_KEY = "playerId";

    private final Preferences preferences;

    /** Uses the game's preferences (per profile). */
    public LocalIdentityProvider() {
        this(Gdx.app.getPreferences(preferencesName(System.getProperty("game.profile"))));
    }

    public LocalIdentityProvider(Preferences preferences) {
        this.preferences = preferences;
    }

    static String preferencesName(String profile) {
        return (profile == null || profile.isBlank()) ? PREFERENCES_NAME : PREFERENCES_NAME + "-" + profile.trim();
    }

    @Override
    public PlayerIdentity getIdentity(String displayName) {
        String id = preferences.getString(ID_KEY, null);
        if (id == null || id.isBlank()) {
            id = UUID.randomUUID().toString();
            preferences.putString(ID_KEY, id);
            preferences.flush();
        }
        return new PlayerIdentity(PROVIDER, id, displayName);
    }
}
