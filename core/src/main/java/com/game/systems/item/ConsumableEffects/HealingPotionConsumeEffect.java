package com.game.systems.item.ConsumableEffects;

import com.game.components.HealthComponent;
import com.game.systems.entity.Entity;
import com.game.systems.item.ConsumableEffect;

public class HealingPotionConsumeEffect implements ConsumableEffect {
    @Override
    public void applyEffect(Entity entity) {
        HealthComponent health = entity.getComponent(HealthComponent.class);

        health.heal(5);
    }
}
