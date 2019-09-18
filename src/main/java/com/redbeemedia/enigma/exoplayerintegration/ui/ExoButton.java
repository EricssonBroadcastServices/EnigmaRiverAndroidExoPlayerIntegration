package com.redbeemedia.enigma.exoplayerintegration.ui;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.ImageButton;

public class ExoButton extends ImageButton {
    public ExoButton(Context context) {
        super(context);
    }

    public ExoButton(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public ExoButton(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public void setLogic(IExoButtonLogic logic) {
        logic.connectTo(this);
    }
}
