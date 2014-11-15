package com.mit.mittunnels;

import java.util.ArrayList;
import java.util.List;

import com.parse.FindCallback;
import com.parse.Parse;
import com.parse.ParseException;
import com.parse.ParseObject;
import com.parse.ParseQuery;
import com.parse.SaveCallback;

import android.support.v7.app.ActionBarActivity;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;


public class MainActivity extends ActionBarActivity {
	float currentx, currenty;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        //Parse initialize
        Parse.initialize(this, "dqqy2pgILHlRNjtKu5MLi4VyY0LkZJY27FgrufLh", "fSFmvqwVMeuhsuzavaa7KJAMg5XDPhN0CcjTDiFE");
        
        Button btnSaveLocation = (Button) findViewById(R.id.btnSaveLocation);
        Button btnGetLocation = (Button) findViewById(R.id.btnGetLocation);
        final ImageView imageView = (ImageView) findViewById(R.id.imgMap);
		
        
        
        btnSaveLocation.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                // Do something in response to button click
            	
            	
            	//get list of network
            	
            	
            	
            	
            	final WifiManager wifiManager = (WifiManager) getSystemService(Context.WIFI_SERVICE);
                registerReceiver(new BroadcastReceiver() {
                        @Override
                        public void onReceive(Context c, Intent intent) 
                        {
                           List<ScanResult> results = wifiManager.getScanResults();
                           for (ScanResult ap : results) {
                               
                        	   ParseObject testObject = new ParseObject("ScanResult");
                        	   testObject.put("x", Float.toString(currentx));
                        	   testObject.put("y", Float.toString(currenty));
                        	   testObject.put("SSID", ap.SSID);
                        	   testObject.put("MAC", ap.BSSID);
                        	   testObject.put("LEVEL", ap.level);
                        	   testObject.saveInBackground();
                        	   Log.d("", "SSID=" + ap.SSID + " MAC=" + ap.BSSID+" LEVEL="+ap.level); 
                           }
                        }

        				
                }, new IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION)); 
                wifiManager.startScan();
            	
            	
            
            }
        });
        

       
        
        
        
        
        imageView.setOnTouchListener(new View.OnTouchListener() {               
            

			@Override
			public boolean onTouch(View v, MotionEvent event) {
		
				Bitmap bmp =  Bitmap.createBitmap(imageView.getMeasuredWidth(), imageView.getMeasuredHeight(), Config.ARGB_8888);
				imageView.setBackgroundResource(R.drawable.statafloorplan);
				Canvas  c = new Canvas(bmp);

	            Paint p = new Paint();
	            int color = Color.WHITE;
	            p.setColor(color);

	            color = Color.RED;
	            p.setColor(color);
	            currentx = event.getX();
	            currenty = event.getY();
	            c.drawCircle(event.getX(), event.getY(), 10, p);

	            imageView.setImageBitmap(bmp);
				
				
				
				
				
				int action = event.getAction();
		            switch (action) {

		            case MotionEvent.ACTION_DOWN:
		                Log.d(Float.toString(event.getRawX()),Float.toString(event.getRawY()));                                            
		                break;
		            }
				return true;
			}
            
            });
        
        
        btnGetLocation.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                // Do something in response to button click
            	
            	
            
            	
            	final ArrayList<AccessPoint> arrayList = new ArrayList<AccessPoint>();
            	
            	final WifiManager wifiManager = (WifiManager) getSystemService(Context.WIFI_SERVICE);
                registerReceiver(new BroadcastReceiver() {
                        @Override
                        public void onReceive(Context c, Intent intent) 
                        {
                           List<ScanResult> results = wifiManager.getScanResults();
                           for (ScanResult ap : results) {
                               
                        	   
                        	   AccessPoint app = new AccessPoint();
                        	   app.MAC = ap.BSSID;
                        	   app.LEVEL = ap.level;
                        	   arrayList.add(app);
                        	   
                        	   
                        	   
                           }
                       /*    try {
							wifiManager.wait();
						} catch (InterruptedException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}*/
                           
                           //sort list
                           int size = arrayList.size();
                           for (int i=0; i< arrayList.size()-1; i++)
                        	   for (int j=0; j< arrayList.size()-1; j++)
                        	   {
                        		   
                        		   AccessPoint app1 = arrayList.get(i);
                        		   AccessPoint app2 = arrayList.get(i+1);
                        		   
                        		   if (app2.LEVEL<app1.LEVEL)
                        		   {
                        			   AccessPoint toMove = arrayList.get(i);
                        			    arrayList.set(i, arrayList.get(i-1));
                        			    arrayList.set(i-1, toMove);
                        		   }
                        		   
                        	   }
                           
                           //find in the cloud
                           int a=1;
                           a=2;
                           String tempm = arrayList.get(0).MAC;
                       /*	ParseQuery<ParseObject> query = ParseQuery.getQuery("ScanResults");
                    	query.whereEqualTo("MAC", arrayList.get(0).MAC);*/
                    	
                    	ParseQuery<ParseObject> query = ParseQuery.getQuery("ScanResult");
                    	query.whereEqualTo("MAC", tempm);
                    	
                    	query.findInBackground(new FindCallback<ParseObject>() {
                    	    public void done(List<ParseObject> scoreList, ParseException e) {
                    	        if (e == null) {
                    	            Log.d("score", "Retrieved " + scoreList.get(0).getString("LOCATION"));
                    	        } else {
                    	            Log.d("score", "Error: " + e.getMessage());
                    	        }
                    	    }
                    	});
                    	
                           
                           /*AlertDialog.Builder builder1 = new AlertDialog.Builder(getBaseContext());
                           builder1.setMessage("Write your message here.");
                           builder1.setCancelable(true);
                           builder1.setPositiveButton("Yes",
                                   new DialogInterface.OnClickListener() {
                               public void onClick(DialogInterface dialog, int id) {
                                   dialog.cancel();
                               }
                           });
                           builder1.setNegativeButton("No",
                                   new DialogInterface.OnClickListener() {
                               public void onClick(DialogInterface dialog, int id) {
                                   dialog.cancel();
                               }
                           });

                           AlertDialog alert11 = builder1.create();
                           alert11.show();*/
                        }

        				
                }, new IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION)); 
                wifiManager.startScan();
            	
            	
            	
            	
            	
            	
            	
            }
        });
        
        
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
