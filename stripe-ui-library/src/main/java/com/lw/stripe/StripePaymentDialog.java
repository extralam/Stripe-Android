package com.lw.stripe;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.drawable.Animatable;
import android.graphics.drawable.Drawable;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkInfo;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.appcompat.widget.AppCompatImageView;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.drawable.DrawableCompat;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.FragmentManager;

import com.lw.stripe.utils.CircleImageView;
import com.stripe.android.SourceCallback;
import com.stripe.android.Stripe;
import com.stripe.android.TokenCallback;
import com.stripe.android.model.Card;
import com.stripe.android.model.Source;
import com.stripe.android.model.SourceParams;
import com.stripe.android.model.Token;
import com.stripe.android.view.CardNumberEditText;
import com.stripe.android.view.ExpiryDateEditText;

import static com.stripe.android.model.Card.CVC_LENGTH_AMERICAN_EXPRESS;
import static com.stripe.android.model.Card.CVC_LENGTH_COMMON;

/**
 * Stripe Payment Dialog
 * - Easy Start a StripePayment
 */
@SuppressLint("SetTextI18n")
public class StripePaymentDialog extends DialogFragment {

    private static final String TAG = "StripePaymentDialog";
    private static final String EXTRA_DEFAULT_PUBLISH_KEY = "EXTRA_DEFAULT_PUBLISH_KEY";
    private static final String EXTRA_CAPTION = "EXTRA_CAPTION";
    private static final String EXTRA_SHOP_IMG = "EXTRA_SHOP_IMG";
    private static final String EXTRA_SHOP_IMG_URL = "EXTRA_SHOP_IMG_URL";
    private static final String EXTRA_TITLE = "EXTRA_TITLE";
    private static final String EXTRA_SUBTITLE = "EXTRA_SUBTITLE";
    private static final String EXTRA_PAY_BUTTON_TEXT = "EXTRA_PAY_BUTTON_TEXT";
    private static final String EXTRA_USE_SOURCE = "EXTRA_USE_SOURCE";
    // Object
    private OnStripePaymentDismissListener onDismissListener;
    private Stripe mStripe;
    private Card mCard;
    private Handler mHandler;
    // UI
    private LinearLayout mStripeDialogCardContainer;
    private LinearLayout mStripeDialogDateContainer;
    private LinearLayout mStripeDialogCvcContainer;
    private LinearLayout mStripeDialogEmailContainer;
    private LinearLayout mStripeDialogInputContainer;
    private CardNumberEditText mCreditCard;
    private ExpiryDateEditText mExpiryDate;
    private EditText mCVC;
    private ImageView mStripeDialogCardIcon;
    private TextView mTitleTextView;
    private TextView mDescriptionTextView;
    private TextView mErrorMessage;
    private TextView mEmailTextView;
    private CircleImageView mShopImageView;
    private Button mStripeDialogPayButton;
    private ImageView mExitButton;
    private ProgressBar mProgressBarLoading;
    private AppCompatImageView mImagePaymentSuccess;
    // VARIABLE
    private String mDefaultPublishKey = "";
    private String mShopName = "";
    private Integer mShopImage = 0;
    private String mShopImageUrl = "";
    private String mDescription = "";
    private String mPayButtonText = "";
    private String mEmail = "";
    private Boolean mUseSource = false;
    private Boolean mPaymentComplete = false;
    private Boolean mPaymentTimeout = false;

    /**
     * On Submit Payment Listener
     */
    private View.OnClickListener mPayClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            Log.d("Button", "Clicked");

            //Reset payment state.
            mErrorMessage.setVisibility(View.GONE);
            mPaymentTimeout = false;

            if (!validateCard()) {
                return;
            }

            if (!isConnected()) {
                setErrorMessage(getString(R.string.stripe_error_connection));
                return;
            }

            hideKeyboard();
            mStripeDialogPayButton.setText("");
            mStripeDialogPayButton.setEnabled(false);
            mProgressBarLoading.setVisibility(View.VISIBLE);

