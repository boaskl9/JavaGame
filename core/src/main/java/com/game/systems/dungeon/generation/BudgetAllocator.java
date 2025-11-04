package com.game.systems.dungeon.generation;

import com.game.systems.dungeon.DungeonTheme;
import com.game.systems.dungeon.RoomTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Allocates rooms from a theme to hit a target budget (cost).
 * Uses weighted selection with category preferences.
 */
public class BudgetAllocator {
    private final DungeonTheme theme;
    private final Random random;
    private final int targetBudget;
    private final float budgetTolerance;  // ±10% by default

    /**
     * Create a budget allocator.
     * @param theme The dungeon theme to allocate from
     * @param targetBudget Target total cost
     * @param budgetTolerance Acceptable deviation (0.1 = ±10%)
     * @param seed Random seed for deterministic generation
     */
    public BudgetAllocator(DungeonTheme theme, int targetBudget, float budgetTolerance, long seed) {
        this.theme = theme;
        this.targetBudget = targetBudget;
        this.budgetTolerance = budgetTolerance;
        this.random = new Random(seed);

        // Set theme random seed for consistent room selection
        theme.setSeed(seed);
    }

    /**
     * Allocate rooms to match the target budget.
     * Strategy: Large rooms first, then medium, then small to fill gaps.
     * @return List of selected room templates
     */
    public List<RoomTemplate> allocateRooms() {
        List<RoomTemplate> selectedRooms = new ArrayList<>();
        int currentBudget = 0;
        int minBudget = (int) (targetBudget * (1.0f - budgetTolerance));
        int maxBudget = (int) (targetBudget * (1.0f + budgetTolerance));

        System.out.println("BudgetAllocator: Target budget " + targetBudget + " (range: " + minBudget + "-" + maxBudget + ")");

        // Phase 1: Add large rooms (if available)
        List<RoomTemplate> largeRooms = theme.getRoomsByCategory("large");
        if (!largeRooms.isEmpty()) {
            int largeRoomBudget = targetBudget / 3;  // Allocate ~33% to large rooms
            while (currentBudget < largeRoomBudget && currentBudget < maxBudget) {
                RoomTemplate room = theme.getRandomRoomByCategory("large");
                if (room != null && currentBudget + room.getCost() <= maxBudget) {
                    selectedRooms.add(room);
                    currentBudget += room.getCost();
                    System.out.println("BudgetAllocator: Added large room " + room.getName() + " (cost: " + room.getCost() + ", total: " + currentBudget + ")");
                } else {
                    break;  // Can't fit more large rooms
                }
            }
        }

        // Phase 2: Add medium rooms
        List<RoomTemplate> mediumRooms = theme.getRoomsByCategory("medium");
        if (!mediumRooms.isEmpty()) {
            while (currentBudget < minBudget) {
                RoomTemplate room = theme.getRandomRoomByCategory("medium");
                if (room != null && currentBudget + room.getCost() <= maxBudget) {
                    selectedRooms.add(room);
                    currentBudget += room.getCost();
                    System.out.println("BudgetAllocator: Added medium room " + room.getName() + " (cost: " + room.getCost() + ", total: " + currentBudget + ")");
                } else {
                    break;
                }
            }
        }

        // Phase 3: Fill remaining budget with small rooms
        List<RoomTemplate> smallRooms = theme.getRoomsByCategory("small");
        if (!smallRooms.isEmpty()) {
            while (currentBudget < targetBudget && currentBudget < maxBudget) {
                RoomTemplate room = theme.getRandomRoomByCategory("small");
                if (room != null && currentBudget + room.getCost() <= maxBudget) {
                    selectedRooms.add(room);
                    currentBudget += room.getCost();
                    System.out.println("BudgetAllocator: Added small room " + room.getName() + " (cost: " + room.getCost() + ", total: " + currentBudget + ")");
                } else {
                    break;
                }
            }
        }

        // Fallback: If no rooms by category, just pick random rooms
        if (selectedRooms.isEmpty()) {
            System.out.println("BudgetAllocator: No rooms found by category, using random selection");
            while (currentBudget < minBudget) {
                RoomTemplate room = theme.getRandomRoom();
                if (room != null && currentBudget + room.getCost() <= maxBudget) {
                    selectedRooms.add(room);
                    currentBudget += room.getCost();
                    System.out.println("BudgetAllocator: Added room " + room.getName() + " (cost: " + room.getCost() + ", total: " + currentBudget + ")");
                } else {
                    break;
                }
            }
        }

        System.out.println("BudgetAllocator: Allocated " + selectedRooms.size() + " rooms (total cost: " + currentBudget + "/" + targetBudget + ")");
        return selectedRooms;
    }

    /**
     * Check if a room can fit in the remaining budget.
     */
    public boolean canAffordRoom(int currentBudget, RoomTemplate room) {
        int maxBudget = (int) (targetBudget * (1.0f + budgetTolerance));
        return currentBudget + room.getCost() <= maxBudget;
    }

    /**
     * Get the remaining budget.
     */
    public int getRemainingBudget(int currentBudget) {
        return targetBudget - currentBudget;
    }
}
