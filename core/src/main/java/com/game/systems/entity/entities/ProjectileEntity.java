package com.game.systems.entity.entities;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.game.components.RenderComponent;
import com.game.integration.WorldManager;
import com.game.systems.combat.AttackSystem;
import com.game.systems.combat.WeaponStats;
import com.game.systems.combat.WeaponType;
import com.game.systems.entity.GameObject;
import com.game.systems.entity.Transform;

import java.util.HashMap;
import java.util.Map;

/**
 * A shot fired by an enemy that flies in a straight line and hurts the first player it touches.
 * It stops at walls, breakables and furniture, or after travelling its maximum range.
 *
 * Multiplayer: the host simulates hits. Clients get a network-controlled copy that only flies
 * (it is deterministic: start position + velocity), and the host's despawn removes it.
 * The transform is the bottom-left of a 16x16 sprite box; the projectile itself is its center.
 */
public class ProjectileEntity extends GameObject {
    public static final String TYPE_PREFIX = "projectile:";
    public static final int SIZE = 16;

    /** What a projectile looks like and does. Looked up by ID so clients can rebuild it. */
    public enum Kind {
        WATER_SPIT("water_spit", "assets/FX/Projectile/EnergyBall.png", 4, 0.1f, 90f, 4f, 2, 60f, 200f);

        public final String id;
        final String texturePath;
        final int frames;
        final float frameDuration;
        final float spriteAngle;   // Direction the sprite points in when not rotated
        final float hitRadius;     // Half-size of the hit box around the center
        public final int damage;
        public final float knockback;
        public final float maxRange;

        Kind(String id, String texturePath, int frames, float frameDuration, float spriteAngle,
             float hitRadius, int damage, float knockback, float maxRange) {
            this.id = id;
            this.texturePath = texturePath;
            this.frames = frames;
            this.frameDuration = frameDuration;
            this.spriteAngle = spriteAngle;
            this.hitRadius = hitRadius;
            this.damage = damage;
            this.knockback = knockback;
            this.maxRange = maxRange;
        }

        public static Kind byId(String id) {
            for (Kind kind : values()) {
                if (kind.id.equals(id)) return kind;
            }
            return null;
        }
    }

    // Shared by all projectiles of a kind
    private static final Map<Kind, TextureRegion[]> FRAMES = new HashMap<>();

    private final WorldManager world;
    private final Kind kind;
    private final Transform transform;
    private final Vector2 velocity;
    private final Vector2 center;
    private final float speed;
    private final GameObject shooter; // Null on clients
    private final WeaponStats hitStats;

    private boolean networkControlled = false;
    private float travelled = 0f;
    private float stateTime = 0f;
    private AttackSystem.DamageCallback damageCallback;

    /**
     * @param centerX, centerY where the projectile starts (its center)
     * @param vx, vy velocity in pixels per second
     */
    public ProjectileEntity(WorldManager world, Kind kind, GameObject shooter,
                            float centerX, float centerY, float vx, float vy) {
        this.world = world;
        this.kind = kind;
        this.shooter = shooter;
        this.center = new Vector2(centerX, centerY);
        this.velocity = new Vector2(vx, vy);
        this.speed = velocity.len();

        transform = new Transform(centerX - SIZE / 2f, centerY - SIZE / 2f);
        addComponent(transform);
        addComponent(new RenderComponent(SIZE, SIZE)); // Size used for Y-sorting

        hitStats = new WeaponStats(WeaponType.BOW, kind.damage, 1f, kind.maxRange, kind.knockback,
            0f, 0f, 0f, 0f, 1f);
    }

    @Override
    public void update(float delta) {
        if (!isActive()) return;
        stateTime += delta;

        float step = speed * delta;
        center.mulAdd(velocity, delta);
        travelled += step;
        transform.setPosition(center.x - SIZE / 2f, center.y - SIZE / 2f);

        if (travelled >= kind.maxRange || hitsObstacle()) {
            setActive(false);
            return;
        }

        // Clients only show the flight; the host decides what gets hit
        if (!networkControlled) {
            checkPlayerHits();
        }

        super.update(delta);
    }

    private boolean hitsObstacle() {
        float r = kind.hitRadius / 2f; // Smaller than the hit box, so shots can skim past corners
        return !world.isPositionWalkable(center.x - r, center.y - r, r * 2f, r * 2f);
    }

    private void checkPlayerHits() {
        Rectangle hitBox = getHitBox();
        for (GameObject obj : world.getGameObjects()) {
            if (!(obj instanceof PlayerEntity player) || !player.isActive() || !player.isAlive()) continue;
            if (player.getCombatCollider() == null) continue;

            if (player.getCombatCollider().getBounds(player).overlaps(hitBox)) {
                AttackSystem.applyHit(this, player, hitStats, damageCallback);
                setActive(false);
                return;
            }
        }
    }

    public Rectangle getHitBox() {
        float r = kind.hitRadius;
        return new Rectangle(center.x - r, center.y - r, r * 2f, r * 2f);
    }

    public void render(SpriteBatch batch) {
        if (!isActive()) return;
        TextureRegion[] frames = framesFor(kind);
        if (frames == null || frames.length == 0) return;

        int index = (int) (stateTime / kind.frameDuration) % frames.length;
        float rotation = velocity.angleDeg() - kind.spriteAngle;
        batch.draw(frames[index], transform.getX(), transform.getY(),
            SIZE / 2f, SIZE / 2f, SIZE, SIZE, 1f, 1f, rotation);
    }

    private static TextureRegion[] framesFor(Kind kind) {
        return FRAMES.computeIfAbsent(kind, k -> {
            try {
                Texture sheet = new Texture(Gdx.files.internal(k.texturePath));
                int frameWidth = sheet.getWidth() / k.frames;
                TextureRegion[] regions = new TextureRegion[k.frames];
                for (int i = 0; i < k.frames; i++) {
                    regions[i] = new TextureRegion(sheet, i * frameWidth, 0, frameWidth, sheet.getHeight());
                }
                return regions;
            } catch (Exception e) {
                System.err.println("ProjectileEntity: Failed to load " + k.texturePath + ": " + e.getMessage());
                return new TextureRegion[0];
            }
        });
    }

    /** Replication type, e.g. "projectile:water_spit". */
    public String getType() {
        return TYPE_PREFIX + kind.id;
    }

    public Kind getKind() {
        return kind;
    }

    public Vector2 getCenter() {
        return new Vector2(center);
    }

    public Vector2 getVelocity() {
        return new Vector2(velocity);
    }

    public GameObject getShooter() {
        return shooter;
    }

    public boolean isNetworkControlled() {
        return networkControlled;
    }

    public void setNetworkControlled(boolean networkControlled) {
        this.networkControlled = networkControlled;
    }

    public void setDamageCallback(AttackSystem.DamageCallback damageCallback) {
        this.damageCallback = damageCallback;
    }

    /**
     * Direction from one point to another as a velocity with the given speed.
     */
    public static Vector2 aim(float fromX, float fromY, float toX, float toY, float speed) {
        Vector2 dir = new Vector2(toX - fromX, toY - fromY);
        if (dir.isZero(0.001f)) {
            dir.set(0, -1);
        }
        return dir.nor().scl(speed);
    }

    /** Spread a velocity by a random angle, in degrees either way. */
    public static Vector2 spread(Vector2 velocity, float maxDegrees) {
        return velocity.rotateDeg(MathUtils.random(-maxDegrees, maxDegrees));
    }
}
