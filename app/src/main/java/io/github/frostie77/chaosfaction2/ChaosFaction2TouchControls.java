package io.github.frostie77.chaosfaction2;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.view.MotionEvent;
import android.view.View;

/**
 * Chaos Faction 2’s game-specific touchscreen controls.
 * The same held-key bridge used by physical inputs keeps multi-touch working.
 */
public final class ChaosFaction2TouchControls extends View {
    public interface Input { void key(String key, boolean down); }

    private final Input input;
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private static final String[] KEYS = {"left", "right", "up", "down", "attack1", "attack2", "melee", "drop"};
    private static final String[] CONTROLS = {"movement", "attack1", "attack2", "melee", "drop"};
    private final SharedPreferences layoutPreferences;
    private final float[] customX = new float[CONTROLS.length], customY = new float[CONTROLS.length];
    private final float[] centerX = new float[CONTROLS.length], centerY = new float[CONTROLS.length];
    private final boolean[] pressed = new boolean[KEYS.length];
    private int movePointer = -1, attack1Pointer = -1, attack2Pointer = -1, meleePointer = -1, dropPointer = -1;
    private float movementX, movementY, attack1X, attack1Y, attack2X, attack2Y, meleeX, meleeY, dropX, dropY, radius;
    private boolean editing;
    private int editPointer = -1, editControl = -1;
    private float dragOffsetX, dragOffsetY;
    private float thumbX, thumbY; // normalized joystick direction
    private int sizeIndex = 1, opacityIndex = 1, padMode, positionIndex = 1;
    private boolean swapped;

    public ChaosFaction2TouchControls(Context context, Input input) {
        super(context);
        this.input = input;
        layoutPreferences = context.getSharedPreferences("chaosfaction2_touch_layout", Context.MODE_PRIVATE);
        loadLayout();
        setClickable(true);
    }

    public void configure(int size, int opacity, int pad, boolean swapped, int position) {
        this.sizeIndex = Math.max(0, Math.min(2, size));
        this.opacityIndex = Math.max(0, Math.min(2, opacity));
        this.padMode = pad == 1 ? 1 : 0;
        this.swapped = swapped;
        this.positionIndex = Math.max(0, Math.min(2, position));
        invalidate();
    }

    private float dp(float n) { return n * getResources().getDisplayMetrics().density; }

    private void loadLayout() {
        for (int i = 0; i < CONTROLS.length; i++) {
            float x = layoutPreferences.getFloat(CONTROLS[i] + "_x", Float.NaN);
            float y = layoutPreferences.getFloat(CONTROLS[i] + "_y", Float.NaN);
            boolean valid = Float.isFinite(x) && Float.isFinite(y) && x >= 0f && x <= 1f && y >= 0f && y <= 1f;
            customX[i] = valid ? x : Float.NaN;
            customY[i] = valid ? y : Float.NaN;
        }
    }

    private void saveLayout() {
        SharedPreferences.Editor editor = layoutPreferences.edit();
        for (int i = 0; i < CONTROLS.length; i++) {
            if (Float.isNaN(customX[i])) {
                editor.remove(CONTROLS[i] + "_x").remove(CONTROLS[i] + "_y");
            } else {
                editor.putFloat(CONTROLS[i] + "_x", customX[i]).putFloat(CONTROLS[i] + "_y", customY[i]);
            }
        }
        editor.apply();
    }

    public boolean hasCustomLayout() {
        for (float x : customX) if (!Float.isNaN(x)) return true;
        return false;
    }

    public void beginEditing() {
        releaseAll();
        editing = true;
        invalidate();
    }

    public void finishEditing(boolean save) {
        if (save) saveLayout(); else loadLayout();
        editing = false;
        releaseAll();
    }

    /** Reset is a preview in the editor; Cancel restores the saved layout. */
    public void resetLayout() {
        java.util.Arrays.fill(customX, Float.NaN);
        java.util.Arrays.fill(customY, Float.NaN);
        if (!editing) saveLayout();
        releaseAll();
    }

    private float bounded(float value, float extent, float length, float topInset) {
        float margin = extent + dp(4);
        if (length <= margin * 2f) return length / 2f;
        return Math.max(Math.min(margin + topInset, length - margin), Math.min(length - margin, value));
    }

    private void position(int id, float x, float y) {
        if (!Float.isNaN(customX[id])) {
            x = (swapped ? 1f - customX[id] : customX[id]) * getWidth();
            y = customY[id] * getHeight();
        }
        float extent = radius * (id == 0 ? 1.04f : 0.56f);
        centerX[id] = bounded(x, extent, getWidth(), 0f);
        centerY[id] = bounded(y, extent, getHeight(), editing ? dp(76) : 0f);
    }

