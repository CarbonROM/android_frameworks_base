package com.google.android.systemui.ambientmusic;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.TimeInterpolator;
import android.animation.ValueAnimator;
import android.app.PendingIntent;
import android.content.Context;
import android.content.res.ColorStateList;
import android.os.SystemClock;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.Interpolator;
import android.widget.ImageView;
import android.widget.TextView;

import com.android.systemui.AutoReinflateContainer;
import com.android.systemui.Interpolators;
import com.android.systemui.doze.DozeReceiver;
import com.android.systemui.R;
import com.android.systemui.statusbar.phone.NotificationPanelView;
import com.android.systemui.statusbar.phone.StatusBar;
import com.android.systemui.statusbar.stack.NotificationStackScrollLayout;

import com.google.android.systemui.ambientmusic.AmbientIndicationAnimatorUpdateListener;
import com.google.android.systemui.ambientmusic.AmbientIndicationActivationListener;

public class AmbientIndicationContainer extends AutoReinflateContainer implements DozeReceiver,
  View.OnLayoutChangeListener, AutoReinflateContainer.InflateListener {

    private View mAmbientIndication;
    private boolean mDozing;
    private ImageView mIcon;
    private CharSequence mIndication;
    private PendingIntent mIntent;
    private StatusBar mStatusBar;
    private TextView mText;
    private int mTextColor;
    private ValueAnimator mTextColorAnimator;
    private Context mContext;

    public AmbientIndicationContainer(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
        mContext = context;
    }

    private void updateBottomPadding() {
        NotificationPanelView notificationPanelView = this.mStatusBar.getPanel();
        if (this.mAmbientIndication.getVisibility() == View.VISIBLE) {
            int padding = this.mStatusBar.getNotificationScrollLayout().getBottom() - this.getTop();
            notificationPanelView.setAmbientIndicationBottomPadding(padding);
        }
    }

    private void updateColors() {
        if (this.mTextColorAnimator != null && this.mTextColorAnimator.isRunning()) {
            this.mTextColorAnimator.cancel();
        }
        int n = this.mText.getTextColors().getDefaultColor();
        int n2 = this.mDozing ? -1 : this.mTextColor;
        if (n == n2) {
            return;
        }
        this.mTextColorAnimator = ValueAnimator.ofArgb((int[])new int[]{n, n2});
        this.mTextColorAnimator.setInterpolator((TimeInterpolator)Interpolators.LINEAR_OUT_SLOW_IN);
        this.mTextColorAnimator.setDuration(200L);
        this.mTextColorAnimator.addUpdateListener((ValueAnimator.AnimatorUpdateListener)new AmbientIndicationAnimatorUpdateListener(this));
        this.mTextColorAnimator.addListener((Animator.AnimatorListener)new AnimatorListenerAdapter(){

            public void onAnimationEnd(Animator animator2) {
                AmbientIndicationContainer.this.mTextColorAnimator = null;
            }
        });
        this.mTextColorAnimator.start();
    }

    public void hideIndication() {
        this.setIndication(null, null);
    }

    public void initializeView(StatusBar statusBar) {
        this.mStatusBar = statusBar;
        this.addInflateListener(new AmbientIndicationInflateListener(this));
        this.addOnLayoutChangeListener((View.OnLayoutChangeListener)new AmbientIndicationLayoutChangeListener(this));
    }

    public void updateAmbientIndicationView(View view) {
        this.mAmbientIndication = this.findViewById(R.id.ambient_indication_container);
        this.mText = (TextView)this.findViewById(R.id.ambient_indication_text);
        this.mIcon = (ImageView)this.findViewById(R.id.ambient_indication_icon);
        this.mTextColor = this.mText.getCurrentTextColor();
        this.updateColors();
        this.setIndication(this.mIndication, this.mIntent);
    }

    public void setActive(boolean bl) {
        if (bl) {
            this.mStatusBar.onActivated((View)this);
            return;
        }
        this.mStatusBar.onActivationReset((View)this);
    }

    public void updateAmbientIndicationBottomPadding() {
        this.updateBottomPadding();
    }

    public void updateAnimator(ValueAnimator valueAnimator) {
        int n = (Integer)valueAnimator.getAnimatedValue();
        this.mText.setTextColor(n);
        this.mIcon.setColorFilter(n);
    }

    @Override
    public void setDozing(boolean bl) {
        this.mDozing = bl;
        this.updateColors();
    }

    @Override
    public void onLayoutChange(View v, int left, int top, int right, int bottom, int oldLeft, int oldTop, int oldRight, int oldBottom) {
        this.updateBottomPadding();
    }

    @Override
    public void onInflated(View view) {
        this.updateAmbientIndicationView(view);
    }

    public void setIndication(CharSequence charSequence, PendingIntent pendingIntent) {
        this.mText.setText(charSequence);
        this.mIndication = charSequence;
        this.mIntent = pendingIntent;
        View view = this.mAmbientIndication;
        boolean bl = pendingIntent != null;
        view.setClickable(bl);
        bl = TextUtils.isEmpty((CharSequence)charSequence);
        if (bl)
            view.setVisibility(View.VISIBLE);
        else
            view.setVisibility(View.INVISIBLE);

        this.updateBottomPadding();
    }

}

