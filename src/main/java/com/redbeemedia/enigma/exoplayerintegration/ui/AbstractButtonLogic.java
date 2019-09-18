package com.redbeemedia.enigma.exoplayerintegration.ui;

import android.view.View;

import com.redbeemedia.enigma.core.player.IEnigmaPlayer;
import com.redbeemedia.enigma.core.util.AndroidThreadUtil;

public abstract class AbstractButtonLogic implements IExoButtonLogic {
    private final IEnigmaPlayer enigmaPlayer;
    private ExoButton exoButton = null;

    public AbstractButtonLogic(IEnigmaPlayer enigmaPlayer) {
        this.enigmaPlayer = enigmaPlayer;
    }

    @Override
    public void connectTo(ExoButton exoButton) {
        exoButton.setOnClickListener(v -> {
            if(isEnabled()) {
                onActivate();
            }
        });
        this.exoButton = exoButton;
        updateButtonGraphics();
    }

    public IEnigmaPlayer getEnigmaPlayer() {
        return enigmaPlayer;
    }

    protected void updateButtonGraphics() {
        boolean enabled = isEnabled();
        if(exoButton != null) {
            final int visibility = enabled ? View.VISIBLE : View.GONE;
            AndroidThreadUtil.runOnUiThread(() -> {
                exoButton.setVisibility(visibility);
            });
        }
    }

    public abstract boolean isEnabled();
    public abstract void onActivate();
}
