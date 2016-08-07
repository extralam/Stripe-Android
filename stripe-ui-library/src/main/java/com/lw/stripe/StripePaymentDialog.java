package com.lw.stripe;

import android.app.Dialog;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
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
import com.lw.stripe.utils.ui.StripeImageView;
import com.stripe.android.Stripe;
import com.stripe.android.TokenCallback;
import com.stripe.android.model.Card;
import com.stripe.android.model.Token;
import com.stripe.exception.AuthenticationException;

import java.lang.reflect.Field;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

/**
 * Stripe Payment Dialog
 * - Easy Start a StripePayment
 */
public class StripePaymentDialog extends DialogFragment {

    /**
     * OnStripePaymentDismissListener
     */
    public interface OnStripePaymentDismissListener {
        /**
         *
         * @param mmDialog      - Current Dialog
         * @param mmToken       {{ @Link com.stripe.android.model.Token}}
         */
        void onSuccess(Dialog mmDialog , Token mmToken);
    }

    private static final String TAG = "StripePaymentDialog";
    private static final String EXTRA_DEFAULT_PUBLISH_KEY = "EXTRA_DEFAULT_PUBLISH_KEY";
    private static final String EXTRA_USER_EMAIL = "EXTRA_USER_EMAIL";
    private static final String EXTRA_SHOP_IMG = "EXTRA_SHOP_IMG";
    private static final String EXTRA_SHOP_NAME = "EXTRA_SHOP_NAME";
    private static final String EXTRA_DESCRIPTION = "EXTRA_DESCRIPTION";
    private static final String EXTRA_CURRENCY = "EXTRA_CURRENCY";
    private static final String EXTRA_AMOUNT= "EXTRA_AMOUNT";

    /**
     * Open the Stripe Payment Dialog
     * @param fm                    - FragmentManager {{@link FragmentManager}}
     * @param publish_key           - Stripe Publish Key (not Secret Key , Secret Key store at server side )
     * @param _email                - User Email
     * @param _shop_img             - Stripe Shop Image
     * @param _shop_name            - Stripe Shop Name
     * @param _description          - Description of your payment (e.g $100 Movie Coupon)
     * @param _currency             - Currency of your payment (e.g HKD)
     * @param _amount               - Amount of your payment (e.g 100 then amount is 10000)
     * @param _OnDismissListener    - Callback Listener
     */
    public static void show(FragmentManager fm ,
                            String publish_key,
                            String _email,
                            String _shop_img,
                            String _shop_name,
                            String _description,
                            String _currency,
                            float _amount,
                            OnStripePaymentDismissListener _OnDismissListener) {
        if(fm == null){
            return;
        }
        StripePaymentDialog myDialogFragment = (StripePaymentDialog) fm.findFragmentByTag(TAG);
        if(myDialogFragment != null) {
            myDialogFragment.dismiss();
        }
        StripePaymentDialog instance = new StripePaymentDialog();
        instance.setDissmissListener(_OnDismissListener);
        Bundle args = new Bundle();
        args.putString(EXTRA_DEFAULT_PUBLISH_KEY, publish_key);
        args.putString(EXTRA_USER_EMAIL , _email);
        args.putString(EXTRA_SHOP_IMG, _shop_img);
        args.putString(EXTRA_SHOP_NAME, _shop_name);
        args.putString(EXTRA_DESCRIPTION, _description);
        args.putString(EXTRA_CURRENCY, _currency);
        args.putFloat(EXTRA_AMOUNT, _amount);
        instance.setArguments(args);
        instance.show(fm,TAG);
    }

    public void setDissmissListener(OnStripePaymentDismissListener dissmissListener) {
        this.onDismissListener = dissmissListener;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mDefaultPublishKey = getArguments().getString(EXTRA_DEFAULT_PUBLISH_KEY);
        mShopName = getArguments().getString(EXTRA_SHOP_NAME);
        mShopImage = getArguments().getString(EXTRA_SHOP_IMG);
        mDescription = getArguments().getString(EXTRA_DESCRIPTION);
        mCurrency = getArguments().getString(EXTRA_CURRENCY);
        mAmount = getArguments().getFloat(EXTRA_AMOUNT);
        mEmail = getArguments().getString(EXTRA_USER_EMAIL);
    }

