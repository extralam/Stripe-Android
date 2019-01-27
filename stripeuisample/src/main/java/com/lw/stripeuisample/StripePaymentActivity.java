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
        String mDefaultPublishKey = "pk_test_your_code";
        StripePaymentDialog.show(
                getSupportFragmentManager(),
                mDefaultPublishKey,
                "test@test.com",
                0,
                "https://stripe.com/img/about/logos/logos/black.png",
                "Your Shop Name Limited",
                "$100 Movie Ticket",
                "Extremely long message to test message clipping and text everything to go wrong should go wrong!",
                true,
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