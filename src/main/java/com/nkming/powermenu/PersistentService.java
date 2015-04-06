/*
 * PersistentService.java
 *
 * Author: Ming Tsang
 * Copyright (c) 2015 Ming Tsang
 * Refer to LICENSE for details
 */

package com.nkming.powermenu;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Configuration;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.IBinder;
import android.support.v4.app.NotificationCompat;
import android.view.View;

import eu.chainfire.libsuperuser.Shell;

/**
 * Service to display the persistent view
 */
public class PersistentService extends Service
{
	/**
	 * Start the service
	 *
	 * @param context
	 */
	public static void start(Context context)
	{
		Intent intent = new Intent(context, PersistentService.class);
		context.startService(intent);
	}

	/**
	 * Stop the service
	 *
	 * @param context
	 */
	public static void stop(Context context)
	{
		Intent intent = new Intent(context, PersistentService.class);
		intent.setAction(ACTION_STOP);
		context.startService(intent);
	}

	/**
	 * Show the persistent view managed by this service. The view will be shown
	 * automatically during the start of this service so you only need to call
	 * this after hideView()
	 *
	 * @param context
	 * @see PersistentService#hideView(android.content.Context)
	 */
	public static void showView(Context context)
	{
		Intent intent = new Intent(context, PersistentService.class);
		intent.setAction(ACTION_SHOW);
		context.startService(intent);
	}

	/**
	 * Temporarily hide the persistent view
	 *
	 * @param context
	 * @see PersistentService#showView(android.content.Context)
	 */
	public static void hideView(Context context)
	{
		Intent intent = new Intent(context, PersistentService.class);
		intent.setAction(ACTION_HIDE);
		context.startService(intent);
	}

	/**
	 * Return if the service is running or not
	 *
	 * @return
	 */
	public static boolean isRunning()
	{
		return mIsRunning;
	}

	@Override
	public IBinder onBind(Intent intent)
	{
		return null;
	}

	@Override
	public void onCreate()
	{
		Log.d(LOG_TAG, "onCreate");
		mIsRunning = true;
		super.onCreate();
		initView();
		initForeground();
	}

	@Override
	public void onDestroy()
	{
		Log.d(LOG_TAG, "onDestroy");
		mIsRunning = false;
		super.onDestroy();
		uninitForeground();
		uninitView();
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId)
	{
		if (intent.getAction() != null)
		{
			switch (intent.getAction())
			{
			case ACTION_STOP:
				stop();
				break;

			case ACTION_SHOW:
				show();
				break;

			case ACTION_HIDE:
				hide();
				break;
			}
		}
		return START_STICKY;
	}

	private static final String LOG_TAG =
			PersistentService.class.getCanonicalName();
	private static final String ACTION_STOP = "stop";
	private static final String ACTION_SHOW = "show";
	private static final String ACTION_HIDE = "hide";

	private void initView()
	{
		if (mView != null)
		{
			uninitView();
		}

		PersistentView.Config conf = new PersistentView.Config();
		conf.handler = new Handler();
		conf.context = this;
		conf.resId = R.layout.persistent_view;
		conf.alpha = 0.5f;
		conf.hiddenW = 0.35f;
		mView = new PersistentView(conf);
		mView.setOnClickListener(new View.OnClickListener()
		{
			@Override
			public void onClick(View v)
			{
				onViewClick();
			}
		});
		mView.setOnLongClickListener(new View.OnLongClickListener()
		{
			@Override
			public boolean onLongClick(View v)
			{
				onViewLongClick();
				return true;
			}
		});
	}

	private void initForeground()
	{
		NotificationCompat.Builder builder = new NotificationCompat.Builder(this);
		builder.setContentTitle(getString(R.string.notification_title))
				.setContentText(getString(R.string.notification_text))
				.setLocalOnly(true)
				.setOnlyAlertOnce(true)
				.setPriority(NotificationCompat.PRIORITY_MIN)
				.setSmallIcon(R.drawable.ic_action_shutdown)
				.setTicker(getString(R.string.notification_ticker));

		Intent activity = new Intent(this, PreferenceActivity.class);
		activity.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK
				| Intent.FLAG_ACTIVITY_CLEAR_TASK);
		PendingIntent pi = PendingIntent.getActivity(this, 0, activity,
				PendingIntent.FLAG_UPDATE_CURRENT);
		builder.setContentIntent(pi);

		startForeground(1, builder.build());
	}

	private void uninitView()
	{
		if (mView != null)
		{
			mView.destroy();
			mView = null;
		}
	}

	private void uninitForeground()
	{
		stopForeground(true);
	}

	private void onViewClick()
	{
		Log.d(LOG_TAG, "onViewClick");
		SystemHelper.sleep(getApplicationContext());
	}

	private void onViewLongClick()
	{
		Log.d(LOG_TAG, "onViewLongClick");
		new AsyncTask<Void, Void, Void>()
		{
			@Override
			protected Void doInBackground(Void... params)
			{
				Shell.SU.run("am start -n "
						+ MainActivity.class.getPackage().getName()
						+ "/" + MainActivity.class.getCanonicalName());
				return null;
			}
		}.execute();
	}

	private void stop()
	{
		mView.hide(new AnimatorListenerAdapter()
		{
			@Override
			public void onAnimationEnd(Animator animation)
			{
				stopSelf();
			}
		});
	}

	private void show()
	{
		mView.show(null);
	}

	private void hide()
	{
		mView.hide(null);
	}

	private PersistentView mView;

	private static boolean mIsRunning = false;
}
