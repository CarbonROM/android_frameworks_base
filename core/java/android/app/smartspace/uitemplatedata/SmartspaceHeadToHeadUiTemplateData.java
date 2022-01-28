/*
 * Copyright (C) 2022 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package android.app.smartspace.uitemplatedata;

import android.annotation.NonNull;
import android.annotation.Nullable;
import android.annotation.SystemApi;
import android.app.smartspace.SmartspaceTarget;
import android.app.smartspace.SmartspaceUtils;
import android.os.Parcel;
import android.text.TextUtils;

import java.util.Objects;

/**
 * Holds all the relevant data needed to render a Smartspace card with the head-to-head Ui Template.
 *
 * @hide
 */
@SystemApi
public final class SmartspaceHeadToHeadUiTemplateData extends SmartspaceDefaultUiTemplateData {

    @Nullable
    private final CharSequence mHeadToHeadTitle;
    @Nullable
    private final SmartspaceIcon mHeadToHeadFirstCompetitorIcon;
    @Nullable
    private final SmartspaceIcon mHeadToHeadSecondCompetitorIcon;
    @Nullable
    private final CharSequence mHeadToHeadFirstCompetitorText;
    @Nullable
    private final CharSequence mHeadToHeadSecondCompetitorText;

    /** Tap action for the head-to-head secondary card. */
    @Nullable
    private final SmartspaceTapAction mHeadToHeadAction;

    SmartspaceHeadToHeadUiTemplateData(@NonNull Parcel in) {
        super(in);
        mHeadToHeadTitle = TextUtils.CHAR_SEQUENCE_CREATOR.createFromParcel(in);
        mHeadToHeadFirstCompetitorIcon = in.readTypedObject(SmartspaceIcon.CREATOR);
        mHeadToHeadSecondCompetitorIcon = in.readTypedObject(SmartspaceIcon.CREATOR);
        mHeadToHeadFirstCompetitorText = TextUtils.CHAR_SEQUENCE_CREATOR.createFromParcel(in);
        mHeadToHeadSecondCompetitorText = TextUtils.CHAR_SEQUENCE_CREATOR.createFromParcel(in);
        mHeadToHeadAction = in.readTypedObject(SmartspaceTapAction.CREATOR);
    }

    private SmartspaceHeadToHeadUiTemplateData(@SmartspaceTarget.UiTemplateType int templateType,
            @Nullable CharSequence titleText,
            @Nullable SmartspaceIcon titleIcon,
            @Nullable CharSequence subtitleText,
            @Nullable SmartspaceIcon subTitleIcon,
            @Nullable SmartspaceTapAction primaryTapAction,
            @Nullable CharSequence supplementalSubtitleText,
            @Nullable SmartspaceIcon supplementalSubtitleIcon,
            @Nullable SmartspaceTapAction supplementalSubtitleTapAction,
            @Nullable CharSequence supplementalAlarmText,
            @Nullable CharSequence headToHeadTitle,
            @Nullable SmartspaceIcon headToHeadFirstCompetitorIcon,
            @Nullable SmartspaceIcon headToHeadSecondCompetitorIcon,
            @Nullable CharSequence headToHeadFirstCompetitorText,
            @Nullable CharSequence headToHeadSecondCompetitorText,
            @Nullable SmartspaceTapAction headToHeadAction) {
        super(templateType, titleText, titleIcon, subtitleText, subTitleIcon, primaryTapAction,
                supplementalSubtitleText, supplementalSubtitleIcon, supplementalSubtitleTapAction,
                supplementalAlarmText);
        mHeadToHeadTitle = headToHeadTitle;
        mHeadToHeadFirstCompetitorIcon = headToHeadFirstCompetitorIcon;
        mHeadToHeadSecondCompetitorIcon = headToHeadSecondCompetitorIcon;
        mHeadToHeadFirstCompetitorText = headToHeadFirstCompetitorText;
        mHeadToHeadSecondCompetitorText = headToHeadSecondCompetitorText;
        mHeadToHeadAction = headToHeadAction;
    }

    @Nullable
    public CharSequence getHeadToHeadTitle() {
        return mHeadToHeadTitle;
    }

    @Nullable
    public SmartspaceIcon getHeadToHeadFirstCompetitorIcon() {
        return mHeadToHeadFirstCompetitorIcon;
    }

    @Nullable
    public SmartspaceIcon getHeadToHeadSecondCompetitorIcon() {
        return mHeadToHeadSecondCompetitorIcon;
    }

    @Nullable
    public CharSequence getHeadToHeadFirstCompetitorText() {
        return mHeadToHeadFirstCompetitorText;
    }

    @Nullable
    public CharSequence getHeadToHeadSecondCompetitorText() {
        return mHeadToHeadSecondCompetitorText;
    }

    @Nullable
    public SmartspaceTapAction getHeadToHeadAction() {
        return mHeadToHeadAction;
    }

