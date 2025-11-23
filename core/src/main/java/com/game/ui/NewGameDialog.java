package com.game.ui;

import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.game.save.SaveManager;

/**
 * Dialog for creating a new game with a custom save name.
 */
public class NewGameDialog extends Window {
    private final TextField saveNameField;
    private final Label errorLabel;
    private NewGameCallback callback;

    public interface NewGameCallback {
        void onConfirm(String saveName);
        void onCancel();
    }

    public NewGameDialog(Skin skin) {
        super("New Game", skin);

        setModal(true);
        setMovable(true);

        Table content = new Table();
        content.pad(20);

        // Instruction label
        Label instructionLabel = new Label("Enter a name for your new save:", skin);
        content.add(instructionLabel).colspan(2).padBottom(20).row();

        // Save name input
        Label nameLabel = new Label("Save Name:", skin);
        content.add(nameLabel).right().padRight(10);

        saveNameField = new TextField(generateDefaultName(), skin);
        saveNameField.setMaxLength(30);
        content.add(saveNameField).width(300).padBottom(10).row();

        // Error label (hidden by default)
        errorLabel = new Label("", skin);
        errorLabel.setColor(1f, 0.3f, 0.3f, 1f);
        errorLabel.setVisible(false);
        content.add(errorLabel).colspan(2).padBottom(10).row();

        // Buttons
        Table buttonTable = new Table();

        TextButton confirmButton = new TextButton("Start", skin);
        confirmButton.addListener(new ClickListener() {
            @Override
            public void clicked(com.badlogic.gdx.scenes.scene2d.InputEvent event, float x, float y) {
                onConfirm();
            }
        });
        buttonTable.add(confirmButton).width(120).height(40).padRight(10);

        TextButton cancelButton = new TextButton("Cancel", skin);
        cancelButton.addListener(new ClickListener() {
            @Override
            public void clicked(com.badlogic.gdx.scenes.scene2d.InputEvent event, float x, float y) {
                onCancel();
            }
        });
        buttonTable.add(cancelButton).width(120).height(40);

        content.add(buttonTable).colspan(2);

        add(content);
        pack();
    }

    /**
     * Generate a default save name like "Save 1", "Save 2", etc.
     */
    private String generateDefaultName() {
        int counter = 1;
        while (SaveManager.getInstance().saveExists("Save " + counter)) {
            counter++;
        }
        return "Save " + counter;
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
        stage.setKeyboardFocus(saveNameField);
    }

    public void setCallback(NewGameCallback callback) {
        this.callback = callback;
    }

    private void onConfirm() {
        String saveName = saveNameField.getText().trim();

        // Validate save name
        if (saveName.isEmpty()) {
            showError("Save name cannot be empty");
            return;
        }

        if (SaveManager.getInstance().saveExists(saveName)) {
            showError("Save name already exists");
            return;
        }

        // Valid name
        if (callback != null) {
            callback.onConfirm(saveName);
        }
        remove();
    }

    private void onCancel() {
        if (callback != null) {
            callback.onCancel();
        }
        remove();
    }

    private void showError(String message) {
        errorLabel.setText(message);
        errorLabel.setVisible(true);
        pack();
    }
}
