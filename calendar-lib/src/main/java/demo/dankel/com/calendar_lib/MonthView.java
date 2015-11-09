package demo.dankel.com.calendar_lib;

import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.drawable.ShapeDrawable;
import android.graphics.drawable.shapes.OvalShape;
import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;
import android.text.TextPaint;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.animation.OvershootInterpolator;
import android.widget.Scroller;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * TODO: document your custom view class.
 */
public class MonthView extends View implements ValueAnimator.AnimatorUpdateListener {
    private static final String TAG = "MonthView";
    private static final float mDateNumTextSize = 14;  // 日期数字字体大小，单位sp
    private static final float mLunarTextSize = 10;  // 农历日期字体大小，单位sp

    private Context context;

    private TextPaint mTextPaint;
    private float mTextWidth;
    private float mTextHeight;
    private Scroller mScroller;

    private Calendar today = Calendar.getInstance();
    private SimpleDateFormat mDateFormat = new SimpleDateFormat("yyyy-MM-dd");

    private int FIRST_DAY_OF_WEEK = Calendar.SUNDAY;    // 周首日

    private Map<Integer, String[][]> mMonthMap = new HashMap<>();     //  记录每个月的表

    private int mMonthOffset = 0;  // 相对于今天的月份差，例：今天是2月， 此值为1，即当前记录的月份是3月
    private int lastPointX;
    private int lastMoveX;

    private OnSelectedDateChangedListener mSelectedDateChangedListener;
    private SelectedDateInfo mLastSelection;
    private float mCellWidth;
    private ShapeDrawable mBackgroundShape;
    private float density;   // 屏幕像素密度系数
    private int mTouchSlop = 0;   //

    public MonthView(Context context) {
        super(context);
        this.context = context;
        init(null, 0);
    }

    public MonthView(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.context = context;
        init(attrs, 0);
    }

