package com.openvehicles.OVMS.ui;

import java.text.SimpleDateFormat;

import android.annotation.TargetApi;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.graphics.Typeface;
import android.support.v7.app.AlertDialog;
import android.app.NotificationManager;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.text.Html;
import android.text.util.Linkify;
import android.util.Log;
import android.util.TypedValue;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.luttu.AppPrefes;
import com.openvehicles.OVMS.R;
import com.openvehicles.OVMS.api.OnResultCommandListener;
import com.openvehicles.OVMS.entities.CarData;
import com.openvehicles.OVMS.entities.CmdSeries;
import com.openvehicles.OVMS.utils.CarsStorage;
import com.openvehicles.OVMS.utils.NotificationData;
import com.openvehicles.OVMS.utils.OVMSNotifications;

import static android.util.TypedValue.COMPLEX_UNIT_DIP;


public class NotificationsFragment extends BaseFragment
		implements OnItemClickListener, AdapterView.OnItemLongClickListener,
		TextView.OnEditorActionListener, OnResultCommandListener {
	private static final String TAG = "NotificationsFragment";

	private ListView mListView;
	private ItemsAdapter mItemsAdapter;
	private OVMSNotifications mNotifications;

	private EditText mCmdInput;

	private AppPrefes appPrefes;
	public boolean mFontMonospace = false;

	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

		// Load prefs:

		appPrefes = new AppPrefes(getActivity(), "ovms");
		mFontMonospace = appPrefes.getData("notifications_font_monospace").equals("on");

		// Create UI:

		RelativeLayout layout = (RelativeLayout) inflater.inflate(R.layout.fragment_notifications, null);

		mListView = (ListView) layout.findViewById(R.id.listView);
		mListView.setOnItemClickListener(this);
		mListView.setOnItemLongClickListener(this);

		mCmdInput = (EditText) layout.findViewById(R.id.cmdInput);
		mCmdInput.setOnEditorActionListener(this);

		setHasOptionsMenu(true);

		return layout;
	}

	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
		inflater.inflate(R.menu.notifications_options, menu);
		menu.findItem(R.id.mi_chk_monospace).setChecked(mFontMonospace);
	}

	@Override
	public void onResume() {
		super.onResume();

		// cancel Android system notification:
		NotificationManager mNotificationManager = (NotificationManager) getActivity()
				.getSystemService(Context.NOTIFICATION_SERVICE);
		mNotificationManager.cancelAll();

		// update list:
		update();
	}

	@Override
	public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

		NotificationData data = (NotificationData) parent.getAdapter().getItem(position);

		if (data.Type == NotificationData.TYPE_COMMAND) {
			// use as history:
			mCmdInput.setText(data.Message);
			mCmdInput.requestFocus();
			mCmdInput.postDelayed(new Runnable() {
				@Override
				public void run() {
					InputMethodManager keyboard = (InputMethodManager)
							getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
					keyboard.showSoftInput(mCmdInput, 0);
				}
			},200);

		} else {
			// display:
			Log.d(TAG, "Displaying notification: #" + position);
			AlertDialog dialog = new AlertDialog.Builder(parent.getContext())
					.setIcon(data.getIcon())
					.setTitle(data.Title)
					.setMessage(data.getMessageFormatted())
					.setCancelable(false)
					.setPositiveButton(R.string.Close, new DialogInterface.OnClickListener() {
						@Override
						public void onClick(DialogInterface dialog, int which) {
							dialog.dismiss();
						}
					})
					.show();

			TextView textView = (TextView) dialog.findViewById(android.R.id.message);

			if (mFontMonospace) {
				textView.setTypeface(Typeface.MONOSPACE);
				textView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 8);
			}

			Linkify.addLinks(textView, Linkify.WEB_URLS);

		}
	}


	@Override
	@TargetApi(11)
	public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
		Log.d(TAG, "Long click on notification: #" + position);

		// copy message text to clipboard:

		NotificationData data = (NotificationData) parent.getAdapter().getItem(position);
		String message = data.getMessageFormatted();

		int currentApiVersion = android.os.Build.VERSION.SDK_INT;
		if (currentApiVersion >= android.os.Build.VERSION_CODES.HONEYCOMB) {
			// Android >= 3.0:
			android.content.ClipboardManager clipboard =  (android.content.ClipboardManager)
					getActivity().getSystemService(Context.CLIPBOARD_SERVICE);
			ClipData clip = ClipData.newPlainText("label", message);
			clipboard.setPrimaryClip(clip);
		} else {
			// Android < 3.0:
			android.text.ClipboardManager clipboard = (android.text.ClipboardManager)
					getActivity().getSystemService(Context.CLIPBOARD_SERVICE);
			clipboard.setText(message);
		}

		Toast.makeText(getActivity().getApplicationContext(),
				R.string.notifications_toast_copied, Toast.LENGTH_SHORT).show();

		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {

		int menuId = item.getItemId();
		boolean newState = !item.isChecked();

		switch(menuId) {

			case R.id.mi_help:
				new AlertDialog.Builder(getActivity())
						.setTitle(R.string.notifications_btn_help)
						.setMessage(Html.fromHtml(getString(R.string.notifications_help)))
						.setPositiveButton(android.R.string.ok, null)
						.show();
				return true;

			case R.id.mi_chk_monospace:
				mFontMonospace = newState;
				appPrefes.SaveData("notifications_font_monospace", newState ? "on" : "off");
				item.setChecked(newState);
				mItemsAdapter.notifyDataSetChanged();
				return true;

		}

		return super.onOptionsItemSelected(item);
	}

	public void update() {
		Context context = getActivity();
		if (context != null)
			initUi(context);
	}

	private void initUi(Context pContext) {

		Log.d(TAG, "initUi: (re-)loading notifications");

		// (re-)load notifications:
		mNotifications = new OVMSNotifications(pContext);
		NotificationData[] data = new NotificationData[mNotifications.notifications.size()];
		mNotifications.notifications.toArray(data);
		
		// attach array to ListView:
		mItemsAdapter = new ItemsAdapter(pContext, this, data);
		mListView.setAdapter(mItemsAdapter);

	}

	private int lastCommandSent = 0;

	@Override
	public boolean onEditorAction(TextView textView, int actionId, KeyEvent keyEvent) {
		boolean handled = false;
		if (actionId == EditorInfo.IME_ACTION_SEND) {

			String cmd = textView.getText().toString();
			if (cmd.length() > 0) {

				// add cmd to notifications:
				String vehicle_id = CarsStorage.get().getLastSelectedCarId();
				mNotifications.addNotification(
						NotificationData.TYPE_COMMAND, vehicle_id + ": " + cmd, cmd);
				initUi(getActivity());

				// send command:
				if (cmd.startsWith("*")) {
					// MMI/USSD command
					sendCommand(cmd, "41," + cmd, this);
				}
				else if (cmd.startsWith("@")) {
					// Modem command
					sendCommand(cmd, "49," + cmd.substring(1), this);
				}
				else if (cmd.startsWith("#")) {
					// MSG command
					String cp[] = cmd.substring(1).split(",");
					try {
						lastCommandSent = Integer.parseInt(cp[0]);
						sendCommand(cmd, cmd.substring(1), this);
					} catch (Exception e) {
						Toast.makeText(getActivity(), getString(R.string.err_unimplemented_operation),
								Toast.LENGTH_SHORT).show();
					}
				}
				else {
					// SMS command
					sendCommand(cmd, "7," + cmd, this);
				}

				handled = true;
			}
		}
		return handled;
	}


	@Override
	public void onResultCommand(String[] result) {
		if (result.length <= 1)
			return;

		int command = Integer.parseInt(result[0]);
		String cmdMessage = getSentCommandMessage(result[0]);
		int resCode = Integer.parseInt(result[1]);

		if (command != 7 && command != 41 && command != 49 && command != lastCommandSent)
			return; // not for us

		String cmdOutput = null;
		if (result.length >= 3 && result[2] != null) {
			cmdOutput = result[2];
			for (int i = 3; i < result.length; i++)
				cmdOutput += "," + result[i];
		}

		String vehicle_id = CarsStorage.get().getLastSelectedCarId();

		switch (resCode) {
			case 0: // ok: result[2] = command output
				int type = (command == 41) ? NotificationData.TYPE_USSD
						: NotificationData.TYPE_RESULT_SUCCESS;
				// suppress first (empty) OK result for cmd 41:
				if (command == 7 || cmdOutput != null) {
					mNotifications.addNotification(
							type, vehicle_id + ": " + cmdMessage,
							(cmdOutput != null) ? cmdOutput : getString(R.string.msg_ok));
					initUi(getActivity());
				}
				break;
			case 1: // failed: result[2] = command output
				mNotifications.addNotification(
						NotificationData.TYPE_RESULT_ERROR, vehicle_id + ": " + cmdMessage,
						getString(R.string.err_failed_smscmd));
				initUi(getActivity());
				break;
			case 2: // unsupported
				Toast.makeText(getActivity(), cmdMessage + " => " + getString(R.string.err_unsupported_operation),
						Toast.LENGTH_SHORT).show();
				break;
			case 3: // unimplemented
				Toast.makeText(getActivity(), cmdMessage + " => " + getString(R.string.err_unimplemented_operation),
						Toast.LENGTH_SHORT).show();
				break;
		}
	}


	private static class ItemsAdapter extends ArrayAdapter<NotificationData> {
		private final LayoutInflater mInflater;
		private final SimpleDateFormat mDateFormat = new SimpleDateFormat("MMM d, HH:mm");
		private NotificationsFragment mFragment;

		public ItemsAdapter(Context context, NotificationsFragment fragment, NotificationData[] items) {
			super(context, R.layout.item_notifications, items);
			mInflater = LayoutInflater.from(context);
			mFragment = fragment;
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			View v = convertView;
			if (v == null) {
				v = mInflater.inflate(R.layout.item_notifications, null);
			}

			NotificationData it = getItem(position);
			if (it != null) {

				// set icon according to notification type:
				ImageView iv = (ImageView) v.findViewById(R.id.textNotificationsIcon);
				iv.setImageResource(it.getIcon());

				// set title, message & timestamp:
				TextView tv = (TextView) v.findViewById(R.id.textNotificationsListTitle);
				tv.setText(it.Title);

				tv = (TextView) v.findViewById(R.id.textNotificationsListMessage);

				if (mFragment.mFontMonospace) {
					tv.setTypeface(Typeface.MONOSPACE);
					tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 10);
				} else {
					tv.setTypeface(Typeface.DEFAULT);
					tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
				}

				if (it.Type == NotificationData.TYPE_COMMAND) {
					tv.setVisibility(View.GONE); // cmd shown in title
				} else {
					tv.setVisibility(View.VISIBLE);
					tv.setText(it.getMessageFormatted());
				}

				tv = (TextView) v.findViewById(R.id.textNotificationsListTimestamp);
				tv.setText(mDateFormat.format(it.Timestamp));

			}
			return v;
		}
	}
}