            if (mUseSource) {
                createStripeSource();
            } else {
                createStripeToken();
            }

            //Start timeout timer.
            Runnable r = new Runnable() {
                @Override
                public void run() {
                    if (getDialog() != null) {
                        if (getDialog().isShowing()) {
                            if (!mPaymentComplete) {
                                mPaymentTimeout = true;
                                mProgressBarLoading.setVisibility(View.GONE);
                                mStripeDialogPayButton.setText(mPayButtonText);
                                mStripeDialogPayButton.setEnabled(true);
                                setErrorMessage(getString(R.string.stripe_error_timeout));
                            }
                        }
                    } else {
                        mPaymentTimeout = true;
                        mProgressBarLoading.setVisibility(View.GONE);
                        mStripeDialogPayButton.setText(mPayButtonText);
                        mStripeDialogPayButton.setEnabled(true);
                        setErrorMessage(getString(R.string.stripe_error_timeout));
                    }
                }
            };
            mHandler.postDelayed(r, getResources().getInteger(R.integer.stripe_payment_timeout));
        }
    };

    /*
     * Stripe token creation callbacks.
     * Instantiate callback as variable to prevent it from being garbage collected.
     */
    private TokenCallback mTokenCallback = new TokenCallback() {
        @Override
        public void onSuccess(Token token) {
            Log.d("Stripe Token Success: ", token.toString());
            setSubmitSuccess(token.getId());
        }

        @Override
        public void onError(@NonNull Exception error) {
            if (error.getMessage().length() > 0) {
                Log.d("Stripe Token Error: ", error.getLocalizedMessage());
                setErrorMessage(error.getLocalizedMessage());
                setSubmitError();
            }
        }
    };
    private SourceCallback mSourceCallback = new SourceCallback() {
        @Override
        public void onSuccess(Source source) {
            Log.d("Stripe Token Success: ", source.toString());
            setSubmitSuccess(source.getId());
        }

        @Override
        public void onError(@NonNull Exception error) {
            if (error.getMessage().length() > 0) {
                Log.d("Stripe Token Error: ", error.getLocalizedMessage());
                setErrorMessage(error.getLocalizedMessage());
                setSubmitError();
            }
        }
    };

    /**
     * Open the Stripe Payment Dialog
     *
     * @param fm                - FragmentManager {{@link FragmentManager}}
     * @param publishKey        - Stripe Publish Key (not Secret Key , Secret Key store at server side )
     * @param shopImg           - Stripe Shop Image Drawable ID
     * @param shopImgUrl   - Set Header Image from URL
     * @param title        - Product name
     * @param subtitle          - Company name or seller
     * @param caption             - Caption badge
     * @param payButtonText     - Text to show on Pay Button
     * @param useSource         - Return Stripe source or token.
     * @param OnDismissListener - Callback Listener
     */
    public static void show(FragmentManager fm,
                            String publishKey,
                            Integer shopImg,
                            String shopImgUrl,
                            String title,
                            String subtitle,
                            String caption,
                            String payButtonText,
                            boolean useSource,
                            OnStripePaymentDismissListener OnDismissListener) {
        if (fm == null) {
            return;
        }
        StripePaymentDialog myDialogFragment = (StripePaymentDialog) fm.findFragmentByTag(TAG);
        if (myDialogFragment != null) {
            myDialogFragment.dismiss();
        }
        StripePaymentDialog instance = new StripePaymentDialog();
        instance.setDismissListener(OnDismissListener);
        Bundle args = new Bundle();
        args.putString(EXTRA_DEFAULT_PUBLISH_KEY, publishKey);
        args.putInt(EXTRA_SHOP_IMG, shopImg);
        args.putString(EXTRA_SHOP_IMG_URL, shopImgUrl);
        args.putString(EXTRA_TITLE, title);
        args.putString(EXTRA_SUBTITLE, subtitle);
        args.putString(EXTRA_CAPTION, caption);
        args.putString(EXTRA_PAY_BUTTON_TEXT, payButtonText);
        args.putBoolean(EXTRA_USE_SOURCE, useSource);
        instance.setArguments(args);
        instance.show(fm, TAG);
    }

    @Override
    public void onDismiss(@NonNull DialogInterface dialog) {
        mHandler.removeCallbacksAndMessages(null);

        super.onDismiss(dialog);
    }

    public void setDismissListener(OnStripePaymentDismissListener dissmissListener) {
        this.onDismissListener = dissmissListener;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        AppCompatDelegate.setCompatVectorFromResourcesEnabled(true);
        if (getArguments() != null) {
            mDefaultPublishKey = getArguments().getString(EXTRA_DEFAULT_PUBLISH_KEY);
            mEmail = getArguments().getString(EXTRA_CAPTION);
            mShopName = getArguments().getString(EXTRA_TITLE);
            mShopImage = getArguments().getInt(EXTRA_SHOP_IMG, 0);
            mShopImageUrl = getArguments().getString(EXTRA_SHOP_IMG_URL);
            mDescription = getArguments().getString(EXTRA_SUBTITLE);
            mPayButtonText = getArguments().getString(EXTRA_PAY_BUTTON_TEXT);
            mUseSource = getArguments().getBoolean(EXTRA_USE_SOURCE);
        }
        mHandler = new Handler();
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        @SuppressLint("InflateParams")
        View v = inflater.inflate(R.layout.stripe_dialog, null, false);
        mStripeDialogCardContainer = v.findViewById(R.id.stripe_dialog_card_container);
        mStripeDialogDateContainer = v.findViewById(R.id.stripe_dialog_date_container);
        mStripeDialogCvcContainer = v.findViewById(R.id.stripe_dialog_cvc_container);
        mStripeDialogEmailContainer = v.findViewById(R.id.stripe_dialog_email_container);
        mStripeDialogInputContainer = v.findViewById(R.id.stripe_dialog_input_container);
        mExitButton = v.findViewById(R.id.stripe_dialog_exit);
        mTitleTextView = v.findViewById(R.id.stripe_dialog_txt1);
        mDescriptionTextView = v.findViewById(R.id.stripe_dialog_txt2);
        mEmailTextView = v.findViewById(R.id.stripe_dialog_email);
        mErrorMessage = v.findViewById(R.id.stripe_dialog_error);
        mShopImageView = v.findViewById(R.id.stripe__logo);
        mShopImageView.showBackground(true);
        mExpiryDate = v.findViewById(R.id.stripe_dialog_date);
        mCreditCard = v.findViewById(R.id.stripe_dialog_card);
        mCVC = v.findViewById(R.id.stripe_dialog_cvc);
        mStripeDialogPayButton = v.findViewById(R.id.stripe_dialog_paybutton);
        mStripeDialogCardIcon = v.findViewById(R.id.stripe_dialog_card_icon);
        mProgressBarLoading = v.findViewById(R.id.progress_bar_loading);
        mImagePaymentSuccess = v.findViewById(R.id.image_payment_success);
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            Drawable wrapDrawable = DrawableCompat.wrap(mProgressBarLoading.getIndeterminateDrawable());
            DrawableCompat.setTint(wrapDrawable, ContextCompat.getColor(getContext(), android.R.color.white));
            mProgressBarLoading.setIndeterminateDrawable(DrawableCompat.unwrap(wrapDrawable));
        }
        return v;
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        getDialog().requestWindowFeature(Window.FEATURE_NO_TITLE);
        super.onActivityCreated(savedInstanceState);
        setupDialog();

        mStripe = new Stripe(getContext(), mDefaultPublishKey);

        if (mShopImage != 0) {
            mShopImageView.setImageDrawable(ContextCompat.getDrawable(getContext(), mShopImage));
        }
        if (!mShopImageUrl.isEmpty()) {
            mShopImageView.setUrl(mShopImageUrl);
        }
        mTitleTextView.setText(mShopName);
        mDescriptionTextView.setText(mDescription);
        mStripeDialogPayButton.setText(mPayButtonText);
        mStripeDialogPayButton.setOnClickListener(mPayClickListener);
        if (mEmail != null && mEmail.length() > 0) {
            mEmailTextView.setText(mEmail);
            mStripeDialogEmailContainer.setVisibility(View.VISIBLE);
        }

        mCreditCard.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (hasFocus) {
                    mStripeDialogCardContainer.setBackgroundResource(R.drawable.stripe_inputbox_background_top_selected);
                } else {
                    mStripeDialogCardContainer.setBackgroundResource(android.R.color.transparent);
                }
            }
        });
        mExpiryDate.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (hasFocus) {
                    mStripeDialogDateContainer.setBackgroundResource(R.drawable.stripe_inputbox_background_left_bottom_selected);
                } else {
                    mStripeDialogDateContainer.setBackgroundResource(android.R.color.transparent);
                }
            }
        });
        mCVC.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (hasFocus) {
                    mStripeDialogCvcContainer.setBackgroundResource(R.drawable.stripe_inputbox_background_right_bottom_selected);
                } else {
                    mStripeDialogCvcContainer.setBackgroundResource(android.R.color.transparent);
                }
            }
        });

        mExpiryDate.setOnKeyListener(new View.OnKeyListener() {
            @Override
            public boolean onKey(View v, int keyCode, KeyEvent event) {
                if (keyCode == KeyEvent.KEYCODE_DEL && event.getAction() == KeyEvent.ACTION_DOWN && mExpiryDate.length() == 0) {
                    onDeleteEmpty(mCreditCard);
                }
                return false;
            }
        });
        mCVC.setOnKeyListener(new View.OnKeyListener() {
            @Override
            public boolean onKey(View v, int keyCode, KeyEvent event) {
                if (keyCode == KeyEvent.KEYCODE_DEL && event.getAction() == KeyEvent.ACTION_DOWN && mCVC.length() == 0) {
                    onDeleteEmpty(mExpiryDate);
                }
                return false;
            }
        });

        mCreditCard.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                if (mCreditCard.getText().length() > 0) {
                    switch (mCreditCard.getCardBrand()) {
                        case Card.VISA:
                            mStripeDialogCardIcon.setImageDrawable(ContextCompat.getDrawable(getContext(), R.drawable.ic_visa));
                            mStripeDialogCardIcon.setVisibility(View.VISIBLE);
                            break;
                        case Card.MASTERCARD:
                            mStripeDialogCardIcon.setImageDrawable(ContextCompat.getDrawable(getContext(), R.drawable.ic_mastercard));
                            mStripeDialogCardIcon.setVisibility(View.VISIBLE);
                            break;
                        case Card.AMERICAN_EXPRESS:
                            mStripeDialogCardIcon.setImageDrawable(ContextCompat.getDrawable(getContext(), R.drawable.ic_amex));
                            mStripeDialogCardIcon.setVisibility(View.VISIBLE);
                            break;
                        case Card.DISCOVER:
                            mStripeDialogCardIcon.setImageDrawable(ContextCompat.getDrawable(getContext(), R.drawable.ic_discover));
                            mStripeDialogCardIcon.setVisibility(View.VISIBLE);
                            break;
                        case Card.DINERS_CLUB:
                            mStripeDialogCardIcon.setImageDrawable(ContextCompat.getDrawable(getContext(), R.drawable.ic_diners));
                            mStripeDialogCardIcon.setVisibility(View.VISIBLE);
                            break;
                        case Card.JCB:
                            mStripeDialogCardIcon.setImageDrawable(ContextCompat.getDrawable(getContext(), R.drawable.ic_jcb));
                            mStripeDialogCardIcon.setVisibility(View.VISIBLE);
                            break;
                        case Card.UNIONPAY:
                            mStripeDialogCardIcon.setImageDrawable(ContextCompat.getDrawable(getContext(), R.drawable.ic_unionpay_template_32));
                            mStripeDialogCardIcon.setVisibility(View.VISIBLE);
                            break;
                        default:
                            //Card type is detected after 4 numbers. Display Unknown type if no type detected after 4 digits entered.
                            if (mCreditCard.getText().length() >= 4) {
                                mStripeDialogCardIcon.setImageDrawable(ContextCompat.getDrawable(getContext(), R.drawable.ic_unknown));
                                mStripeDialogCardIcon.setVisibility(View.VISIBLE);
                            } else {
                                mStripeDialogCardIcon.setVisibility(View.GONE);
                            }
                            break;
                    }
                } else {
                    mStripeDialogCardIcon.setVisibility(View.GONE);
                }

                if (mCreditCard.getText().length() == 19 && mCreditCard.isCardNumberValid()) {
                    mExpiryDate.requestFocus();
                }
            }
        });
        //Setting error color required or else text color is transparent. Setting in XML does not work.
        mCreditCard.setErrorColor(ContextCompat.getColor(getContext(), android.R.color.holo_red_light));

        mExpiryDate.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void afterTextChanged(Editable editable) {
                if (mExpiryDate.getText().length() == 5 && mExpiryDate.isDateValid()) {
                    mCVC.requestFocus();
                }
            }
        });
        mExpiryDate.setErrorColor(ContextCompat.getColor(getContext(), android.R.color.holo_red_light));

        mCVC.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void afterTextChanged(Editable editable) {
                //Limit length to 3 if card is not AMEX.
                if (!Card.AMERICAN_EXPRESS.equals(mCreditCard.getCardBrand()) && editable.length() == 4) {
                    mCVC.setText(mCVC.getText().toString().substring(0, 3));
                    mCVC.setSelection(3);
                }

                if (isCvcMaximalLength(mCreditCard.getCardBrand(), mCVC.getText().toString())) {
                    Log.d("CVC", "Validated");
                    //Validate credit card and set focus on submit button if successful.
                    if (validateCard()) {
                        Log.d("Card", "Validated");
                        //Button cannot be lit up via focused due to keyboard submit handling.
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                            mStripeDialogPayButton.setTranslationZ(6);
                        }
                    }
                }
            }
        });
        mCVC.setOnEditorActionListener(new EditText.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    mStripeDialogPayButton.performClick();
                    hideKeyboard();
                    return true;
                }

                return false;
            }
        });

        mExitButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dismiss();
                if (onDismissListener != null) {
                    onDismissListener.onDismiss();
                }
            }
        });
    }

    private boolean validateCard() {
        mErrorMessage.setVisibility(View.GONE);
        mCreditCard.setError(null);
        mExpiryDate.setError(null);
        mCVC.setError(null);

        //Get expiry date fields or set invalid fields if null.
        int[] MMYY = mExpiryDate.getValidDateFields();
        if (MMYY == null) {
            MMYY = new int[2];
            MMYY[0] = -1;
            MMYY[1] = -1;
        }

        mCard = new Card(
                mCreditCard.getText().toString(),
                MMYY[0],
                MMYY[1],
                mCVC.getText().toString());
        if (mCard.validateCard()) {
            return true;
        } else if (!mCard.validateNumber()) {
            mCreditCard.setError(getString(R.string.stripe_invalidate_card_number));
        } else if (!mCard.validateExpiryDate()) {
            mExpiryDate.setError(getString(R.string.stripe_invalidate_expirydate));
        } else if (!mCard.validateCVC()) {
            mCVC.setError(getString(R.string.stripe_invalidate_cvc));
        } else {
            setErrorMessage(getString(R.string.stripe_invalidate_card_detail));
        }

        return false;
    }

    private boolean isCvcMaximalLength(@NonNull @Card.CardBrand String cardBrand, String cvcText) {
        if (Card.AMERICAN_EXPRESS.equals(cardBrand)) {
            return cvcText.trim().length() == CVC_LENGTH_AMERICAN_EXPRESS;
        } else {
            return cvcText.trim().length() == CVC_LENGTH_COMMON;
        }
    }

    private void createStripeToken() {
        mStripe.createToken(mCard, mDefaultPublishKey, mTokenCallback);
    }

    private void createStripeSource() {
        SourceParams cardSourceParams = SourceParams.createCardParams(mCard);
        mStripe.createSource(cardSourceParams, mSourceCallback);
    }

    private void setSubmitSuccess(final String id) {
        if (getDialog() != null && getDialog().isShowing()) {
            if (!mPaymentTimeout) {
                mPaymentComplete = true;

                mProgressBarLoading.setVisibility(View.GONE);
                mImagePaymentSuccess.setVisibility(View.VISIBLE);
                Drawable d = mImagePaymentSuccess.getDrawable();
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    if (d instanceof Animatable) {
                        ((Animatable) d).start();
                    }
                }

                Runnable r = new Runnable() {
                    @Override
                    public void run() {
                        if (getDialog() != null) {
                            if (getDialog().isShowing()) {
                                dismiss();
                                if (onDismissListener != null) {
                                    onDismissListener.onSuccess(getDialog(), id);
                                }
                            }
                        } else {
                            //Dialog was dismissed. Send callback.
                            if (onDismissListener != null) {
                                onDismissListener.onSuccess(getDialog(), id);
                            }
                        }
                    }
                };
                mHandler.postDelayed(r, 2000);
            }
        }
    }

    private void setSubmitError() {
        mHandler.removeCallbacksAndMessages(null);
        mProgressBarLoading.setVisibility(View.GONE);
        mStripeDialogPayButton.setText(mPayButtonText);
        mStripeDialogPayButton.setEnabled(true);
        setErrorMessage(getString(R.string.stripe_error_payment));
    }

    private void setErrorMessage(String errorMessage) {
        mErrorMessage.setText(errorMessage);
        mErrorMessage.setVisibility(View.VISIBLE);
    }

    private void setupDialog() {
        if (getDialog().getWindow() != null) {
            //Set dialog fragment full screen.
            setCancelable(false);
            getDialog().getWindow().setGravity(Gravity.CENTER_HORIZONTAL | Gravity.CENTER_VERTICAL);
            getDialog().getWindow().setBackgroundDrawableResource(android.R.color.transparent);
            getDialog().getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            getDialog().getWindow().setWindowAnimations(android.R.style.Animation_Dialog);
        }
    }

    private void onDeleteEmpty(EditText editText) {
        String fieldText = editText.getText().toString();
        if (fieldText.length() > 1) {
            editText.setText(
                    fieldText.substring(0, fieldText.length() - 1));
        }
        editText.requestFocus();
        editText.setSelection(editText.length());
    }

    private void hideKeyboard() {
        //Hide keyboard
        try {
            InputMethodManager imm = (InputMethodManager) getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(mStripeDialogInputContainer.getWindowToken(), 0);
            mStripeDialogInputContainer.requestFocus();
        } catch (IllegalStateException e) {
            Log.e("Hide Keyboard", e.toString());
        } catch (NullPointerException e) {
            Log.e("Hide Keyboard", e.toString());
        }
    }

    private boolean isConnected() {
        ConnectivityManager connectivityManager = (ConnectivityManager) getContext().getSystemService(Context.CONNECTIVITY_SERVICE);
        if (connectivityManager != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                Network[] networks = connectivityManager.getAllNetworks();
                NetworkInfo networkInfo;
                for (Network network : networks) {
                    networkInfo = connectivityManager.getNetworkInfo(network);
                    if (networkInfo.getState() == NetworkInfo.State.CONNECTED) {
                        return true;
                    }
                }
            } else {
                NetworkInfo[] networkInfos = connectivityManager.getAllNetworkInfo();
                if (networkInfos != null) {
                    for (NetworkInfo networkInfo : networkInfos) {
                        if (networkInfo.getState() == NetworkInfo.State.CONNECTED)
                            return true;
                    }
                }
            }
        }

        return false;
    }

    /**
     * OnStripePaymentDismissListener
     */
    public interface OnStripePaymentDismissListener {
        /**
         * @param dialog - Current Dialog
         * @param token  {{ @Link com.stripe.android.model.Token}}
         */
        void onSuccess(Dialog dialog, String token);

        void onDismiss();
    }
}
