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
import android.os.Parcelable;
import android.text.TextUtils;

import java.util.List;
import java.util.Objects;

/**
 * Holds all the relevant data needed to render a Smartspace card with the carousel Ui Template.
 *
 * @hide
 */
@SystemApi
public final class SmartspaceCarouselUiTemplateData extends SmartspaceDefaultUiTemplateData {

    /** Lists of {@link CarouselItem}. */
    @NonNull
    private final List<CarouselItem> mCarouselItems;

    /** Tap action for the entire carousel secondary card, including the blank space */
    @Nullable
    private final SmartspaceTapAction mCarouselAction;

    SmartspaceCarouselUiTemplateData(@NonNull Parcel in) {
        super(in);
        mCarouselItems = in.createTypedArrayList(CarouselItem.CREATOR);
        mCarouselAction = in.readTypedObject(SmartspaceTapAction.CREATOR);
    }

    private SmartspaceCarouselUiTemplateData(@SmartspaceTarget.UiTemplateType int templateType,
            @Nullable CharSequence titleText,
            @Nullable SmartspaceIcon titleIcon,
            @Nullable CharSequence subtitleText,
            @Nullable SmartspaceIcon subTitleIcon,
            @Nullable SmartspaceTapAction primaryTapAction,
            @Nullable CharSequence supplementalSubtitleText,
            @Nullable SmartspaceIcon supplementalSubtitleIcon,
            @Nullable SmartspaceTapAction supplementalSubtitleTapAction,
            @Nullable CharSequence supplementalAlarmText,
            @NonNull List<CarouselItem> carouselItems,
            @Nullable SmartspaceTapAction carouselAction) {
        super(templateType, titleText, titleIcon, subtitleText, subTitleIcon, primaryTapAction,
                supplementalSubtitleText, supplementalSubtitleIcon, supplementalSubtitleTapAction,
                supplementalAlarmText);
        mCarouselItems = carouselItems;
        mCarouselAction = carouselAction;
    }

    @NonNull
    public List<CarouselItem> getCarouselItems() {
        return mCarouselItems;
    }

    @Nullable
    public SmartspaceTapAction getCarouselAction() {
        return mCarouselAction;
    }