    private void computeLayout() {
        float scale = new float[]{0.84f, 1f, 1.16f}[sizeIndex];
        float inset = dp(new float[]{14, 26, 42}[positionIndex]);
        radius = dp(59) * scale;
        movementX = swapped ? getWidth() - inset - radius : inset + radius;
        movementY = getHeight() - inset - radius;
        attack2X = swapped ? inset + radius : getWidth() - inset - radius;
        attack2Y = getHeight() - inset - radius;
        float direction = swapped ? 1 : -1;
        // Two attack buttons, with the melee modifier and weapon drop above.
        attack1X = attack2X + direction * radius * 1.40f;
        attack1Y = attack2Y;
        meleeX = attack1X;
        meleeY = attack1Y - radius * 1.40f;
        dropX = attack2X;
        dropY = attack2Y - radius * 1.40f;
        position(0, movementX, movementY);
        position(1, attack1X, attack1Y);
        position(2, attack2X, attack2Y);
        position(3, meleeX, meleeY);
        position(4, dropX, dropY);
        movementX = centerX[0]; movementY = centerY[0];
        attack1X = centerX[1]; attack1Y = centerY[1];
        attack2X = centerX[2]; attack2Y = centerY[2];
        meleeX = centerX[3]; meleeY = centerY[3];
        dropX = centerX[4]; dropY = centerY[4];
    }

    private void set(int index, boolean down) {
        if (pressed[index] == down) return;
        pressed[index] = down;
        input.key(KEYS[index], down);
        invalidate();
    }

    private void updateMove(float x, float y) {
        float dx = (x - movementX) / radius;
        float dy = (y - movementY) / radius;
        float distance = (float) Math.hypot(dx, dy);
        if (distance < 0.27f) {
            dx = 0f;
            dy = 0f;
        } else if (distance > 1f) {
            dx /= distance;
            dy /= distance;
        }
        // Both layouts allow diagonals for jumping and shield/stomp moves.
        thumbX = dx;
        thumbY = dy;
        set(0, dx < -0.28f);
        set(1, dx > 0.28f);
        set(2, dy < -0.28f);
        set(3, dy > 0.28f);
        invalidate();
    }

    public void releaseAll() {
        movePointer = attack1Pointer = attack2Pointer = meleePointer = dropPointer = -1;
        editPointer = editControl = -1;
        thumbX = thumbY = 0f;
        for (int k = 0; k < pressed.length; k++) set(k, false);
        invalidate();
    }

    private boolean near(float x, float y, float cx, float cy, float range) {
        return Math.hypot(x - cx, y - cy) <= range;
    }

    private int tone(int alpha, int red, int green, int blue) {
        return Color.argb(Math.max(0, Math.min(255, alpha)), red, green, blue);
    }

