package com.game.entity;

import com.game.components.AnimationComponent;
import com.game.components.ColliderComponent;
import com.game.components.RenderComponent;
import com.game.systems.entity.Transform;

/**
 * Example NPC entity.
 * Extends Entity to automatically get health system and living entity features.
 */
public class NPC extends com.game.systems.entity.Entity {
    private static final int SIZE = 16;
    private static final int DEFAULT_MAX_HEALTH = 50;

    private Transform transform;
    private AnimationComponent animation;
    private String name;

    public NPC(String name, float x, float y, int health) {
        super(health > 0 ? health : DEFAULT_MAX_HEALTH);
        this.name = name;

        // Add components
        transform = new Transform(x, y);
        addComponent(transform);

        animation = new AnimationComponent();
        addComponent(animation);

        ColliderComponent collider = new ColliderComponent(SIZE - 4, SIZE - 4, 2, 2);
        addComponent(collider);

        RenderComponent render = new RenderComponent(SIZE, SIZE);
        addComponent(render);
    }

    @Override
    public void update(float delta) {
        // NPCs can have AI behavior here
        super.update(delta);
    }

    @Override
    protected void onDeath() {
        System.out.println(name + " has died!");
        super.onDeath();
        // Could drop items, play death animation, etc.
    }

    public String getName() {
        return name;
    }

    public Transform getTransform() {
        return transform;
    }
}
