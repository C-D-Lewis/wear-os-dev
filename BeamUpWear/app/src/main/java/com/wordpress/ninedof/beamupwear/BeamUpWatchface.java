package com.wordpress.ninedof.beamupwear;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
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
        BAR_ANIM_DURATION = 500, DIGIT_ANIM_DURATION = 300, BEAM_ANIM_DURATION = DIGIT_ANIM_DURATION,
        DIGIT_DELAY = 300,
        NUM_DIGITS = 4, BAR_HEIGHT = 10,
        SEPARATION = 5,
        TIME_SIZE_ROUND = 100, TIME_SIZE_SQUARE = 90,
        DATE_SIZE_ROUND = 40, DATE_SIZE_SQUARE = 30;

    @Override
    public Engine onCreateEngine() {
        return new Engine();
    }

    private class Engine extends CanvasWatchFaceService.Engine {

        private final Handler mUpdateHandler = new InteractiveUpdateHandler(this);
        private Calendar mCalendar;
        private Paint mTimePaint, mDatePaint;

        private BeamUpDigit[] mDigits = new BeamUpDigit[NUM_DIGITS];
        private BeamUpBeam[] mBeams = new BeamUpBeam[NUM_DIGITS];
        private BeamUpBar mBar = new BeamUpBar();
        private Rect mTimeBounds = new Rect();
        private String mDateStr = "";

        private int mTimeSize, mDateSize, mColonSize, mSurfaceWidth, mSurfaceHeight;
        private boolean mIsRound;

        @Override
        public void onCreate(SurfaceHolder holder) {
            super.onCreate(holder);
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

            for (int i = 0; i < NUM_DIGITS; i++) {
                mDigits[i] = new BeamUpDigit(i);
                mBeams[i] = new BeamUpBeam(i);
            }
        }

        private void calculateSizeMetrics() {
            mTimePaint.setTextSize(mIsRound ? TIME_SIZE_ROUND : TIME_SIZE_SQUARE);
            mDatePaint.setTextSize(mIsRound ? DATE_SIZE_ROUND : DATE_SIZE_SQUARE);

            // Font metrics
            Rect bounds = new Rect();
            mTimePaint.getTextBounds("0", 0, 1, bounds);
            mTimeSize = bounds.right; //(mIsRound ? 8 : 0);
            mTimePaint.getTextBounds(":", 0, 1, bounds);
            mColonSize = bounds.right;
            mDatePaint.getTextBounds("0", 0, 1, bounds);
            mDateSize = bounds.right;

            // Time bounds rect
            int totalWidth = (4 * mTimeSize) + mColonSize + (4 * SEPARATION);
            mTimeBounds.left = (mSurfaceWidth - totalWidth) / 2;
            mTimeBounds.top = (mSurfaceHeight - mTimeSize) / 2;
            mTimeBounds.right = mTimeBounds.left + totalWidth;
            mTimeBounds.bottom = mTimeBounds.top + mTimeSize;

            // Bar
            mBar.bounds.top = mTimeBounds.top + mTimeSize + SEPARATION;
            mBar.bounds.bottom = mBar.bounds.top + BAR_HEIGHT;

            // Digits
            for (int i = 0; i < NUM_DIGITS; i++) mDigits[i].yPosition = mTimeBounds.top + mTimeSize;

            // Beam metrics
            for (int i = 0; i < NUM_DIGITS; i++) mBeams[i].updateMetrics();
        }

        @Override
        public void onDraw(Canvas canvas, Rect bounds) {
            canvas.drawColor(Color.BLACK);

            for (int i = 0; i < NUM_DIGITS; i++) {
                mBeams[i].draw(canvas);
                mDigits[i].draw(canvas);
            }

            int colonXOffset = mTimeBounds.left + (2 * mTimeSize) + mColonSize;
            colonXOffset += mIsRound ? (5 * SEPARATION) : (4 * SEPARATION);
            canvas.drawText(":", colonXOffset, mTimeBounds.top + mTimeSize, mTimePaint);

            int dateXOffset = mTimeBounds.right;
            int dateYOffset = mTimeBounds.top + mTimeSize + mDateSize + BAR_HEIGHT + (2 * SEPARATION);
            dateXOffset += mIsRound ? (SEPARATION + 2) : (SEPARATION / 2) + 1;
            canvas.drawText(mDateStr, dateXOffset, dateYOffset, mDatePaint);

            mBar.draw(canvas);
        }

        // Either on tick or on timer update
        private void onTimeUpdate(long millis) {
            mCalendar.setTimeInMillis(millis);

            int seconds = mCalendar.get(Calendar.SECOND);
            switch (seconds) {
                case 0:
                    updateTimeDisplay();
                    invalidate();
                    break;
                case 1:
                    mBar.animateReturn();

                    for (int i = 0; i < NUM_DIGITS; i++) {
                        mDigits[i].color = Color.WHITE;
                    }
                    break;
                case 15:
                    mBar.animateFirstQuarter();
                    break;
                case 30:
                    mBar.animateSecondQuarter();
                    break;
                case 45:
                    mBar.animateThirdQuarter();
                    break;
                case 59:
                    mBar.animateFourthQuarter();
                    beginMinuteAnimation();
                default: break;
            }

            // Enable for testing beam alignment
//            for (int i = 0; i < NUM_DIGITS; i += 1) {
//                mBeams[i].bounds.bottom = 1000;
//                mBeams[i].color = DIRTY_WHITE;
//                mDigits[i].value = 8;
//            }
        }

        @Override
        public void onSurfaceChanged(SurfaceHolder holder, int format, int width, int height) {
            super.onSurfaceChanged(holder, format, width, height);

            mSurfaceWidth = width;
            mSurfaceHeight = height;
            calculateSizeMetrics();
        }

        @Override
        public void onApplyWindowInsets(WindowInsets insets) {
            super.onApplyWindowInsets(insets);

            mIsRound = insets.isRound();
            calculateSizeMetrics();
        }

        private boolean[] updateDigitChanges() {
            boolean[] changes = new boolean[4];
            if ((mDigits[0].value == 0 && mDigits[1].value == 9 && mDigits[2].value == 5 && mDigits[3].value == 9) ||
                    (mDigits[0].value == 1 && mDigits[1].value == 9 && mDigits[2].value == 5 && mDigits[3].value == 9) ||
                    (mDigits[0].value == 2 && mDigits[1].value == 3 && mDigits[2].value == 5 && mDigits[3].value == 9)) {
                changes[0] = true;
            }
            if (mDigits[2].value == 5 && mDigits[3].value == 9) {
                changes[1] = true;
            }
            if (mDigits[3].value == 9) {
                changes[2] = true;
            }
            changes[3] = true;
            return changes;
        }

        private void beginMinuteAnimation() {
            boolean[] changes = updateDigitChanges();
            for (int i = 0; i < NUM_DIGITS; i++) {
                if (changes[i])  {
                    mBeams[i].beginAnimationSequence();
                    mDigits[i].beginAnimationSequence();
                }
            }
        }

        @Override
        public void onTimeTick() {
            super.onTimeTick();
            onTimeUpdate(System.currentTimeMillis());
        }

        @Override
        public void onDestroy() {
            super.onDestroy();
            mUpdateHandler.removeMessages(MSG_UPDATE_TIME);
        }

        @Override
        public void onVisibilityChanged(boolean visible) {
            super.onVisibilityChanged(visible);

            if (visible) {
                long millis = System.currentTimeMillis();
                mCalendar.setTimeInMillis(millis);
                while (mCalendar.get(Calendar.SECOND) % 15 != 0) {
                    millis -= 500;
                    mCalendar.setTimeInMillis(millis);
                }
                onTimeUpdate(millis);
            }

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
            if (isInteractive()) mUpdateHandler.sendEmptyMessage(MSG_UPDATE_TIME);
        }

        private void onInteractiveUpdate() {
            invalidate();
            if (isInteractive()) {
                onTimeUpdate(System.currentTimeMillis());

                long delayMs = UPDATE_RATE_MS - (System.currentTimeMillis() % UPDATE_RATE_MS);
                mUpdateHandler.sendEmptyMessageDelayed(MSG_UPDATE_TIME, delayMs);
            }
        }


        // -------- Timeline -----------------------------------
        // 59.0 s x Bar to 4/4
        //        - Beam animations, digit animations scheduled
        // 0s     x Time change
        // 1s     x Bar to 0/4
        //
        //--------- Transitions --------------------------------
        // Time (59s) | Event
        // T+0         | Beam DOWN (300)
        // T+500       | Digit UP (300)
        //   DUP+400   | Digit Down (300)
        // BDN+1400    | Beam UP (300)

        private class BeamUpBeam {
            ValueAnimator downAnimation, upAnimation;
            ValueAnimator.AnimatorUpdateListener updateListener;
            Rect bounds = new Rect();
            Paint paint = new Paint();
            int index;
            int color = Color.WHITE;

            BeamUpBeam(int index) {
                this.index = index;

                updateMetrics();
                paint.setStyle(Paint.Style.FILL);

                updateListener = new ValueAnimator.AnimatorUpdateListener() {
                    @Override
                    public void onAnimationUpdate(ValueAnimator animation) {
                        bounds.bottom = (int) animation.getAnimatedValue();
                        if (isInteractive()) invalidate();
                    }
                };
            }

            void updateMetrics() {
                int x = mTimeBounds.left + 2;
                if (!mIsRound) {
                    x -= (SEPARATION - 2);
                }
                switch (this.index) {
                    case 1:
                        x += mTimeSize + (1 * SEPARATION);
                        break;
                    case 2:
                        x += (2 * mTimeSize) + (3 * SEPARATION) + mColonSize;
                        break;
                    case 3:
                        x += (3 * mTimeSize) + (4 * SEPARATION) + mColonSize;
                        break;
                    default: break;
                }
                bounds.left = x;
                bounds.right = bounds.left + mTimeSize;
            }

            void beginAnimationSequence() {
                downAnimation = ValueAnimator.ofInt(bounds.bottom, mTimeBounds.top + mTimeSize);
                downAnimation.setStartDelay(0);
                downAnimation.setDuration(BEAM_ANIM_DURATION);
                downAnimation.addListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        super.onAnimationEnd(animation);

                        // Schedule up
                        upAnimation = ValueAnimator.ofInt(bounds.bottom, 0);
                        upAnimation.setStartDelay(1400);
                        upAnimation.setDuration(BEAM_ANIM_DURATION);
                        beginAnimation(upAnimation);
                    }
                });
                beginAnimation(downAnimation);
            }

            void beginAnimation(ValueAnimator animation) {
                animation.start();
                animation.addUpdateListener(updateListener);
            }

            void draw(Canvas canvas) {
                paint.setColor(color);
                canvas.drawRect(bounds, paint);
            }
        }

        private class BeamUpDigit {
            ValueAnimator downAnimation, upAnimation;
            ValueAnimator.AnimatorUpdateListener updateListener;
            int index, color, value, yPosition;

            BeamUpDigit(int index) {
                color = Color.WHITE;
                this.index = index;

                updateListener = new ValueAnimator.AnimatorUpdateListener() {
                    @Override
                    public void onAnimationUpdate(ValueAnimator animation) {
                        yPosition = (int) animation.getAnimatedValue();
                        if (isInteractive()) invalidate();
                    }
                };
            }

            void beginAnimationSequence() {
                upAnimation = ValueAnimator.ofInt(yPosition, -mTimeSize);
                upAnimation.setStartDelay(450);
                upAnimation.setDuration(DIGIT_ANIM_DURATION);

                upAnimation.addListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationStart(Animator animation) {
                        super.onAnimationStart(animation);

                        color = Color.BLACK;
                    }

                    @Override
                    public void onAnimationEnd(Animator animation) {
                        super.onAnimationEnd(animation);

                        // Schedule down
                        downAnimation = ValueAnimator.ofInt(yPosition, mTimeBounds.top + mTimeSize);
                        downAnimation.setStartDelay(400);
                        downAnimation.setDuration(DIGIT_ANIM_DURATION);
                        downAnimation.addListener(new AnimatorListenerAdapter() {
                            @Override
                            public void onAnimationEnd(Animator animation) {
                                super.onAnimationEnd(animation);

                                mUpdateHandler.postDelayed(new Runnable() {
                                    @Override
                                    public void run() {
                                        color = Color.WHITE;
                                    }
                                }, 200);
                            }
                        });
                        beginAnimation(downAnimation);
                    }
                });
                beginAnimation(upAnimation);
            }

            void beginAnimation(ValueAnimator animation) {
                animation.start();
                animation.addUpdateListener(updateListener);
            }

            void draw(Canvas canvas) {
                mTimePaint.setColor(color);

                int x = mTimeBounds.left + mTimeSize + (mIsRound ? (3 * SEPARATION) : (2 * SEPARATION));
                switch (this.index) {
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

                canvas.drawText(String.valueOf(value), x, yPosition, mTimePaint);
            }
        }

        private class BeamUpBar {

            Rect bounds = new Rect();
            Paint paint = new Paint();
            ValueAnimator mAnimation = null;
            private ValueAnimator.AnimatorUpdateListener mAnimationListener = new ValueAnimator.AnimatorUpdateListener() {
                @Override
                public void onAnimationUpdate(ValueAnimator animation) {
                    bounds.right = (int) animation.getAnimatedValue();
                    if (isInteractive()) invalidate();
                }
            };

            BeamUpBar() {
                paint.setColor(Color.WHITE);
                paint.setStyle(Paint.Style.FILL);
            }

            void draw(Canvas canvas) {
                canvas.drawRect(bounds, paint);
            }

            void animateFirstQuarter() {
                mAnimation = ValueAnimator.ofInt(bounds.right, mSurfaceWidth / 4);
                beginAnimation();
            }

            void animateSecondQuarter() {
                mAnimation = ValueAnimator.ofInt(bounds.right, mSurfaceWidth / 2);
                beginAnimation();
            }

            void animateThirdQuarter() {
                mAnimation = ValueAnimator.ofInt(bounds.right, (3 * mSurfaceWidth) / 4);
                beginAnimation();
            }

            void animateFourthQuarter() {
                mAnimation = ValueAnimator.ofInt(bounds.right, mSurfaceWidth);
                beginAnimation();
            }

            void animateReturn() {
                mAnimation = ValueAnimator.ofInt(bounds.right, 0);
                beginAnimation();
            }

            private void beginAnimation() {
                mAnimation.setDuration(BAR_ANIM_DURATION);
                mAnimation.start();
                mAnimation.addUpdateListener(mAnimationListener);
            }

        }

    }

    private static class InteractiveUpdateHandler extends Handler {

        private final BeamUpWatchface.Engine engine;

        InteractiveUpdateHandler(BeamUpWatchface.Engine engine) {
            this.engine = engine;
        }

        @Override
        public void handleMessage(Message msg) {
            if (engine != null) engine.onInteractiveUpdate();
        }

    }


}