    @Override protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        computeLayout();
        int alpha = new int[]{80, 136, 196}[opacityIndex];
        if (padMode == 1) drawDpad(canvas, alpha);
        else drawJoystick(canvas, alpha);
        drawAction(canvas, attack1X, attack1Y, radius * 0.56f, "Attack 1", pressed[4], alpha);
        drawAction(canvas, attack2X, attack2Y, radius * 0.56f, "Attack 2", pressed[5], alpha);
        drawAction(canvas, meleeX, meleeY, radius * 0.56f, "Melee", pressed[6], alpha);
        drawAction(canvas, dropX, dropY, radius * 0.56f, "Drop", pressed[7], alpha);
        if (editing && editControl >= 0) {
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(dp(3));
            paint.setColor(0xFF79D4FF);
            canvas.drawCircle(centerX[editControl], centerY[editControl],
                radius * (editControl == 0 ? 1.10f : 0.63f), paint);
        }
    }

    /** Outer fixed ring and a moving thumbstick, rather than a MOVE text disc. */
    private void drawJoystick(Canvas canvas, int alpha) {
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(tone(alpha / 2, 8, 7, 12));
        canvas.drawCircle(movementX, movementY, radius * 1.04f, paint);

        paint.setColor(tone(alpha, 42, 34, 53));
        canvas.drawCircle(movementX, movementY, radius * 0.89f, paint);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(dp(3));
        paint.setColor(tone(alpha + 80, 224, 197, 148));
        canvas.drawCircle(movementX, movementY, radius * 0.89f, paint);

        paint.setStyle(Paint.Style.FILL);
        paint.setColor(tone(alpha / 2, 151, 127, 154));
        canvas.drawCircle(movementX, movementY, radius * 0.60f, paint);

        float x = movementX + thumbX * radius * 0.52f;
        float y = movementY + thumbY * radius * 0.52f;
        float puck = radius * 0.43f;
        paint.setColor(tone(Math.min(245, alpha + 40), 94, 74, 108));
        canvas.drawCircle(x, y, puck, paint);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(dp(2.5f));
        paint.setColor(tone(Math.min(255, alpha + 110), 250, 218, 167));
        canvas.drawCircle(x, y, puck, paint);

        paint.setStyle(Paint.Style.FILL);
        paint.setColor(tone(alpha + 70, 205, 177, 212));
        canvas.drawCircle(x - puck * 0.23f, y - puck * 0.23f, puck * 0.22f, paint);
    }

    /** A real cross-shaped D-pad with four individually highlighted arms. */
    private void drawDpad(Canvas canvas, int alpha) {
        float w = radius * 0.36f;
        float len = radius * 0.93f;
        canvas.save();
        canvas.translate(movementX, movementY);

        Path cross = new Path();
        cross.moveTo(-w, -len);
        cross.lineTo(w, -len);
        cross.lineTo(w, -w);
        cross.lineTo(len, -w);
        cross.lineTo(len, w);
        cross.lineTo(w, w);
        cross.lineTo(w, len);
        cross.lineTo(-w, len);
        cross.lineTo(-w, w);
        cross.lineTo(-len, w);
        cross.lineTo(-len, -w);
        cross.lineTo(-w, -w);
        cross.close();

        paint.setStyle(Paint.Style.FILL);
        paint.setColor(tone(alpha, 47, 37, 59));
        canvas.drawPath(cross, paint);
        float edge = dp(3);
        paint.setColor(tone(alpha + 55, 17, 14, 23));
        canvas.drawRoundRect(-w + edge, -len + edge, w - edge, -w, dp(5), dp(5), paint);
        canvas.drawRoundRect(-w + edge, w, w - edge, len - edge, dp(5), dp(5), paint);
        canvas.drawRoundRect(-len + edge, -w + edge, -w, w - edge, dp(5), dp(5), paint);
        canvas.drawRoundRect(w, -w + edge, len - edge, w - edge, dp(5), dp(5), paint);

        paint.setColor(tone(alpha + 70, 178, 103, 60));
        if (pressed[2]) canvas.drawRoundRect(-w + edge, -len + edge, w - edge, -w, dp(5), dp(5), paint);
        if (pressed[3]) canvas.drawRoundRect(-w + edge, w, w - edge, len - edge, dp(5), dp(5), paint);
        if (pressed[0]) canvas.drawRoundRect(-len + edge, -w + edge, -w, w - edge, dp(5), dp(5), paint);
        if (pressed[1]) canvas.drawRoundRect(w, -w + edge, len - edge, w - edge, dp(5), dp(5), paint);

        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(dp(3));
        paint.setStrokeJoin(Paint.Join.ROUND);
        paint.setColor(tone(alpha + 85, 224, 197, 148));
        canvas.drawPath(cross, paint);

        paint.setStyle(Paint.Style.FILL);
        drawArrow(canvas, 0f, -radius * 0.66f, 0f, alpha);
        drawArrow(canvas, radius * 0.66f, 0f, 90f, alpha);
        drawArrow(canvas, 0f, radius * 0.66f, 180f, alpha);
        drawArrow(canvas, -radius * 0.66f, 0f, 270f, alpha);

        paint.setColor(tone(alpha + 45, 25, 20, 29));
        canvas.drawCircle(0, 0, radius * 0.20f, paint);
        canvas.restore();
    }

    private void drawArrow(Canvas canvas, float x, float y, float degrees, int alpha) {
        float a = radius * 0.14f;
        Path arrow = new Path();
        arrow.moveTo(0f, -a);
        arrow.lineTo(a, a * 0.70f);
        arrow.lineTo(-a, a * 0.70f);
        arrow.close();
        canvas.save();
        canvas.translate(x, y);
        canvas.rotate(degrees);
        paint.setColor(tone(alpha + 95, 240, 220, 178));
        canvas.drawPath(arrow, paint);
        canvas.restore();
    }

    private void drawAction(Canvas canvas, float x, float y, float r, String label,
                            boolean down, int alpha) {
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(down ? tone(225, 143, 82, 51) : tone(alpha, 52, 41, 65));
        canvas.drawCircle(x, y, r, paint);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(dp(2));
        paint.setColor(tone(alpha + 60, 224, 197, 148));
        canvas.drawCircle(x, y, r, paint);
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(Color.WHITE);
        paint.setTextAlign(Paint.Align.CENTER);
        paint.setFakeBoldText(true);
        paint.setTextSize(r * 0.53f);
        float labelWidth = paint.measureText(label);
        if (labelWidth > r * 1.70f) paint.setTextSize(paint.getTextSize() * r * 1.70f / labelWidth);
        Paint.FontMetrics fm = paint.getFontMetrics();
        canvas.drawText(label, x, y - (fm.ascent + fm.descent) / 2f, paint);
    }

    @Override public boolean onTouchEvent(MotionEvent event) {
        computeLayout();
        if (editing) return editTouch(event);
        int action = event.getActionMasked();
        int changed = event.getActionIndex();
        if (action == MotionEvent.ACTION_CANCEL) {
            releaseAll();
            return true;
        }
        if (action == MotionEvent.ACTION_DOWN || action == MotionEvent.ACTION_POINTER_DOWN) {
            float x = event.getX(changed), y = event.getY(changed);
            int pointer = event.getPointerId(changed);
            if (movePointer == -1 && near(x, y, movementX, movementY, radius * 1.27f)) {
                movePointer = pointer;
                updateMove(x, y);
                return true;
            }
            if (attack1Pointer == -1 && near(x, y, attack1X, attack1Y, radius * 0.62f)) {
                attack1Pointer = pointer;
                set(4, true);
                return true;
            }
            if (attack2Pointer == -1 && near(x, y, attack2X, attack2Y, radius * 0.62f)) {
                attack2Pointer = pointer;
                set(5, true);
                return true;
            }
            if (meleePointer == -1 && near(x, y, meleeX, meleeY, radius * 0.62f)) {
                meleePointer = pointer; set(6, true); return true;
            }
            if (dropPointer == -1 && near(x, y, dropX, dropY, radius * 0.62f)) {
                dropPointer = pointer; set(7, true); return true;
            }
            // Unclaimed first touches pass through to the Flash menus.
            return action != MotionEvent.ACTION_DOWN;
        }
        if (action == MotionEvent.ACTION_MOVE) {
            if (movePointer != -1) {
                int p = event.findPointerIndex(movePointer);
                if (p >= 0) updateMove(event.getX(p), event.getY(p));
            }
            return true;
        }
        if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_POINTER_UP) {
            int pointer = event.getPointerId(changed);
            if (pointer == movePointer) {
                movePointer = -1;
                thumbX = thumbY = 0f;
                for (int j = 0; j < 4; j++) set(j, false);
                invalidate();
            }
            if (pointer == attack1Pointer) { attack1Pointer = -1; set(4, false); }
            if (pointer == attack2Pointer) { attack2Pointer = -1; set(5, false); }
            if (pointer == meleePointer) { meleePointer = -1; set(6, false); }
            if (pointer == dropPointer) { dropPointer = -1; set(7, false); }
            return true;
        }
        return true;
    }

    private boolean editTouch(MotionEvent event) {
        int action = event.getActionMasked(), changed = event.getActionIndex();
        if (action == MotionEvent.ACTION_CANCEL) {
            editPointer = editControl = -1;
        } else if ((action == MotionEvent.ACTION_DOWN || action == MotionEvent.ACTION_POINTER_DOWN) && editPointer == -1) {
            float x = event.getX(changed), y = event.getY(changed), nearest = Float.MAX_VALUE;
            for (int i = 0; i < CONTROLS.length; i++) {
                float distance = (float) Math.hypot(x - centerX[i], y - centerY[i]);
                if (distance <= radius * (i == 0 ? 1.27f : 0.62f) && distance < nearest) {
                    editControl = i;
                    nearest = distance;
                }
            }
            if (editControl >= 0) {
                editPointer = event.getPointerId(changed);
                dragOffsetX = x - centerX[editControl];
                dragOffsetY = y - centerY[editControl];
            }
        } else if (action == MotionEvent.ACTION_MOVE && editPointer != -1) {
            int p = event.findPointerIndex(editPointer);
            if (p >= 0 && getWidth() > 0 && getHeight() > 0) {
                float extent = radius * (editControl == 0 ? 1.04f : 0.56f);
                float x = bounded(event.getX(p) - dragOffsetX, extent, getWidth(), 0f);
                float y = bounded(event.getY(p) - dragOffsetY, extent, getHeight(), dp(76));
                customX[editControl] = swapped ? 1f - x / getWidth() : x / getWidth();
                customY[editControl] = y / getHeight();
            }
        } else if ((action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_POINTER_UP) &&
                event.getPointerId(changed) == editPointer) {
            editPointer = editControl = -1;
        }
        invalidate();
        // Editing consumes every gesture, including empty space, so none reaches the game.
        return true;
    }
}
