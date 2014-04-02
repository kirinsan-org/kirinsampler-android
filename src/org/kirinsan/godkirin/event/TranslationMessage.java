package org.kirinsan.godkirin.event;

/**
 * 座標変換を行うメッセージ。x,yの値は受信時に相手デバイスへの座標が加算されてから伝達される。
 */
public class TranslationMessage extends ApplicationMessage {
	private static final long serialVersionUID = 1L;
	public float x;
	public float y;
}
