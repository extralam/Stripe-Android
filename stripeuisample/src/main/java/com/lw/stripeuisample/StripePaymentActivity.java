package com.lw.stripeuisample;

import android.app.Dialog;
import android.os.Bundle;
import android.util.Log;
import android.view.View;

import com.lw.stripe.StripePaymentDialog;

public class StripePaymentActivity extends StripeBaseActivity {

    @Override
    public int getLayout() {
        return R.layout.activity_stripe_payment;
    }

    @Override
    public void initView(Bundle savedInstanceState) {

    }

    public void onClick(View v) {
        stripeTest();
    }

    @Override
    public void init(Bundle savedInstanceState) {
        stripeTest();
    }

    private void stripeTest() {
        String mDefaultPublishKey = "pk_test_7djDh48O7jQH8ZIbaVr12IER";
        StripePaymentDialog.show(
                getSupportFragmentManager(),
                mDefaultPublishKey,
                "",
                0,
                "https://lh3.googleusercontent.com/R-vJInTblK1KBOqZaSDm_ac270QBHsiIcU9agHnN-rrp9K_lkN8rLzGIH8asCfkb420Q=s512-rw",
                "Your Shop Name Limited",
                "$100 Movie Ticket",
                "Extremely long message to test message clipping and text everything to go wrong should go wrong!",
                false,
                new StripePaymentDialog.OnStripePaymentDismissListener() {
                    @Override
                    public void onSuccess(Dialog dialog, String id) {
                        Log.d("Stripe Success", "Object ID : " + id);
                    }
                    @Override
                    public void onDismiss() {
                        Log.d("Stripe Dismiss", "Dismissed");
                    }
                });
    }
}