package watchfacetest.ninedof.com.watchfacetest;

import android.animation.ValueAnimator;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.os.Handler;
import android.os.Message;
import android.support.v4.content.res.ResourcesCompat;
import android.support.wearable.watchface.CanvasWatchFaceService;
import android.support.wearable.watchface.WatchFaceStyle;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.WindowInsets;


import java.util.Calendar;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;

public class BeamUpWatchface extends CanvasWatchFaceService {

    private static final String TAG = BeamUpWatchface.class.getSimpleName();

    private static final long UPDATE_RATE_MS = TimeUnit.SECONDS.toMillis(1);
    private static final int MSG_UPDATE_TIME = 0;

    private static int DIRTY_WHITE = Color.rgb(200, 200, 200);
    private static final int
        ANIM_DURATION = 500,
        BAR_HEIGHT = 10,
        NUM_DIGITS = 4,
        SEPARATION = 5,
        TIME_SIZE_ROUND = 100,
        TIME_SIZE_SQUARE = 90,
        DATE_SIZE_ROUND = 40,
        DATE_SIZE_SQUARE = 30;

    @Override
    public Engine onCreateEngine() {
        return new Engine();
    }

    private class Engine extends CanvasWatchFaceService.Engine {

        private final Handler mUpdateHandler = new UpdateHandler(this);
        private Calendar mCalendar;
        private Paint mTimePaint, mDatePaint, mBarPaint;

