package com.redbeemedia.enigma.exoplayerintegration.ui;

import android.content.Context;
import android.os.Handler;
import android.util.AttributeSet;
import android.view.View;
import android.widget.ImageButton;

import com.redbeemedia.enigma.core.util.AndroidThreadUtil;
import com.redbeemedia.enigma.core.virtualui.BaseVirtualButtonListener;
import com.redbeemedia.enigma.core.virtualui.IVirtualButton;

public class ExoButton extends ImageButton {
    private boolean hideIfDisabled = false;

    public ExoButton(Context context) {
        super(context);
    }

    public ExoButton(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public ExoButton(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public void setVirtualButton(IVirtualButton virtualButton, Handler handler) {
        setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                virtualButton.click();
            }
        });
        virtualButton.addListener(new BaseVirtualButtonListener() {
            @Override
            public void onStateChanged() {
                updateButtonGraphics(virtualButton);
            }
        }, handler);
        updateButtonGraphics(virtualButton);
    }

    private void updateButtonGraphics(IVirtualButton virtualButton) {
        final boolean enabled = virtualButton.isEnabled();
        final int visibility = calculateVisibility(virtualButton);
        AndroidThreadUtil.runOnUiThread(() -> {
            ExoButton.this.setVisibility(visibility);
            ExoButton.this.setEnabled(enabled);
            ExoButton.this.setAlpha(enabled ? 1f : 0.5f);
            ExoButton.this.invalidate();
        });
    }

    private int calculateVisibility(IVirtualButton virtualButton) {
        if(hideIfDisabled && !virtualButton.isEnabled()) {
            return View.GONE;
        } else {
            return virtualButton.isRelevant() ? View.VISIBLE : View.GONE;
        }
    }

    public void hideIfDisabled() {
        hideIfDisabled = true;
    }
}
