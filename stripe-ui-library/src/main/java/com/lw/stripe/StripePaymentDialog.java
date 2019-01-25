package com.lw.stripe;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.content.Context;
import android.os.Build;
import android.os.Bundle;
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
import android.widget.TextView;

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

import java.lang.reflect.Field;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

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
    private static final String EXTRA_USER_EMAIL = "EXTRA_USER_EMAIL";
    private static final String EXTRA_SHOP_IMG = "EXTRA_SHOP_IMG";
    private static final String EXTRA_SHOP_IMG_URL = "EXTRA_SHOP_IMG_URL";
    private static final String EXTRA_SHOP_NAME = "EXTRA_SHOP_NAME";
    private static final String EXTRA_DESCRIPTION = "EXTRA_DESCRIPTION";
    private static final String EXTRA_CURRENCY = "EXTRA_CURRENCY";
    private static final String EXTRA_AMOUNT = "EXTRA_AMOUNT";
    private static final String EXTRA_USE_SOURCE = "EXTRA_USE_SOURCE";
    // Object
    private OnStripePaymentDismissListener onDismissListener;
    private Stripe mStripe;
    private Card mCard;
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
    // VARIABLE
    private String mDefaultPublishKey = "";
    private String mShopName = "";
    private Integer mShopImage = 0;
    private String mShopImageUrl = "";
    private String mDescription = "";
    private String mCurrency = "";
    private String mEmail = "";
    private float mAmount;
    private Boolean mUseSource = false;

    /**
     * On Submit Payment Listener
     */
    private View.OnClickListener mPayClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            Log.d("Button", "Clicked");
            if (validateCard()) {
                hideKeyboard();
                if (mUseSource) {
                    createStripeSource();
                } else {
                    createStripeToken();
                }
            }
        }
    };

    /**
     * Open the Stripe Payment Dialog
     *
     * @param fm                 - FragmentManager {{@link FragmentManager}}
     * @param publish_key        - Stripe Publish Key (not Secret Key , Secret Key store at server side )
     * @param email             - User Email
     * @param shop_img_url          - Stripe Shop Image
     * @param shop_name         - Stripe Shop Name
     * @param description       - Description of your payment (e.g $100 Movie Coupon)
     * @param currency          - Currency of your payment (e.g HKD)
     * @param amount            - Amount of your payment (e.g 100 then amount is 10000)
     * @param OnDismissListener - Callback Listener
     */
    public static void show(FragmentManager fm,
                            String publish_key,
                            String email,
                            Integer shop_img,
                            String shop_img_url,
                            String shop_name,
                            String description,
                            String currency,
                            float amount,
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
        args.putString(EXTRA_DEFAULT_PUBLISH_KEY, publish_key);
        args.putString(EXTRA_USER_EMAIL, email);
        args.putString(EXTRA_SHOP_NAME, shop_name);
        args.putInt(EXTRA_SHOP_IMG, shop_img);
        args.putString(EXTRA_SHOP_IMG_URL, shop_img_url);
        args.putString(EXTRA_DESCRIPTION, description);
        args.putString(EXTRA_CURRENCY, currency);
        args.putFloat(EXTRA_AMOUNT, amount);
        args.putBoolean(EXTRA_USE_SOURCE, useSource);
        instance.setArguments(args);
        instance.show(fm, TAG);
    }

    public void setDismissListener(OnStripePaymentDismissListener dissmissListener) {
        this.onDismissListener = dissmissListener;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mDefaultPublishKey = getArguments().getString(EXTRA_DEFAULT_PUBLISH_KEY);
            mEmail = getArguments().getString(EXTRA_USER_EMAIL);
            mShopName = getArguments().getString(EXTRA_SHOP_NAME);
            mShopImage = getArguments().getInt(EXTRA_SHOP_IMG , 0);
            mShopImageUrl = getArguments().getString(EXTRA_SHOP_IMG_URL);
            mDescription = getArguments().getString(EXTRA_DESCRIPTION);
            mCurrency = getArguments().getString(EXTRA_CURRENCY);
            mAmount = getArguments().getFloat(EXTRA_AMOUNT);
            mUseSource = getArguments().getBoolean(EXTRA_USE_SOURCE);
        }
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
            mShopImageView.setImageDrawable(getResources().getDrawable(mShopImage));
        }
        if (!mShopImageUrl.isEmpty()) {
            mShopImageView.setUrl(mShopImageUrl);
        }
        mTitleTextView.setText(mShopName);
        mDescriptionTextView.setText(mDescription);
        mStripeDialogPayButton.setText(getString(R.string.__stripe_pay) + " " + mCurrency + " " + (mAmount / 100));
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
            mCreditCard.setError(getString(R.string.__stripe_invalidate_card_number));
        } else if (!mCard.validateExpiryDate()) {
            mExpiryDate.setError(getString(R.string.__stripe_invalidate_expirydate));
        } else if (!mCard.validateCVC()) {
            mCVC.setError(getString(R.string.__stripe_invalidate_cvc));
        } else {
            mErrorMessage.setText(R.string.__stripe_invalidate_card_detail);
            mErrorMessage.setVisibility(View.VISIBLE);
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
        mStripe.createToken(mCard, mDefaultPublishKey, new TokenCallback() {
            @Override
            public void onSuccess(Token token) {
                if (onDismissListener != null) {
                    onDismissListener.onSuccess(getDialog(), token.getId());
                }
            }
            @Override
            public void onError(Exception error) {
                if (error != null && error.getMessage().length() > 0) {
                    mErrorMessage.setText(error.getLocalizedMessage());
                    mErrorMessage.setVisibility(View.VISIBLE);
                }
            }
        });
    }

    private void createStripeSource() {
        SourceParams cardSourceParams = SourceParams.createCardParams(mCard);
        Stripe stripe = new Stripe(getContext(), mDefaultPublishKey);
        stripe.createSource(cardSourceParams, new SourceCallback() {
            @Override
            public void onSuccess(Source source) {
                if (onDismissListener != null) {
                    onDismissListener.onSuccess(getDialog(), source.getId());
                }
            }
            @Override
            public void onError(Exception error) {
                if (error != null && error.getMessage().length() > 0) {
                    mErrorMessage.setText(error.getLocalizedMessage());
                    mErrorMessage.setVisibility(View.VISIBLE);
                }
            }
        });
    }

    @Override
    public void onDetach() {
        super.onDetach();
        try {
            Field childFragmentManager = Fragment.class.getDeclaredField("mChildFragmentManager");
            childFragmentManager.setAccessible(true);
            childFragmentManager.set(this, null);
        } catch (NoSuchFieldException e) {
            throw new RuntimeException(e);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }

    }

    private void setupDialog() {
        if (getDialog().getWindow() != null) {
            // special the dialog fragment, make it full screen
            setCancelable(false);
//        getDialog().getWindow().clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
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

    /**
     * OnStripePaymentDismissListener
     */
    public interface OnStripePaymentDismissListener {
        /**
         * @param dialog - Current Dialog
         * @param token  {{ @Link com.stripe.android.model.Token}}
         */
        void onSuccess(Dialog dialog, String token);
    }
}
