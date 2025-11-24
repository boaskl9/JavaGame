package com.game.networking;

/**
 * Sent by client to server with player input for this frame.
 * Contains movement, attack, and aim information.
 */
public class InputPacket extends Packet {
    private static final long serialVersionUID = 1L;

    private int playerId;
    private float movementX;
    private float movementY;
    private boolean attackPressed;
    private boolean attackJustPressed;
    private float aimDirectionX;
    private float aimDirectionY;
    private boolean running;

    public InputPacket() {
    }

    public InputPacket(int playerId, float movementX, float movementY,
                       boolean attackPressed, boolean attackJustPressed,
                       float aimDirectionX, float aimDirectionY, boolean running) {
        this.playerId = playerId;
        this.movementX = movementX;
        this.movementY = movementY;
        this.attackPressed = attackPressed;
        this.attackJustPressed = attackJustPressed;
        this.aimDirectionX = aimDirectionX;
        this.aimDirectionY = aimDirectionY;
        this.running = running;
    }

    @Override
    public PacketType getType() {
        return PacketType.INPUT;
    }

    public int getPlayerId() {
        return playerId;
    }

    public float getMovementX() {
        return movementX;
    }

    public float getMovementY() {
        return movementY;
    }

    public boolean isAttackPressed() {
        return attackPressed;
    }

    public boolean isAttackJustPressed() {
        return attackJustPressed;
    }

    public float getAimDirectionX() {
        return aimDirectionX;
    }

    public float getAimDirectionY() {
        return aimDirectionY;
    }

    public boolean isRunning() {
        return running;
    }
}
