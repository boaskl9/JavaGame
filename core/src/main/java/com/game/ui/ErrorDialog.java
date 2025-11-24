package com.game.ui;

import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;

/**
 * Simple error dialog for showing error messages to the user.
 */
public class ErrorDialog extends Dialog {

    public ErrorDialog(String title, String message, Skin skin) {
        super(title, skin);

        this.getTitleLabel().setAlignment(0);
        this.padTop(20);

        Table content = new Table();
        content.pad(20);

        // Error message
        Label messageLabel = new Label(message, skin);
        messageLabel.setWrap(true);
        content.add(messageLabel).width(400).padBottom(20).row();

        this.getContentTable().add(content);

        // OK button
        TextButton okButton = new TextButton("OK", skin);
        okButton.addListener(new ClickListener() {
            @Override
            public void clicked(com.badlogic.gdx.scenes.scene2d.InputEvent event, float x, float y) {
                hide();
            }
        });

        this.button(okButton);
        this.setModal(true);
        this.setMovable(false);
    }

    @Override
    public Dialog show(Stage stage) {
        super.show(stage);

        // Center the dialog
        setPosition(
            (stage.getWidth() - getWidth()) / 2,
            (stage.getHeight() - getHeight()) / 2
        );

        return this;
    }
}
