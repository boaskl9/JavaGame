package com.game.systems.entity;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Generic game object with component-based architecture.
 * Completely standalone - can be used in any project.
 */
public class GameObject {
    private Map<Class<? extends Component>, Component> components;
    private List<Component> componentList;
    private boolean active;

    public GameObject() {
        this.components = new HashMap<>();
        this.componentList = new ArrayList<>();
        this.active = true;
    }

    /**
     * Add a component to this game object.
     */
    public <T extends Component> void addComponent(T component) {
        components.put(component.getClass(), component);
        componentList.add(component);
        component.onAttach();
    }

    /**
     * Get a component by type.
     */
    @SuppressWarnings("unchecked")
    public <T extends Component> T getComponent(Class<T> componentClass) {
        return (T) components.get(componentClass);
    }

    /**
     * Check if this object has a specific component.
     */
    public <T extends Component> boolean hasComponent(Class<T> componentClass) {
        return components.containsKey(componentClass);
    }

    /**
     * Remove a component from this object.
     */
    public <T extends Component> void removeComponent(Class<T> componentClass) {
        Component component = components.remove(componentClass);
        if (component != null) {
            componentList.remove(component);
            component.onDetach();
        }
    }

    /**
     * Update all components.
     */
    public void update(float delta) {
        if (!active) return;

        for (Component component : componentList) {
            component.update(delta);
        }
    }

    /**
     * Get all components.
     */
    public List<Component> getAllComponents() {
        return new ArrayList<>(componentList);
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }
}
