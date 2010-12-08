package nz.co.wholemeal.christchurchmetro;

import android.util.Log;

import java.util.List;
import java.util.ArrayList;
import java.io.IOException;

import org.json.JSONObject;
import org.json.JSONTokener;
import org.json.JSONException;
import org.json.JSONArray;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.params.HttpParams;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.NameValuePair;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;

/*
 * Represents a bus stop
 */

class Stop {

  public static String gisURL = "http://arcgis.ecan.govt.nz/ArcGIS/rest/services/Beta/Bus_Routes/MapServer/2/query";
  public static String etaURL = "http://rtt.metroinfo.org.nz/RTT/Public/RoutePositionET.aspx";

  private String name;
  private String platformTag;
  private String platformNumber;
  private String roadName;
  private String routes;
  private double latitude;
  private double longitude;

  /* Instantiate a Stop from a stop number */
  public Stop(String stopNumber) {
    JSONObject json = getJSONForStopNumber(stopNumber);
    try {
      JSONArray features = json.getJSONArray("features");
      JSONObject attributes = features.getJSONObject(0).getJSONObject("attributes");
      name = attributes.getString("Name");
      platformTag = attributes.getString("PlatformTa");
      platformNumber = attributes.getString("PlatformNo");
      roadName = attributes.getString("RoadName");
      routes = attributes.getString("Routes");
      latitude = attributes.getDouble("Lat");
      longitude = attributes.getDouble("Long");
    } catch (JSONException e) {
    }
  }

  private JSONObject getJSONForStopNumber(String stopNumber) {
    HttpPost httppost = new HttpPost(gisURL);
    HttpClient httpclient = new DefaultHttpClient();
    String body = null;
    JSONObject json = null;

    try {
      List<NameValuePair> formparams = new ArrayList<NameValuePair>(2);
      // Casino = 30846
      formparams.add(new BasicNameValuePair("where", "PlatformNo=" + stopNumber));
      formparams.add(new BasicNameValuePair("outfields", "Name,PlatformTa,RoadName,PlatformNo,Routes,Lat,Long"));
      formparams.add(new BasicNameValuePair("f", "pjson"));
      UrlEncodedFormEntity entity = new UrlEncodedFormEntity(formparams, "UTF-8");
      httppost.setEntity(entity);

      HttpResponse response = httpclient.execute(httppost);
      body = EntityUtils.toString(response.getEntity());
    } catch (IOException e) {
      Log.e("ChristchurchMetro", "IOException: " + e);
    }

    try {
      json = (JSONObject) new JSONTokener(body).nextValue();
    } catch (JSONException e) {
      Log.e("ChristchurchMetro", "JSONException: " + e);
    }
    return json;
  }

  public String getName() {
    return name;
  }

  public String getPlatformTag() {
    return platformTag;
  }

  public String getPlatformNumber() {
    return platformNumber;
  }

  public String getEtaHtml(int limit) {
    HttpGet httpget = new HttpGet(getEtaUrl(10));
    HttpClient httpclient = new DefaultHttpClient();
    String body = null;

    try {
      HttpResponse response = httpclient.execute(httpget);
      body = EntityUtils.toString(response.getEntity());
    } catch (IOException e) {
      Log.e("ChristchurchMetro", "IOException: " + e);
    }
    return body;
  }

  public String getEtaUrl(int limit) {
    return etaURL + "?MaxETRows=" + limit + "&PlatformTag=" + getPlatformTag();
  }
}
