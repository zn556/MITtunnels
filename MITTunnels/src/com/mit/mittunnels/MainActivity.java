package com.mit.mittunnels;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import javax.security.auth.callback.Callback;

import com.larvalabs.svgandroid.SVG;
import com.larvalabs.svgandroid.SVGParser;
import com.parse.FindCallback;
import com.parse.Parse;
import com.parse.ParseException;
import com.parse.ParseObject;
import com.parse.ParseQuery;
import com.parse.SaveCallback;

import android.support.v7.app.ActionBarActivity;
import android.annotation.TargetApi;
import android.app.AlertDialog;
import android.app.PendingIntent.OnFinished;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.PictureDrawable;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.Display;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

public class MainActivity extends ActionBarActivity {

	private static final int SWIPE_THRESHOLD = 5;
	private static final int SWIPE_VELOCITY_THRESHOLD = 5;
	private static final int SWIPE_VELOCITY_MAPMOVE = 30;
	private static final int STATA = 1;
	private static final int TUNNEL = 2;
	private int currentMap = STATA;
	private int CURRENT_VIEW_WIDTH = 600;
	private int CURRENT_VIEW_HEIGHT = 600;

	private Button btnSaveLocation;
	private Button btnGetLocation;
	private Button btnChangeMap;
	private Button btnZoomIn;
	private Button btnZoomOut;
	private ImageView imageView;
	private boolean isMapMoving = false;
	private Point pointStart;
	private Point pointStartStart;
	private Point pointCurrent;

	private int mapWidth;
	private int mapHeight;
	private int currentSavingNumber;

	private int spotX = -1;
	private int spotY = -1;

	private int x = 250;
	private int y = 250;
	private ArrayList<AccessPoint> _spots;
	private ArrayList<AccessPoint> _scannedAccessPoints;
	private WifiManager wifiManager;
	private BroadcastReceiver broadcastReceiver;
	private static List<ParseObject> allObjects = new ArrayList<ParseObject>();

	private Point determinedCurrentLocation = new Point(-1, -1);
	ProgressDialog progressSaving;

	public void checkBorders() {
		if (x < (int) CURRENT_VIEW_WIDTH / 2)
			x = (int) CURRENT_VIEW_WIDTH / 2;
		if (x > mapWidth - (int) CURRENT_VIEW_WIDTH / 2)
			x = mapWidth - (int) CURRENT_VIEW_WIDTH / 2;
		if (y < (int) CURRENT_VIEW_HEIGHT / 2)
			y = (int) CURRENT_VIEW_HEIGHT / 2;
		if (y > mapHeight - (int) CURRENT_VIEW_HEIGHT / 2)
			y = mapHeight - (int) CURRENT_VIEW_HEIGHT / 2;
	}

	private void zoomIn() {
		if (CURRENT_VIEW_WIDTH <= 100)
			return;
		CURRENT_VIEW_WIDTH -= 100;
		CURRENT_VIEW_HEIGHT -= 100;
		drawMap();
	}

	private void zoomOut() {
		if (CURRENT_VIEW_WIDTH >= 600)
			return;
		if (CURRENT_VIEW_WIDTH >= mapWidth)
			return;
		CURRENT_VIEW_WIDTH += 100;
		CURRENT_VIEW_HEIGHT += 100;
		drawMap();
	}

	public void onSwipeRight() {
		x -= SWIPE_VELOCITY_MAPMOVE;
		checkBorders();
		drawMap();
	}

	public void onSwipeLeft() {
		x += SWIPE_VELOCITY_MAPMOVE;
		checkBorders();
		drawMap();
	}

	public void onSwipeTop() {
		y += SWIPE_VELOCITY_MAPMOVE;
		checkBorders();
		drawMap();
	}

	public void onSwipeBottom() {
		y -= SWIPE_VELOCITY_MAPMOVE;
		checkBorders();
		drawMap();
	}

	private PictureDrawable pictureDrawable = null;
	Bitmap bitmap;
	Canvas canvas;

