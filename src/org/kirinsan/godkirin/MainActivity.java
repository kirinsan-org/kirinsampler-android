package org.kirinsan.godkirin;

import java.util.UUID;

import org.androidannotations.annotations.AfterInject;
import org.androidannotations.annotations.Bean;
import org.androidannotations.annotations.EActivity;
import org.androidannotations.annotations.OptionsItem;
import org.androidannotations.annotations.OptionsMenu;
import org.androidannotations.annotations.Touch;
import org.androidannotations.annotations.UiThread;
import org.androidannotations.annotations.ViewById;
import org.androidannotations.annotations.res.DrawableRes;
import org.apache.mina.core.session.IoSession;
import org.kirinsan.godkirin.event.ApplicationMessage;
import org.kirinsan.godkirin.event.CreateImageEvent;
import org.kirinsan.godkirin.event.DisconnectEvent;
import org.kirinsan.godkirin.event.MatchingEvent;
import org.kirinsan.godkirin.event.MatchingEvent.Translation;
import org.kirinsan.godkirin.event.MoveImageEvent;
import org.kirinsan.godkirin.event.SwipeEvent;
import org.kirinsan.godkirin.event.TranslationMessage;
import org.kirinsan.kirinsampler.R;

import android.gesture.GestureOverlayView;
import android.graphics.drawable.AnimationDrawable;
import android.graphics.drawable.Drawable;
import android.location.Location;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.util.DisplayMetrics;
import android.view.GestureDetector;
import android.view.GestureDetector.SimpleOnGestureListener;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.Interpolator;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesClient;
import com.google.android.gms.location.LocationClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.nineoldandroids.animation.Animator;
import com.nineoldandroids.animation.ObjectAnimator;
import com.nineoldandroids.view.ViewHelper;
import com.nineoldandroids.view.ViewPropertyAnimator;

@EActivity(R.layout.activity_main)
@OptionsMenu(R.menu.main)
public class MainActivity extends ActionBarActivity {
	private String deviceId = UUID.randomUUID().toString(); // デバイスを識別する使い捨てID
	private String targetDeviceId; // 接続相手のデバイスID
	private Translation translation; // 接続相手のスクリーン座標変換
	private GestureDetector gestureDetector;
	private Interpolator interpolator = new AccelerateDecelerateInterpolator();
	private LocationClient locationClient; // 位置情報をモニタリング
	private Location location;
	private DisplayMetrics outMetrics;

	@ViewById
	FrameLayout main;
	@ViewById
	GestureOverlayView swipeView;

	@Bean
	SwipeMatcherServerConnector connector;

	@DrawableRes
	Drawable god;

	@AfterInject
	void init() {
		// ディスプレイ情報を取得
		outMetrics = getResources().getDisplayMetrics();

		// サーバーからのメッセージ受信準備
		connector.setHandler(new SwipeMatcherServerConnector.Callbacks() {
			@Override
			public void matchingDetected(IoSession session, MatchingEvent matchingEvent) {
				onMatchingDetected(matchingEvent);
			}

			@Override
			public void applicationMessage(IoSession session, ApplicationMessage msg) {
				// 座標変換メッセージの場合は先に座標変換を行う
				if (msg instanceof TranslationMessage) {
					TranslationMessage tm = (TranslationMessage) msg;
					tm.x += translation.x;
					tm.y += translation.y;
				}

				if (msg instanceof CreateImageEvent) {
					CreateImageEvent e = (CreateImageEvent) msg;
					createImage(e);
				} else if (msg instanceof MoveImageEvent) {
					MoveImageEvent e = (MoveImageEvent) msg;
					moveImage(e);
				} else if (msg instanceof DisconnectEvent) {
					DisconnectEvent e = (DisconnectEvent) msg;
					remoteDeviceDisconnected(e);
				}
			}

			@Override
			public void remoteDisconnected(IoSession session, DisconnectEvent e) {
				remoteDeviceDisconnected(e);
			}
		});

		// スワイプイベントをサーバーに送る準備
		gestureDetector = new GestureDetector(this, new SimpleOnGestureListener() {
			@Override
			public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
				SwipeEvent e = new SwipeEvent();
				e.deviceId = deviceId;
				e.from.x = e1.getX();
				e.from.y = e1.getY();
				e.to.x = e2.getX();
				e.to.y = e2.getY();
				e.viewSize.width = swipeView.getWidth();
				e.viewSize.height = swipeView.getHeight();

				// 位置情報があれば付与する
				if (location != null) {
					e.geoLocation.longitude = (float) location.getLongitude();
					e.geoLocation.latitude = (float) location.getLatitude();
					e.geoLocation.radius = location.getAccuracy();
				}

				// 送る
				connector.send(e);
				return true;
			}
		});