    /**
     * @see Parcelable.Creator
     */
    @NonNull
    public static final Creator<SmartspaceHeadToHeadUiTemplateData> CREATOR =
            new Creator<SmartspaceHeadToHeadUiTemplateData>() {
                @Override
                public SmartspaceHeadToHeadUiTemplateData createFromParcel(Parcel in) {
                    return new SmartspaceHeadToHeadUiTemplateData(in);
                }

                @Override
                public SmartspaceHeadToHeadUiTemplateData[] newArray(int size) {
                    return new SmartspaceHeadToHeadUiTemplateData[size];
                }
            };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(@NonNull Parcel out, int flags) {
        super.writeToParcel(out, flags);
        TextUtils.writeToParcel(mHeadToHeadTitle, out, flags);
        out.writeTypedObject(mHeadToHeadFirstCompetitorIcon, flags);
        out.writeTypedObject(mHeadToHeadSecondCompetitorIcon, flags);
        TextUtils.writeToParcel(mHeadToHeadFirstCompetitorText, out, flags);
        TextUtils.writeToParcel(mHeadToHeadSecondCompetitorText, out, flags);
        out.writeTypedObject(mHeadToHeadAction, flags);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof SmartspaceHeadToHeadUiTemplateData)) return false;
        if (!super.equals(o)) return false;
        SmartspaceHeadToHeadUiTemplateData that = (SmartspaceHeadToHeadUiTemplateData) o;
        return SmartspaceUtils.isEqual(mHeadToHeadTitle, that.mHeadToHeadTitle) && Objects.equals(
                mHeadToHeadFirstCompetitorIcon, that.mHeadToHeadFirstCompetitorIcon)
                && Objects.equals(
                mHeadToHeadSecondCompetitorIcon, that.mHeadToHeadSecondCompetitorIcon)
                && SmartspaceUtils.isEqual(mHeadToHeadFirstCompetitorText,
                that.mHeadToHeadFirstCompetitorText)
                && SmartspaceUtils.isEqual(mHeadToHeadSecondCompetitorText,
                that.mHeadToHeadSecondCompetitorText)
                && Objects.equals(
                mHeadToHeadAction, that.mHeadToHeadAction);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), mHeadToHeadTitle, mHeadToHeadFirstCompetitorIcon,
                mHeadToHeadSecondCompetitorIcon, mHeadToHeadFirstCompetitorText,
                mHeadToHeadSecondCompetitorText,
                mHeadToHeadAction);
    }

    @Override
    public String toString() {
        return super.toString() + " + SmartspaceHeadToHeadUiTemplateData{"
                + "mH2HTitle=" + mHeadToHeadTitle
                + ", mH2HFirstCompetitorIcon=" + mHeadToHeadFirstCompetitorIcon
                + ", mH2HSecondCompetitorIcon=" + mHeadToHeadSecondCompetitorIcon
                + ", mH2HFirstCompetitorText=" + mHeadToHeadFirstCompetitorText
                + ", mH2HSecondCompetitorText=" + mHeadToHeadSecondCompetitorText
                + ", mH2HAction=" + mHeadToHeadAction
                + '}';
    }

    /**
     * A builder for {@link SmartspaceHeadToHeadUiTemplateData} object.
     *
     * @hide
     */
    @SystemApi
    public static final class Builder extends SmartspaceDefaultUiTemplateData.Builder {

        private CharSequence mHeadToHeadTitle;
        private SmartspaceIcon mHeadToHeadFirstCompetitorIcon;
        private SmartspaceIcon mHeadToHeadSecondCompetitorIcon;
        private CharSequence mHeadToHeadFirstCompetitorText;
        private CharSequence mHeadToHeadSecondCompetitorText;
        private SmartspaceTapAction mHeadToHeadAction;

        /**
         * A builder for {@link SmartspaceHeadToHeadUiTemplateData}.
         */
        public Builder() {
            super(SmartspaceTarget.UI_TEMPLATE_HEAD_TO_HEAD);
        }

        /**
         * Sets the head-to-head card's title
         */
        @NonNull
        public Builder setHeadToHeadTitle(@Nullable CharSequence headToHeadTitle) {
            mHeadToHeadTitle = headToHeadTitle;
            return this;
        }

        /**
         * Sets the head-to-head card's first competitor icon
         */
        @NonNull
        public Builder setHeadToHeadFirstCompetitorIcon(
                @Nullable SmartspaceIcon headToHeadFirstCompetitorIcon) {
            mHeadToHeadFirstCompetitorIcon = headToHeadFirstCompetitorIcon;
            return this;
        }

        /**
         * Sets the head-to-head card's second competitor icon
         */
        @NonNull
        public Builder setHeadToHeadSecondCompetitorIcon(
                @Nullable SmartspaceIcon headToHeadSecondCompetitorIcon) {
            mHeadToHeadSecondCompetitorIcon = headToHeadSecondCompetitorIcon;
            return this;
        }

        /**
         * Sets the head-to-head card's first competitor text
         */
        @NonNull
        public Builder setHeadToHeadFirstCompetitorText(
                @Nullable CharSequence headToHeadFirstCompetitorText) {
            mHeadToHeadFirstCompetitorText = headToHeadFirstCompetitorText;
            return this;
        }

        /**
         * Sets the head-to-head card's second competitor text
         */
        @NonNull
        public Builder setHeadToHeadSecondCompetitorText(
                @Nullable CharSequence headToHeadSecondCompetitorText) {
            mHeadToHeadSecondCompetitorText = headToHeadSecondCompetitorText;
            return this;
        }

        /**
         * Sets the head-to-head card's tap action
         */
        @NonNull
        public Builder setHeadToHeadAction(@Nullable SmartspaceTapAction headToHeadAction) {
            mHeadToHeadAction = headToHeadAction;
            return this;
        }

        /**
         * Builds a new SmartspaceHeadToHeadUiTemplateData instance.
         */
        @NonNull
        public SmartspaceHeadToHeadUiTemplateData build() {
            return new SmartspaceHeadToHeadUiTemplateData(getTemplateType(), getTitleText(),
                    getTitleIcon(), getSubtitleText(), getSubTitleIcon(), getPrimaryTapAction(),
                    getSupplementalSubtitleText(), getSupplementalSubtitleIcon(),
                    getSupplementalSubtitleTapAction(), getSupplementalAlarmText(),
                    mHeadToHeadTitle,
                    mHeadToHeadFirstCompetitorIcon,
                    mHeadToHeadSecondCompetitorIcon, mHeadToHeadFirstCompetitorText,
                    mHeadToHeadSecondCompetitorText,
                    mHeadToHeadAction);
        }
    }
}