package org.kirinsan.godkirin;

import java.net.InetSocketAddress;

import org.androidannotations.annotations.AfterInject;
import org.androidannotations.annotations.Background;
import org.androidannotations.annotations.EBean;
import org.apache.mina.core.service.IoConnector;
import org.apache.mina.core.service.IoHandlerAdapter;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.filter.codec.ProtocolCodecFilter;
import org.apache.mina.filter.codec.serialization.ObjectSerializationCodecFactory;
import org.apache.mina.filter.compression.CompressionFilter;
import org.apache.mina.transport.socket.nio.NioSocketConnector;
import org.kirinsan.godkirin.event.ApplicationMessage;
import org.kirinsan.godkirin.event.DisconnectEvent;
import org.kirinsan.godkirin.event.MatchingEvent;

@EBean
public class SwipeMatcherServerConnector {
	public static interface Callbacks {
		void matchingDetected(IoSession session, MatchingEvent matchingEvent);

		void applicationMessage(IoSession session, ApplicationMessage msg);

		void remoteDisconnected(IoSession session, DisconnectEvent e);
	}

	private final IoConnector connector = new NioSocketConnector();
	private IoSession session;
	private Callbacks callbacks;

	@AfterInject
	void init() {
		connector.getFilterChain().addLast("compress", new CompressionFilter());
		connector.getFilterChain().addLast("codec", new ProtocolCodecFilter(new ObjectSerializationCodecFactory()));
		//		if (BuildConfig.DEBUG) {
		//			connector.setDefaultRemoteAddress(new InetSocketAddress("192.168.10.54", 40404));
		//		} else {
		//			connector.setDefaultRemoteAddress(new InetSocketAddress("133.242.154.31", 40404));
		//		}
		connector.setDefaultRemoteAddress(new InetSocketAddress("49.212.97.201", 40404));
		connector.setHandler(new IoHandlerAdapter() {
			@Override
			public void exceptionCaught(IoSession session, Throwable cause) throws Exception {
				cause.printStackTrace();
			}

			@Override
			public void sessionOpened(IoSession session) throws Exception {
				SwipeMatcherServerConnector.this.session = session;
			}

			@Override
			public void messageReceived(IoSession session, Object message) throws Exception {
				if (message instanceof MatchingEvent) {
					MatchingEvent matchingEvent = (MatchingEvent) message;
					if (callbacks != null) {
						callbacks.matchingDetected(session, matchingEvent);
					}
				} else if (message instanceof ApplicationMessage) {
					ApplicationMessage msg = (ApplicationMessage) message;
					if (callbacks != null) {
						callbacks.applicationMessage(session, msg);
					}
				} else if (message instanceof DisconnectEvent) {
					DisconnectEvent e = (DisconnectEvent) message;
					if (callbacks != null) {
						callbacks.remoteDisconnected(session, e);
					}
				}
			}

			@Override
			public void sessionClosed(IoSession session) throws Exception {
				SwipeMatcherServerConnector.this.session = null;
			}
		});
	}

	public void setHandler(Callbacks callbacks) {
		this.callbacks = callbacks;
	}

	/**
	 * サーバーに接続する。
	 */
	@Background
	void connect() {
		connector.connect();
	}

	/**
	 * コネクターを開放する。
	 */
	public void dispose() {
		connector.dispose();
	}

	/**
	 * コネクションを切断する。
	 */
	public void closeSession() {
		if (session != null) {
			session.close(true);
		}
	}

	/**
	 * スワイプイベントをサーバーに送信
	 * @param se
	 */
	@Background
	void send(Object se) {
		if (session != null) {
			session.write(se);
		}
	}

	@Override
	protected void finalize() throws Throwable {
		connector.dispose(true);
		super.finalize();
	}
}
