package io.github.frostie77.chaosfaction2;

import static org.junit.Assert.*;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.os.SystemClock;
import android.view.InputDevice;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import io.github.frostfrontend.FrostFrontendView;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.android.controller.ActivityController;
import org.robolectric.annotation.Config;

/** Exercise the production Android view with actual MotionEvents and private preferences. */
@RunWith(org.robolectric.RobolectricTestRunner.class)
@Config(sdk = 28, qualifiers = "mdpi")
public class TouchLayoutTest {
    private Context context;
    private SharedPreferences layouts;
    private final List<String> inputs = new ArrayList<>();

    @Before public void setup() {
        context = RuntimeEnvironment.getApplication();
        layouts = context.getSharedPreferences("chaosfaction2_touch_layout", Context.MODE_PRIVATE);
        layouts.edit().clear().commit();
        context.getSharedPreferences("chaosfaction2_settings", Context.MODE_PRIVATE).edit().clear()
            .putBoolean("updates_automatic", false).commit();
    }

    private ChaosFaction2TouchControls controls() {
        ChaosFaction2TouchControls view = new ChaosFaction2TouchControls(context,
            (key, down) -> inputs.add(key + ":" + down));
        view.layout(0, 0, 1000, 600);
        return view;
    }

    private static Object field(Object object, String name) throws Exception {
        Field field = object.getClass().getDeclaredField(name);
        field.setAccessible(true);
        return field.get(object);
    }

    private static float[] center(ChaosFaction2TouchControls view, int id) throws Exception {
        Canvas canvas = new Canvas(Bitmap.createBitmap(view.getWidth(), view.getHeight(), Bitmap.Config.ARGB_8888));
        view.draw(canvas);
        return new float[]{((float[]) field(view, "centerX"))[id], ((float[]) field(view, "centerY"))[id]};
    }

    private static boolean touch(ChaosFaction2TouchControls view, int action, int[] ids, float... xy) {
        MotionEvent.PointerProperties[] properties = new MotionEvent.PointerProperties[ids.length];
        MotionEvent.PointerCoords[] coords = new MotionEvent.PointerCoords[ids.length];
        for (int i = 0; i < ids.length; i++) {
            properties[i] = new MotionEvent.PointerProperties();
            properties[i].id = ids[i];
            properties[i].toolType = MotionEvent.TOOL_TYPE_FINGER;
            coords[i] = new MotionEvent.PointerCoords();
            coords[i].x = xy[i * 2]; coords[i].y = xy[i * 2 + 1];
            coords[i].pressure = 1f; coords[i].size = 1f;
        }
        long now = SystemClock.uptimeMillis();
        MotionEvent event = MotionEvent.obtain(now, now, action, ids.length, properties, coords,
            0, 0, 1f, 1f, 0, 0, InputDevice.SOURCE_TOUCHSCREEN, 0);
        try { return view.onTouchEvent(event); } finally { event.recycle(); }
    }

    private static void drag(ChaosFaction2TouchControls view, int id, float x, float y) throws Exception {
        float[] origin = center(view, id);
        touch(view, MotionEvent.ACTION_DOWN, new int[]{7}, origin);
        touch(view, MotionEvent.ACTION_MOVE, new int[]{7}, x, y);
        touch(view, MotionEvent.ACTION_UP, new int[]{7}, x, y);
    }

    @Test public void saveRestoresEveryControlAndMovesItsGameplayHitArea() throws Exception {
        ChaosFaction2TouchControls view = controls();
        view.beginEditing();
        float[][] positions = {{170, 300}, {400, 260}, {570, 260}, {740, 260}, {900, 260}};
        for (int i = 0; i < positions.length; i++) drag(view, i, positions[i][0], positions[i][1]);
        assertTrue("Editing must not press game keys", inputs.isEmpty());
        view.finishEditing(true);
        ChaosFaction2TouchControls restored = controls();
        for (int i = 0; i < positions.length; i++) assertArrayEquals(positions[i], center(restored, i), 0.01f);
        touch(restored, MotionEvent.ACTION_DOWN, new int[]{0}, positions[1]);
        touch(restored, MotionEvent.ACTION_UP, new int[]{0}, positions[1]);
        assertEquals(Arrays.asList("attack1:true", "attack1:false"), inputs);
    }

    @Test public void cancelDiscardsDraggedAndResetPositions() throws Exception {
        ChaosFaction2TouchControls view = controls();
        view.beginEditing(); drag(view, 1, 580, 240); view.finishEditing(true);
        view.beginEditing(); drag(view, 1, 250, 160); view.finishEditing(false);
        assertArrayEquals(new float[]{580, 240}, center(view, 1), 0.01f);
        view.beginEditing(); view.resetLayout(); view.finishEditing(false);
        assertArrayEquals(new float[]{580, 240}, center(view, 1), 0.01f);
        view.beginEditing(); view.resetLayout(); view.finishEditing(true);
        assertFalse(view.hasCustomLayout());
        assertFalse(controls().hasCustomLayout());
        assertTrue(layouts.getAll().isEmpty());
    }

    @Test public void positionsScaleWithScreenAndMirrorWithControlSides() throws Exception {
        ChaosFaction2TouchControls view = controls();
        view.beginEditing(); drag(view, 1, 580, 240); view.finishEditing(true);
        view.layout(0, 0, 1500, 900);
        assertArrayEquals(new float[]{870, 360}, center(view, 1), 0.01f);
        view.configure(1, 1, 0, true, 1);
        assertArrayEquals(new float[]{630, 360}, center(view, 1), 0.01f);
        view.beginEditing(); drag(view, 1, 300, 300); view.finishEditing(true);
        view.configure(1, 1, 0, false, 1);
        assertArrayEquals(new float[]{1200, 300}, center(view, 1), 0.01f);
    }

