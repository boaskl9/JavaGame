package com.game.systems.dungeon.generation;

import com.game.systems.dungeon.RoomTemplate;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * Intelligent room selection strategy with configurable filters and scoring.
 * Handles progression from expansive rooms early to dead-ends late.
 */
public class RoomSelector {
    private final Random random;
    private final List<RoomTemplate> availableRooms;
    private RoomTemplate lastPlacedRoom = null;
    private int roomsPlaced = 0;

    public RoomSelector(List<RoomTemplate> rooms, Random random) {
        this.availableRooms = new ArrayList<>(rooms);
        this.random = random;
    }

    /**
     * Select the next room based on current generation state.
     * @param budgetRemaining Budget remaining
     * @param totalBudget Total budget for the dungeon
     * @return Selected room or null if none available
     */
    public RoomTemplate selectRoom(int budgetRemaining, int totalBudget) {
        // Calculate generation progress (0.0 = start, 1.0 = end)
        float progress = 1.0f - (budgetRemaining / (float) totalBudget);

        // Filter rooms by budget
        List<RoomTemplate> affordable = filterByBudget(availableRooms, budgetRemaining);
        if (affordable.isEmpty()) {
            return null;
        }

        // Apply contextual filters
        List<RoomTemplate> candidates = affordable;
        candidates = filterDeadEndsEarly(candidates, roomsPlaced);
        candidates = filterDuplicates(candidates, lastPlacedRoom);

        if (candidates.isEmpty()) {
            // Fallback: just use affordable rooms
            candidates = affordable;
        }

        // Score remaining candidates
        RoomTemplate selected = selectByScore(candidates, progress);

        // Track selection
        if (selected != null) {
            lastPlacedRoom = selected;
            roomsPlaced++;
        }

        return selected;
    }

    /**
     * Filter rooms that fit within budget.
     */
    private List<RoomTemplate> filterByBudget(List<RoomTemplate> rooms, int budget) {
        List<RoomTemplate> result = new ArrayList<>();
        for (RoomTemplate room : rooms) {
            if (room.getCost() <= budget) {
                result.add(room);
            }
        }
        return result;
    }

    /**
     * Filter out dead-ends during the first few rooms to prevent tiny dungeons.
     * Dead-ends are rooms with only 1 door.
     */
    private List<RoomTemplate> filterDeadEndsEarly(List<RoomTemplate> rooms, int placedCount) {
        // Allow dead-ends after 3 rooms have been placed
        if (placedCount >= 3) {
            return rooms;
        }

        List<RoomTemplate> result = new ArrayList<>();
        for (RoomTemplate room : rooms) {
            if (room.getDoors().size() > 1) {
                result.add(room);
            }
        }

        // If filtering removed everything, return original list
        return result.isEmpty() ? rooms : result;
    }

    /**
     * Reduce likelihood of placing the same room consecutively.
     * Filters out the last placed room from candidates.
     */
    private List<RoomTemplate> filterDuplicates(List<RoomTemplate> rooms, RoomTemplate last) {
        if (last == null || rooms.size() <= 1) {
            return rooms;
        }

        List<RoomTemplate> result = new ArrayList<>();
        for (RoomTemplate room : rooms) {
            // Check by name to avoid placing same room type consecutively
            if (!room.getName().equals(last.getName())) {
                result.add(room);
            }
        }

        // If filtering removed everything, return original list
        return result.isEmpty() ? rooms : result;
    }

    /**
     * Select room using weighted scoring based on dungeon progress.
     * Early: prefer rooms with many doors (expansive)
     * Late: prefer rooms with few doors (dead-ends)
     */
    private RoomTemplate selectByScore(List<RoomTemplate> candidates, float progress) {
        if (candidates.isEmpty()) {
            return null;
        }

        if (candidates.size() == 1) {
            return candidates.get(0);
        }

        // Calculate scores for each candidate
        Map<RoomTemplate, Float> scores = new HashMap<>();
        for (RoomTemplate room : candidates) {
            float score = scoreRoom(room, progress);
            scores.put(room, score);
        }

        // Weighted random selection based on scores
        return weightedRandomSelect(candidates, scores);
    }

    /**
     * Score a room based on its characteristics and dungeon progress.
     * @param room The room to score
     * @param progress Generation progress (0.0 = start, 1.0 = end)
     * @return Score (higher = more likely to be selected)
     */
    private float scoreRoom(RoomTemplate room, float progress) {
        float score = 1.0f;

        // Door count scoring: early game prefers many doors, late game prefers few
        int doorCount = room.getDoors().size();
        float doorScore = scoreDoorCount(doorCount, progress);
        score *= doorScore;

        // Base weight from template (if any)
        score *= room.getWeight();

        return score;
    }

    /**
     * Score based on door count and generation progress.
     * Early: high score for many doors
     * Late: high score for few doors
     */
    private float scoreDoorCount(int doorCount, float progress) {
        // Early game (progress < 0.3): strongly prefer many doors
        // Mid game (0.3-0.7): neutral
        // Late game (progress > 0.7): strongly prefer few doors

        if (progress < 0.3f) {
            // Early: 4 doors = 2.0x, 3 doors = 1.5x, 2 doors = 1.0x, 1 door = 0.5x
            return 0.5f + (doorCount * 0.375f);
        } else if (progress > 0.7f) {
            // Late: inverse - prefer fewer doors
            // 1 door = 2.0x, 2 doors = 1.5x, 3 doors = 1.0x, 4 doors = 0.5x
            return 2.5f - (doorCount * 0.375f);
        } else {
            // Mid: neutral
            return 1.0f;
        }
    }

    /**
     * Perform weighted random selection from candidates.
     */
    private RoomTemplate weightedRandomSelect(List<RoomTemplate> candidates, Map<RoomTemplate, Float> scores) {
        // Calculate total weight
        float totalWeight = 0;
        for (RoomTemplate room : candidates) {
            totalWeight += scores.get(room);
        }

        if (totalWeight <= 0) {
            // Fallback: uniform random
            return candidates.get(random.nextInt(candidates.size()));
        }

        // Random selection based on weights
        float randomValue = random.nextFloat() * totalWeight;
        float cumulativeWeight = 0;

        for (RoomTemplate room : candidates) {
            cumulativeWeight += scores.get(room);
            if (randomValue <= cumulativeWeight) {
                return room;
            }
        }

        // Fallback (shouldn't happen)
        return candidates.get(candidates.size() - 1);
    }

    /**
     * Reset the selector state (useful for multiple generation attempts).
     */
    public void reset() {
        lastPlacedRoom = null;
        roomsPlaced = 0;
    }
}