	private void drawMap() {

		// if (pictureDrawable == null)
		// return;

		// crop the bitmap
		// we are at x1,y1
		int x1 = x - (int) CURRENT_VIEW_WIDTH / 2;
		int y1 = y - (int) CURRENT_VIEW_HEIGHT / 2;
		int x2 = x + (int) CURRENT_VIEW_WIDTH / 2;
		int y2 = y + (int) CURRENT_VIEW_HEIGHT / 2;

		// exit from the map

		if (x1 < 0)
			x1 = 0;
		if (x2 > mapWidth)
			x2 = mapWidth - 1;
		if (y1 < 0)
			y1 = 0;
		if (y2 > mapHeight)
			y2 = mapHeight - 1;

		String str = Integer.toString(x1) + " " + Integer.toString(y1) + " "
				+ Integer.toString(x2) + " " + Integer.toString(y2) + " ";
		Bitmap yourBitmap = null;
		Bitmap yourBitmapFinal = null;
		try {

			yourBitmap = Bitmap.createBitmap(bitmap, x1, y1,
					Math.min(x2 - x1, mapWidth - x1),
					Math.min(y2 - y1, mapHeight - y1));
		} catch (Exception ex) {
			Log.d("d", ex.getMessage());
		}

		Bitmap largeWhiteBitmap = Bitmap.createBitmap(CURRENT_VIEW_WIDTH,
				CURRENT_VIEW_HEIGHT, Bitmap.Config.ARGB_8888);
		Canvas yourcanvas = new Canvas(largeWhiteBitmap);
		yourcanvas.drawColor(0xffffffff);

		yourcanvas.drawBitmap(yourBitmap, 0, 0, new Paint());

		// draw spots

		for (int i = 0; i < _spots.size(); i++) {
			AccessPoint point = _spots.get(i);
			if ((point.X >= x1) && (point.X <= x2) && (point.Y >= y1)
					&& (point.Y <= y2)) {
				// draw it

				Paint p = new Paint();
				int color = Color.WHITE;
				p.setColor(color);

				color = Color.RED;
				p.setColor(color);
				yourcanvas.drawCircle(point.X - x1, point.Y - y1, 10, p);
			}
		}

		if ((spotX != -1) && (spotY != -1)) {
			// draw current point
			Paint p = new Paint();
			int color = Color.WHITE;
			p.setColor(color);

			color = Color.BLUE;
			p.setColor(color);
			yourcanvas.drawCircle(spotX, spotY, 10, p);
		}

		if ((determinedCurrentLocation.x != -1)
				&& (determinedCurrentLocation.y != -1)) {
			// draw current point
			Paint p = new Paint();
			int color = Color.WHITE;
			p.setColor(color);

			color = Color.GREEN;
			p.setColor(color);
			yourcanvas.drawCircle(determinedCurrentLocation.x-x1,
					determinedCurrentLocation.y-y1, 10, p);
		}

		imageView.setImageBitmap(largeWhiteBitmap);

	}

	private void loadMap() {

		switch (currentMap) {
		case STATA:

			bitmap = BitmapFactory.decodeResource(getResources(),
					R.drawable.gfinal);

			break;
		case TUNNEL:
			bitmap = BitmapFactory.decodeResource(getResources(),
					R.drawable.tunnelmap);

			break;
		}

		mapWidth = bitmap.getWidth();
		mapHeight = bitmap.getHeight();

		canvas = new Canvas(bitmap.copy(Bitmap.Config.ARGB_8888, true));
	}

	public void changeMap() {
		switch (currentMap) {
		case STATA:

			currentMap = TUNNEL;
			btnChangeMap.setText("Stata");

			break;
		case TUNNEL:

			currentMap = STATA;
			btnChangeMap.setText("Tunnels");
			break;
		}
		_spots.clear();
		while (_spots.size() > 0)
			_spots.clear();
		loadMap();

		getPointsFromDatabase();
		drawMap();
	}

	@TargetApi(Build.VERSION_CODES.HONEYCOMB_MR2)
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		// Parse initialize
		Parse.initialize(this, "dqqy2pgILHlRNjtKu5MLi4VyY0LkZJY27FgrufLh",
				"fSFmvqwVMeuhsuzavaa7KJAMg5XDPhN0CcjTDiFE");

		btnSaveLocation = (Button) findViewById(R.id.btnSaveLocation);
		btnGetLocation = (Button) findViewById(R.id.btnGetLocation);
		btnChangeMap = (Button) findViewById(R.id.btnChangeMap);
		btnZoomIn = (Button) findViewById(R.id.btnZoomIn);
		btnZoomOut = (Button) findViewById(R.id.btnZoomOut);
		imageView = (ImageView) findViewById(R.id.imgMap);
		pointStart = new Point();
		pointStartStart = new Point(-1, -1);

		pointCurrent = new Point();
		_spots = new ArrayList<AccessPoint>();
		_scannedAccessPoints = new ArrayList<AccessPoint>();

		progressSaving = new ProgressDialog(this);
		progressSaving.setTitle("Saving");
		progressSaving.setMessage("Wait while saving...");

