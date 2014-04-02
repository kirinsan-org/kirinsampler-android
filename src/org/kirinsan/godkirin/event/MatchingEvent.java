package org.kirinsan.godkirin.event;

import java.io.Serializable;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

public class MatchingEvent implements Serializable {
	private static final long serialVersionUID = 1L;
	public String deviceId;
	public Translation translation;

	public MatchingEvent() {
		translation = new Translation();
	}

	@Override
	public String toString() {
		return ToStringBuilder.reflectionToString(this, ToStringStyle.MULTI_LINE_STYLE);
	}

	public static class Translation implements Serializable {
		private static final long serialVersionUID = 1L;
		public float x;
		public float y;

		@Override
		public String toString() {
			return ToStringBuilder.reflectionToString(this, ToStringStyle.SHORT_PREFIX_STYLE);
		}
	}
}
