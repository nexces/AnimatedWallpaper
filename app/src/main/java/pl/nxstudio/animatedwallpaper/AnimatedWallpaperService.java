/**
 * http://code.tutsplus.com/tutorials/create-a-live-wallpaper-on-android-using-an-animated-gif--cms-23088
 */

package pl.nxstudio.animatedwallpaper;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Canvas;
import android.graphics.Point;
import android.os.Build;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.service.wallpaper.WallpaperService;
import android.graphics.Movie;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Display;
import android.view.SurfaceHolder;
import android.view.WindowManager;

import java.io.InputStream;
import java.util.prefs.Preferences;

public class AnimatedWallpaperService extends WallpaperService implements SharedPreferences.OnSharedPreferenceChangeListener {
    private AnimatedWallpaperEngine engine;
    private SharedPreferences sharedPreferences;
    public AnimatedWallpaperService() {
    }

    @Override
    public void onCreate() {
        super.onCreate();
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        sharedPreferences.registerOnSharedPreferenceChangeListener(this);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        sharedPreferences.unregisterOnSharedPreferenceChangeListener(this);
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        engine.setMovie(getMovieFromSettings());
        engine.recalculateScaling();
    }

    private Movie getMovieFromSettings() {
        Log.d("Service", "Attempting to read image from settings");
        try {
            SharedPreferences defaultSharedPreferences = PreferenceManager
                    .getDefaultSharedPreferences(getApplicationContext());
            SharedPreferences sharedPreferences = getSharedPreferences("app", MODE_PRIVATE);

            String imageName = sharedPreferences.getString("image", defaultSharedPreferences.getString("image", ""));

            Log.i("Engine", "Selected image: " + imageName);
            InputStream stream = getResources().getAssets().open(imageName);
            Movie movie = Movie.decodeStream(stream);
            if (movie == null) {
                Log.e("Engine", "and we're fucked :(");
                return null;
            }
            return movie;
        } catch (java.io.IOException e) {
            Log.w("Engine", "Could not load asset");
            return null;
        }
    }

    @Override
    public WallpaperService.Engine onCreateEngine() {
        Movie movie = getMovieFromSettings();
        engine = new AnimatedWallpaperEngine();
        engine.setMovie(movie);
        return engine;
    }

    public class AnimatedWallpaperEngine extends WallpaperService.Engine {
        private final int frameDuration = 10;

        private SurfaceHolder holder;
        private Movie movie;
        private boolean visible;
        private Handler handler;

        private Display display;

        private float scalingFactor;

        public AnimatedWallpaperEngine() {
            Log.d("Engine", "Starting engine");
            scalingFactor = 1.000f;
            WindowManager wm = (WindowManager) getApplicationContext().getSystemService(Context.WINDOW_SERVICE);
            display = wm.getDefaultDisplay();
            handler = new Handler();
        }

        public void setMovie(Movie movie) {
            this.movie = movie;
            recalculateScaling();
        }

        private void recalculateScaling() {
            Log.d("Engine", "Recalculating scaling factor");
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

            Log.d("Engine", "Screen dimensions: " + widthPixels + " x " + heightPixels);
            Log.d("Engine", "Movie dimensions: " + movie.width() + " x " + movie.height());
            Log.d("Engine", "Width scaling factor: " + Float.toString((float) displayMetrics.widthPixels / (float) movie.width()));
            Log.d("Engine", "Height scaling factor: " + Float.toString((float) displayMetrics.heightPixels / (float) movie.height()));
            if (widthPixels / movie.width() > heightPixels / movie.height()) {
                Log.d("Engine", "Using width for scaling");
                scalingFactor = (float) widthPixels / (float) movie.width();
            } else {
                Log.d("Engine", "Using height for scaling");
                scalingFactor = (float) heightPixels / (float) movie.height();
            }
            Log.d("AnimatedWallpaperEngine", "Used scaling factor: " + Float.toString(scalingFactor));
            Log.d("AnimatedWallpaperEngine", "Scaled movie dimensions: " + (movie.width() * scalingFactor) + " x " + (movie.height() * scalingFactor));
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

                // scale canvas using its center as reference (zoom in)
                canvas.scale(scalingFactor, scalingFactor, canvas.getWidth() / 2, canvas.getHeight() / 2);

                // place movie in the center of already scaled (!) canvas
                movie.draw(canvas, (canvas.getWidth() - movie.width()) / 2, (canvas.getHeight() - movie.height()) / 2);

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
            Log.d("Engine", "Surface has changed!");

            recalculateScaling();
        }
    }


}
