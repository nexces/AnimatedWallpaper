/**
 * http://code.tutsplus.com/tutorials/create-a-live-wallpaper-on-android-using-an-animated-gif--cms-23088
 */

package pl.nxstudio.animatedwallpaper;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Point;
import android.os.Build;
import android.os.Handler;
import android.service.wallpaper.WallpaperService;
import android.graphics.Movie;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Display;
import android.view.SurfaceHolder;
import android.view.WindowManager;

import java.io.InputStream;

public class AnimatedWallpaperService extends WallpaperService {
    public AnimatedWallpaperService() {
    }

    @Override
    public WallpaperService.Engine onCreateEngine() {
        try {
//            InputStream stream = getResources().getAssets().open("tumblr_nlrel8NNEC1rxgairo1_400.gif");
            InputStream stream = getResources().getAssets().open("tumblr_mto0rmVzQI1s52080o1_500.gif");
            Movie movie = Movie.decodeStream(stream);
            if (movie == null) {
                Log.e("AnimatedWallpaper", "and we're fucked :(");
                return null;
            }

            return new AnimatedWallpaperEngine(movie);
        } catch (java.io.IOException e) {
            Log.w("AnimatedWallpaper", "Could not load asset");
            return null;
        }
    }

    public class AnimatedWallpaperEngine extends WallpaperService.Engine {
        private final int frameDuration = 10;

        private SurfaceHolder holder;
        private Movie movie;
        private boolean visible;
        private Handler handler;

        private Display display;
//        private DisplayMetrics displayMetrics;

        private float scalingFactor;
        private float verticalShift;
        private float horizontalShift;

        public AnimatedWallpaperEngine(Movie movie) {
            Log.d("AnimatedWallpaperEngine", "Starting engine");
            scalingFactor = 1.000f;
            verticalShift = 0;
            horizontalShift = 0;
            this.movie = movie;
            handler = new Handler();

            WindowManager wm = (WindowManager) getApplicationContext().getSystemService(Context.WINDOW_SERVICE);
            display = wm.getDefaultDisplay();
        }

        @Override
        public void onCreate(SurfaceHolder surfaceHolder) {
            super.onCreate(surfaceHolder);
            this.holder = surfaceHolder;
        }

        private Runnable drawGIF = new Runnable() {
            public void run() {
                draw();
            }
        };

        private void draw() {
            if (visible) {
                Canvas canvas = holder.lockCanvas();
                canvas.save();

                Log.v("AnimatedWallpaperEngine", "Size: " + canvas.getWidth() + " x " + canvas.getHeight());

                // scale canvas using its center as reference (zoom in)
                canvas.scale(scalingFactor, scalingFactor, canvas.getWidth() / 2, canvas.getHeight() / 2);

                // place movie in the center of already scaled (!) canvas
                movie.draw(canvas, horizontalShift, verticalShift);

                canvas.restore();
                holder.unlockCanvasAndPost(canvas);
                movie.setTime((int) (System.currentTimeMillis() % movie.duration()));

                handler.removeCallbacks(drawGIF);
                handler.postDelayed(drawGIF, frameDuration);
            }
        }

        @Override
        public void onVisibilityChanged(boolean visible) {
            this.visible = visible;
            if (visible) {
                handler.post(drawGIF);
            } else {
                handler.removeCallbacks(drawGIF);
            }
        }

        @Override
        public void onDestroy() {
            super.onDestroy();
            handler.removeCallbacks(drawGIF);
        }

        @Override
        public void onSurfaceChanged(SurfaceHolder holder, int format, int width, int height) {
            Log.d("AnimatedWallpaperEngine", "Surface has changed!");

            Canvas canvas = holder.lockCanvas();

            DisplayMetrics displayMetrics = new DisplayMetrics();
            display.getMetrics(displayMetrics);

            // since SDK_INT = 1;
            int widthPixels = displayMetrics.widthPixels;
            int heightPixels = displayMetrics.heightPixels;

            // includes window decorations (statusbar bar/menu bar)
            if (Build.VERSION.SDK_INT >= 14 && Build.VERSION.SDK_INT < 17) {
                try {
                    widthPixels = (Integer) Display.class.getMethod("getRawWidth").invoke(display);
                    heightPixels = (Integer) Display.class.getMethod("getRawHeight").invoke(display);
                } catch (Exception ignored) {
                }
            }

            // includes window decorations (statusbar bar/menu bar)
            if (Build.VERSION.SDK_INT >= 17) {
                try {
                    Point realSize = new Point();
                    Display.class.getMethod("getRealSize", Point.class).invoke(display, realSize);
                    widthPixels = realSize.x;
                    heightPixels = realSize.y;
                } catch (Exception ignored) {
                }
            }

            Log.d("AnimatedWallpaperEngine", "Screen dimensions: " + widthPixels + " x " + heightPixels);
            Log.d("AnimatedWallpaperEngine", "Canvas dimensions: " + canvas.getWidth() + " x " + canvas.getHeight());
            Log.d("AnimatedWallpaperEngine", "Movie dimensions: " + movie.width() + " x " + movie.height());
            Log.d("AnimatedWallpaperEngine", "Width scaling factor: " + Float.toString((float) displayMetrics.widthPixels / (float) movie.width()));
            Log.d("AnimatedWallpaperEngine", "Height scaling factor: " + Float.toString((float) displayMetrics.heightPixels / (float) movie.height()));
            if (widthPixels / movie.width() > heightPixels / movie.height()) {
                Log.d("AnimatedWallpaperEngine", "Using width for scaling");
                scalingFactor = (float) widthPixels / (float) movie.width();
//                horizontalShift = 0;
//                verticalShift = (heightPixels - (movie.height() * scalingFactor)) / 2 / scalingFactor;
            } else {
                Log.d("AnimatedWallpaperEngine", "Using height for scaling");
                scalingFactor = (float) heightPixels / (float) movie.height();
//                horizontalShift = (widthPixels - (movie.width() * scalingFactor)) / 2 / scalingFactor;
//                verticalShift = 0;
            }
            Log.d("AnimatedWallpaperEngine", "Used scaling factor: " + Float.toString(scalingFactor));
            Log.d("AnimatedWallpaperEngine", "Scaled movie dimensions: " + (movie.width() * scalingFactor) + " x " + (movie.height() * scalingFactor));
//            Log.d("AnimatedWallpaperEngine", "Calculated shifts: " + horizontalShift + " x " + verticalShift);

//            scalingFactor = 1.000f;
            horizontalShift = (canvas.getWidth() - movie.width()) / 2;
            verticalShift = (canvas.getHeight() - movie.height()) / 2;

            holder.unlockCanvasAndPost(canvas);
        }
    }


}
