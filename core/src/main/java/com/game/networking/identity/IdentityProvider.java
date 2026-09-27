package com.game.networking.identity;

/**
 * Supplies the identity this machine joins games with.
 * {@link LocalIdentityProvider} is the default; a Steam implementation would return the Steam ID.
 */
public interface IdentityProvider {

    /**
     * @param displayName the name the player chose to show to others
     */
    PlayerIdentity getIdentity(String displayName);
}
