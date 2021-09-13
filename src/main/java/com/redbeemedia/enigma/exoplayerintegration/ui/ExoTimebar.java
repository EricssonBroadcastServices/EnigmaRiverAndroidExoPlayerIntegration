package com.redbeemedia.enigma.exoplayerintegration.ui;

import android.content.Context;
import android.util.AttributeSet;

import androidx.annotation.Nullable;

import com.google.android.exoplayer2.ui.DefaultTimeBar;

public class ExoTimebar extends DefaultTimeBar {

    private boolean allowUpdate = false;

    public void setAllowUpdate(boolean allowUpdate) {
        this.allowUpdate = allowUpdate;
    }

    public boolean isAllowUpdate() {
        return allowUpdate;
    }

    public ExoTimebar(Context context) {
        super(context);
    }

    public ExoTimebar(Context context, @Nullable @org.jetbrains.annotations.Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public ExoTimebar(Context context, @Nullable @org.jetbrains.annotations.Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public ExoTimebar(Context context, @Nullable @org.jetbrains.annotations.Nullable AttributeSet attrs, int defStyleAttr, @Nullable @org.jetbrains.annotations.Nullable AttributeSet timebarAttrs) {
        super(context, attrs, defStyleAttr, timebarAttrs);
    }

    public ExoTimebar(Context context, @Nullable @org.jetbrains.annotations.Nullable AttributeSet attrs, int defStyleAttr, @Nullable @org.jetbrains.annotations.Nullable AttributeSet timebarAttrs, int defStyleRes) {
        super(context, attrs, defStyleAttr, timebarAttrs, defStyleRes);
    }

    public void setPosition(long position) {
        if(allowUpdate) {
            if(position!=0) {super.setPosition(position); }
        }
    }

    @Override
    public void setDuration(long duration) {
        if (allowUpdate) { super.setDuration(duration); }
    }
}
