/*
 * Copyright (C) 2013 The Android Open Source Project
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

package com.example.android.cardreader;

import android.app.Activity;
import android.app.ProgressDialog;
import android.nfc.NfcAdapter;
import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.widget.*;
import android.os.Handler;
import android.speech.tts.TextToSpeech;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.VolleyLog;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.example.android.common.logger.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.util.*;
/**
 * Generic UI for sample discovery.
 */
public class CardReaderFragment extends Fragment implements LoyaltyCardReader.AccountCallback {

    public static final String TAG = "CardReaderFragment";
    public static final String Token = "$1$KanqUlDe$iWxHE5IaqsZAC6x6nqKSt1";
    public static final String url = "https://cs427smartdoorapi.000webhostapp.com/index.php/key/verify";
    private TextToSpeech tts;
    // Recommend NfcAdapter flags for reading from other Android devices. Indicates that this
    // activity is interested in NFC-A devices (including other Android devices), and that the
    // system should not check for the presence of NDEF-formatted data (e.g. Android Beam).
    public static int READER_FLAGS =
            NfcAdapter.FLAG_READER_NFC_A | NfcAdapter.FLAG_READER_SKIP_NDEF_CHECK;
    public LoyaltyCardReader mLoyaltyCardReader;
    private TextView mAccountField;
    private ImageView img;
    private RequestQueue mRequestQueue;
    /** Called when sample is created. Displays generic UI with welcome text. */
    @Override
    public void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
    }



    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View v = inflater.inflate(R.layout.main_fragment, container, false);
        Snackbar snackbar = Snackbar
                .make(v, "Welcome to out project", Snackbar.LENGTH_LONG);
        snackbar.show();
        mRequestQueue = Volley.newRequestQueue(getActivity());

        if (v != null) {
             img = (ImageView) v.findViewById(R.id.lock_representation);
            mAccountField = (TextView) v.findViewById(R.id.card_account_field);
            mAccountField.setText("Waiting...");

            Animation anim = new AlphaAnimation(0.0f, 1.0f);
            anim.setDuration(50); //You can manage the blinking time with this parameter
            anim.setStartOffset(20);
            anim.setRepeatMode(Animation.REVERSE);
            anim.setRepeatCount(Animation.INFINITE);
            mAccountField.startAnimation(anim);
            mLoyaltyCardReader = new LoyaltyCardReader(this);


            // Disable Android Beam and register our card reader callback
            enableReaderMode();
        }
        tts = new TextToSpeech(getActivity(),new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int status) {
                if (status != TextToSpeech.ERROR) {
                    tts.setLanguage(Locale.UK);
                }
            }
        });
        return v;
    }

    @Override
    public void onPause() {
        super.onPause();
        disableReaderMode();
    }

    @Override
    public void onResume() {
        super.onResume();
        enableReaderMode();
    }

    private void enableReaderMode() {
        Log.i(TAG, "Enabling reader mode");
        Activity activity = getActivity();
        NfcAdapter nfc = NfcAdapter.getDefaultAdapter(activity);
        if (nfc != null) {
            nfc.enableReaderMode(activity, mLoyaltyCardReader, READER_FLAGS, null);
        }
    }

    private void disableReaderMode() {
        Log.i(TAG, "Disabling reader mode");
        Activity activity = getActivity();
        NfcAdapter nfc = NfcAdapter.getDefaultAdapter(activity);
        if (nfc != null) {
            nfc.disableReaderMode(activity);
        }
    }

    private void makeServerRequest(final String account) throws JSONException {

        JSONObject jsonBody = new JSONObject();

        jsonBody.put("room_number", "31");
        jsonBody.put("access_key", account);
        final String requestBody = jsonBody.toString();

        final ProgressDialog pDialog = new ProgressDialog(getActivity());
        pDialog.setMessage("Checking Key...");
        pDialog.show();

        JsonObjectRequest jsonObjReq = new JsonObjectRequest(Request.Method.POST,
                url, null,
                new Response.Listener<JSONObject>() {


                    @Override
                    public void onResponse(JSONObject response) {
                        Log.d(TAG, response.toString());
                       // pDialog.setMessage(response.toString());
                        int id;
                        pDialog.hide();

                        try {
                            if(response.get("message").toString().trim().equals("Key Verified.")){
                                id = R.drawable.unlocked;
                                mAccountField.setText("Access Granted");
                                tts.speak("Access Granted", TextToSpeech.QUEUE_FLUSH, null);
                            }else{
                                id = R.drawable.locked;
                                mAccountField.setText("Access Denied");
                                tts.speak("Access Denied", TextToSpeech.QUEUE_FLUSH, null);
                            }

                            Handler handler = new Handler();
                            handler.postDelayed(new Runnable() {
                                @Override
                                public void run() {
                                    mAccountField.setText("Waiting...");
                                    img.setImageResource(R.drawable.locked);                    }
                            }, 2000);
                        } catch (JSONException e) {
                            e.printStackTrace();
                            id = R.drawable.locked;
                            mAccountField.setText("Access Denied");
                            tts.speak("Access Denied", TextToSpeech.QUEUE_FLUSH, null);

                            Handler handler = new Handler();
                            handler.postDelayed(new Runnable() {
                                @Override
                                public void run() {
                                    mAccountField.setText("Waiting...");
                                    img.setImageResource(R.drawable.locked);                    }
                            }, 2000);
                        }


                        img.setImageResource(id);


                    }
                }, new Response.ErrorListener() {

            @Override
            public void onErrorResponse(VolleyError error) {
                //pDialog.setMessage(error.getMessage());
                pDialog.hide();
                img.setImageResource(R.drawable.locked);
                mAccountField.setText("Access Denied");
                tts.speak("Access Denied", TextToSpeech.QUEUE_FLUSH, null);

                Handler handler = new Handler();
                handler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        mAccountField.setText("Waiting...");
                        img.setImageResource(R.drawable.locked);                    }
                }, 2000);
            }
        }) {

            @Override
            public Map<String, String> getHeaders() throws AuthFailureError {
                HashMap<String, String> headers = new HashMap<String, String>();
                headers.put("Content-Type", "application/json");
                headers.put("client-service", "frontend-client");
                headers.put("auth-key", "simplerestapi");
                headers.put("user-id", "1");
                headers.put("authorization", Token);
                return headers;
            }

            @Override
            public byte[] getBody() {
                try {
                    return requestBody == null ? null : requestBody.getBytes("utf-8");
                } catch (UnsupportedEncodingException uee) {
                    VolleyLog.wtf("Unsupported Encoding while trying to get the bytes of %s using %s", requestBody, "utf-8");
                    return null;
                }
            }

        };

        mRequestQueue.add(jsonObjReq);

    }



    @Override
    public void onAccountReceived(final String account) {
        // This callback is run on a background thread, but updates to UI elements must be performed
        // on the UI thread.
        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {

                /*
                int id;
                if(account.trim().equals("12345") ){
                    id = R.drawable.unlocked;
                    //    img.setImageResource(R.drawable.unlocked);
                }else{
                    id = R.drawable.locked;
                    //    img.setImageResource(R.drawable.locked);
                }
                ImageView img= getActivity().findViewById(R.id.lock_representation);*/

                try {
                    makeServerRequest(account.trim());
                } catch (JSONException e) {
                    e.printStackTrace();
                }




            }
        });
    }
}
