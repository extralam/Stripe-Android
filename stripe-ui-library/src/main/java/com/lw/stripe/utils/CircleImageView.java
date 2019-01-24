package com.lw.stripe.utils;

/*
  Copyright 2015-present Yukari Sakurai
  <p/>
  Licensed under the Apache License, Version 2.0 (the "License");
  you may not use this file except in compliance with the License.
  You may obtain a copy of the License at
  <p/>
  http://www.apache.org/licenses/LICENSE-2.0
  <p/>
  Unless required by applicable law or agreed to in writing, software
  distributed under the License is distributed on an "AS IS" BASIS,
  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  See the License for the specific language governing permissions and
  limitations under the License.
 */

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapShader;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Shader;
import android.graphics.drawable.BitmapDrawable;
import android.os.Build;
import android.util.AttributeSet;
import android.view.View;

import com.lw.stripe.R;

import androidx.core.content.res.ResourcesCompat;

public class CircleImageView extends DownloadImageView {

    private static final ScaleType SCALE_TYPE = ScaleType.CENTER_CROP;
    private static final Bitmap.Config BITMAP_CONFIG = Bitmap.Config.ARGB_8888;

    private int borderWidth = 0;
    private int canvasSize;
    private Paint paint;
    private Paint paintBorder;
    private boolean isBackground = false;

    public CircleImageView(Context context) {
        super(context, null);
    }

    public CircleImageView(Context context, AttributeSet attrs) {
        super(context, attrs);

        paint = new Paint();
        paint.setAntiAlias(true);

        paintBorder = new Paint();
        paintBorder.setStyle(Paint.Style.FILL_AND_STROKE);
        paintBorder.setAntiAlias(true);
        paintBorder.setColor(getResources().getColor(android.R.color.transparent));
    }

    public CircleImageView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    public void setBorderWidth(int borderWidth) {
        this.borderWidth = borderWidth;
        this.invalidate();
    }

    public void setBorderColor(int borderColor) {
        if (paintBorder != null)
            paintBorder.setColor(borderColor);

        this.invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        BitmapDrawable drawable = (BitmapDrawable) getDrawable();

        if (drawable == null) return;
        if (getWidth() == 0 || getHeight() == 0) return;

        Bitmap srcBmp = drawable.getBitmap();
        if (srcBmp == null) return;

        Bitmap image = getSquareBitmap(srcBmp);

        canvasSize = getWidth();
        if (getWidth() < canvasSize)
            canvasSize = getWidth();

        BitmapShader shader = new BitmapShader(
                Bitmap.createScaledBitmap(image, canvasSize, canvasSize, false),
                Shader.TileMode.CLAMP,
                Shader.TileMode.CLAMP);
        paint.setShader(shader);

        float circleCenter = canvasSize / 2f;
        // circleCenter is the x or y of the view's center
        // radius is the radius in pixels of the cirle to be drawn
        // paint contains the shader that will texture the shape
        canvas.drawCircle(circleCenter, circleCenter, circleCenter, paintBorder);
        canvas.drawCircle(circleCenter, circleCenter, circleCenter - borderWidth / 2f, paint);
    }

    public void showBackground(boolean isBackground) {
        this.isBackground = isBackground;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            if (isBackground) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    setForeground(ResourcesCompat.getDrawable(getResources(), R.drawable.bg_circle_ripple, null));
                }
                setBackground(getResources().getDrawable(R.drawable.bg_circle_white));
            }
        } else {
            if (isBackground)
                setBackgroundDrawable(getResources().getDrawable(R.drawable.bg_circle_white));
        }
    }

    private Bitmap getSquareBitmap(Bitmap srcBmp) {
        if (srcBmp.getWidth() == srcBmp.getHeight()) return srcBmp;

        //Rectangle to square. Equivarent to ScaleType.CENTER_CROP
        int dim = Math.min(srcBmp.getWidth(), srcBmp.getHeight()) * 100 / 100;
        Bitmap dstBmp = Bitmap.createBitmap(dim, dim, BITMAP_CONFIG);

        Canvas canvas = new Canvas(dstBmp);
        float left = srcBmp.getWidth() > dim ? (dim - srcBmp.getWidth()) / 2f : 0;
        float top = srcBmp.getHeight() > dim ? ((dim - srcBmp.getHeight()) / 2f) : 0;
        if (this.isBackground) {
            canvas.drawColor(Color.WHITE);
        }
        canvas.drawBitmap(srcBmp, left, top, null);

        return dstBmp;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int width = measureWidth(widthMeasureSpec);
        int height = measureHeight(heightMeasureSpec);
        setMeasuredDimension(width, height);
    }

    private int measureWidth(int measureSpec) {
        int result;
        int specMode = MeasureSpec.getMode(measureSpec);
        int specSize = MeasureSpec.getSize(measureSpec);

        if (specMode == MeasureSpec.EXACTLY) {
            // The parent has determined an exact size for the child.
            result = specSize;
        } else if (specMode == View.MeasureSpec.AT_MOST) {
            // The child can be as large as it wants up to the specified size.
            result = specSize;
        } else {
            // The parent has not imposed any constraint on the child.
            result = canvasSize;
        }

        return result;
    }

    private int measureHeight(int measureSpecHeight) {
        int result;
        int specMode = MeasureSpec.getMode(measureSpecHeight);
        int specSize = MeasureSpec.getSize(measureSpecHeight);

        if (specMode == MeasureSpec.EXACTLY) {
            // We were told how big to be
            result = specSize;
        } else if (specMode == MeasureSpec.AT_MOST) {
            // The child can be as large as it wants up to the specified size.
            result = specSize;
        } else {
            // Measure the text (beware: ascent is a negative number)
            result = canvasSize;
        }

        return result;
    }
}