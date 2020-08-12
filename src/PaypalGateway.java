package com.vkpdeveloper.PaypalGateway;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.content.Context;

import com.google.appinventor.components.annotations.UsesLibraries;
import com.google.appinventor.components.annotations.*;
import com.google.appinventor.components.annotations.androidmanifest.ActivityElement;
import com.google.appinventor.components.runtime.*;
import com.google.appinventor.components.common.ComponentCategory;

import com.paypal.android.sdk.payments.PayPalConfiguration;
import com.paypal.android.sdk.payments.PayPalPayment;
import com.paypal.android.sdk.payments.PayPalService;
import com.paypal.android.sdk.payments.PaymentActivity;
import com.paypal.android.sdk.payments.PaymentConfirmation;

import org.json.JSONException;
import java.math.BigDecimal;

@DesignerComponent(version = 1, description = "", category = ComponentCategory.EXTENSION, nonVisible = true, iconName = "https://res.cloudinary.com/dfozqceqg/image/upload/v1581122053/paypal.png")
@SimpleObject(external = true)
@UsesPermissions(permissionNames = "android.permission.WRITE_EXTERNAL_STORAGE, android.permission.INTERNET, android.permission.ACCESS_NETWORK_STATE")
@UsesLibraries(libraries="paypal.jar")
@UsesActivities(activities = {@ActivityElement(configChanges = "keyboardHidden|orientation|screenSize", name="com.paypal.android.sdk.payments.PaymentActivity", screenOrientation="portrait"), @ActivityElement(name="com.paypal.android.sdk.payments.PayPalService",exported="false"), @ActivityElement(configChanges = "keyboardHidden|orientation|screenSize", name="com.paypal.android.sdk.payments.LoginActivity", screenOrientation="portrait"), @ActivityElement(configChanges = "keyboardHidden|orientation|screenSize", name="com.paypal.android.sdk.payments.PaymentConfirmActivity", screenOrientation="portrait"), @ActivityElement(configChanges = "keyboardHidden|orientation|screenSize", name="com.paypal.android.sdk.payments.PaymentMethodActivity", screenOrientation="portrait")})

public class PaypalGateway extends AndroidNonvisibleComponent {

    private final ComponentContainer container;
    private Context context;
    private String clientId = "";
    private static final int PAYPAL_REQUEST_CODE = 123;
    String paymentAmount = "";
    private Activity activity;
    private static PayPalConfiguration config = new PayPalConfiguration();

    public PaypalGateway(ComponentContainer container) {
        super(container.$form());
        this.container = container;
        context = (Context)container.$context();
        this.activity = container.$context();
    }

    @SimpleFunction(description = "Configure the PayPal Payment Gateway by passing clientId and ENVIRONMENT")
    public void ConfigPaypal(String clientid, String env, String merchantname) {
        this.clientId = clientid;
        if (env == "live") {
            config.environment(PayPalConfiguration.ENVIRONMENT_PRODUCTION).clientId(this.clientId)
                    .merchantName(merchantname);
        } else if (env == "sandbox") {
            config.environment(PayPalConfiguration.ENVIRONMENT_SANDBOX).clientId(this.clientId)
                    .merchantName(merchantname);
        }
    }

    @SimpleFunction(description = "Start Payment Gateway by passing amount, currencyType and description for Payment")
    public void StartPayment(String amount, String currency, String description) {
        this.paymentAmount = amount;
        Intent intent = new Intent(this.context, PayPalService.class);
        intent.putExtra(PayPalService.EXTRA_PAYPAL_CONFIGURATION, config);
        this.form.startService(intent);
        getPayment(amount, currency, description, context);

    }

    public void getPayment(String amount, String currency, String description, Context context) {
        PayPalPayment payment = new PayPalPayment(new BigDecimal(String.valueOf(amount)), currency, description,
                PayPalPayment.PAYMENT_INTENT_SALE);
        Intent intent2 = new Intent(this.context, PaymentActivity.class);
        intent2.putExtra(PayPalService.EXTRA_PAYPAL_CONFIGURATION, config);
        intent2.putExtra(PaymentActivity.EXTRA_PAYMENT, payment);
        this.form.startActivityForResult(intent2, PAYPAL_REQUEST_CODE);
    }

    private void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == PAYPAL_REQUEST_CODE) {
            if (resultCode == Activity.RESULT_OK) {
                PaymentConfirmation confirm = data.getParcelableExtra(PaymentActivity.EXTRA_RESULT_CONFIRMATION);
                if (confirm != null) {
                    try {
                        String paymentDetails = confirm.toJSONObject().toString(4);
                        OnPaymentSuccessful(paymentDetails);

                    } catch (JSONException e) {
                        OnPaymentFailed(e.toString());
                    }
                }
            } else if (resultCode == Activity.RESULT_CANCELED) {
                OnPaymentCancelled();
            } else if (resultCode == PaymentActivity.RESULT_EXTRAS_INVALID) {
                OnConfigError();
            }
        }
    }

    @SimpleEvent(description = "Event for OnPaymentCancelled()")
    public void OnPaymentCancelled() {
        EventDispatcher.dispatchEvent(this, "OnPaymentCancelled");
    }

    @SimpleEvent(description = "Event for OnPaymentFailed()")
    public void OnPaymentFailed(String errorMessage) {
        EventDispatcher.dispatchEvent(this, "OnPaymentSuccessful", errorMessage);
    }

    @SimpleEvent(description = "Event for OnConfigError()")
    public void OnConfigError() {
        EventDispatcher.dispatchEvent(this, "OnConfigError");
    }

    @SimpleEvent(description = "Event for OnPaymentSuccessful()")
    public void OnPaymentSuccessful(String successDetails) {
        EventDispatcher.dispatchEvent(this, "OnPaymentSuccessful", successDetails);
    }

}
