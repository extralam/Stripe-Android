[![](https://jitpack.io/v/extralam/Stripe-Android.svg)](https://jitpack.io/#extralam/Stripe-Android)
# Stripe-Android
Android Stripe UI
- Implement A Stripe UI Helper

## ScreenShot
<br>
<img height="700" src="https://github.com/extralam/Stripe-Android/blob/master/screenshot/sample_screenshot.jpg?raw=true"/>
<br>
# Video ScreenShot
<img width="800" src="https://github.com/extralam/Stripe-Android/blob/master/screenshot/screenshot_vide.gif?raw=true"/>
<br>

Add it in your root build.gradle at the end of repositories:


##Gradle
- Step 1. Add the JitPack repository to your build file
- Add it in your root build.gradle at the end of repositories:
```java
allprojects {
	repositories {
		...
		maven { url "https://jitpack.io" }
	}
}
```
- Step 2. Add the dependency
```java
compile 'compile 'com.github.extralam:Stripe-Android:1.0.0''
```

## Sample Code
```java
String mDefaultPublishKey = "pk_test_your_code";
StripePaymentDialog.show(
        getSupportFragmentManager(),
        mDefaultPublishKey,
        "test@test.com",
        "https://stripe.com/img/about/logos/logos/black.png",
        "Your Shop Name Limited",
        "$100 Movie Ticket",
        "hkd",
        100,
        new StripePaymentDialog.OnStripePaymentDismissListener() {
            @Override
            public void onSuccess(Dialog mmDialog, Token mmToken) {
                Log.d("atest","id : " + mmToken.getId());
            }
        });
```

## About me

An android developer in Hong Kong.
