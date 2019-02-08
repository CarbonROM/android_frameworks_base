/* Copyright (C) 2016 Tcl Corporation Limited */
/*
 *
 *[FEATURE]-ADD-BEGIN by TCTNB.cheng.liu
 *
 */


package com.tct.internal;


import android.animation.Animator;
import android.animation.ObjectAnimator;
import android.animation.Animator.AnimatorListener;
import android.app.ActivityManager;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Rect;
import android.os.Handler;
import android.os.Message;
import android.os.SystemClock;
import android.util.FloatProperty;
import android.util.Property;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.DecelerateInterpolator;
import android.webkit.WebView;
import android.widget.AbsListView;
import android.widget.AbsoluteLayout;
import android.widget.ScrollView;

public class TCTLongScreenshotUtils
{
    public View mMainScrollView=null;
    public Rect mScreenRect=new Rect();

    public int mMainScrollViewTop=0;//top visable edge
    public float mTouchY=0.0f;
    public float mTouchX=0.0f;
    public long mDownTime = 0;

    public boolean bMoving=false;

    public MotionEvent.PointerCoords[] mTmpPointerCoords = new MotionEvent.PointerCoords[2];
    public MotionEvent.PointerProperties[] mTmpPointerProperties = new MotionEvent.PointerProperties[2];

    public TCTLongScreenshotUtils()
    {
        bMoving=false;
        int i = 0;
        while (i < 2)
        {
            mTmpPointerProperties[i] = new MotionEvent.PointerProperties();
            mTmpPointerProperties[i].id=i;
            mTmpPointerCoords[i] = new MotionEvent.PointerCoords();
            mTmpPointerCoords[i].pressure = 1.0F;
            mTmpPointerCoords[i].size = 1.0F;
            i += 1;
        }
    }

    private void TCTInit()
    {
        int[] mTempLoc = new int[2];
        mMainScrollView.getLocationOnScreen(mTempLoc);
        mMainScrollViewTop=mTempLoc[1]<0?0:mTempLoc[1];

        mTouchY=mMainScrollViewTop+1;
        mTouchX=(mMainScrollView.getWidth() / 2);
        mDownTime = SystemClock.uptimeMillis();
        //mMainScrollView.setVerticalScrollBarEnabled(false);
        //this.mHandler.sendEmptyMessage(2);
        TCTDispatchFakeTouchEvent(MotionEvent.ACTION_DOWN);
    }

    public void TCTfindScrollView(View rootview)
    {
        if ((rootview == null) || (rootview.getVisibility() != View.VISIBLE)||mMainScrollView!=null)
        {
            return ;
        }

        int[] pos=new int[2];
        rootview.getLocationOnScreen(pos);
        Rect rectScrollView=new Rect();
        rectScrollView.left=pos[0];
        rectScrollView.top=pos[1];
        rectScrollView.right=pos[0]+rootview.getWidth();
        rectScrollView.bottom=pos[1]+rootview.getHeight();
        if(rectScrollView.right<=mScreenRect.left||rectScrollView.left>=mScreenRect.right
                ||rectScrollView.top>=mScreenRect.bottom||rectScrollView.bottom<=mScreenRect.top)
        {
            return;
        }
        if (rootview.canScrollVertically(1))
        {
            android.util.Log.i("sshot", "Width:"+rootview.getWidth()+"/Height:"+rootview.getHeight());
            mMainScrollView=rootview;
            return ;
        }
        if ((rootview instanceof ViewGroup))
        {
            ViewGroup localViewGroup = (ViewGroup)rootview;
            int nSize = localViewGroup.getChildCount();
            for(int i=0;i<nSize;i++)
            {
                TCTfindScrollView(localViewGroup.getChildAt(i));
            }
        }
    }


    public int TCTGetScrollViewVisibleHeight()
    {
        int nHeight = mMainScrollView.getHeight();
        if (mMainScrollViewTop + nHeight <= mScreenRect.height())
            return nHeight;
        return mScreenRect.height() - mMainScrollViewTop;
    }