    public MonthView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        this.context = context;
        init(attrs, defStyle);
    }

    private void init(AttributeSet attrs, int defStyle) {
        // Load attributes
        final TypedArray a = getContext().obtainStyledAttributes(attrs, R.styleable.MonthView, defStyle, 0);
        FIRST_DAY_OF_WEEK = a.getInt(R.styleable.MonthView_firstDayOfWeek, Calendar.SUNDAY);
        a.recycle();

        // Set up a default TextPaint object
        mTextPaint = new TextPaint();
        mTextPaint.setFlags(Paint.ANTI_ALIAS_FLAG);
        mTextPaint.setTextAlign(Paint.Align.CENTER);

        mScroller = new Scroller(context);
        mTouchSlop = ViewConfiguration.get(context).getScaledTouchSlop();

        // Update TextPaint and text measurements from attributes
        invalidateTextPaintAndMeasurements();

        calculateDaysOnNearestThreeMonth();
    }

    @Override
    public void onAnimationUpdate(ValueAnimator animation) {
        invalidate();
    }

    public interface OnSelectedDateChangedListener {
        void onChanged(Date mSelectedDate);
    }

    /**
     * 选择的日期信息封装，属性动画需要用
     */
    public static class SelectedDateInfo implements Parcelable {
        private float radius;
        private float row;
        private float column;
        private float selectedOffSet;

        public float getSelectedOffSet() {
            return selectedOffSet;
        }

        public void setSelectedOffSet(float selectedOffSet) {
            this.selectedOffSet = selectedOffSet;
        }

        public float getRadius() {
            return radius;
        }

        public void setRadius(float radius) {
            this.radius = radius;
        }

        public float getRow() {
            return row;
        }

        public void setRow(float row) {
            this.row = row;
        }

        public float getColumn() {
            return column;
        }

        public void setColumn(float column) {
            this.column = column;
        }

        @Override
        public int describeContents() {
            return 0;
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            dest.writeFloat(this.radius);
            dest.writeFloat(this.row);
            dest.writeFloat(this.column);
            dest.writeFloat(this.selectedOffSet);
        }

        public SelectedDateInfo() {
        }

        protected SelectedDateInfo(Parcel in) {
            this.radius = in.readFloat();
            this.row = in.readFloat();
            this.column = in.readFloat();
            this.selectedOffSet = in.readFloat();
        }

        public static final Creator<SelectedDateInfo> CREATOR = new Creator<SelectedDateInfo>() {
            public SelectedDateInfo createFromParcel(Parcel source) {
                return new SelectedDateInfo(source);
            }

            public SelectedDateInfo[] newArray(int size) {
                return new SelectedDateInfo[size];
            }
        };
    }

    /**
     * 计算当前月上一个月，当前月和当前月下一个月的日期编排
     */
    private void calculateDaysOnNearestThreeMonth() {
        calculateDateByMonthOffset(mMonthOffset - 1);
        calculateDateByMonthOffset(mMonthOffset);
        calculateDateByMonthOffset(mMonthOffset + 1);
    }

    /**
     * 通过相对于今天的月份差，计算当前月的日期并保存
     *
     * @param monthOffset 相对应今天的月份差
     */
    private void calculateDateByMonthOffset(int monthOffset) {
//        if (null != mMonthMap.get(monthOffset) && !mReBuildFlag) {
//            return;
//        }

        Calendar calendar = (Calendar) today.clone();
        calendar.add(Calendar.MONTH, monthOffset);
        calendar.set(Calendar.DAY_OF_MONTH, 1);

        int weekOfDayOne = calendar.get(Calendar.DAY_OF_WEEK);   // 1号是星期几
        int mDateNumOnFirstRow;

        if (FIRST_DAY_OF_WEEK == Calendar.SUNDAY) {
            mDateNumOnFirstRow = weekOfDayOne - FIRST_DAY_OF_WEEK;
        } else {
            if (weekOfDayOne == Calendar.SUNDAY) {
                mDateNumOnFirstRow = 6;
            } else {
                mDateNumOnFirstRow = weekOfDayOne - FIRST_DAY_OF_WEEK;
            }
        }

        String[][] mMonthDateArray = new String[6][7];
        StringBuilder mDateStrBuilder;
        String mMonthStr, mDateStr, mCompleteDateStr;

        int mMaxDayOfMonth = calendar.getActualMaximum(Calendar.DAY_OF_MONTH);
        for (int day = 1; day <= mMaxDayOfMonth; day++) {
            int row = (mDateNumOnFirstRow + day - 1) / 7;   // 行，从0开始
            int column = (mDateNumOnFirstRow + day - 1) % 7;    // 列，从0开始

            // 月份字符串
            int month = calendar.get(Calendar.MONTH);
            month += 1;   // #Calendar# 类里面0表示1月
            if (month < 10) {
                mMonthStr = "0" + month;
            } else {
                mMonthStr = String.valueOf(month);
            }

            // 日期字符串
            if (day < 10) {
                mDateStr = "0" + day;
            } else {
                mDateStr = String.valueOf(day);
            }

            Calendar copy = (Calendar) calendar.clone();
            copy.set(Calendar.DAY_OF_MONTH, day);
            Lunar lunar = new Lunar(copy.getTime());

            mDateStrBuilder = new StringBuilder();
            mCompleteDateStr = mDateStrBuilder.append(calendar.get(Calendar.YEAR)).append("-").append(mMonthStr).append("-").append(mDateStr).append("-").append(lunar.getLunarDayString()).toString();

            mMonthDateArray[row][column] = mCompleteDateStr;
            mDateStrBuilder = null;
        }

        mMonthMap.put(monthOffset, mMonthDateArray);
//        mReBuildFlag = false;
    }

    private void invalidateTextPaintAndMeasurements() {
        DisplayMetrics displayMetrics = context.getResources().getDisplayMetrics();
        density = displayMetrics.density;
        mTextPaint.setTextSize(mDateNumTextSize * density);
        mTextPaint.setColor(0xff505050);

        Paint.FontMetrics fontMetrics = mTextPaint.getFontMetrics();
        mTextHeight = fontMetrics.bottom;

        Log.d(TAG, fontMetrics.top + "," + fontMetrics.ascent + "," + fontMetrics.descent + "," + fontMetrics.bottom + "");
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int measureWidth = MeasureSpec.getSize(widthMeasureSpec);
        String[][] mCurrMonthArray = mMonthMap.get(mMonthOffset);
        float height;
        if (null == mCurrMonthArray[4][0]) {
            height = measureWidth * 4 / 7;
        } else if (null == mCurrMonthArray[5][0]) {
            height = measureWidth * 5 / 7;
        } else {
            height = measureWidth * 6 / 7;
        }
        setMeasuredDimension(measureWidth, (int) height);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        mCellWidth = w / 7F;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        drawSelectedCircle(canvas);

        drawLastMonth(canvas);
        drawCurrMonth(canvas);
        drawNextMonth(canvas);

    }

    private void drawSelectedCircle(Canvas canvas) {
        if (mLastSelection == null) {
            return;
        }
        float radius = mLastSelection.getRadius();
        canvas.save();
        canvas.translate(mLastSelection.getColumn() * mCellWidth + (mCellWidth - radius) / 2 + mLastSelection.getSelectedOffSet() * getWidth(), mLastSelection.getRow() * mCellWidth + (mCellWidth - radius) / 2);
        Log.d(TAG, "radius:" + radius + "; coordinate:" + mLastSelection.getRow() + "," + mLastSelection.getColumn() + "; offset:" + mLastSelection.getSelectedOffSet());

        if (mBackgroundShape == null) {
            OvalShape mCircleBackground = new OvalShape();
            mCircleBackground.resize(radius, radius);
            mBackgroundShape = new ShapeDrawable(mCircleBackground);
            mBackgroundShape.getPaint().setColor(0xFF3F79CA);
        }
        mBackgroundShape.getShape().resize(radius, radius);
        mBackgroundShape.draw(canvas);
        canvas.restore();
    }

    private void drawMonth(Canvas canvas, int monthOffset) {
        canvas.save();
        canvas.translate(getWidth() * monthOffset, 0);

        float dayWidth = getWidth() / 7f;

        Paint mPaint = new Paint(0);
        mPaint.setColor(0x00ff0000);
//
//        Calendar mCalendar = (Calendar) today.clone();
//        mCalendar.add(Calendar.MONTH, monthOffset);
//        mCalendar.set(Calendar.DAY_OF_MONTH, 1);

//        int weekOfDayOne = mCalendar.get(Calendar.DAY_OF_WEEK);   // 1号是星期几
//        int daysOnFirstRow;
//
//        if (FIRST_DAY_OF_WEEK == Calendar.SUNDAY) {
//            daysOnFirstRow = weekOfDayOne - FIRST_DAY_OF_WEEK;
//        } else {
//            if (weekOfDayOne == Calendar.SUNDAY) {
//                daysOnFirstRow = 6;
//            } else {
//                daysOnFirstRow = weekOfDayOne - FIRST_DAY_OF_WEEK;
//            }
//        }
//
//        int maxDayOfMonth = mCalendar.getActualMaximum(Calendar.DAY_OF_MONTH);

        Paint.FontMetricsInt fontMetrics = mTextPaint.getFontMetricsInt();

//        for (int day = 1; day <= maxDayOfMonth; day++) {
//
//            int row = (daysOnFirstRow + day - 1) / 7;   // 行，从0开始
//            int column = (daysOnFirstRow + day) % 7;    // 列，从1开始
//            if (column == 0) {
//                column = 7;
//            }
//            canvas.drawRect((column - 1) * dayWidth, row * dayWidth, column * dayWidth, (row + 1) * dayWidth, mPaint);
//            float baseline = row * dayWidth + (dayWidth - fontMetrics.bottom + fontMetrics.top) / 2 - fontMetrics.top;
//            canvas.drawText(String.valueOf(day), (column - 1) * dayWidth + dayWidth / 2, baseline, mTextPaint);
//        }

        String[][] mCurrMonthDateArray = mMonthMap.get(monthOffset);

        for (int row = 0; row < mCurrMonthDateArray.length; row++) {
            for (int column = 0; column < mCurrMonthDateArray[row].length; column++) {
                if (null == mCurrMonthDateArray[row][column]) {
                    continue;
                }
                String mDateStr = mCurrMonthDateArray[row][column].split("-")[2];
                if (mDateStr.startsWith("0")) {
                    mDateStr = mDateStr.substring(1);
                }
                canvas.drawRect(column * dayWidth, row * dayWidth, (column + 1) * dayWidth, (row + 1) * dayWidth, mPaint);
                float baseline = row * dayWidth + (dayWidth - fontMetrics.bottom + fontMetrics.top) / 2 - fontMetrics.top;
                if (null != mLastSelection && mLastSelection.getRow() == row && mLastSelection.getColumn() == column && mMonthOffset == (int) mLastSelection.getSelectedOffSet()) {
                    mTextPaint.setColor(Color.WHITE);
                }
                canvas.drawText(mDateStr, column * dayWidth + dayWidth / 2, baseline - 20, mTextPaint);
                mTextPaint.setTextSize(mLunarTextSize * density);
                canvas.drawText(mCurrMonthDateArray[row][column].split("-")[3], column * dayWidth + dayWidth / 2, baseline + mTextHeight, mTextPaint);
                mTextPaint.setTextSize(mDateNumTextSize * density);
                mTextPaint.setColor(0xff505050);
            }
        }

        canvas.restore();
    }

    private void drawNextMonth(Canvas canvas) {
        drawMonth(canvas, mMonthOffset + 1);
    }

    private void drawCurrMonth(Canvas canvas) {
        drawMonth(canvas, mMonthOffset);
    }

    private void drawLastMonth(Canvas canvas) {
        drawMonth(canvas, mMonthOffset - 1);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                lastPointX = (int) event.getX();
                break;
            case MotionEvent.ACTION_MOVE:
                int totalMoveX = (int) (lastPointX - event.getX()) + lastMoveX;
                smoothScrollTo(totalMoveX, 0);
                break;
            case MotionEvent.ACTION_UP:
                if (Math.abs(lastPointX - event.getX()) > mTouchSlop) {
                    if (lastPointX > event.getX()) {
                        if (Math.abs(lastPointX - event.getX()) >= getWidth() / 5) {
                            mMonthOffset++;
                            calculateDaysOnNearestThreeMonth();
                        }
                        smoothScrollTo(getWidth() * mMonthOffset, 0);
                        lastMoveX = getWidth() * mMonthOffset;
                    } else if (lastPointX < event.getX()) {
                        if (Math.abs(lastPointX - event.getX()) >= getWidth() / 5) {
                            mMonthOffset--;
                            calculateDaysOnNearestThreeMonth();
                        }
                        smoothScrollTo(getWidth() * mMonthOffset, 0);
                        lastMoveX = getWidth() * mMonthOffset;
                    }
                } else {
                    showSelectedDate(event.getX(), event.getY());
                }
                break;
        }
        return true;
    }

    private void showSelectedDate(float x, float y) {
        float rectWidth = getWidth() / 7F;
        int row = (int) (y / rectWidth);
        int column = (int) (x / rectWidth);

        String[][] mCurrMonthArray = mMonthMap.get(mMonthOffset);
        String mCurrDateStr = mCurrMonthArray[row][column];
        if (null != mCurrDateStr) {
            try {

                mCurrDateStr = mCurrDateStr.substring(0, mCurrDateStr.lastIndexOf("-"));
                Date mSelectedDate = mDateFormat.parse(mCurrDateStr);

                if (mLastSelection == null) {
                    mLastSelection = new SelectedDateInfo();
                    mLastSelection.setColumn(column);
                    mLastSelection.setRow(row);
                    mLastSelection.setSelectedOffSet(mMonthOffset);

                    showDateSelectedAnimation();
                } else {
                    showSelectionChangedAnimation(row, column, mMonthOffset);
                }

                if (mSelectedDateChangedListener != null) {
                    mSelectedDateChangedListener.onChanged(mSelectedDate);
                }
            } catch (ParseException e) {
                e.printStackTrace();
            }
        }
    }

    private void showSelectionChangedAnimation(int newRow, int newColumn, int newMonthOffset) {
        ValueAnimator mRowAnimator = ObjectAnimator.ofFloat(mLastSelection, "row", newRow);
        mRowAnimator.addUpdateListener(this);
        ValueAnimator mColumnAnimator = ObjectAnimator.ofFloat(mLastSelection, "column", newColumn);
        mColumnAnimator.addUpdateListener(this);
        ValueAnimator mOffsetAnimator = ObjectAnimator.ofFloat(mLastSelection, "selectedOffSet", newMonthOffset);
        mOffsetAnimator.addUpdateListener(this);

        AnimatorSet mSelectionChangedAnimationSet = new AnimatorSet();
        mSelectionChangedAnimationSet.playTogether(mRowAnimator, mColumnAnimator, mOffsetAnimator);
        mSelectionChangedAnimationSet.start();
    }

    private void showDateSelectedAnimation() {
        ValueAnimator mStartAnimator = ObjectAnimator.ofFloat(mLastSelection, "radius", mCellWidth - 20);
        mStartAnimator.setInterpolator(new OvershootInterpolator());
        mStartAnimator.addUpdateListener(this);
        mStartAnimator.start();
    }

    private void smoothScrollTo(int fx, int fy) {
        int dx = fx - mScroller.getFinalX();
        int dy = fy - mScroller.getFinalY();
        smoothScrollBy(dx, dy);
    }

    private void smoothScrollBy(int dx, int dy) {
        mScroller.startScroll(mScroller.getFinalX(), mScroller.getFinalY(), dx, dy, 300);
        invalidate();
    }

    @Override
    public void computeScroll() {
        if (mScroller.computeScrollOffset()) {
            scrollTo(mScroller.getCurrX(), mScroller.getCurrY());
            postInvalidate();
        } else {
            requestLayout();
        }
    }

    public void setOnSelectedDateChangedListener(OnSelectedDateChangedListener mSelectedDateChangedListener) {
        this.mSelectedDateChangedListener = mSelectedDateChangedListener;
    }

    /**
     * 是否把周首日设置为周日
     *
     * @param flag true则周日为每周首日，false则周一为每周首日
     */
    public void setSundayFirstDayOfWeek(boolean flag) {
        if (flag) {
            FIRST_DAY_OF_WEEK = Calendar.SUNDAY;
        } else {
            FIRST_DAY_OF_WEEK = Calendar.MONDAY;
        }
        mLastSelection = null;
        calculateDaysOnNearestThreeMonth();
        invalidate();
    }

    /**
     * 回到今月的月视图
     */
    public void backToToday() {
        mMonthOffset = 0;
        mLastSelection = null;
        calculateDaysOnNearestThreeMonth();
        scrollTo(0, 0);
        invalidate();
    }

    @Override
    protected Parcelable onSaveInstanceState() {
        Parcelable state = super.onSaveInstanceState();
        Bundle mSaveState = new Bundle();
        mSaveState.putParcelable("saveInstanceState", state);
        mSaveState.putParcelable("selectedDateInfo", mLastSelection);
        mSaveState.putInt("firstDayOfWeek", FIRST_DAY_OF_WEEK);

        return mSaveState;
    }

    @Override
    protected void onRestoreInstanceState(Parcelable state) {
        Bundle mSaveState = (Bundle) state;
        super.onRestoreInstanceState(mSaveState.getParcelable("saveInstanceState"));

        FIRST_DAY_OF_WEEK = mSaveState.getInt("firstDayOfWeek");
        mLastSelection = mSaveState.getParcelable("selectedDateInfo");
        invalidate();
    }

}
