package com.redbear.simplecontrols;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Timer;
import java.util.TimerTask;

import android.text.format.Time;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.IBinder;
import android.util.Log;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.view.inputmethod.EditorInfo;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

public class SimpleControls extends Activity {
	private final static String TAG = SimpleControls.class.getSimpleName();

	private Button connectBtn = null;
	private TextView rssiValue = null;
	private TextView currentTemp1 = null;
	private TextView currentTemp2 = null;
	private EditText timerEditText = null;
	private int activePump = 1;
	private LinearLayout layout1 = null;
	private LinearLayout layout2 = null;
	private Button startBtn = null;
	private Button pauseBtn = null;
	private Button resetBtn = null;

	private ToggleButton digitalOutBtn, digitalInBtn, AnalogInBtn;

	private Spinner targetTemp1Spinner, targetTemp2Spinner;
	private SeekBar servoSeekBar, PWMSeekBar;
	private ArrayAdapter<String> tt1Array;
	private ArrayAdapter<String> tt2Array;

	private CountDownTimer timer;
	private Time finishingTime = new Time();

	private BluetoothGattCharacteristic characteristicTx = null;
	private RBLService mBluetoothLeService;
	private BluetoothAdapter mBluetoothAdapter;
	private BluetoothDevice mDevice = null;
	private String mDeviceAddress;

	private boolean flag = true;
	private boolean connState = false;
	private boolean scanFlag = false;

	private byte[] data = new byte[3];
	private static final int REQUEST_ENABLE_BT = 1;
	private static final long SCAN_PERIOD = 2000;

	private final ServiceConnection mServiceConnection = new ServiceConnection() {

		@Override
		public void onServiceConnected(ComponentName componentName,
									   IBinder service) {
			mBluetoothLeService = ((RBLService.LocalBinder) service)
					.getService();
			if (!mBluetoothLeService.initialize()) {
				Log.e(TAG, "Unable to initialize Bluetooth");
				finish();
			}
		}

		@Override
		public void onServiceDisconnected(ComponentName componentName) {
			mBluetoothLeService = null;
		}
	};

	private final BroadcastReceiver mGattUpdateReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			final String action = intent.getAction();