		// 位置情報をモニタリング
		locationClient = new LocationClient(this, new GooglePlayServicesClient.ConnectionCallbacks() {
			@Override
			public void onDisconnected() {
			}

			@Override
			public void onConnected(Bundle bundle) {
				LocationRequest request = LocationRequest.create();
				locationClient.requestLocationUpdates(request, new LocationListener() {
					@Override
					public void onLocationChanged(Location location) {
						MainActivity.this.location = location;
						System.out.println(location);
					}
				});
			}
		}, new GooglePlayServicesClient.OnConnectionFailedListener() {
			@Override
			public void onConnectionFailed(ConnectionResult result) {
				System.out.println(result);
			}
		});
	}

	protected void onStart() {
		super.onStart();
		connector.connect();
		locationClient.connect();
	}

	@Override
	protected void onStop() {
		connector.closeSession();
		locationClient.disconnect();
		super.onStop();
	}

	@Override
	protected void onDestroy() {
		connector.dispose();
		super.onDestroy();
	}

	@Touch
	public void swipeView(MotionEvent event) {
		gestureDetector.onTouchEvent(event);
	}

	@UiThread
	void onMatchingDetected(MatchingEvent e) {
		this.targetDeviceId = e.deviceId;
		this.translation = e.translation;
		swipeView.setVisibility(View.GONE);

		// 相手デバイスが右にあるなら自分の画面に画像を表示して、相手のデバイスに対応する画像のリクエストを送る
		if (translation.x > 0) {
			CreateImageEvent ev = new CreateImageEvent();
			ev.resId = R.drawable.god;
			ev.toDeviceId = targetDeviceId;
			ev.fromDeviceId = deviceId;
			ev.viewId = R.id.sharedImageView;
			ev.x = 0;
			ev.y = 0;
			ev.width = (int) (god.getIntrinsicWidth() / outMetrics.density);
			ev.height = (int) (god.getIntrinsicHeight() / outMetrics.density);
			connector.send(ev);
			createImage(ev);
		}
	}

	@UiThread
	void createImage(CreateImageEvent e) {
		// dpで送られてくるのでピクセルに変換
		e.width *= outMetrics.density;
		e.height *= outMetrics.density;

		final ImageView imageView = new ImageView(this);
		imageView.setId(e.viewId);
		imageView.setImageResource(e.resId);
		ViewHelper.setTranslationX(imageView, e.x);
		ViewHelper.setTranslationY(imageView, e.y);

		FrameLayout.LayoutParams layoutParams = new FrameLayout.LayoutParams(e.width, e.height);
		layoutParams.gravity = Gravity.CENTER;
		imageView.setLayoutParams(layoutParams);

		main.addView(imageView);

		ObjectAnimator.ofFloat(imageView, "alpha", 0, 1).start();

		imageView.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View v) {
				MoveImageEvent e = new MoveImageEvent();
				e.viewId = imageView.getId();
				e.fromDeviceId = deviceId;
				e.toDeviceId = targetDeviceId;
				e.x = translation.x;
				e.y = translation.y;
				connector.send(e);
				moveImage(e);
			}
		});
	}

	@UiThread
	void moveImage(MoveImageEvent e) {
		final ImageView view = (ImageView) findViewById(e.viewId);
		if (view == null) return;

		ViewPropertyAnimator.animate(view).setDuration(500).setInterpolator(interpolator).translationX(e.x).translationY(e.y).setListener(new AnimatorAdapter() {
			@Override
			public void onAnimationStart(Animator animation) {
				startFrameAnimation(view);
			}

			@Override
			public void onAnimationEnd(Animator animation) {
				stopFrameAnimation(view);
			}
		});
	}

	@UiThread
	void startFrameAnimation(ImageView view) {
		((AnimationDrawable) view.getDrawable()).start();
	}

	@UiThread
	void stopFrameAnimation(ImageView view) {
		((AnimationDrawable) view.getDrawable()).stop();
	}

	@UiThread
	void remoteDeviceDisconnected(DisconnectEvent e) {
		reset();
		Toast.makeText(this, R.string.remote_device_disconnected, Toast.LENGTH_SHORT).show();
	}

	@UiThread
	void reset() {
		targetDeviceId = null;
		final View view = findViewById(R.id.sharedImageView);
		if (view != null) {
			ViewPropertyAnimator.animate(view).rotation(720).scaleX(0).scaleY(0).setListener(new AnimatorAdapter() {
				@Override
				public void onAnimationEnd(Animator animation) {
					main.removeView(view);
				}
			});
		}
		swipeView.setVisibility(View.VISIBLE);
	}

	@OptionsItem
	void menuDisconnect() {
		if (targetDeviceId != null) {
			DisconnectEvent e = new DisconnectEvent();
			e.fromDeviceId = deviceId;
			e.toDeviceId = targetDeviceId;
			connector.send(e);
			Toast.makeText(this, R.string.disconnected, Toast.LENGTH_SHORT).show();
		}
		reset();
	}
}