    public void TCTScrollView(boolean bForward,int nDistance)
    {
        TCTInit();
        if (mMainScrollView.canScrollVertically(1))
        {
            if(bForward)
            {
                mTouchY -= nDistance;
            }
            else
            {
                mTouchY += nDistance;
            }

            TCTDispatchFakeTouchEvent(MotionEvent.ACTION_MOVE);
            try {
                Thread.sleep(50);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            avoidFly();
            TCTDispatchFakeTouchEvent(MotionEvent.ACTION_UP);
            //TCTScreenScroll(bForward, nDistance);
            //avoidFly();
            //TCTDispatchFakeTouchEvent(MotionEvent.ACTION_UP);
        }
    }

    public void TCTDispatchFakeTouchEvent(int nType) {
        android.util.Log.i("sshot", "==>TCTDispatchFakeTouchEvent mTouchY/mTouchX/nType = " + mTouchY + "/" +mTouchX + "/" + nType);

        mTmpPointerCoords[0].x = mTouchX;
        mTmpPointerCoords[0].y = mTouchY;
        MotionEvent localMotionEvent = MotionEvent.obtain(mDownTime, SystemClock.uptimeMillis(), nType, 1, mTmpPointerProperties, mTmpPointerCoords, 0, 0, 1.0F, 1.0F, 0, 0, 0, 0);
        android.util.Log.i("sshot", "==>dispatchFakeTouchEvent");
        mMainScrollView.dispatchTouchEvent(localMotionEvent);

        android.util.Log.i("sshot", "==>localMotionEvent.recycle()");
        localMotionEvent.recycle();
    }

    private static int getTopActivityTaskID(Context mContext) {
        final android.app.ActivityManager am = (android.app.ActivityManager) mContext.getSystemService(Context.ACTIVITY_SERVICE);
        android.content.pm.ActivityInfo aInfo = null;
        java.util.List<android.app.ActivityManager.RunningTaskInfo> list = am.getRunningTasks(1);
        if (list.size() != 0) {
            android.app.ActivityManager.RunningTaskInfo topRunningTask = list.get(0);
            android.util.Log.i("sshot","==>GLSS getTopActivityTaskID:"+topRunningTask.topActivity.getPackageName());
            return topRunningTask.id;
        } else {
            return -1;
        }
    }
    //=========Open API for sshot============begin
    public static Bitmap TCTCaptureTOPActivity(Context mContext)
    {
        int nTaskID=getTopActivityTaskID(mContext);
        android.util.Log.i("sshot","==>TCTCaptureTOPActivity, nTaskID:"+nTaskID);

        //TCTScrollForSShot(mContext,true,100);
        ActivityManager mAm = (ActivityManager) mContext.getSystemService(Context.ACTIVITY_SERVICE);
        Bitmap bmp = mAm.TCTgetTaskThumbnail(nTaskID);
        return bmp;
    }

    /**
     *
     * @param mContext
     * @param bForward
     * @param nDistance
     * @return
     */
    public static  int TCTScrollForSShot(Context mContext,boolean bForward,int nDistance)
    {
        ActivityManager mAm = (ActivityManager) mContext.getSystemService(Context.ACTIVITY_SERVICE);
        int nValue=mAm.TCTScrollForSShot(bForward,nDistance);
        android.util.Log.i("sshot","==>TCTLongScreenshotUtils TCTScrollForSShot: return = "+nValue);
        return nValue;
    }
    //=========for sshot============end




    ////////////////////////////////////////////////////////
    public final Property<TCTLongScreenshotUtils, Float> MOVE_Y = new FloatProperty<TCTLongScreenshotUtils>("moveY") {
        @Override
        public void setValue(TCTLongScreenshotUtils object, float value) {
            object.onMoveY(value);
        }

        @Override
        public Float get(TCTLongScreenshotUtils object) {
            return 0.0f;
        }
    };

    private void onMoveY(float y) {
        mTouchY = y;
        TCTDispatchFakeTouchEvent(MotionEvent.ACTION_MOVE);
    }

    private void onMoveYEnd() {
        android.util.Log.i("sshot","onMoveYEnd()");
        //avoidFly();
        TCTDispatchFakeTouchEvent(MotionEvent.ACTION_UP);
    }

    private void avoidFly() {
        android.util.Log.i("sshot","avoidFly");
        for (int i = 0; i < 5; i++) {
            mTouchX -= 1f;
            TCTDispatchFakeTouchEvent(MotionEvent.ACTION_MOVE);
        }
        for (int i = 0; i < 5; i++) {
            mTouchX += 1f;
            TCTDispatchFakeTouchEvent(MotionEvent.ACTION_MOVE);
        }
    }

    private void TCTScreenScroll(boolean bForward,int nDistance){
        int nDuration = 100;
        ObjectAnimator anim = null;
        if (bForward) {
            anim = ObjectAnimator.ofFloat(this, MOVE_Y, mMainScrollViewTop+1, mMainScrollViewTop+1 - nDistance);
        }else {
            anim = ObjectAnimator.ofFloat(this, MOVE_Y, mMainScrollViewTop+1, mMainScrollViewTop+1 + nDistance);
        }
        anim.setDuration(nDuration);
        anim.setInterpolator(new DecelerateInterpolator());
        anim.addListener(new AnimatorListener() {
            @Override
            public void onAnimationStart(Animator animation) {

                //onMoveYStart();
            }

            @Override
            public void onAnimationEnd(Animator animation) {

                onMoveYEnd();
            }

            @Override
            public void onAnimationCancel(Animator animation) {

                onMoveYEnd();
            }

            @Override
            public void onAnimationRepeat(Animator animation) {

                
            }});
        anim.start();
        android.util.Log.i("sshot","anim.start()");
    }
}
