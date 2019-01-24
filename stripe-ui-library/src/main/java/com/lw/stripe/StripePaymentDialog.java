package com.lw.stripe;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.lw.stripe.utils.ui.CircleImageView;
import com.stripe.android.Stripe;
import com.stripe.android.TokenCallback;
import com.stripe.android.model.Card;
import com.stripe.android.model.Token;
import com.stripe.android.view.CardNumberEditText;
import com.stripe.android.view.ExpiryDateEditText;

import java.lang.reflect.Field;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

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
    private static final String EXTRA_SHOP_NAME = "EXTRA_SHOP_NAME";
    private static final String EXTRA_DESCRIPTION = "EXTRA_DESCRIPTION";
    private static final String EXTRA_CURRENCY = "EXTRA_CURRENCY";
    private static final String EXTRA_AMOUNT = "EXTRA_AMOUNT";
    // Object
    private OnStripePaymentDismissListener onDismissListener;
    private Stripe mStripe;
    // UI
    private LinearLayout mStripeDialogCardContainer;
    private LinearLayout mStripeDialogDateContainer;
    private LinearLayout mStripeDialogCvcContainer;
    private LinearLayout mStripeDialogEmailContainer;
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
    private String mLastInput = "";
    private String mDefaultPublishKey = "";
    private String mShopName = "";
    private String mShopImage = "";
    private String mDescription = "";
    private String mCurrency = "";
    private String mEmail = "";
    private float mAmount;

    /**
     * On Submit Payment Listener
     */
    private View.OnClickListener mPayClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            mErrorMessage.setVisibility(View.GONE);
            if (mCreditCard.getText().toString().length() <= 0) {
                mCreditCard.setError(getString(R.string.__stripe_invalidate_card_number));
                return;
            }
            if (mCVC.getText().toString().length() <= 0) {
                mCVC.setError(getString(R.string.__stripe_invalidate_cvc));
                return;
            }
            if (mExpiryDate.getText().toString().length() <= 0) {
                mExpiryDate.setError(getString(R.string.__stripe_invalidate_expirydate));
                return;
            }
            String mmExpireDate = mExpiryDate.getText().toString();
            String[] mmMMYY = mmExpireDate.split("/");

            Card mmCard = new Card(
                    mCreditCard.getText().toString(),
                    Integer.parseInt(mmMMYY[0]),
                    Integer.parseInt(mmMMYY[1]),
                    mCVC.getText().toString());
            if (mmCard.validateCard()) {
                mStripe.createToken(mmCard, mDefaultPublishKey, new TokenCallback() {
                    @Override
                    public void onError(Exception error) {
                        if (error != null && error.getMessage().length() > 0) {
                            mErrorMessage.setText(error.getLocalizedMessage());
                            mErrorMessage.setVisibility(View.VISIBLE);
                        }
                    }

                    @Override
                    public void onSuccess(Token token) {
                        if (onDismissListener != null) {
                            onDismissListener.onSuccess(getDialog(), token);
                        }
                    }
                });
            } else if (!mmCard.validateNumber()) {
                mCreditCard.setError(getString(R.string.__stripe_invalidate_card_number));
            } else if (!mmCard.validateExpiryDate()) {
                mExpiryDate.setError(getString(R.string.__stripe_invalidate_expirydate));
            } else if (!mmCard.validateCVC()) {
                mCVC.setError(getString(R.string.__stripe_invalidate_cvc));
            } else {
                mErrorMessage.setText(R.string.__stripe_invalidate_card_detail);
                mErrorMessage.setVisibility(View.VISIBLE);
            }
        }
    };

    /**
     * Open the Stripe Payment Dialog
     *
     * @param fm                 - FragmentManager {{@link FragmentManager}}
     * @param publish_key        - Stripe Publish Key (not Secret Key , Secret Key store at server side )
     * @param _email             - User Email
     * @param _shop_img          - Stripe Shop Image
     * @param _shop_name         - Stripe Shop Name
     * @param _description       - Description of your payment (e.g $100 Movie Coupon)
     * @param _currency          - Currency of your payment (e.g HKD)
     * @param _amount            - Amount of your payment (e.g 100 then amount is 10000)
     * @param _OnDismissListener - Callback Listener
     */
    public static void show(FragmentManager fm,
                            String publish_key,
                            String _email,
                            String _shop_img,
                            String _shop_name,
                            String _description,
                            String _currency,
                            float _amount,
                            OnStripePaymentDismissListener _OnDismissListener) {
        if (fm == null) {
            return;
        }
        StripePaymentDialog myDialogFragment = (StripePaymentDialog) fm.findFragmentByTag(TAG);
        if (myDialogFragment != null) {
            myDialogFragment.dismiss();
        }
        StripePaymentDialog instance = new StripePaymentDialog();
        instance.setDismissListener(_OnDismissListener);
        Bundle args = new Bundle();
        args.putString(EXTRA_DEFAULT_PUBLISH_KEY, publish_key);
        args.putString(EXTRA_USER_EMAIL, _email);
        args.putString(EXTRA_SHOP_IMG, _shop_img);
        args.putString(EXTRA_SHOP_NAME, _shop_name);
        args.putString(EXTRA_DESCRIPTION, _description);
        args.putString(EXTRA_CURRENCY, _currency);
        args.putFloat(EXTRA_AMOUNT, _amount);
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
            mShopName = getArguments().getString(EXTRA_SHOP_NAME);
            mShopImage = getArguments().getString(EXTRA_SHOP_IMG);
            mDescription = getArguments().getString(EXTRA_DESCRIPTION);
            mCurrency = getArguments().getString(EXTRA_CURRENCY);
            mAmount = getArguments().getFloat(EXTRA_AMOUNT);
            mEmail = getArguments().getString(EXTRA_USER_EMAIL);
        }
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        @SuppressLint("InflateParams")
        View v = inflater.inflate(R.layout.stripe__dialog_, null, false);
        mStripeDialogCardContainer = v.findViewById(R.id.stripe_dialog_card_container);
        mStripeDialogDateContainer = v.findViewById(R.id.stripe_dialog_date_container);
        mStripeDialogCvcContainer = v.findViewById(R.id.stripe_dialog_cvc_container);
        mStripeDialogEmailContainer = v.findViewById(R.id.stripe_dialog_email_container);
        mExitButton = v.findViewById(R.id.stripe_dialog_exit);
        mTitleTextView = v.findViewById(R.id.stripe_dialog_txt1);
        mDescriptionTextView = v.findViewById(R.id.stripe_dialog_txt2);
        mEmailTextView = v.findViewById(R.id.stripe_dialog_email);
        mErrorMessage = v.findViewById(R.id.stripe_dialog_error);
        mShopImageView = v.findViewById(R.id.stripe__logo);
        mShopImageView.setWithBackground(true);
        mExpiryDate = v.findViewById(R.id.stripe_dialog_date);
        mCreditCard = v.findViewById(R.id.stripe_dialog_card);
        mCVC = v.findViewById(R.id.stripe_dialog_cvc);
        mStripeDialogPayButton = v.findViewById(R.id.stripe_dialog_paybutton);
        mStripeDialogCardIcon = v.findViewById(R.id.stripe_dialog_card_icon);
        return v;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        getDialog().requestWindowFeature(Window.FEATURE_NO_TITLE);
        super.onActivityCreated(savedInstanceState);
        setupDialog();

        mStripe = new Stripe(getContext(), mDefaultPublishKey);

        mShopImageView.setUrl(mShopImage);
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
                    mStripeDialogCardContainer.setBackgroundResource(R.drawable.stripe_inputbox_background_selected_top);
                } else {
                    mStripeDialogCardContainer.setBackgroundResource(android.R.color.transparent);
                }
            }
        });
        mExpiryDate.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (hasFocus) {
                    mStripeDialogDateContainer.setBackgroundResource(R.drawable.stripe_inputbox_background_selected_left_bottom);
                } else {
                    mStripeDialogDateContainer.setBackgroundResource(android.R.color.transparent);
                }
            }
        });
        mCVC.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (hasFocus) {
                    mStripeDialogCvcContainer.setBackgroundResource(R.drawable.stripe_inputbox_background_selected_right_bottom);
                } else {
                    mStripeDialogCvcContainer.setBackgroundResource(android.R.color.transparent);
                }
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
                    Card mmCard = new Card(mCreditCard.getText().toString(), 0, 0, "");
                    switch (mmCard.getBrand()) {
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

        mExitButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dismiss();
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

    /**
     * OnStripePaymentDismissListener
     */
    public interface OnStripePaymentDismissListener {
        /**
         * @param mmDialog - Current Dialog
         * @param mmToken  {{ @Link com.stripe.android.model.Token}}
         */
        void onSuccess(Dialog mmDialog, Token mmToken);
    }
}
