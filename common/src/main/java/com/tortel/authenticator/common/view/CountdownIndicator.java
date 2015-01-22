/*
 * Copyright 2011 Google Inc. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.tortel.authenticator.common.view;

import android.content.Context;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;

import com.tortel.authenticator.common.R;

/**
 * Circular countdown indicator. The indicator is a filled arc which starts as a full circle ({@code
 * 360} degrees) and shrinks to {@code 0} degrees the less time is remaining.
 *
 * @author klyubin@google.com (Alex Klyubin)
 */
public class CountdownIndicator extends View {
    private static final int DEFAULT_BG_COLOR = 0xffffff;
    private final Paint mBackgroundPaint;
    private final Paint mRemainingSectorPaint;
    private static final int DEFAULT_COLOR = 0xff3060c0;
    // Drawing variables
    private RectF mDrawingRect;
    private float mRemainingSectorSweepAngle;
    private float mRemainingSectorStartAngle;


    /**
     * Countdown phase starting with {@code 1} when a full cycle is remaining and shrinking to
     * {@code 0} the closer the countdown is to zero.
     */
    private double mPhase;

    public CountdownIndicator(Context context) {
        this(context, null);
    }

    public CountdownIndicator(Context context, AttributeSet attrs) {
        super(context, attrs);

        int color = DEFAULT_COLOR;
        int bgColor = DEFAULT_BG_COLOR;
        Resources.Theme theme = context.getTheme();
        TypedArray appearance = theme.obtainStyledAttributes(
                attrs, R.styleable.CountdownIndicator, 0, 0);
        if (appearance != null) {
            int n = appearance.getIndexCount();
            for (int i = 0; i < n; i++) {
                int attr = appearance.getIndex(i);
                if(attr == R.styleable.CountdownIndicator_countdown_bgcolor){
                    bgColor = appearance.getColor(attr, DEFAULT_COLOR);
                } else if(attr == R.styleable.CountdownIndicator_countdown_color){
                    color = appearance.getColor(attr, DEFAULT_COLOR);
                }
            }
        }

        mRemainingSectorPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mRemainingSectorPaint.setColor(color);
        mBackgroundPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mBackgroundPaint.setColor(bgColor);
        mDrawingRect = new RectF(1, 1, getWidth() - 1, getHeight() - 1);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        mRemainingSectorSweepAngle = (float) (mPhase * 360);
        mRemainingSectorStartAngle = 270 - mRemainingSectorSweepAngle;

        // Draw the sector/filled arc
        // We need to leave the leftmost column and the topmost row out of the drawingRect because
        // in anti-aliased mode drawArc and drawOval use these areas for some reason.

        // Draw the background first
        canvas.drawOval(mDrawingRect, mBackgroundPaint);

        if (mRemainingSectorStartAngle < 360) {
            canvas.drawArc(
                    mDrawingRect,
                    mRemainingSectorStartAngle,
                    mRemainingSectorSweepAngle,
                    true,
                    mRemainingSectorPaint);
        } else {
            // 360 degrees is equivalent to 0 degrees for drawArc, hence the drawOval below.
            canvas.drawOval(mDrawingRect, mRemainingSectorPaint);
        }
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        // Set the drawing bounds
        mDrawingRect.set(1, 1, getWidth() - 1, getHeight() - 1);
    }

    /**
     * Sets the phase of this indicator.
     *
     * @param phase phase {@code [0, 1]}: {@code 1} when the maximum amount of time is remaining,
     *              {@code 0} when no time is remaining.
     */
    public void setPhase(double phase) {
        if ((phase < 0) || (phase > 1)) {
            throw new IllegalArgumentException("phase: " + phase);
        }

        mPhase = phase;
        invalidate();
    }
}