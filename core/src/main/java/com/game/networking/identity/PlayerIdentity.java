package com.game.networking.identity;

/**
 * Who a player is, independent of the name they typed.
 *
 * An identity comes from a provider ("local" for a per-install ID, later e.g. "steam") and an ID
 * that is unique within that provider. The host stores guests' characters under {@link #key()},
 * so a different provider can be added without changing the save format.
 */
public final class PlayerIdentity {
    private final String provider;
    private final String id;
    private final String displayName;

    public PlayerIdentity(String provider, String id, String displayName) {
        if (provider == null || provider.isBlank() || id == null || id.isBlank()) {
            throw new IllegalArgumentException("Identity needs a provider and an id");
        }
        this.provider = provider;
        this.id = id;
        this.displayName = (displayName == null || displayName.isBlank()) ? "Player" : displayName.trim();
    }

    /** Save key for this identity, e.g. "local:3f2a..." or "steam:7656119...". */
    public String key() {
        return key(provider, id);
    }

    public static String key(String provider, String id) {
        return provider + ":" + id;
    }

    public String getProvider() {
        return provider;
    }

    public String getId() {
        return id;
    }

    public String getDisplayName() {
        return displayName;
    }

    @Override
    public String toString() {
        return displayName + " (" + key() + ")";
    }
}
