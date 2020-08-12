package com.vkpdeveloper.PaytmPayment;

import com.google.appinventor.components.annotations.UsesLibraries;
import com.google.appinventor.components.annotations.*;
import com.google.appinventor.components.annotations.androidmanifest.ActivityElement;
import com.google.appinventor.components.common.*;
import android.content.Context;
import com.google.appinventor.components.runtime.*;
import com.paytm.pgsdk.*;
import android.app.Activity;
import android.app.ProgressDialog;
import android.os.AsyncTask;
import android.os.Bundle;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import android.app.Activity;
import android.util.Log;
import org.json.JSONException;
import org.json.JSONObject;
import com.vkpdeveloper.PaytmPayment.JSONParser;

/*
    @author vkpdeveloper (Vaibhav Pahtak)
 */
@DesignerComponent(version = 1,
        description = "Paytm Extension by Vaibhav Pathak with Custom Amount",
        designerHelpDescription = "Paytm Extension by Vaibhav Pathak with Custom Amount",
        category = ComponentCategory.EXTENSION,
        nonVisible = true,
        androidMinSdk = 21,
        iconName = "")
@SimpleObject(external = true)
@UsesPermissions(permissionNames = "android.permission.INTERNET, android.permission.ACCESS_NETWORK_STATE")
@UsesLibraries(libraries = "paytm.jar")
@UsesActivities(activities = {@ActivityElement(configChanges = "keyboardHidden|orientation|screenSize", name="com.paytm.pgsdk.PaytmPGActivity", screenOrientation="portrait")})

public class PaytmPayment extends AndroidNonvisibleComponent implements Component {
    public static final String LOG_TAG = "PaytmPayment";
    private final Activity activity;
    String channelID = "WAP";
    private ComponentContainer container;
    private PaytmOrder order;
    private Context context;
    String mid = "";
    String txnAmount = "";
    String orderId = "";
    String checksumURL = "";
    String callbackURL = "";
    String custId = "";
    String industryTypeID = "Retail";
    String website = "WEBSTAGING";
    String CHECKSUMHASH = "";
    static String json = "";

    public PaytmPayment(ComponentContainer container) {
        super(container.$form());
        this.container = container;
        this.activity = container.$context();
        this.context = (Context) container.$context();
    }


    @DesignerProperty(defaultValue = "YOUR MID", editorType = "string")
    @SimpleProperty(userVisible = false)
    public void MID(String value) {
        String trim = value.trim();
        this.mid = trim;
    }

    @DesignerProperty(defaultValue = "WEBSTAGING", editorType = "string")
    @SimpleProperty(userVisible = false)
    public void Website(String value) {
        String trim = value.trim();
        this.website = trim;
    }

    @DesignerProperty(defaultValue = "Checksum Generator URL", editorType = "string")
    @SimpleProperty(userVisible = false)
    public void ChecksumURL(String value) {
        String trim = value.trim();
        this.checksumURL = trim;
    }

    @DesignerProperty(defaultValue = "Callback URL", editorType = "string")
    @SimpleProperty(userVisible = false)
    public void CallbackURL(String value) {
        String trim = value.trim();
        this.callbackURL = trim;
    }

    @DesignerProperty(defaultValue = "Retail", editorType = "string")
    @SimpleProperty(userVisible = false)
    public void IndustryType(String value) {
        String trim = value.trim();
        this.industryTypeID = trim;
    }

    @SimpleFunction
     public void setPaymentDetails(String orderId, String customerId, String amount) throws IOException {
        this.orderId = orderId;
        this.custId = customerId;
        this.txnAmount = amount;
    }


    public class sendUserDetailTOServerdd extends AsyncTask<ArrayList<String>, Void, String> {

        private ProgressDialog dialog = new ProgressDialog(activity);

        String url = checksumURL;
        String varifyurl = callbackURL;
        String CHECKSUMHASH = "";

        protected void onPreExecute() {
            this.dialog.setMessage("Please wait");
            this.dialog.show();
        }

        protected String doInBackground(ArrayList<String>... alldata) {
            JSONParser jsonParser = new JSONParser(container);
            String param=
                    "MID="+mid+
                            "&ORDER_ID=" + orderId+
                            "&CUST_ID="+custId+
                            "&CHANNEL_ID=WAP&TXN_AMOUNT="+txnAmount+"&WEBSITE="+website+
                            "&CALLBACK_URL="+ varifyurl+"?ORDER_ID="+orderId+"&INDUSTRY_TYPE_ID="+industryTypeID;

            JSONObject jsonObject = jsonParser.makeHttpRequest(url,"POST",param);
            Log.e("CheckSum result >>",jsonObject.toString());
            if(jsonObject != null){
                Log.e("CheckSum result >>",jsonObject.toString());
                try {
                    CHECKSUMHASH=jsonObject.has("CHECKSUMHASH")?jsonObject.getString("CHECKSUMHASH"):"";
                    Log.e("CheckSum result >>",CHECKSUMHASH);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
            return CHECKSUMHASH;
        }

        protected void onPostExecute(String result) {
            if (dialog.isShowing()) {
                dialog.dismiss();
            }

            PaytmPGService service = PaytmPGService.getProductionService();
            HashMap<String, String> paramMap = new HashMap<String, String>();
            paramMap.put("MID", mid);
            paramMap.put("ORDER_ID", orderId);
            paramMap.put("CUST_ID", custId);
            paramMap.put("CHANNEL_ID", "WAP");
            paramMap.put("TXN_AMOUNT", txnAmount);
            paramMap.put("WEBSITE", website);
            paramMap.put("CALLBACK_URL" ,varifyurl);
            paramMap.put("CHECKSUMHASH" ,CHECKSUMHASH);
            paramMap.put("INDUSTRY_TYPE_ID", industryTypeID);

            order = new PaytmOrder(paramMap);
            Log.e("checksum ", "param "+ paramMap.toString());
            service.initialize(order,null);
            service.startPaymentTransaction(activity, true, true, new PaytmPaymentTransactionCallback() {
                public void onTransactionResponse(Bundle bundle) {
                    Log.e(LOG_TAG, "onTransactionResponse");
                }

                public void networkNotAvailable() {
                    Log.e(LOG_TAG, "networkNotAvailable");
                }

                public void clientAuthenticationFailed(String s) {
                    Log.e(LOG_TAG, "clientAuthFail");
                }

                public void someUIErrorOccurred(String s) {
                    Log.e(LOG_TAG, "someUIErrorOccured");
                }

                public void onErrorLoadingWebPage(int i, String s, String s1) {
                    Log.e(LOG_TAG, "onErrorLoadingPage");
                }

                public void onBackPressedCancelTransaction() {
                    Log.e(LOG_TAG, "onBackPressed");
                }

                public void onTransactionCancel(String s, Bundle bundle) {
                    Log.e(LOG_TAG, "onTransactionCancel");
                }
            });


        }

    }


    @SimpleFunction
    public void startPayment() {
        startNewPayment();
    }

    void startNewPayment() {
        sendUserDetailTOServerdd dl = new sendUserDetailTOServerdd();
        dl.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }

}

