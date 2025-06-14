package com.example.myapplication;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;

import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;

public class DrawingView extends View {

    private static final String TAG = "DrawingView";

    private Path drawPath;
    private Paint drawPaint;
    private Paint canvasPaint;

    private int paintColor = Color.BLACK;
    private Canvas drawCanvas;
    private Bitmap canvasBitmap;

    private List<DrawingStroke> strokes = new ArrayList<>();
    private List<DrawingStroke> undoneStrokes = new ArrayList<>();

    private float thinBrush = 5f;
    private float mediumBrush = 15f;
    private float thickBrush = 30f;
    private float currentBrushSize = mediumBrush;

    private boolean isErasing = false;

    public DrawingView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        setupDrawing();
    }

    private void setupDrawing() {
        drawPath = new Path();
        drawPaint = new Paint();
        drawPaint.setColor(paintColor);
        drawPaint.setAntiAlias(true);
        drawPaint.setStrokeWidth(currentBrushSize);
        drawPaint.setStyle(Paint.Style.STROKE);
        drawPaint.setStrokeJoin(Paint.Join.ROUND);
        drawPaint.setStrokeCap(Paint.Cap.ROUND);

        canvasPaint = new Paint(Paint.DITHER_FLAG);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        if (w > 0 && h > 0) {
            if (canvasBitmap == null || w != oldw || h != oldh) {
                canvasBitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
                drawCanvas = new Canvas(canvasBitmap);
                drawCanvas.drawColor(Color.WHITE);

                redrawCanvasFromHistory();
            }
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        canvas.drawBitmap(canvasBitmap, 0, 0, canvasPaint);

        canvas.drawPath(drawPath, drawPaint);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        float touchX = event.getX();
        float touchY = event.getY();

        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                setPaintPropertiesForStroke();
                drawPath.moveTo(touchX, touchY);
                undoneStrokes.clear();
                break;
            case MotionEvent.ACTION_MOVE:
                drawPath.lineTo(touchX, touchY);
                break;
            case MotionEvent.ACTION_UP:
                drawCanvas.drawPath(drawPath, drawPaint);
                strokes.add(new DrawingStroke(new Path(drawPath), new Paint(drawPaint)));
                drawPath.reset();
                break;
            default:
                return false;
        }
        invalidate();
        return true;
    }

    private void setPaintPropertiesForStroke() {
        if (isErasing) {
            drawPaint.setXfermode(null);
            drawPaint.setColor(Color.WHITE);
            drawPaint.setStrokeWidth(currentBrushSize * 2);
            drawPaint.setAlpha(255);
        } else {
            drawPaint.setXfermode(null);
            drawPaint.setColor(paintColor);
            drawPaint.setStrokeWidth(currentBrushSize);
            drawPaint.setAlpha(255);
        }
    }

    public void setColor(int newColor) {
        paintColor = newColor;
        if (isErasing) {
            setErase(false);
        } else {
            drawPaint.setColor(paintColor);
            drawPaint.setXfermode(null);
            drawPaint.setAlpha(255);
        }
        invalidate();
    }

    public void setBrushSize(float newSize) {
        currentBrushSize = newSize;
        if (isErasing) {
            drawPaint.setStrokeWidth(currentBrushSize * 2);
        } else {
            drawPaint.setStrokeWidth(currentBrushSize);
        }
    }

    public float getThinBrushSize() { return thinBrush; }
    public float getMediumBrushSize() { return mediumBrush; }
    public float getThickBrushSize() { return thickBrush; }

    public void setErase(boolean isErase) {
        this.isErasing = isErase;
        if (isErase) {
            drawPaint.setColor(Color.WHITE);
            drawPaint.setXfermode(null);
        } else {
            drawPaint.setColor(paintColor);
            drawPaint.setXfermode(null);
        }
        setPaintPropertiesForStroke();
        invalidate();
    }

    public void undo() {
        if (!strokes.isEmpty()) {
            undoneStrokes.add(strokes.remove(strokes.size() - 1));
            redrawCanvasFromHistory();
            invalidate();
        }
    }

    public void redo() {
        if (!undoneStrokes.isEmpty()) {
            strokes.add(undoneStrokes.remove(undoneStrokes.size() - 1));
            redrawCanvasFromHistory();
            invalidate();
        }
    }

    private void redrawCanvasFromHistory() {
        if (getWidth() > 0 && getHeight() > 0) {
            canvasBitmap = Bitmap.createBitmap(getWidth(), getHeight(), Bitmap.Config.ARGB_8888);
            drawCanvas = new Canvas(canvasBitmap);
            drawCanvas.drawColor(Color.WHITE);

            for (DrawingStroke stroke : strokes) {
                drawCanvas.drawPath(stroke.path, stroke.paint);
            }
        } else {
            Log.w(TAG, "Cannot redraw canvas: View has no dimensions yet.");
        }
    }

    public void startNew() {
        strokes.clear();
        undoneStrokes.clear();
        if (canvasBitmap != null && drawCanvas != null) {
            canvasBitmap.eraseColor(Color.WHITE);
        }
        drawPath.reset();
        invalidate();
    }

    public Bitmap getDrawingBitmap() {
        if (canvasBitmap != null) {
            return Bitmap.createBitmap(canvasBitmap);
        }
        return null;
    }

    public void setBackgroundImage(Bitmap bitmap) {
        if (canvasBitmap != null && drawCanvas != null && getWidth() > 0 && getHeight() > 0) {
            drawCanvas.drawColor(Color.WHITE, PorterDuff.Mode.CLEAR);
            Bitmap scaledBitmap = Bitmap.createScaledBitmap(bitmap, getWidth(), getHeight(), true);
            drawCanvas.drawBitmap(scaledBitmap, 0, 0, null);

            for (DrawingStroke stroke : strokes) {
                drawCanvas.drawPath(stroke.path, stroke.paint);
            }
            invalidate();
        } else {
            Log.w(TAG, "Cannot set background image: View or canvas not ready.");
        }
    }

    private static class DrawingStroke {
        Path path;
        Paint paint;

        DrawingStroke(Path path, Paint paint) {
            this.path = path;
            this.paint = paint;
        }
    }
}