        private BeamUpDigit[] mDigits = new BeamUpDigit[NUM_DIGITS];
        private Rect mTimeBounds = new Rect();
        private Rect mBarRect = new Rect();
        private String mDateStr = "";
        private ValueAnimator.AnimatorUpdateListener mAnimationUpdateListener = new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                mBarRect.right = (int) animation.getAnimatedValue();
                if (isInteractive()) {
                    invalidate();
                }
            }
        };
        private int mTimeSize, mDateSize, mColonSize, mSurfaceWidth, mSurfaceHeight;
        private boolean mIsRound;

        private class BeamUpDigit {
            int position;
            int color;
            int value;

            public BeamUpDigit(int position) {
                value = 0;
                color = Color.WHITE;
                this.position = position;
            }

            public void draw(Canvas canvas) {
                mTimePaint.setColor(color);

                int x = mTimeBounds.left + mTimeSize;
                x += mIsRound ? (3 * SEPARATION) : (2 * SEPARATION);
                switch (this.position) {
                    case 1:
                        x += mTimeSize + SEPARATION;
                        break;
                    case 2:
                        x += (2 * mTimeSize) + (3 * SEPARATION) + mColonSize;
                        break;
                    case 3:
                        x += (3 * mTimeSize) + (4 * SEPARATION) + mColonSize;
                        break;
                    default: break;
                }

                canvas.drawText(String.valueOf(value), x, mTimeBounds.top + mTimeSize, mTimePaint);
            }
        }

        @Override
        public void onCreate(SurfaceHolder holder) {
            super.onCreate(holder);
            Log.d(TAG, "onCreate");

            setWatchFaceStyle(new WatchFaceStyle.Builder(BeamUpWatchface.this).build());

            mCalendar = Calendar.getInstance(TimeZone.getDefault());

            Typeface imagine = ResourcesCompat.getFont(getApplicationContext(), R.font.imagine);
            mTimePaint = new Paint();
            mTimePaint.setTypeface(imagine);
            mTimePaint.setTextAlign(Paint.Align.RIGHT);
            mTimePaint.setColor(Color.WHITE);

            mDatePaint = new Paint();
            mDatePaint.setTypeface(imagine);
            mDatePaint.setTextAlign(Paint.Align.RIGHT);
            mDatePaint.setColor(DIRTY_WHITE);

            mBarPaint = new Paint();
            mBarPaint.setColor(Color.WHITE);
            mBarPaint.setStyle(Paint.Style.FILL);
            mBarRect.left = 0;
            mBarRect.right = 0;

            for (int i = 0; i < NUM_DIGITS; i++) {
                mDigits[i] = new BeamUpDigit(i);
            }
        }

        private void calculateSizeMetrics() {
            mTimePaint.setTextSize(mIsRound ? TIME_SIZE_ROUND : TIME_SIZE_SQUARE);
            mDatePaint.setTextSize(mIsRound ? DATE_SIZE_ROUND : DATE_SIZE_SQUARE);

            // Calculate width of Imagine wide character
            Rect bounds = new Rect();
            mTimePaint.getTextBounds("0", 0, 1, bounds);
            mTimeSize = bounds.right;
            mTimePaint.getTextBounds(":", 0, 1, bounds);
            mColonSize = bounds.right;

            mDatePaint.getTextBounds("0", 0, 1, bounds);
            mDateSize = bounds.right;

            // Full width is (4 * char width) + (4 * separation) + colon width
            int totalWidth = (4 * mTimeSize) + mColonSize + (4 * SEPARATION);
            mTimeBounds.left = (mSurfaceWidth - totalWidth) / 2;
            mTimeBounds.top = (mSurfaceHeight - mTimeSize) / 2;
            mTimeBounds.right = mTimeBounds.left + totalWidth;
            mTimeBounds.bottom = mTimeBounds.top + mTimeSize;

            mBarRect.top = mTimeBounds.top + mTimeSize + SEPARATION;
            mBarRect.bottom = mBarRect.top + BAR_HEIGHT;
        }

        @Override
        public void onSurfaceChanged(SurfaceHolder holder, int format, int width, int height) {
            super.onSurfaceChanged(holder, format, width, height);
            Log.d(TAG, "onSurfaceChanged");

            mSurfaceWidth = width;
            mSurfaceHeight = height;

            calculateSizeMetrics();
        }

        @Override
        public void onApplyWindowInsets(WindowInsets insets) {
            super.onApplyWindowInsets(insets);
            Log.d(TAG, "onApplyWindowInsets");

            mIsRound = insets.isRound();
            calculateSizeMetrics();
        }

        @Override
        public void onDraw(Canvas canvas, Rect bounds) {
            // Background
            canvas.drawColor(Color.BLACK);

            // mTimeBounds rect
//            mTimePaint.setColor(Color.RED);
//            canvas.drawRect(mTimeBounds, mTimePaint);

            // Digits
            for (int i = 0; i < NUM_DIGITS; i++) {
                mDigits[i].draw(canvas);
            }

            // Colon
            int colonXOffset = mTimeBounds.left + (2 * mTimeSize) + mColonSize;
            colonXOffset += mIsRound ? (5 * SEPARATION) : (4 * SEPARATION);
            canvas.drawText(":", colonXOffset, mTimeBounds.top + mTimeSize, mTimePaint);

            // Date
            int dateXOffset = mTimeBounds.right;
            int dateYOffset = mTimeBounds.top + mTimeSize + mDateSize + BAR_HEIGHT + (2 * SEPARATION);
            dateXOffset += mIsRound ? (SEPARATION + 2) : (SEPARATION / 2) + 1;
            canvas.drawText(mDateStr, dateXOffset, dateYOffset, mDatePaint);

            // Bar
            canvas.drawRect(mBarRect, mBarPaint);

            // Alignment cross
//            mTimePaint.setColor(Color.GREEN);
//            canvas.drawLine(0, 0, mSurfaceWidth, mSurfaceHeight, mTimePaint);
//            canvas.drawLine(0, mSurfaceHeight, mSurfaceWidth, 0, mTimePaint);
        }

        @Override
        public void onTimeTick() {
            super.onTimeTick();

            onTimeUpdate();
        }

        @Override
        public void onDestroy() {
            super.onDestroy();
            mUpdateHandler.removeMessages(MSG_UPDATE_TIME);
        }

        @Override
        public void onVisibilityChanged(boolean visible) {
            super.onVisibilityChanged(visible);

            mCalendar.setTimeZone(TimeZone.getDefault());
            updateTimerIsRunning();
            updateTimeDisplay();
            invalidate();
        }

        @Override
        public void onAmbientModeChanged(boolean inAmbientMode) {
            super.onAmbientModeChanged(inAmbientMode);
            updateTimerIsRunning();
        }

        // Either on tick or on timer update
        private void onTimeUpdate() {
            mCalendar.setTimeInMillis(System.currentTimeMillis());
            int seconds = mCalendar.get(Calendar.SECOND);

            ValueAnimator animation = null;
            switch (seconds) {
                case 0:
                    updateTimeDisplay();
                    invalidate();
                    break;
                case 1:
                    animation = ValueAnimator.ofInt(mBarRect.right, 0);
                    break;
                case 15:
                    animation = ValueAnimator.ofInt(0, mSurfaceWidth / 4);
                    break;
                case 30:
                    animation = ValueAnimator.ofInt(mBarRect.right, mSurfaceWidth / 2);
                    break;
                case 45:
                    animation = ValueAnimator.ofInt(mBarRect.right, (3 * mSurfaceWidth) / 4);
                    break;
                case 59:
                    animation = ValueAnimator.ofInt(mBarRect.right, mSurfaceWidth);
                default: break;
            }

            if (animation != null) {
                animation.setDuration(ANIM_DURATION);
                animation.start();
                animation.addUpdateListener(mAnimationUpdateListener);
            }
        }

        private String getDayStr(int day) {
            switch (day) {
                case Calendar.MONDAY: return "MON";
                case Calendar.TUESDAY: return "TUE";
                case Calendar.WEDNESDAY: return "WED";
                case Calendar.THURSDAY: return "THU";
                case Calendar.FRIDAY: return "FRI";
                case Calendar.SATURDAY: return "SAT";
                case Calendar.SUNDAY: return "SUN";
                default: return "???";
            }
        }

        private void updateTimeDisplay() {
            String timeStr = String.format("%02d:%02d", mCalendar.get(Calendar.HOUR_OF_DAY), mCalendar.get(Calendar.MINUTE));
            mDigits[0].value = Integer.parseInt(String.valueOf(timeStr.charAt(0)));
            mDigits[1].value = Integer.parseInt(String.valueOf(timeStr.charAt(1)));
            mDigits[2].value = Integer.parseInt(String.valueOf(timeStr.charAt(3))); // skip :
            mDigits[3].value = Integer.parseInt(String.valueOf(timeStr.charAt(4)));
            mDateStr = String.format("%s %02d", getDayStr(mCalendar.get(Calendar.DAY_OF_WEEK)), mCalendar.get(Calendar.DAY_OF_MONTH));
        }

        private boolean isInteractive() {
            return isVisible() && !isInAmbientMode();
        }

        private void updateTimerIsRunning() {
            mUpdateHandler.removeMessages(MSG_UPDATE_TIME);

            if (isInteractive()) {
                mUpdateHandler.sendEmptyMessage(MSG_UPDATE_TIME);
            }
        }

        private void onInteractiveUpdate() {
            invalidate();

            if (isInteractive()) {
                onTimeUpdate();

                // Schedule next update
                long delayMs = UPDATE_RATE_MS - (System.currentTimeMillis() % UPDATE_RATE_MS);
                mUpdateHandler.sendEmptyMessageDelayed(MSG_UPDATE_TIME, delayMs);
            }
        }
    }

    private static class UpdateHandler extends Handler {

        private final BeamUpWatchface.Engine engine;

        UpdateHandler(BeamUpWatchface.Engine engine) {
            this.engine = engine;
        }

        @Override
        public void handleMessage(Message msg) {
            if (engine != null) {
                engine.onInteractiveUpdate();
            }
        }

    }
}
