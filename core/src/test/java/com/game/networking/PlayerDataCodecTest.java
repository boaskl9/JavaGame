package com.game.networking;

import com.game.integration.WorldManager;
import com.game.save.PlayerData;
import com.game.systems.entity.entities.PlayerEntity;
import com.game.systems.inventory.EquipmentSlot;
import com.game.systems.item.ItemFactory;
import com.game.testsupport.GameTestBase;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PlayerDataCodecTest extends GameTestBase {

    @Test
    void inventoryEquipmentAndHealthSurviveAJsonRoundTrip() {
        PlayerEntity original = new PlayerEntity(new WorldManager(10, 10), 5, 6);
        original.getInventory().addItem(ItemFactory.create("wood", 12));
        original.getInventory().getEquipment().equipItem(EquipmentSlot.WEAPON, ItemFactory.create("wooden_sword", 1));
        original.getHealthComponent().setHealth(17);

        String json = PlayerDataCodec.toJson(original);
        PlayerData data = PlayerDataCodec.fromJson(json);

        PlayerEntity restored = new PlayerEntity(new WorldManager(10, 10), 0, 0);
        PlayerDataCodec.apply(restored, data);

        assertEquals(12, restored.getInventory().countItem("wood"));
        assertEquals("wooden_sword", restored.getEquippedWeaponId());
        assertEquals(17, restored.getHealth());
    }

    @Test
    void aDeadCharacterComesBackWithAtLeastOneHitPoint() {
        PlayerEntity original = new PlayerEntity(new WorldManager(10, 10), 0, 0);
        original.getHealthComponent().setHealth(0);

        PlayerEntity restored = new PlayerEntity(new WorldManager(10, 10), 0, 0);
        PlayerDataCodec.apply(restored, PlayerDataCodec.fromJson(PlayerDataCodec.toJson(original)));

        assertEquals(1, restored.getHealth());
    }

    @Test
    void garbageOrMissingDataIsIgnored() {
        assertNull(PlayerDataCodec.fromJson(null));
        assertNull(PlayerDataCodec.fromJson("{not json"));
    }

    @Test
    void stateSnapshotsDescribeTheLivePlayer() {
        PlayerEntity player = new PlayerEntity(new WorldManager(10, 10), 40, 50);
        player.getInventory().getEquipment().equipItem(EquipmentSlot.WEAPON, ItemFactory.create("wooden_sword", 1));

        Packets.PlayerState state = PlayerDataCodec.state(player, 4, "Maps/prototype.tmx");

        assertEquals(4, state.playerId);
        assertEquals("Maps/prototype.tmx", state.levelId);
        assertEquals(40, state.x);
        assertEquals(50, state.y);
        assertEquals("wooden_sword", state.weaponId);
        assertEquals(player.getMaxHealth(), state.maxHp);
    }
}