		// detect whether the Android device is connected to the Internet
		if (!isNetworkAvailable()) {

			AlertDialog myAlertDialog = new AlertDialog.Builder(
					MainActivity.this).create();
			myAlertDialog.setMessage("No Internet connection.");
			myAlertDialog.setButton(DialogInterface.BUTTON_POSITIVE, "OK",
					new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int which) {

				}
			});

			myAlertDialog.show();

		}

		loadMap();

		getPointsFromDatabase();

		btnChangeMap.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				// Do something in response to button click
				changeMap();
			}
		});

		btnZoomIn.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				// Do something in response to button click
				zoomIn();
			}
		});

		btnZoomOut.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				// Do something in response to button click
				zoomOut();
			}
		});

		btnSaveLocation.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				// get list of network

				if (btnSaveLocation.getText().equals("Scan this location")) {
					if ((spotX == -1) || (spotY == -1)) {
						AlertDialog alertDialogNoLocation = new AlertDialog.Builder(
								MainActivity.this).create();
						alertDialogNoLocation
						.setMessage("Please select a location first");
						alertDialogNoLocation.setButton(
								DialogInterface.BUTTON_POSITIVE, "OK",
								new DialogInterface.OnClickListener() {
									public void onClick(DialogInterface dialog,
											int which) {
										// System.exit(0);
									}
								});

						alertDialogNoLocation.show();
						return;
					}

					wifiManager = (WifiManager) getSystemService(Context.WIFI_SERVICE);
					_scannedAccessPoints.clear();

					broadcastReceiver = new BroadcastReceiver() {

						@Override
						public void onReceive(Context c, Intent intent) {
							List<ScanResult> results = wifiManager
									.getScanResults();

							for (ScanResult ap : results) {
								AccessPoint accessPoint = new AccessPoint();
								accessPoint.LEVEL = ap.level;
								accessPoint.MAC = ap.BSSID;
								accessPoint.SSID = ap.SSID;
								accessPoint.X = spotX + x - CURRENT_VIEW_WIDTH
										/ 2;
								accessPoint.Y = spotY + y - CURRENT_VIEW_HEIGHT
										/ 2;

								_scannedAccessPoints.add(accessPoint);

								btnSaveLocation.setText("Found: "
										+ _scannedAccessPoints.size()
										+ " AP(s). Press again to save");
							}
						}
					};

					registerReceiver(broadcastReceiver, new IntentFilter(
							WifiManager.SCAN_RESULTS_AVAILABLE_ACTION));
					wifiManager.startScan();

				} else {
					// save to database
					progressSaving.show();
					unregisterReceiver(broadcastReceiver);
					btnSaveLocation.setText("Saving...");
					btnSaveLocation.setEnabled(false);

					AccessPoint point = new AccessPoint();
					point.X = spotX + x - CURRENT_VIEW_WIDTH / 2;
					point.Y = spotY + y - CURRENT_VIEW_HEIGHT / 2;
					_spots.add(point);
					currentSavingNumber = 0;
					for (int i = 0; i < _scannedAccessPoints.size(); i++) {
						AccessPoint accessPoint = _scannedAccessPoints.get(i);

						ParseObject testObject = new ParseObject("ScanResult");
						testObject.put("x", Integer.toString(accessPoint.X));
						testObject.put("y", Integer.toString(accessPoint.Y));
						testObject.put("SSID", accessPoint.SSID);
						testObject.put("MAC", accessPoint.MAC);
						testObject.put("LEVEL", accessPoint.LEVEL);
						testObject.put("deviceName", getDeviceName());

						switch (currentMap) {
						case STATA:

							testObject.put("Map", "Stata");

							break;
						case TUNNEL:

							testObject.put("Map", "Tunnel");
							break;
						}

						testObject.saveInBackground(new SaveCallback() {
							public void done(ParseException e) {
								if (e == null) {
									currentSavingNumber++;
									if (currentSavingNumber >= _scannedAccessPoints
											.size()) {

										btnSaveLocation
										.setText("Scan this location");
										btnSaveLocation.setEnabled(true);
										_scannedAccessPoints.clear();

										spotX = -1;
										spotY = -1;
										progressSaving.dismiss();
									}

								} else {
									progressSaving.dismiss();
									AlertDialog myAlertDialog = new AlertDialog.Builder(
											MainActivity.this).create();
									myAlertDialog.setMessage("Saving error"
											+ e.getMessage());
									myAlertDialog
									.setButton(
											DialogInterface.BUTTON_POSITIVE,
											"OK",
											new DialogInterface.OnClickListener() {
												public void onClick(
														DialogInterface dialog,
														int which) {

												}
											});

									myAlertDialog.show();
									btnSaveLocation
									.setText("Scan this location");
									btnSaveLocation.setEnabled(true);
									_scannedAccessPoints.clear();
									spotX = -1;
									spotY = -1;
								}
							}
						});
					}
				}
			}
		});

		btnGetLocation.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				// get list of network

				wifiManager = (WifiManager) getSystemService(Context.WIFI_SERVICE);
				_scannedAccessPoints.clear();

				broadcastReceiver = new BroadcastReceiver() {

					@Override
					public void onReceive(Context c, Intent intent) {
						List<ScanResult> results = wifiManager.getScanResults();

						for (ScanResult ap : results) {
							AccessPoint accessPoint = new AccessPoint();
							accessPoint.LEVEL = ap.level;
							accessPoint.MAC = ap.BSSID;
							accessPoint.SSID = ap.SSID;

							_scannedAccessPoints.add(accessPoint);
						}
						unregisterReceiver(broadcastReceiver);

						// we have two lists: _scannedAccessPoints and _spots
						 boolean pointAdded = false;
						 ArrayList<ArrayList<AccessPoint>> _locations = new ArrayList<ArrayList<AccessPoint>>();
	                        for (int i = 0; i < _spots.size(); i++) {
	                            pointAdded = false;
	                            for(int j=0; j<_locations.size();j++){
	                                if(pointAdded == false
	                                        && _spots.get(i).X == _locations.get(j).get(0).X
	                                        && _spots.get(i).Y == _locations.get(j).get(0).Y){
	                                    _locations.get(j).add(_spots.get(i));
	                                    pointAdded = true;
	                                }

	                            }
	                            if(pointAdded == false){
	                                ArrayList<AccessPoint> new_list = new ArrayList<AccessPoint>();
	                                new_list.add(_spots.get(i));
	                                _locations.add(new_list);
	                            }

	                        }
	                        AccessPoint closest_point = _locations.get(0).get(0);
	                        double closest_distance = 10^10;

	                        for (int i=0; i < _locations.size(); i++){
	                            double distance_sum = 0;
	                            for (int k = 0; k < _locations.get(i).size(); k++){
	                            	distance_sum += _locations.get(i).get(k).LEVEL^2;
	                            }
	                            //distance_sum=10^10;
	                            for(int j = 0; j < _scannedAccessPoints.size(); j++){
	                                distance_sum += _scannedAccessPoints.get(j).LEVEL^2;
	                            	
	                            	for(int k = 0; k < _locations.get(i).size(); k++){
	                                    if(_locations.get(i).get(k).MAC.equals(_scannedAccessPoints.get(j).MAC)){
	                                    	//distance_sum -= _locations.get(i).get(k).LEVEL^2 + _scannedAccessPoints.get(j).LEVEL^2;
	                                        distance_sum -= 2*_locations.get(i).get(k).LEVEL * _scannedAccessPoints.get(j).LEVEL;
	                                    	//distance_sum--;
	                                    }
	                                }
	                            }
	                            if(distance_sum < closest_distance){
	                                closest_distance = distance_sum;
	                                closest_point = _locations.get(i).get(0);
	                            }
	                        }


							determinedCurrentLocation.x = closest_point.X;
							determinedCurrentLocation.y = closest_point.Y;

						drawMap();
					}
				};

				registerReceiver(broadcastReceiver, new IntentFilter(
						WifiManager.SCAN_RESULTS_AVAILABLE_ACTION));
				wifiManager.startScan();

			}

		});

		imageView.setOnTouchListener(new View.OnTouchListener() {

			@Override
			public boolean onTouch(View v, MotionEvent event) {

				int action = event.getAction();
				switch (action) {

				case MotionEvent.ACTION_DOWN:

					isMapMoving = true;

					pointStart.x = (int) event.getX();
					pointStart.y = (int) event.getY();

					pointStartStart.x = (int) event.getX();
					pointStartStart.y = (int) event.getY();

					break;

				case MotionEvent.ACTION_MOVE:

					if (isMapMoving) {

						pointCurrent.x = (int) event.getX();
						pointCurrent.y = (int) event.getY();

						try {
							int diffY = pointCurrent.y - pointStart.y;
							int diffX = pointCurrent.x - pointStart.x;

							pointStart.x = (int) event.getX();
							pointStart.y = (int) event.getY();

							if (Math.abs(diffX) > Math.abs(diffY)) {
								if (Math.abs(diffX) > SWIPE_THRESHOLD) {
									if (diffX > 0) {
										onSwipeRight();
									} else {
										onSwipeLeft();
									}
								}

							} else if (Math.abs(diffY) > SWIPE_THRESHOLD) {
								if (diffY > 0) {
									onSwipeBottom();
								} else {
									onSwipeTop();
								}
							}

						} catch (Exception exception) {
							exception.printStackTrace();
						}

					}

					break;

				case MotionEvent.ACTION_UP:
					isMapMoving = false;

					imageView.invalidate();

					pointCurrent.x = (int) event.getX();
					pointCurrent.y = (int) event.getY();

					// checking for new spot
					int xx = pointStartStart.x;
					int yy = pointStartStart.y;

					int xx1 = pointCurrent.x;
					int yy1 = pointCurrent.y;
					if ((Math.abs(pointStartStart.x - pointCurrent.x) < 10)
							&& (Math.abs(pointStartStart.y - pointCurrent.y) < 10)) {

						int[] viewCoords = new int[2];
						imageView.getLocationOnScreen(viewCoords);

						int touchX = (int) pointStartStart.x;
						int touchY = (int) pointStartStart.y;

						float ww = imageView.getWidth();
						float hh = imageView.getHeight();

						ww = touchX / ww;
						hh = touchY / hh;

						touchX = Math.round(CURRENT_VIEW_WIDTH * ww);
						touchY = Math.round(CURRENT_VIEW_HEIGHT * hh);

						spotX = touchX;
						spotY = touchY;
						drawMap();
					}
					break;
				}
				return true;
			}

		});

		// show the map
		x = (int) mapWidth / 2;
		y = (int) mapHeight / 2;

		drawMap();
	}

	@Override
	public void onWindowFocusChanged(boolean hasFocus) {
		super.onWindowFocusChanged(hasFocus);
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

	private boolean isNetworkAvailable() {
		ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
		NetworkInfo activeNetworkInfo = connectivityManager
				.getActiveNetworkInfo();
		return activeNetworkInfo != null && activeNetworkInfo.isConnected();
	}

	public void getPointsFromDatabase() {
		_spots.clear();
		final ProgressDialog progress = new ProgressDialog(this);
		progress.setTitle("Loading");
		progress.setMessage("Wait while loading...");

		ParseQuery<ParseObject> query = ParseQuery.getQuery("ScanResult");
		query.setLimit(1000);
		switch (currentMap) {
		case STATA:
			query.whereEqualTo("Map", "Stata");
			break;
		case TUNNEL:
			query.whereEqualTo("Map", "Tunnel");
			break;
		}

		progress.show();
		query.findInBackground(new FindCallback<ParseObject>() {
			public void done(List<ParseObject> objects, ParseException e) {
				if (e == null) {
					allObjects.addAll(objects);
					int skip = 0;
					int limit = 1000;
					if (objects.size() == limit) {
						skip = skip + limit;
						ParseQuery query = new ParseQuery("ScanResult");
						switch (currentMap) {
						case STATA:
							query.whereEqualTo("Map", "Stata");
							break;
						case TUNNEL:
							query.whereEqualTo("Map", "Tunnel");
							break;
						}
						query.setSkip(skip);
						query.setLimit(limit);
						query.findInBackground(this);
					}
					// We have a full PokeDex
					else {
						// USE FULL DATA AS INTENDED

						for (int i = 0; i < allObjects.size(); i++) {
							String x = allObjects.get(i).getString("x");
							String y = allObjects.get(i).getString("y");
							int fl_x = Integer.parseInt(x);
							int fl_y = Integer.parseInt(y);
							AccessPoint point = new AccessPoint();
							point.X = fl_x;
							point.Y = fl_y;
							point.LEVEL = allObjects.get(i).getInt("LEVEL");
							point.MAC = allObjects.get(i).getString("MAC");
							point.SSID = allObjects.get(i).getString("SSID");

							switch (currentMap) {
							case STATA:
								if (allObjects.get(i).getString("Map")
										.equals("Stata"))
									_spots.add(point);

								break;
							case TUNNEL:

								if (allObjects.get(i).getString("Map")
										.equals("Tunnel"))
									_spots.add(point);
								break;
							}
						}
						progress.dismiss();
						drawMap();
					}
				} else {
				}
			}
		});
	}
	
	public String getDeviceName() {
		  String manufacturer = Build.MANUFACTURER;
		  String model = Build.MODEL;
		  if (model.startsWith(manufacturer)) {
		    return capitalize(model);
		  } else {
		    return capitalize(manufacturer) + " " + model;
		  }
		}


		private String capitalize(String s) {
		  if (s == null || s.length() == 0) {
		    return "";
		  }
		  char first = s.charAt(0);
		  if (Character.isUpperCase(first)) {
		    return s;
		  } else {
		    return Character.toUpperCase(first) + s.substring(1);
		  }
		} 
}