    // Object
    private OnStripePaymentDismissListener onDismissListener;
    private Stripe mStripe;

    // UI
    private LinearLayout mStripe_dialog_card_container;
    private LinearLayout mStripe_dialog_date_container;
    private LinearLayout mStripe_dialog_cvc_container;
    private LinearLayout mStripe_dialog_email_container;
    private EditText mCreditCard;
    private EditText mExpiryDate;
    private EditText mCVC;
    private ImageView mStripeDialogCardIcon;
    private TextView mTitleTextView;
    private TextView mDescriptionTextView;
    private TextView mErrorMessage;
    private TextView mEmailTextView;
    private CircleImageView mShopImageView;
    private Button mStripe_dialog_paybutton;
    private StripeImageView mExitButton;

    // VARIABLE
    private String mLastInput;
    private String mDefaultPublishKey = null;
    private String mShopName = null;
    private String mShopImage = null;
    private String mDescription = null;
    private String mCurrency = null;
    private String mEmail = null;
    private float mAmount;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.stripe__dialog_ , null , false);
        mStripe_dialog_card_container = (LinearLayout) v.findViewById(R.id.stripe_dialog_card_container);
        mStripe_dialog_date_container = (LinearLayout) v.findViewById(R.id.stripe_dialog_date_container);
        mStripe_dialog_cvc_container = (LinearLayout) v.findViewById(R.id.stripe_dialog_cvc_container);
        mStripe_dialog_email_container = (LinearLayout) v.findViewById(R.id.stripe_dialog_email_container);
        mExitButton = (StripeImageView) v.findViewById(R.id.stripe_dialog_exit);
        mTitleTextView = (TextView) v.findViewById(R.id.stripe_dialog_txt1);
        mDescriptionTextView = (TextView) v.findViewById(R.id.stripe_dialog_txt2);
        mEmailTextView = (TextView) v.findViewById(R.id.stripe_dialog_email);
        mErrorMessage = (TextView) v.findViewById(R.id.stripe_dialog_error);
        mShopImageView = (CircleImageView) v.findViewById(R.id.stripe__logo);
        mShopImageView.setWithBackground(true);
        mExpiryDate = (EditText) v.findViewById(R.id.stripe_dialog_date);
        mCreditCard = (EditText) v.findViewById(R.id.stripe_dialog_card);
        mCVC = (EditText) v.findViewById(R.id.stripe_dialog_cvc);
        mStripe_dialog_paybutton = (Button) v.findViewById(R.id.stripe_dialog_paybutton);
        mStripeDialogCardIcon = (ImageView) v.findViewById(R.id.stripe_dialog_card_icon);
        return v;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        getDialog().requestWindowFeature(Window.FEATURE_NO_TITLE);
        super.onActivityCreated(savedInstanceState);
        setupDialog();

        try {
            mStripe = new Stripe(mDefaultPublishKey);
        }catch (AuthenticationException e){

        }

        mShopImageView.setUrl(mShopImage);
        mTitleTextView.setText(mShopName);
        mDescriptionTextView.setText(mDescription);
        mStripe_dialog_paybutton.setText(getString(R.string.__stripe_pay) + " " + mCurrency + " " + (mAmount / 100));
        mStripe_dialog_paybutton.setOnClickListener(mPayClickListener);
        if(mEmail != null && mEmail.length() > 0){
            mEmailTextView.setText(mEmail);
            mStripe_dialog_email_container.setVisibility(View.VISIBLE);
        }

        mExpiryDate.addTextChangedListener(mCreditCardExpireDateTextWatcher);
        mCreditCard.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if(hasFocus){
                    mStripe_dialog_card_container.setBackgroundResource(R.drawable.stripe_inputbox_background_selected_top);
                }else{
                    mStripe_dialog_card_container.setBackgroundResource(android.R.color.transparent);
                }
            }
        });
        mExpiryDate.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if(hasFocus){
                    mStripe_dialog_date_container.setBackgroundResource(R.drawable.stripe_inputbox_background_selected_left_bottom);
                }else{
                    mStripe_dialog_date_container.setBackgroundResource(android.R.color.transparent);
                }
            }
        });
        mCVC.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if(hasFocus){
                    mStripe_dialog_cvc_container.setBackgroundResource(R.drawable.stripe_inputbox_background_selected_right_bottom);
                }else{
                    mStripe_dialog_cvc_container.setBackgroundResource(android.R.color.transparent);
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
                if(mCreditCard.getText().length() > 0) {
                    Card mmCard = new Card(mCreditCard.getText().toString(), 0, 0, "");
                    switch (mmCard.getType()) {
                        case Card.VISA:
                            mStripeDialogCardIcon.setImageResource(R.drawable.ic__visa);
                            mStripeDialogCardIcon.setVisibility(View.VISIBLE);
                            break;
                        case Card.MASTERCARD:
                            mStripeDialogCardIcon.setImageResource(R.drawable.ic__mastercard);
                            mStripeDialogCardIcon.setVisibility(View.VISIBLE);
                            break;
                        case Card.AMERICAN_EXPRESS:
                            mStripeDialogCardIcon.setImageResource(R.drawable.ic__ae);
                            mStripeDialogCardIcon.setVisibility(View.VISIBLE);
                            break;
                        default:
                            mStripeDialogCardIcon.setVisibility(View.GONE);
                    }
                }else{
                    mStripeDialogCardIcon.setVisibility(View.GONE);
                }
            }
        });

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

    private void setupDialog(){
        // special the dialog fragment, make it full screen
        setCancelable(false);
//        getDialog().getWindow().clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
        getDialog().getWindow().setGravity(Gravity.CENTER_HORIZONTAL | Gravity.CENTER_VERTICAL);
        getDialog().getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        getDialog().getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        getDialog().getWindow().setWindowAnimations(android.R.style.Animation_Dialog);
    }

    /**
     * Credit Card Edittext Change Listener
     */
    private TextWatcher mCreditCardExpireDateTextWatcher = new TextWatcher() {
        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {

        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {

        }

        @Override
        public void afterTextChanged(Editable s) {
            String input = s.toString();
            SimpleDateFormat formatter = new SimpleDateFormat("MM/yy", Locale.GERMANY);
            Calendar expiryDateDate = Calendar.getInstance();
            try {
                expiryDateDate.setTime(formatter.parse(input));
            } catch (ParseException e) {
                if (s.length() == 2 && !mLastInput.endsWith("/")) {
                    int month = Integer.parseInt(input);
                    if (month <= 12) {
                        mExpiryDate.setText(mExpiryDate.getText().toString() + "/");
                        mExpiryDate.setSelection(mExpiryDate.getText().toString().length());
                    }else{
                        mExpiryDate.setText(mExpiryDate.getText().toString().substring(0,1));
                        mExpiryDate.setSelection(mExpiryDate.getText().toString().length());
                    }
                } else if (s.length() == 2 && mLastInput.endsWith("/")) {
                    int month = Integer.parseInt(input);
                    if (month <= 12) {
                        mExpiryDate.setText(mExpiryDate.getText().toString().substring(0, 1));
                        mExpiryDate.setSelection(mExpiryDate.getText().toString().length());
                    } else {
                        mExpiryDate.setText("");
                        mExpiryDate.setSelection(mExpiryDate.getText().toString().length());
                    }
                } else if (s.length() == 1) {
                    int month = Integer.parseInt(input);
                    if (month > 1) {
                        mExpiryDate.setText("0" + mExpiryDate.getText().toString() + "/");
                        mExpiryDate.setSelection(mExpiryDate.getText().toString().length());
                    }
                } else {

                }
                mLastInput = mExpiryDate.getText().toString();
                return;
            }
        }
    };

    /**
     * On Submit Payment Listener
     */
    private View.OnClickListener mPayClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            mErrorMessage.setVisibility(View.GONE);
            if(mCreditCard.getText().toString().length() <= 0){
                mCreditCard.setError(getString(R.string.__stripe_invalidate_card_number));
                return;
            }
            if(mCVC.getText().toString().length() <= 0){
                mCVC.setError(getString(R.string.__stripe_invalidate_cvc));
                return;
            }
            if(mExpiryDate.getText().toString().length() <= 0){
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
            if(mmCard.validateCard()) {
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
            }else if (!mmCard.validateNumber()) {
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

}
