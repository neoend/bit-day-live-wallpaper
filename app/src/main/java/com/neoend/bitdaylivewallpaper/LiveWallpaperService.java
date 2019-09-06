package com.neoend.bitdaylivewallpaper;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.os.Handler;
import android.service.wallpaper.WallpaperService;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.SurfaceHolder;

import java.util.Calendar;

public class LiveWallpaperService extends WallpaperService {

    @Override
    public Engine onCreateEngine() {
        return new BitDayLiveEngine();
    }

    public class BitDayLiveEngine extends Engine {

        private static final String TAG = "BitDayLiveEngine";

        private final Handler handler = new Handler();
        private BroadcastReceiver receiver = null;
        private float xOffset = 0.5f, yOffset = 0.5f;
        private Bitmap lastBackground = null, lastBackgroundScaled = null;
        private int lastHour = -1, lastWidth = -1, lastHeight = -1;
        private final Runnable drawRunner =
                new Runnable() {
                    @Override
                    public void run() {
                        draw();
                    }
                };

        @Override
        public void onVisibilityChanged(boolean visible) {
            Log.d(TAG, "onVisibilityChanged()");
            if (visible) {
                handler.post(drawRunner);
                IntentFilter filter = new IntentFilter(Intent.ACTION_TIME_TICK);
                filter.matchAction(Intent.ACTION_TIME_CHANGED);
                filter.matchAction(Intent.ACTION_TIMEZONE_CHANGED);

                receiver = new BroadcastReceiver() {

                    private int lastHour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);

                    @Override
                    public void onReceive(Context context, Intent intent) {
                        int currentHour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);

                        if (lastHour != currentHour) {
                            draw();
                        }

                        lastHour = currentHour;
                    }
                };

                registerReceiver(receiver, filter);
            } else {
                killResources();
            }
        }

        @Override
        public void onSurfaceDestroyed(SurfaceHolder holder) {
            super.onSurfaceDestroyed(holder);
            killResources();
        }

        @Override
        public void onOffsetsChanged(float xOffset, float yOffset, float xStep, float yStep, int xPixels, int yPixels) {
            this.xOffset = xOffset;
            this.yOffset = yOffset;
            draw();
        }

        private void killResources() {
            handler.removeCallbacks(drawRunner);
            if (receiver != null) {
                unregisterReceiver(receiver);
                receiver = null;
            }
        }

        public void draw() {

            if (isPreview()) {
                xOffset = yOffset = 0.5f;
            }

            final SurfaceHolder holder = getSurfaceHolder();

            Canvas canvas = null;

            try {
                canvas = holder.lockCanvas();

                canvas.drawColor(Color.BLACK);

                Resources resources = getResources();
                DisplayMetrics metrics = resources.getDisplayMetrics();
                Bitmap background = getBackground(resources);

                float
                        x = (metrics.widthPixels - background.getWidth()) * xOffset,
                        y = (metrics.heightPixels - background.getHeight()) * yOffset;

                canvas.drawBitmap(background, x, y, null);
            } finally {
                if (canvas != null) holder.unlockCanvasAndPost(canvas);
            }

            handler.removeCallbacks(drawRunner);
        }

        private int getBackgroundForHour(int hour) {
            if (hour >= 0 && hour <= 630)
                return R.drawable.wall11_late_night;
            else if (hour >= 2300)
                return R.drawable.wall10_mid_night;
            else if (hour >= 1900)
                return R.drawable.wall9_early_night;
            else if (hour >= 1830)
                return R.drawable.wall8_late_evening;
            else if (hour >= 1800)
                return R.drawable.wall7_mid_evening;
            else if (hour >= 1600)
                return R.drawable.wall6_early_evening;
            else if (hour >= 1400)
                return R.drawable.wall5_late_afternoon;
            else if (hour >= 1200)
                return R.drawable.wall4_mid_afternoon;
            else if (hour >= 1000)
                return R.drawable.wall3_early_afternoon;
            else if (hour >= 730)
                return R.drawable.wall2_late_morning;
            else if (hour >= 700)
                return R.drawable.wall1_mid_morning;
            else if (hour > 630)
                return R.drawable.wall0_early_morning;
            else
                return R.drawable.wall2_late_morning;
        }

        public int getHourMinute() {
            Calendar calendar = Calendar.getInstance();
            int hour = calendar.get(Calendar.HOUR_OF_DAY);
            int min = calendar.get(Calendar.MINUTE);
            String time = String.format("%d%02d", hour, min);
            Log.d(TAG, "time : " + time);
            return Integer.parseInt(time);
        }

        public Bitmap getBackground(Resources resources) {
            DisplayMetrics metrics = resources.getDisplayMetrics();

            int
                    currentWidth = metrics.widthPixels,
                    currentHeight = metrics.heightPixels,
                    currentHour = getHourMinute();
                    //currentHour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);

            if (lastHour != currentHour) {
                int id = getBackgroundForHour(currentHour);
                lastBackground = BitmapFactory.decodeResource(resources, id);
            }

            if (lastHour != currentHour
                    || lastWidth != currentWidth
                    || lastHeight != currentHeight) {

                lastBackgroundScaled = createBitmapFillDisplay(
                        lastBackground,
                        currentWidth,
                        currentHeight
                );

                lastHour = currentHour;
                lastWidth = currentWidth;
                lastHeight = currentHeight;
            }

            return lastBackgroundScaled;
        }

        private Bitmap createBitmapFillDisplay(Bitmap bitmap, float displayWidth, float displayHeight) {

            float
                    bitmapWidth = bitmap.getWidth(),
                    bitmapHeight = bitmap.getHeight(),
                    xScale = displayWidth / bitmapWidth,
                    yScale = displayHeight / bitmapHeight,
                    scale = Math.max(xScale, yScale),
                    scaledWidth = scale * bitmapWidth,
                    scaledHeight = scale * bitmapHeight;

            Bitmap scaledImage = Bitmap.createBitmap((int) scaledWidth, (int) scaledHeight, Config.ARGB_8888);

            Canvas canvas = new Canvas(scaledImage);

            Matrix transformation = new Matrix();
            transformation.preScale(scale, scale);

            Paint paint = new Paint();
            paint.setFilterBitmap(true);

            canvas.drawBitmap(bitmap, transformation, paint);

            return scaledImage;
        }
    }
}
