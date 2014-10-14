
package org.solarex.getip;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.widget.TextView;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.conn.util.InetAddressUtils;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.URL;
import java.util.Collections;
import java.util.List;

public class MainActivity extends Activity {
    private TextView tv_external, tv_internal, mac;
    private String external, internal, macAddress;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        tv_external = (TextView) this.findViewById(R.id.tv_external);
        tv_internal = (TextView) this.findViewById(R.id.tv_internal);
        mac = (TextView) this.findViewById(R.id.mac);

        if (!isNetworkConnectd(this)) {
            String networkMsg = "Network not connected.";
            tv_external.setText("External IP: " + networkMsg);
            tv_internal.setText("Internal IP: " + networkMsg);
        } else {
            try {
                new GetIPTask().execute(new URL("http://ifconfig.me/all.json"));
            } catch (MalformedURLException e) {
                Utils.log("Exception happened, ex = " + e.getMessage());
            }
        }
    }

    private boolean isNetworkConnectd(Context context) {
        boolean isConnected = false;
        ConnectivityManager cm =
                (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);

        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
        isConnected = activeNetwork != null &&
                activeNetwork.isConnectedOrConnecting();
        return isConnected;
    }

    private class GetIPTask extends AsyncTask<URL, Integer, HttpResponse> {
        private ProgressDialog progressDialog = new ProgressDialog(MainActivity.this);

        @Override
        protected HttpResponse doInBackground(URL... params) {
            HttpResponse response = null;
            // String url = "http://ifconfig.me/all.json";
            try {
                HttpClient client = new DefaultHttpClient();
                HttpGet request = new HttpGet(params[0].toString());
                response = client.execute(request);
            } catch (Exception e) {
                Utils.log("Exception happened, ex = " + e.getMessage());
            }

            try {
                List<NetworkInterface> interfaces = Collections.list(NetworkInterface
                        .getNetworkInterfaces());
                for (NetworkInterface networkInterface : interfaces) {
                    List<InetAddress> inetAddresses = Collections.list(networkInterface
                            .getInetAddresses());
                    for (InetAddress inetAddress : inetAddresses) {
                        Utils.log(inetAddress.toString());
                        if (!inetAddress.isLoopbackAddress()
                                && InetAddressUtils.isIPv4Address(inetAddress.getHostAddress())) {
                            internal = inetAddress.getHostAddress().toUpperCase();
                        }
                    }
                }
                for (NetworkInterface networkInterface : interfaces) {
                    Utils.log(networkInterface.toString());
                    byte[] macArray = networkInterface.getHardwareAddress();
                    if (macArray != null) {
                        StringBuilder sBuilder = new StringBuilder();
                        for (int i = 0; i < macArray.length; i++) {
                            sBuilder.append(String.format("%02X:", macArray[i]));
                        }
                        Utils.log(sBuilder.toString());
                        if (sBuilder.length() > 0) {
                            sBuilder.deleteCharAt(sBuilder.length() - 1);
                        }
                        macAddress = sBuilder.toString();
                    }
                }
            } catch (SocketException e) {
                Utils.log("Exception happened, ex = " + e.getMessage());
            }

            return response;
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            progressDialog.setMessage("Getting data from web......");
            progressDialog.setCancelable(false);
            progressDialog.show();
        }

        @Override
        protected void onPostExecute(HttpResponse result) {
            super.onPostExecute(result);
            try {
                if (null != result) {
                    BufferedReader reader = new BufferedReader(new InputStreamReader(result
                            .getEntity()
                            .getContent()));
                    String line = null;
                    StringBuilder sBuilder = new StringBuilder();
                    while ((line = reader.readLine()) != null) {
                        sBuilder.append(line);
                    }
                    reader.close();
                    try {
                        JSONObject jsonObject = new JSONObject(sBuilder.toString());
                        external = jsonObject.getString("ip_addr");
                    } catch (JSONException e) {
                        Utils.log("Exception happened, ex = " + e.getMessage());
                    }
                } else {
                    external = "External IP: " + " Network Exception happened.";
                }

            } catch (IllegalStateException e) {
                Utils.log("Exception happened, ex = " + e.getMessage());
            } catch (IOException e) {
                Utils.log("Exception happened, ex = " + e.getMessage());
            }
            tv_external.setTextSize(20);
            tv_internal.setTextSize(20);
            mac.setTextSize(20);
            tv_external.setText("External IP: " + external);
            tv_internal.setText("Internal IP: " + internal);
            mac.setText("MAC Address: " + macAddress);
            progressDialog.dismiss();
        }
    }
}