			if (RBLService.ACTION_GATT_DISCONNECTED.equals(action)) {
				Toast.makeText(getApplicationContext(), "Disconnected",
						Toast.LENGTH_SHORT).show();
				setButtonDisable();
			} else if (RBLService.ACTION_GATT_SERVICES_DISCOVERED
					.equals(action)) {
				Toast.makeText(getApplicationContext(), "Connected",
						Toast.LENGTH_SHORT).show();

				getGattService(mBluetoothLeService.getSupportedGattService());
			} else if (RBLService.ACTION_DATA_AVAILABLE.equals(action)) {
				data = intent.getByteArrayExtra(RBLService.EXTRA_DATA);

				readAnalogInValue(data);
			} else if (RBLService.ACTION_GATT_RSSI.equals(action)) {
				displayData(intent.getStringExtra(RBLService.EXTRA_DATA));
			}
		}
	};

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		requestWindowFeature(Window.FEATURE_CUSTOM_TITLE);
		setContentView(R.layout.main);
		getWindow().setFeatureInt(Window.FEATURE_CUSTOM_TITLE, R.layout.title);

		rssiValue = (TextView) findViewById(R.id.rssiValue);
		currentTemp1 = (TextView) findViewById(R.id.currentTemp1);
		currentTemp2 = (TextView) findViewById(R.id.currentTemp2);

		timerEditText = (EditText) findViewById(R.id.editText2);

		timerEditText.setOnEditorActionListener(new EditText.OnEditorActionListener() {
			@Override
			public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
				if (actionId == EditorInfo.IME_ACTION_DONE) {

					return true;
				}
				return false;
			}
		});


		resetBtn = (Button) findViewById(R.id.Reset);
		resetBtn.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				timer.cancel();
				timerEditText.setText("60:00");
			}
		});

		pauseBtn = (Button) findViewById(R.id.Pause);
		pauseBtn.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				timer.cancel();
			}
		});

		startBtn = (Button) findViewById(R.id.Start);
		startBtn.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {

				String timeString = timerEditText.getText().toString();

				if( timeString.indexOf( ":" ) == -1 || timeString.indexOf( ":" ) != timeString.lastIndexOf(":")) {
					Toast.makeText(getApplicationContext(), "Invalid Time. Stopping.",
							Toast.LENGTH_SHORT).show();
				}

				int min = Integer.parseInt(timeString.substring(0, timeString.indexOf(":")));
				int sec = Integer.parseInt( timeString.substring( timeString.indexOf(":" ) + 1, timeString.length() ));

				long milifuture = ( min*60 + sec ) * 1000;

				try {
					timer = new CountDownTimer(milifuture, 1000) {

						public void onTick(long millisUntilFinished) {

							SimpleDateFormat formatter = new SimpleDateFormat("mm:ss");

							// Create a calendar object that will convert the date and time value in milliseconds to date.
							Calendar calendar = Calendar.getInstance();
							calendar.setTimeInMillis(millisUntilFinished);
							String remaining = formatter.format(calendar.getTime());

							timerEditText.setText(remaining);
							//mTextField.setText("seconds remaining: " + millisUntilFinished / 1000);
						}

						public void onFinish() {
							//mTextField.setText("done!");
						}
					}.start();
				}
				catch( Throwable ex )
				{
					ex.printStackTrace();
				}
				getTemp();
			}

		});

		connectBtn = (Button) findViewById(R.id.connect);
		connectBtn.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				if (scanFlag == false) {
					Toast.makeText(getApplicationContext(), "Connecting...",
							Toast.LENGTH_SHORT).show();


					scanLeDevice();

					Timer mTimer = new Timer();
					mTimer.schedule(new TimerTask() {

						@Override
						public void run() {
							if (mDevice != null) {
								mDeviceAddress = mDevice.getAddress();
								mBluetoothLeService.connect(mDeviceAddress);
								scanFlag = true;
							} else {
								runOnUiThread(new Runnable() {
									public void run() {
										Toast toast = Toast
												.makeText(
														SimpleControls.this,
														"Couldn't search Ble Shiled device!",
														Toast.LENGTH_SHORT);
										toast.setGravity(0, 0, Gravity.CENTER);
										toast.show();
									}
								});
							}
						}
					}, SCAN_PERIOD);
				}

				System.out.println(connState);
				if (connState == false) {
					mBluetoothLeService.connect(mDeviceAddress);
				} else {
					mBluetoothLeService.disconnect();
					mBluetoothLeService.close();
					setButtonDisable();
				}
			}
		});

		digitalOutBtn = (ToggleButton) findViewById(R.id.DOutBtn);
		digitalOutBtn.setOnCheckedChangeListener(new OnCheckedChangeListener() {

			@Override
			public void onCheckedChanged(CompoundButton buttonView,
										 boolean isChecked) {
				BLEPacket newPacket = new BLEPacket(BLEPacket.OVERRIDE);
				newPacket.setData( new byte[] {(byte) (isChecked ? 1 : 0) });

				characteristicTx.setValue(newPacket.getPacket());
				mBluetoothLeService.writeCharacteristic(characteristicTx);
			}
		});

		ArrayList<String> numberList = new ArrayList<String>();
		for (int i = 60; i < 231; i++)
			numberList.add(String.valueOf(i));

		tt1Array = new ArrayAdapter<String>(getApplicationContext(),
				R.layout.spinner_item, numberList);

		tt2Array = new ArrayAdapter<String>(getApplicationContext(),
				R.layout.spinner_item, numberList);

		targetTemp1Spinner = (Spinner) findViewById(R.id.targetTemp1);
		targetTemp1Spinner.setAdapter(tt1Array);
		targetTemp1Spinner.setSelection(tt1Array.getPosition("152"));
     	targetTemp1Spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {

			@Override
			public void onItemSelected(AdapterView<?> parentView, View selectedItemView, int position, long id) {

				if (!connState)
					return;

				int selectedTemp = Integer.parseInt(tt1Array.getItem(position));
				BLEPacket newPacket = new BLEPacket(BLEPacket.SET_PUMP);
				newPacket.setData(new byte[]{(byte) 0x01, (byte) selectedTemp});

				characteristicTx.setValue(newPacket.getPacket());
				mBluetoothLeService.writeCharacteristic(characteristicTx);
			}

			@Override
			public void onNothingSelected(AdapterView<?> parentView) {
			}

		});

		targetTemp2Spinner = (Spinner) findViewById(R.id.targetTemp2);
		targetTemp2Spinner.setAdapter(tt2Array);
		targetTemp2Spinner.setSelection(tt2Array.getPosition("152"));
		targetTemp2Spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
			@Override
			public void onItemSelected(AdapterView<?> parentView, View selectedItemView, int position, long id) {

				if (!connState)
					return;

				int selectedTemp = Integer.parseInt(tt2Array.getItem(position));
				BLEPacket newPacket = new BLEPacket(BLEPacket.SET_PUMP);
				newPacket.setData(new byte[]{(byte) 0x02, (byte) selectedTemp});

				characteristicTx.setValue(newPacket.getPacket());
				mBluetoothLeService.writeCharacteristic(characteristicTx);
			}

			@Override
			public void onNothingSelected(AdapterView<?> parentView) {
			}

		});

		layout1 = (LinearLayout) findViewById( R.id.layoutPump1 );
		layout2 = (LinearLayout) findViewById( R.id.layoutPump2 );
