package com.openvehicles.OVMS.ui;

import android.os.Bundle;
import android.util.Log;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.openvehicles.OVMS.R;
import com.openvehicles.OVMS.api.OnResultCommandListenner;
import com.openvehicles.OVMS.entities.CarData;
import com.openvehicles.OVMS.entities.CarData.DataStale;
import com.openvehicles.OVMS.ui.utils.Ui;

public class CarFragment extends BaseFragment implements OnClickListener, OnResultCommandListenner {
	private CarData mCarData;

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		return inflater.inflate(R.layout.fragment_car, null);
	}
	
	@Override
	public void update(CarData pCarData) {
		mCarData = pCarData;
		
		updateLastUpdatedView(pCarData);
		updateCarBodyView(pCarData);
	}
	
	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);

		registerForContextMenu(findViewById(R.id.btn_wakeup));
		registerForContextMenu(findViewById(R.id.txt_homelink));
		registerForContextMenu(findViewById(R.id.tabCarImageHomelink));
		
		findViewById(R.id.btn_lock_car).setOnClickListener(this);
		findViewById(R.id.btn_valet_maode).setOnClickListener(this);
	}
	
	@Override
	public void registerForContextMenu(View view) {
		super.registerForContextMenu(view);
		view.setOnClickListener(this);
	}
	
	@Override
	public void onClick(View v) {
		if (mCarData == null) return;
			
		switch (v.getId()) {
		case R.id.btn_lock_car:
			Ui.showPinDialog(getActivity(), mCarData.car_locked ? R.string.lb_unlock_car : R.string.lb_lock_car,
				new Ui.OnChangeListener<String>() {
					@Override
					public void onAction(String pData) {
						String cmd;
						int resId;
						if (mCarData.car_locked) {
							resId = R.string.lb_unlock_car;
							cmd = "22,"+pData;
						} else {
							resId = R.string.lb_lock_car;
							cmd = "20,"+pData;
						}
						sendCommand(resId, cmd, CarFragment.this);						
					}
				});
			break;
		case R.id.btn_valet_maode:
			Ui.showPinDialog(getActivity(), R.string.lb_valet_mode,
					mCarData.car_valetmode ? R.string.lb_valet_mode_off : R.string.lb_valet_mode_on,
				new Ui.OnChangeListener<String>() {
					@Override
					public void onAction(String pData) {
						String cmd;
						int resId;
						if (mCarData.car_valetmode) {
							resId = R.string.lb_valet_mode_off;
							cmd = "23,"+pData;
						} else {
							resId = R.string.lb_valet_mode_on;
							cmd = "21,"+pData;
						}
						sendCommand(resId, cmd, CarFragment.this);						
					}
				});
			break;
		default:
			v.performLongClick();
		}
	}

	private static final int MI_WAKEUP		= Menu.FIRST;
	private static final int MI_HL_01		= Menu.FIRST + 1;
	private static final int MI_HL_02		= Menu.FIRST + 2;
	private static final int MI_HL_03		= Menu.FIRST + 3;
	@Override
	public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
		super.onCreateContextMenu(menu, v, menuInfo);
		switch (v.getId()) {
		case R.id.btn_wakeup:
			menu.setHeaderTitle(R.string.lb_wakeup_car);
			menu.add(0, MI_WAKEUP, 0, R.string.Wakeup);
			menu.add(R.string.Cancel);
			break;
		case R.id.tabCarImageHomelink:
		case R.id.txt_homelink:
			menu.setHeaderTitle("Homelink");
			menu.add(0, MI_HL_01, 0, "1");
			menu.add(0, MI_HL_02, 0, "2");
			menu.add(0, MI_HL_03, 0, "3");
			menu.add(R.string.Cancel);
			break;
		}
	}
	
	@Override
	public boolean onContextItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case MI_WAKEUP:
			sendCommand(R.string.msg_wakeup_car, "18", this);			
			return true;
		case MI_HL_01:
			sendCommand(R.string.msg_issuing_homelink, "24,0", this);			
			return true;
		case MI_HL_02:
			sendCommand(R.string.msg_issuing_homelink, "24,1", this);			
			return true;
		case MI_HL_03:
			sendCommand(R.string.msg_issuing_homelink, "24,2", this);			
			return true;
		default:
			return false;
		}
	}
	
	@Override
	public void onResultCommand(String[] result) {
		if (result.length >= 3) {
			Toast.makeText(getActivity(), result[2], Toast.LENGTH_SHORT).show();
		}
		cancelCommand();
	}
	
	// This updates the part of the view with times shown.
	// It is called by a periodic timer so it gets updated every few seconds.
	public void updateLastUpdatedView(CarData pCarData) {
		if ((pCarData == null) || (pCarData.car_lastupdated == null)) return;

		// First the last updated section...
		TextView tv = (TextView)findViewById(R.id.txt_last_updated);
		long now = System.currentTimeMillis();
		long seconds = (now - pCarData.car_lastupdated.getTime()) / 1000;
		long minutes = (seconds)/60;
		long hours = minutes/60;
		long days = minutes/(60*24);
		Log.d("OVMS", "Last updated: " + seconds + " secs ago");

		if (pCarData.car_lastupdated == null) {
			tv.setText("");
			tv.setTextColor(0xFFFFFFFF);
		}
		else if (minutes == 0) {
			tv.setText("live");
			tv.setTextColor(0xFFFFFFFF);
		}
		else if (minutes == 1) {
			tv.setText("1 min");
			tv.setTextColor(0xFFFFFFFF);
		}
		else if (days > 1) {
			tv.setText(String.format("%d days",days));
			tv.setTextColor(0xFFFF0000);
		}
		else if (hours > 1) {
			tv.setText(String.format("%d hours",hours));
			tv.setTextColor(0xFFFF0000);
		}
		else if (minutes > 60) {
			tv.setText(String.format("%d mins",minutes));
			tv.setTextColor(0xFFFF0000);			
		}
		else {
			tv.setText(String.format("%d mins",minutes));
			tv.setTextColor(0xFFFFFFFF);
		}

		// Then the parking timer...
//		LinearLayout parkinglayoutv = (LinearLayout)findViewById(R.id.tabCarLayoutParking);
		tv = (TextView)findViewById(R.id.txt_parked_time);
		if ((!pCarData.car_started) && (pCarData.car_parked_time != null)) {
			// Car is parked
//			parkinglayoutv.setVisibility(View.VISIBLE);
			tv.setVisibility(View.VISIBLE);
			
			seconds = (now - pCarData.car_parked_time.getTime()) / 1000;
			minutes = (seconds)/60;
			hours = minutes/60;
			days = minutes/(60*24);

			if (minutes == 0)
				tv.setText("just now");
			else if (minutes == 1)
				tv.setText("1 min");
			else if (days > 1)
				tv.setText(String.format("%d days",days));
			else if (hours > 1)
				tv.setText(String.format("%d hours",hours));
			else if (minutes > 60)
				tv.setText(String.format("%d mins",minutes));
			else
				tv.setText(String.format("%d mins",minutes));
		} else {
//			parkinglayoutv.setVisibility(View.INVISIBLE);
			tv.setVisibility(View.INVISIBLE);
		}

		// The signal strength indicator
		ImageView iv = (ImageView)findViewById(R.id.img_signal_rssi);
		iv.setImageResource(Ui.getDrawableIdentifier(getActivity(), "signal_strength_" + pCarData.car_gsm_bars));
	}
	
	// This updates the main informational part of the view.
	// It is called when the server gets new data.
	public void updateCarBodyView(CarData pCarData) {
		if ((pCarData == null) || (pCarData.car_lastupdated == null)) return;

		// Now, the car background image	
		ImageView iv = (ImageView)findViewById(R.id.tabCarImageCarOutline);
		iv.setImageResource(Ui.getDrawableIdentifier(getActivity(), "ol_"+pCarData.sel_vehicle_image));

		// Ambient weather
		TextView tv = (TextView) findViewById(R.id.tabCarTextAmbient);
		if (pCarData.stale_ambient_temp == DataStale.NoValue) {
			tv.setText("");
			tv.setTextColor(0xFF808080);
		}
		else if ((pCarData.stale_ambient_temp == DataStale.Stale)||(!pCarData.car_coolingpump_on)) {
			tv.setText(pCarData.car_temp_ambient);
			tv.setTextColor(0xFF808080);
		}
		else {
			tv.setText(pCarData.car_temp_ambient);
			tv.setTextColor(0xFFFFFFFF);
		}

		// TPMS
//		String tirePressureDisplayFormat = "%s\n%s";
        TextView fltv = (TextView) findViewById(R.id.textFLWheel);
        TextView fltvv = (TextView) findViewById(R.id.textFLWheelVal);

        TextView frtv = (TextView) findViewById(R.id.textFRWheel);
        TextView frtvv = (TextView) findViewById(R.id.textFRWheelVal);
        
        TextView rltv = (TextView) findViewById(R.id.textRLWheel);
        TextView rltvv = (TextView) findViewById(R.id.textRLWheelVal);
        
        TextView rrtv = (TextView) findViewById(R.id.textRRWheel);
        TextView rrtvv = (TextView) findViewById(R.id.textRRWheelVal);
        
    	iv = (ImageView)findViewById(R.id.tabCarImageCarTPMSBoxes);
        if (pCarData.stale_tpms == DataStale.NoValue) {
        	iv.setVisibility(View.INVISIBLE);
    		fltv.setText(null);
    		frtv.setText(null);
    		rltv.setText(null);
    		rrtv.setText(null);
    		
    		fltvv.setText(null);
    		frtvv.setText(null);
    		rltvv.setText(null);
    		rrtvv.setText(null);
    		
        } else {
        	iv.setVisibility(View.VISIBLE);

        	fltv.setText(pCarData.car_tpms_fl_p);
    		frtv.setText(pCarData.car_tpms_fr_p);
    		rltv.setText(pCarData.car_tpms_rl_p);
    		rrtv.setText(pCarData.car_tpms_rr_p);
    		
        	fltvv.setText(pCarData.car_tpms_fl_t);
    		frtvv.setText(pCarData.car_tpms_fr_t);
    		rltvv.setText(pCarData.car_tpms_rl_t);
    		rrtvv.setText(pCarData.car_tpms_rr_t);
    		
    		if (pCarData.stale_tpms == DataStale.Stale) {
    			fltv.setTextColor(0xFF808080);
    			frtv.setTextColor(0xFF808080);
    			rltv.setTextColor(0xFF808080);
    			rrtv.setTextColor(0xFF808080);

    			fltvv.setTextColor(0xFF808080);
    			frtvv.setTextColor(0xFF808080);
    			rltvv.setTextColor(0xFF808080);
    			rrtvv.setTextColor(0xFF808080);
    			
    		} else {
    			fltv.setTextColor(0xFFFFFFFF);
    			frtv.setTextColor(0xFFFFFFFF);
    			rltv.setTextColor(0xFFFFFFFF);
    			rrtv.setTextColor(0xFFFFFFFF);

    			fltvv.setTextColor(0xFFFFFFFF);
    			frtvv.setTextColor(0xFFFFFFFF);
    			rltvv.setTextColor(0xFFFFFFFF);
    			rrtvv.setTextColor(0xFFFFFFFF);
    		}
        }

        // Temperatures
		TextView pemtv = (TextView) findViewById(R.id.tabCarTextPEM);
		TextView motortv = (TextView) findViewById(R.id.tabCarTextMotor);
		TextView batterytv = (TextView) findViewById(R.id.tabCarTextBattery);
		if (pCarData.stale_car_temps == DataStale.NoValue) {
			pemtv.setText("");
			motortv.setText("");
			batterytv.setText("");
		} else {
			pemtv.setText(pCarData.car_temp_pem);
			motortv.setText(pCarData.car_temp_motor);
			batterytv.setText(pCarData.car_temp_battery);
			if ((pCarData.stale_car_temps == DataStale.Stale)||(!pCarData.car_coolingpump_on)) {
				pemtv.setTextColor(0xFF808080);
				motortv.setTextColor(0xFF808080);
				batterytv.setTextColor(0xFF808080);
			} else {
				pemtv.setTextColor(0xFFFFFFFF);
				motortv.setTextColor(0xFFFFFFFF);
				batterytv.setTextColor(0xFFFFFFFF);
			}
		}

		// Speed
		tv = (TextView) findViewById(R.id.tabCarTextSpeed);
		if (!pCarData.car_started) tv.setText("");
		else tv.setText(pCarData.car_speed);
				
		// Car Hood
		iv = (ImageView) findViewById(R.id.tabCarImageCarHoodOpen);
		iv.setVisibility(pCarData.car_bonnet_open ? View.VISIBLE : View.INVISIBLE);
		
		// Left Door
		iv = (ImageView) findViewById(R.id.tabCarImageCarLeftDoorOpen);
		iv.setVisibility(pCarData.car_frontleftdoor_open ? View.VISIBLE : View.INVISIBLE);
		
		// Right Door
		iv = (ImageView) findViewById(R.id.tabCarImageCarRightDoorOpen);
		iv.setVisibility(pCarData.car_frontrightdoor_open ? View.VISIBLE : View.INVISIBLE);
		
		// Trunk
		iv = (ImageView) findViewById(R.id.tabCarImageCarTrunkOpen);
		iv.setVisibility(pCarData.car_trunk_open ? View.VISIBLE : View.INVISIBLE);
		
		// Headlights
		iv = (ImageView) findViewById(R.id.tabCarImageCarHeadlightsON);
		iv.setVisibility(pCarData.car_headlights_on ? View.VISIBLE : View.INVISIBLE);

		// Car locked
		iv = (ImageView) findViewById(R.id.tabCarImageCarLocked);
		iv.setImageResource(pCarData.car_locked ? R.drawable.carlock : R.drawable.carunlock);
		
		// Valet mode
		iv = (ImageView) findViewById(R.id.tabCarImageCarValetMode);
		iv.setImageResource(pCarData.car_valetmode ? R.drawable.carvaleton : R.drawable.carvaletoff);
		
		// Charge Port
	    iv = (ImageView) findViewById(R.id.tabCarImageCarChargePortOpen);
	    if (!pCarData.car_chargeport_open) {
			iv.setVisibility(View.INVISIBLE);
	    } else {
			iv.setVisibility(View.VISIBLE);
			
			if (pCarData.car_charge_substate_i_raw == 0x07) {
				// We need to connect the power cable
				iv.setImageResource(R.drawable.roadster_outline_cu);
			}
			else if ((pCarData.car_charge_state_i_raw == 0x0d)||(pCarData.car_charge_state_i_raw == 0x0e)||(pCarData.car_charge_state_i_raw == 0x101)) {
				// Preparing to charge, timer wait, or fake 'starting' state
				iv.setImageResource(R.drawable.roadster_outline_ce);
			}
			else if ((pCarData.car_charge_state_i_raw == 0x01)||
					 (pCarData.car_charge_state_i_raw == 0x02)||
					 (pCarData.car_charge_state_i_raw == 0x0f)||
					 (pCarData.car_charging)) {
				// Charging
				iv.setImageResource(R.drawable.roadster_outline_cp);
			}
			else if (pCarData.car_charge_state_i_raw == 0x04) {
				// Charging done
				iv.setImageResource(R.drawable.roadster_outline_cd);
			}
			else if ((pCarData.car_charge_state_i_raw >= 0x15)&&(pCarData.car_charge_state_i_raw <= 0x19)) {
				// Stopped
				iv.setImageResource(R.drawable.roadster_outline_cs);
			}
			else {
				// Fake 0x115 'stopping' state, or something else not understood
				iv.setImageResource(R.drawable.roadster_outline_cp);
			}
	    }
	}
}
