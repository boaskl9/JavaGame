package com.game.ui;

import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;

/**
 * Dialog for entering server IP to connect to multiplayer game.
 */
public class MultiplayerDialog extends Dialog {
    private TextField ipField;
    private MultiplayerCallback callback;

    public interface MultiplayerCallback {
        void onConnect(String serverIp);
        void onCancel();
    }

    public MultiplayerDialog(Skin skin) {
        super("Connect to Server", skin);

        this.getTitleLabel().setAlignment(0);
        this.padTop(20);

        Table content = new Table();
        content.pad(20);
        content.defaults().padBottom(15);

        // IP input field
        Label ipLabel = new Label("Server IP:", skin);
        content.add(ipLabel).left().row();

        ipField = new TextField("localhost", skin);
        ipField.setMessageText("localhost");
        content.add(ipField).width(300).row();

        // Info label
        Label infoLabel = new Label("Enter the IP address of the server to connect to.\nFor local testing, use 'localhost'", skin);
        infoLabel.setFontScale(0.7f);
        content.add(infoLabel).padTop(10).padBottom(20).row();

        this.getContentTable().add(content);

        // Buttons
        Table buttonTable = new Table();
        buttonTable.defaults().width(120).height(40).pad(5);

        TextButton connectButton = new TextButton("Connect", skin);
        connectButton.addListener(new ClickListener() {
            @Override
            public void clicked(com.badlogic.gdx.scenes.scene2d.InputEvent event, float x, float y) {
                onConnect();
            }
        });
        buttonTable.add(connectButton);

        TextButton cancelButton = new TextButton("Cancel", skin);
        cancelButton.addListener(new ClickListener() {
            @Override
            public void clicked(com.badlogic.gdx.scenes.scene2d.InputEvent event, float x, float y) {
                onCancel();
            }
        });
        buttonTable.add(cancelButton);

        this.getButtonTable().add(buttonTable);

        this.setModal(true);
        this.setMovable(false);
    }

    private void onConnect() {
        String serverIp = ipField.getText().trim();

        if (serverIp.isEmpty()) {
            serverIp = "localhost";
        }

        if (callback != null) {
            callback.onConnect(serverIp);
        }

        this.hide();
    }

    private void onCancel() {
        if (callback != null) {
            callback.onCancel();
        }

        this.hide();
    }

    public void setCallback(MultiplayerCallback callback) {
        this.callback = callback;
    }

    @Override
    public Dialog show(Stage stage) {
        super.show(stage);

        // Center the dialog
        setPosition(
            (stage.getWidth() - getWidth()) / 2,
            (stage.getHeight() - getHeight()) / 2
        );

        // Focus the text field
        stage.setKeyboardFocus(ipField);

        return this;
    }
}