    @Test public void dragClampsToScreenAndKeepsToolbarAccessible() throws Exception {
        ChaosFaction2TouchControls view = controls();
        view.beginEditing(); drag(view, 4, -100, -100);
        float[] top = center(view, 4);
        assertTrue(top[0] >= 33 && top[1] >= 109);
        drag(view, 4, 5000, 5000);
        float[] bottom = center(view, 4);
        assertTrue(bottom[0] <= 967 && bottom[1] <= 567);
        view.finishEditing(true);
        view.configure(2, 1, 0, false, 1);
        view.layout(0, 0, 700, 400);
        float[] resized = center(view, 4);
        assertTrue(resized[0] <= 700 - 38 && resized[1] <= 400 - 38);
    }

    @Test public void editorTracksPointerIdsAndConsumesUnclaimedTouches() throws Exception {
        ChaosFaction2TouchControls view = controls();
        view.beginEditing();
        assertTrue(touch(view, MotionEvent.ACTION_DOWN, new int[]{0}, 450, 100));
        touch(view, MotionEvent.ACTION_UP, new int[]{0}, 450, 100);
        float[] attack = center(view, 2), move = center(view, 0);
        touch(view, MotionEvent.ACTION_DOWN, new int[]{7}, attack);
        touch(view, MotionEvent.ACTION_POINTER_DOWN | (1 << MotionEvent.ACTION_POINTER_INDEX_SHIFT),
            new int[]{7, 31}, attack[0], attack[1], move[0], move[1]);
        touch(view, MotionEvent.ACTION_MOVE, new int[]{31, 7}, move[0], move[1], 520, 250);
        touch(view, MotionEvent.ACTION_POINTER_UP, new int[]{31, 7}, move[0], move[1], 520, 250);
        touch(view, MotionEvent.ACTION_UP, new int[]{7}, 520, 250);
        assertArrayEquals(new float[]{520, 250}, center(view, 2), 0.01f);
        assertArrayEquals(move, center(view, 0), 0.01f);
        assertTrue(inputs.isEmpty());
    }

    @Test public void enteringEditorReleasesHeldKeysAndOldPauseAreaPassesThrough() throws Exception {
        ChaosFaction2TouchControls view = controls();
        float[] attack = center(view, 1);
        touch(view, MotionEvent.ACTION_DOWN, new int[]{0}, attack);
        view.beginEditing();
        assertEquals(Arrays.asList("attack1:true", "attack1:false"), inputs);
        view.finishEditing(false);
        inputs.clear();
        assertFalse(touch(view, MotionEvent.ACTION_DOWN, new int[]{0}, 40, 33));
        assertTrue(inputs.isEmpty());
    }

    @Test public void invalidSavedCoordinatesFallBackToDefaults() throws Exception {
        layouts.edit().putFloat("attack1_x", Float.POSITIVE_INFINITY).putFloat("attack1_y", 0.5f)
            .putFloat("drop_x", -1f).putFloat("drop_y", 2f).commit();
        ChaosFaction2TouchControls view = controls();
        assertFalse(view.hasCustomLayout());
        for (int i = 0; i < 5; i++) {
            float[] point = center(view, i);
            assertTrue(Float.isFinite(point[0]) && Float.isFinite(point[1]));
        }
    }

    @Test public void activityKeepsFrontendPausedDuringEditAndBackgroundResume() throws Exception {
        try (java.io.FileOutputStream game = context.openFileOutput("chaos-faction-2.swf", Context.MODE_PRIVATE)) {
            game.write(new byte[]{'F', 'W', 'S', 8}); // Presence fixture; WebView does not run Flash in this unit test.
        }
        context.getSharedPreferences("chaosfaction2_settings", Context.MODE_PRIVATE).edit()
            .putInt("touch_mode", 2).commit();
        ActivityController<MainActivity> controller = Robolectric.buildActivity(MainActivity.class).setup();
        try {
            MainActivity activity = controller.get();
            FrostFrontendView frontend = (FrostFrontendView) field(activity, "frontend");
            ChaosFaction2TouchControls view = (ChaosFaction2TouchControls) field(activity, "touchControls");
            Method item = MainActivity.class.getDeclaredMethod("handleItem", String.class, int.class);
            item.setAccessible(true);
            frontend.open();
            item.invoke(activity, "touch_edit", 1);
            assertTrue(frontend.isOpen());
            assertEquals(View.GONE, frontend.getVisibility());
            assertEquals(View.VISIBLE, view.getVisibility());
            controller.pause().resume();
            assertTrue(frontend.isOpen());
            assertEquals(View.VISIBLE, view.getVisibility());
            activity.onBackPressed();
            assertTrue(frontend.isOpen());
            assertEquals(View.VISIBLE, frontend.getVisibility());
            assertEquals(View.GONE, view.getVisibility());
            item.invoke(activity, "touch_edit", 1);
            activity.dispatchKeyEvent(new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_BUTTON_START));
            assertTrue(frontend.isOpen());
            assertFalse((boolean) field(activity, "editingTouchLayout"));
        } finally {
            controller.pause().stop().destroy();
            context.deleteFile("chaos-faction-2.swf");
        }
    }
}
