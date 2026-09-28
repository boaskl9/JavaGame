package com.game.ui;

import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.game.networking.Packets;

/**
 * Shown when joining a host: continue as one of the world's characters, or create a new one.
 * Characters someone is playing right now can't be picked.
 */
public class CharacterSelectDialog extends Window {
    private static final int MAX_NAME_LENGTH = 16;

    private final Skin skin;
    private final Table characterTable;
    private final Label messageLabel;
    private final TextField nameField;
    private final CharacterSelectCallback callback;

    public interface CharacterSelectCallback {
        void onPlay(String characterId);
        void onCreate(String name);
        void onLeave();
    }

    public CharacterSelectDialog(Skin skin, CharacterSelectCallback callback) {
        super("Choose a Character", skin);
        this.skin = skin;
        this.callback = callback;

        setModal(true);
        setMovable(false);

        Table content = new Table();
        content.pad(20);
        content.defaults().left();

        messageLabel = new Label("", skin);
        messageLabel.setColor(1f, 0.3f, 0.3f, 1f);
        messageLabel.setWrap(true);
        content.add(messageLabel).width(320).padBottom(10).row();

        content.add(new Label("Continue as:", skin)).padBottom(10).row();
        characterTable = new Table();
        characterTable.top();
        ScrollPane scroll = new ScrollPane(characterTable, skin);
        scroll.setFadeScrollBars(false);
        scroll.setScrollingDisabled(true, false);
        content.add(scroll).width(320).height(200).padBottom(20).row();

        content.add(new Label("New character:", skin)).padBottom(10).row();
        Table newRow = new Table();
        nameField = new TextField("", skin);
        nameField.setMessageText("Name");
        nameField.setMaxLength(MAX_NAME_LENGTH);
        nameField.setTextFieldListener((field, c) -> {
            if (c == '\r' || c == '\n') create();
        });
        newRow.add(nameField).width(200).padRight(10);
        newRow.add(button("Create", this::create)).width(110).height(40);
        content.add(newRow).padBottom(20).row();

        content.add(button("Leave", callback::onLeave)).width(120).height(40).center();

        add(content);
        pack();
    }

    /**
     * Show the host's characters (again, when the list changed). The typed name is kept.
     * @param message why the previous choice was refused, or null
     */
    public void setCharacters(Packets.CharacterInfo[] characters, String message) {
        messageLabel.setText(message != null ? message : "");
        messageLabel.setVisible(message != null);

        characterTable.clearChildren();
        if (characters == null || characters.length == 0) {
            characterTable.add(new Label("No characters in this world yet.", skin)).pad(10);
            return;
        }
        for (Packets.CharacterInfo character : characters) {
            String text = character.name;
            if (character.inUse) {
                text += "  (playing)";
            } else if (character.lastPlayedByYou) {
                text += "  (yours)";
            }
            TextButton button = button(text, () -> callback.onPlay(character.id));
            button.setDisabled(character.inUse);
            characterTable.add(button).width(290).height(40).padBottom(6).row();
        }
    }

    private void create() {
        String name = nameField.getText().trim();
        if (name.isEmpty()) {
            setMessage("Enter a name for your character.");
            return;
        }
        callback.onCreate(name);
    }

    private void setMessage(String message) {
        messageLabel.setText(message);
        messageLabel.setVisible(true);
    }

    private TextButton button(String text, Runnable action) {
        TextButton button = new TextButton(text, skin);
        button.addListener(new ChangeListener() { // Not fired while disabled
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                action.run();
            }
        });
        return button;
    }

    /**
     * Show the dialog in the center of the stage.
     */
    public void show(Stage stage) {
        stage.addActor(this);
        setPosition(
            (stage.getWidth() - getWidth()) / 2,
            (stage.getHeight() - getHeight()) / 2
        );
        stage.setKeyboardFocus(nameField);
    }
}
