# Multiplayer Implementation Plan

## Vision
Stardew Valley-style host-based multiplayer where one player owns the save file and others connect to play together.

## Current Architecture - Key Points

### Strengths ✅
- Component-based entity system (network-friendly)
- Clean update/render separation (critical for networking)
- Stateless combat system (runs identically on server/client)
- Existing JSON serialization (reusable for network packets)
- WorldManager is entity-agnostic (doesn't assume one player)

### Blockers ⚠️
1. **PlayerEntity Singleton** - Only one player can exist (`private static PlayerEntity instance`)
2. **Global Input** - Input polling is global (`Gdx.input.isKeyPressed()`)
3. **Single-player loops** - Item pickup, camera follow, etc. assume one player
4. **UI assumes one inventory** - `UIManagerNew(PlayerInventory inventory)`

---

## Implementation Phases

### Phase 1: Local Multi-Player (No Networking) - 1-2 weeks

**Goal:** 2 players on same machine, different keyboards/controllers

**Core Changes:**
1. Remove `PlayerEntity` singleton → regular entity
2. Create `PlayerManager` to track `List<PlayerEntity>`
3. Abstract input with `InputSource` interface:
   ```java
   interface InputSource {
       Vector2 getMovementInput();
       boolean isAttackPressed();
       Vector2 getAimDirection();
   }

   class LocalKeyboardInput implements InputSource { ... }
   class NetworkInputSource implements InputSource { ... }
   ```
4. Update game loops to iterate all players:
   - Item pickups: Check all players
   - Collisions: Check all players
   - Camera: Follow midpoint of all players

**Test:** Player 1 (WASD) and Player 2 (Arrow keys) playing together locally

---

### Phase 2: Networking Layer - 2-3 weeks

**Network Transport Abstraction:**
```java
interface NetworkTransport {
    void start();
    void stop();
    void sendToPeer(PlayerId id, Packet packet);
    void broadcast(Packet packet);
    void onReceive(PacketListener listener);
}

// Phase 2: Implement this
class NettyTransport implements NetworkTransport { ... }

// Future: Easy to add Steam
class SteamNetworkingTransport implements NetworkTransport { ... }
```

**Why Netty:**
- Peer-to-peer capable (host forwards ports)
- TCP or UDP support
- Well-documented, stable
- No lock-in, fully transparent

**Architecture:**
```
Host (Server + Client):
├── Runs full simulation
├── Owns save file
└── Broadcasts state to guests

Guest (Client):
├── Sends input to host
├── Receives world state
└── Renders using received state
```

**Network Protocol:**
```java
// Client → Server
class InputPacket {
    int playerId;
    Vector2 moveDirection;
    boolean isAttacking;
    float attackAngle;
}

// Server → Client
class StateUpdatePacket {
    PlayerState[] players;      // Positions, health, animations
    EnemyState[] enemies;       // Only nearby enemies (optimization)
    ItemState[] items;          // Only nearby items
    long timestamp;
}

// Events (RPC-style)
class ItemPickupEvent { int playerId; String itemId; }
class DamageEvent { int targetId; int damage; }
```

**Authority Model:**
- **Server authoritative:** Combat, pickups, enemy AI, world changes
- **Client predicts:** Local player movement (feels responsive)
- **Server corrects:** Sends authoritative position if prediction wrong

---

### Phase 3: Save System & Polish - 1-2 weeks

**Multi-Player Save Format:**
```json
{
  "saveName": "MyWorld",
  "hostPlayerId": 0,
  "players": [
    {"playerId": 0, "name": "Host", "x": 100, "y": 200, "inventory": {...}},
    {"playerId": 1, "name": "Guest1", "x": 105, "y": 200, "inventory": {...}}
  ],
  "world": {
    "currentLevelId": "Maps/WestArea.tmx",
    "furnitureByLevel": {...},
    "droppedItemsByLevel": {...}
  }
}
```

**Host Ownership Model:**
- Only host can save/load
- Guest progress stored in host's save
- Guest reconnects: Loads their data from host save

**Optimizations:**
- Delta compression (only send changed values)
- Interest management (only sync nearby entities)
- Snapshot interpolation (smooth remote player movement)

---

## Specific Code Changes Required

### High Priority (Blocking)
| File | Change | Reason |
|------|--------|--------|
| `PlayerEntity.java` | Remove `static instance`, add `playerId` field | Enable multiple players |
| `GameScreen.java` | Create `PlayerManager`, iterate players in loops | Multi-player support |
| `InputManager.java` | Create `InputSource` abstraction | Separate local/network input |
| `GameScreen.checkItemPickups()` | Loop through all players | All players can pickup items |
| `GameScreen.updateCamera()` | Follow midpoint of all players | Camera for multiplayer |

### Medium Priority
| File | Change | Reason |
|------|--------|--------|
| `SaveManager.java` | Support `List<PlayerData>` instead of single `PlayerData` | Multi-player saves |
| `UIManagerNew.java` | Support multiple inventories or switching | Per-player UI |

### Low Priority (Polish)
- Player-to-player collision detection
- Lag compensation / rollback
- Bandwidth optimization

---

## Testing Strategy

**Phase 1 Tests:**
- [ ] 2 players can move independently (WASD + Arrows)
- [ ] Both players can attack enemies
- [ ] Both players can pick up items
- [ ] Camera follows both players correctly
- [ ] Both players have separate inventories

**Phase 2 Tests:**
- [ ] Host can start server
- [ ] Guest can connect to host
- [ ] Input from guest reaches host
- [ ] State updates from host reach guest
- [ ] Combat works across network
- [ ] Item pickups sync correctly
- [ ] Connection loss handled gracefully

**Phase 3 Tests:**
- [ ] Save file includes all players
- [ ] Guest can rejoin and load their progress
- [ ] Only host can save

---

## Risk Mitigation

| Risk | Mitigation |
|------|-----------|
| Singleton refactor breaks existing code | Do Phase 1 first, test locally before networking |
| Network lag ruins gameplay | Implement client prediction early |
| NAT traversal issues | Start with LAN/port-forwarding, add Steam later |
| Desyncs between clients | Server-authoritative for all critical state |
| Too much bandwidth | Delta compression, interest management |

---

## Key Architecture Decisions

1. **Why remove PlayerEntity singleton?**
   - Current: Hardcoded assumption of one player
   - Future: List of players, each with unique ID

2. **Why InputSource abstraction?**
   - Current: Direct `Gdx.input` polling
   - Future: Local input OR network input packets

3. **Why Netty over Kryonet?**
   - More flexible (TCP/UDP)
   - Better for internet play
   - Easy to swap for Steam later via `NetworkTransport` interface

4. **Why server-authoritative?**
   - Prevents cheating
   - Single source of truth
   - Easier than P2P consensus

5. **Why Phase 1 before networking?**
   - Test multiplayer gameplay locally first
   - Validate architecture changes without network complexity
   - Build confidence before adding networking layer

---

## Next Steps When Ready

1. **Start Phase 1:**
   - Create `PlayerManager.java`
   - Refactor `PlayerEntity` to remove singleton
   - Create `InputSource.java` interface
   - Test with 2 local players

2. **After Phase 1 works:**
   - Implement `NetworkTransport` interface
   - Create `NettyTransport` implementation
   - Design packet protocol
   - Test with 2 machines on LAN

3. **Polish:**
   - Multi-player saves
   - Lag compensation
   - Steam integration (optional)

---

## Estimated Total Effort
- **Phase 1:** 1-2 weeks
- **Phase 2:** 2-3 weeks
- **Phase 3:** 1-2 weeks
- **Total:** 6-8 weeks for working multiplayer

---

## Important Notes

- **Do NOT implement networking until Phase 1 works** - Local multiplayer first!
- **Keep game logic separate from NetworkTransport** - Easy to swap implementations
- **Server is authoritative** - Clients send input, server sends state
- **Test often** - Each phase should be fully working before moving to next