    /**
     * @see Parcelable.Creator
     */
    @NonNull
    public static final Creator<SmartspaceCarouselUiTemplateData> CREATOR =
            new Creator<SmartspaceCarouselUiTemplateData>() {
                @Override
                public SmartspaceCarouselUiTemplateData createFromParcel(Parcel in) {
                    return new SmartspaceCarouselUiTemplateData(in);
                }

                @Override
                public SmartspaceCarouselUiTemplateData[] newArray(int size) {
                    return new SmartspaceCarouselUiTemplateData[size];
                }
            };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(@NonNull Parcel out, int flags) {
        super.writeToParcel(out, flags);
        out.writeTypedList(mCarouselItems);
        out.writeTypedObject(mCarouselAction, flags);
    }


    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof SmartspaceCarouselUiTemplateData)) return false;
        if (!super.equals(o)) return false;
        SmartspaceCarouselUiTemplateData that = (SmartspaceCarouselUiTemplateData) o;
        return mCarouselItems.equals(that.mCarouselItems) && Objects.equals(mCarouselAction,
                that.mCarouselAction);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), mCarouselItems, mCarouselAction);
    }

    @Override
    public String toString() {
        return super.toString() + " + SmartspaceCarouselUiTemplateData{"
                + "mCarouselItems=" + mCarouselItems
                + ", mCarouselActions=" + mCarouselAction
                + '}';
    }

    /**
     * A builder for {@link SmartspaceCarouselUiTemplateData} object.
     *
     * @hide
     */
    @SystemApi
    public static final class Builder extends SmartspaceDefaultUiTemplateData.Builder {

        private final List<CarouselItem> mCarouselItems;
        private SmartspaceTapAction mCarouselAction;

        /**
         * A builder for {@link SmartspaceCarouselUiTemplateData}.
         */
        public Builder(@NonNull List<CarouselItem> carouselItems) {
            super(SmartspaceTarget.UI_TEMPLATE_CAROUSEL);
            mCarouselItems = Objects.requireNonNull(carouselItems);
        }

        /**
         * Sets the card tap action.
         */
        @NonNull
        public Builder setCarouselAction(@NonNull SmartspaceTapAction carouselAction) {
            mCarouselAction = carouselAction;
            return this;
        }

        /**
         * Builds a new SmartspaceCarouselUiTemplateData instance.
         *
         * @throws IllegalStateException if the carousel data is invalid.
         */
        @NonNull
        public SmartspaceCarouselUiTemplateData build() {
            if (mCarouselItems.isEmpty()) {
                throw new IllegalStateException("Carousel data is empty");
            }
            return new SmartspaceCarouselUiTemplateData(getTemplateType(), getTitleText(),
                    getTitleIcon(), getSubtitleText(), getSubTitleIcon(), getPrimaryTapAction(),
                    getSupplementalSubtitleText(), getSupplementalSubtitleIcon(),
                    getSupplementalSubtitleTapAction(), getSupplementalAlarmText(),
                    mCarouselItems,
                    mCarouselAction);
        }
    }

    /** Holds all the relevant data needed to render a carousel item. */
    public static final class CarouselItem implements Parcelable {

        /** Text which is above the image item. */
        @Nullable
        private final CharSequence mUpperText;

        /** Image item. Can be empty. */
        @Nullable
        private final SmartspaceIcon mImage;

        /** Text which is under the image item. */
        @Nullable
        private final CharSequence mLowerText;

        /**
         * Tap action for this {@link CarouselItem} instance. {@code mCarouselAction} is used if not
         * being set.
         */
        @Nullable
        private final SmartspaceTapAction mTapAction;

        CarouselItem(@NonNull Parcel in) {
            mUpperText = TextUtils.CHAR_SEQUENCE_CREATOR.createFromParcel(in);
            mImage = in.readTypedObject(SmartspaceIcon.CREATOR);
            mLowerText = TextUtils.CHAR_SEQUENCE_CREATOR.createFromParcel(in);
            mTapAction = in.readTypedObject(SmartspaceTapAction.CREATOR);
        }

        private CarouselItem(@Nullable CharSequence upperText, @Nullable SmartspaceIcon image,
                @Nullable CharSequence lowerText, @Nullable SmartspaceTapAction tapAction) {
            mUpperText = upperText;
            mImage = image;
            mLowerText = lowerText;
            mTapAction = tapAction;
        }

        @Nullable
        public CharSequence getUpperText() {
            return mUpperText;
        }

        @Nullable
        public SmartspaceIcon getImage() {
            return mImage;
        }

        @Nullable
        public CharSequence getLowerText() {
            return mLowerText;
        }

        @Nullable
        public SmartspaceTapAction getTapAction() {
            return mTapAction;
        }

        /**
         * @see Parcelable.Creator
         */
        @NonNull
        public static final Creator<CarouselItem> CREATOR =
                new Creator<CarouselItem>() {
                    @Override
                    public CarouselItem createFromParcel(Parcel in) {
                        return new CarouselItem(in);
                    }

                    @Override
                    public CarouselItem[] newArray(int size) {
                        return new CarouselItem[size];
                    }
                };

        @Override
        public int describeContents() {
            return 0;
        }

        @Override
        public void writeToParcel(@NonNull Parcel out, int flags) {
            TextUtils.writeToParcel(mUpperText, out, flags);
            out.writeTypedObject(mImage, flags);
            TextUtils.writeToParcel(mLowerText, out, flags);
            out.writeTypedObject(mTapAction, flags);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof CarouselItem)) return false;
            CarouselItem that = (CarouselItem) o;
            return SmartspaceUtils.isEqual(mUpperText, that.mUpperText) && Objects.equals(
                    mImage,
                    that.mImage) && SmartspaceUtils.isEqual(mLowerText, that.mLowerText)
                    && Objects.equals(mTapAction, that.mTapAction);
        }

        @Override
        public int hashCode() {
            return Objects.hash(mUpperText, mImage, mLowerText, mTapAction);
        }

        @Override
        public String toString() {
            return "CarouselItem{"
                    + "mUpperText=" + mUpperText
                    + ", mImage=" + mImage
                    + ", mLowerText=" + mLowerText
                    + ", mTapAction=" + mTapAction
                    + '}';
        }

        /**
         * A builder for {@link CarouselItem} object.
         *
         * @hide
         */
        @SystemApi
        public static final class Builder {

            private CharSequence mUpperText;
            private SmartspaceIcon mImage;
            private CharSequence mLowerText;
            private SmartspaceTapAction mTapAction;

            /**
             * Sets the upper text.
             */
            @NonNull
            public Builder setUpperText(@Nullable CharSequence upperText) {
                mUpperText = upperText;
                return this;
            }

            /**
             * Sets the image.
             */
            @NonNull
            public Builder setImage(@Nullable SmartspaceIcon image) {
                mImage = image;
                return this;
            }


            /**
             * Sets the lower text.
             */
            @NonNull
            public Builder setLowerText(@Nullable CharSequence lowerText) {
                mLowerText = lowerText;
                return this;
            }

            /**
             * Sets the tap action.
             */
            @NonNull
            public Builder setTapAction(@Nullable SmartspaceTapAction tapAction) {
                mTapAction = tapAction;
                return this;
            }

            /**
             * Builds a new CarouselItem instance.
             *
             * @throws IllegalStateException if all the rendering data is empty.
             */
            @NonNull
            public CarouselItem build() {
                if (TextUtils.isEmpty(mUpperText) && mImage == null && TextUtils.isEmpty(
                        mLowerText)) {
                    throw new IllegalStateException("Carousel data is empty");
                }
                return new CarouselItem(mUpperText, mImage, mLowerText, mTapAction);
            }
        }
    }
}