/*
		AnalogInBtn = (ToggleButton) findViewById(R.id.AnalogInBtn);
		AnalogInBtn.setOnCheckedChangeListener(new OnCheckedChangeListener() {

			@Override
			public void onCheckedChanged(CompoundButton buttonView,
					boolean isChecked) {
				byte[] buf = new byte[] { (byte) 0xA0, (byte) 0x00, (byte) 0x00 };

				if (isChecked == true)
					buf[1] = 0x01;
				else
					buf[1] = 0x00;

				characteristicTx.setValue(buf);
				mBluetoothLeService.writeCharacteristic(characteristicTx);
			}
		});

		servoSeekBar = (SeekBar) findViewById(R.id.ServoSeekBar);
		servoSeekBar.setEnabled(false);
		servoSeekBar.setMax(180);
		servoSeekBar.setOnSeekBarChangeListener(new OnSeekBarChangeListener() {

			@Override
			public void onStopTrackingTouch(SeekBar seekBar) {

			}

			@Override
			public void onStartTrackingTouch(SeekBar seekBar) {

			}

			@Override
			public void onProgressChanged(SeekBar seekBar, int progress,
					boolean fromUser) {
				byte[] buf = new byte[] { (byte) 0x03, (byte) 0x00, (byte) 0x00 };

				buf[1] = (byte) servoSeekBar.getProgress();

				characteristicTx.setValue(buf);
				mBluetoothLeService.writeCharacteristic(characteristicTx);
			}
		});
*/
		/*
		PWMSeekBar = (SeekBar) findViewById(R.id.PWMSeekBar);
		PWMSeekBar.setEnabled(false);
		PWMSeekBar.setMax(255);
		PWMSeekBar.setOnSeekBarChangeListener(new OnSeekBarChangeListener() {

			@Override
			public void onStopTrackingTouch(SeekBar seekBar) {

			}

			@Override
			public void onStartTrackingTouch(SeekBar seekBar) {

			}

			@Override
			public void onProgressChanged(SeekBar seekBar, int progress,
					boolean fromUser) {
				byte[] buf = new byte[] { (byte) 0x02, (byte) 0x00, (byte) 0x00 };

				buf[1] = (byte) PWMSeekBar.getProgress();

				characteristicTx.setValue(buf);
				mBluetoothLeService.writeCharacteristic(characteristicTx);
			}
		});
*/
		if (!getPackageManager().hasSystemFeature(
				PackageManager.FEATURE_BLUETOOTH_LE)) {
			Toast.makeText(this, "Ble not supported", Toast.LENGTH_SHORT)
					.show();
			finish();
		}

		final BluetoothManager mBluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
		mBluetoothAdapter = mBluetoothManager.getAdapter();
		if (mBluetoothAdapter == null) {
			Toast.makeText(this, "Ble not supported", Toast.LENGTH_SHORT)
					.show();
			finish();
			return;
		}

		Intent gattServiceIntent = new Intent(SimpleControls.this,
				RBLService.class);
		bindService(gattServiceIntent, mServiceConnection, BIND_AUTO_CREATE);
	}

	@Override
	protected void onResume() {
		super.onResume();

		if (!mBluetoothAdapter.isEnabled()) {
			Intent enableBtIntent = new Intent(
					BluetoothAdapter.ACTION_REQUEST_ENABLE);
			startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
		}

		registerReceiver(mGattUpdateReceiver, makeGattUpdateIntentFilter());
	}

	private void displayData(String data) {
		if (data != null) {
			rssiValue.setText(data);
		}
	}


	private void getTemp()
	{
		BLEPacket newPacket = new BLEPacket(BLEPacket.TEMP);
		newPacket.setData( new byte[] {(byte)0x01 });

		characteristicTx.setValue(newPacket.getPacket());
		mBluetoothLeService.writeCharacteristic(characteristicTx);
	}


	private void processTempReading( BLEPacket packet )
	{
		byte[] data = packet.getData();

		TextView ll = (TextView) findViewById(R.id.customtitlebar );
		ll.setTextColor(Color.parseColor("#FFFFFF"));
		ll.setText( ( connState ? "Connected" : "Not Connected" ) + " - OFF");

		for( int i=0; i < data.length; i++ ) {
			if( data[i] == 0x01 ) {
				int test = (data[i+1] << 8) + ( data[i+2] & 0xFF );
				float temp = (float)( test / 100.0 );
				currentTemp1.setText(String.valueOf(temp) + "F");
				if( data[i+3] == 0x01 ) {
					ll.setTextColor( Color.parseColor("#00FF00"));
					ll.setText( ( connState ? "Connected" : "Not Connected" ) + " - ON");
				}
			}
			else if( data[i] == 0x02 ) {
				int test = (data[i + 1] << 8) + ( data[i + 2] & 0xFF );
				float temp = (float) (test / 100.0);
				currentTemp2.setText(String.valueOf(temp) + "F");
				if (data[i + 3] == 0x01) {
					ll.setTextColor(Color.parseColor("#00FF00"));
					ll.setText( ( connState ? "Connected" : "Not Connected" ) + " - ON");
				}
			}
		}
	}

	private void refreshActivePump()
	{
		layout1.setBackgroundColor( Color.parseColor("#FFFFFF") );
		layout2.setBackgroundColor( Color.parseColor("#FFFFFF") );

		if( activePump == 1 )
			layout1.setBackgroundColor(Color.parseColor("#F5F5F0"));
		else if( activePump == 2 )
			layout2.setBackgroundColor(Color.parseColor("#F5F5F0"));
	}

	private void processActivePump(BLEPacket packet) {
		byte[] data = packet.getData();

		activePump = data[0];

		String activeTemp = String.valueOf( (int)(data[1] & 0xFF) );

		if (activePump == 1)
			targetTemp1Spinner.setSelection(tt1Array.getPosition(activeTemp));
		else if (activePump == 2)
			targetTemp2Spinner.setSelection(tt2Array.getPosition(activeTemp));

	}

	private void readAnalogInValue(byte[] data) {

		for (int i = 0; i < data.length; i++) {
			try
			{
				if (data[i] == BLEPacket.HEADER) {
					BLEPacket packet = new BLEPacket(Arrays.copyOfRange(data, i, data.length - 1));

					if( packet.getType() == BLEPacket.READ_TEMP ) {
						processTempReading(packet);
						break;
					}
					else if( packet.getType() == BLEPacket.READ_PUMP ){
						processActivePump( packet );
						break;
					}
				}
			}
			catch( ParseException e )
			{
				continue;
			}
			catch( ArrayIndexOutOfBoundsException ex )
			{
			}
		}
		refreshActivePump();
	}

	private void setButtonEnable() {
		flag = true;
		connState = true;

		connectBtn.setText("Disconnect");
	}

	private void setButtonDisable() {
		flag = false;
		connState = false;

		connectBtn.setText("Connect");
		TextView ll = (TextView) findViewById(R.id.customtitlebar );
		String title = ll.getText().toString();
		ll.setText( "Not Connected " + title.substring( title.lastIndexOf(" - ") + 1, title.length()));

	}

	private void startReadRssi() {
		new Thread() {
			public void run() {

				while (flag) {
					mBluetoothLeService.readRssi();
					try {
						sleep(500);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
			};
		}.start();
	}

	private void getGattService(BluetoothGattService gattService) {
		if (gattService == null)
			return;

		setButtonEnable();
		startReadRssi();

		characteristicTx = gattService
				.getCharacteristic(RBLService.UUID_BLE_SHIELD_TX);

		BluetoothGattCharacteristic characteristicRx = gattService
				.getCharacteristic(RBLService.UUID_BLE_SHIELD_RX);
		mBluetoothLeService.setCharacteristicNotification(characteristicRx,
				true);
		mBluetoothLeService.readCharacteristic(characteristicRx);
	}

	private static IntentFilter makeGattUpdateIntentFilter() {
		final IntentFilter intentFilter = new IntentFilter();

		intentFilter.addAction(RBLService.ACTION_GATT_CONNECTED);
		intentFilter.addAction(RBLService.ACTION_GATT_DISCONNECTED);
		intentFilter.addAction(RBLService.ACTION_GATT_SERVICES_DISCOVERED);
		intentFilter.addAction(RBLService.ACTION_DATA_AVAILABLE);
		intentFilter.addAction(RBLService.ACTION_GATT_RSSI);

		return intentFilter;
	}

	private void scanLeDevice() {
		new Thread() {

			@Override
			public void run() {
				mBluetoothAdapter.startLeScan(mLeScanCallback);

				try {
					Thread.sleep(SCAN_PERIOD);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}

				mBluetoothAdapter.stopLeScan(mLeScanCallback);
			}
		}.start();
	}

	private BluetoothAdapter.LeScanCallback mLeScanCallback = new BluetoothAdapter.LeScanCallback() {

		@Override
		public void onLeScan(final BluetoothDevice device, final int rssi,
				byte[] scanRecord) {
			runOnUiThread(new Runnable() {
				@Override
				public void run() {
 					if (device != null) {
						String str = device.getName();
						//if (device.getName().contains("Shield")
							//	|| device.getName().contains("Biscuit")) {
						if( str == null )
							return;
						if( device.getName().contains( "BLE Mini" )) {
							mDevice = device;
						}
					}
				}
			});
		}
	};

	@Override
	protected void onStop() {
		super.onStop();

		flag = false;

		unregisterReceiver(mGattUpdateReceiver);
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();

		if (mServiceConnection != null)
			unbindService(mServiceConnection);
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		// User chose not to enable Bluetooth.
		if (requestCode == REQUEST_ENABLE_BT
				&& resultCode == Activity.RESULT_CANCELED) {
			finish();
			return;
		}

		super.onActivityResult(requestCode, resultCode, data);
	}
}
