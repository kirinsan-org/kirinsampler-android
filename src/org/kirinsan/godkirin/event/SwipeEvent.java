package org.kirinsan.godkirin.event;

import java.io.Serializable;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

public class SwipeEvent implements Serializable {
	private static final long serialVersionUID = 1L;
	public String deviceId;
	public Point from;
	public Point to;
	public GeoLocation geoLocation;
	public Size viewSize;

	public SwipeEvent() {
		from = new Point();
		to = new Point();
		geoLocation = new GeoLocation();
		viewSize = new Size();
	}

	@Override
	public String toString() {
		return ToStringBuilder.reflectionToString(this, ToStringStyle.MULTI_LINE_STYLE);
	}

	public static class Point implements Serializable {
		private static final long serialVersionUID = 1L;
		public float x;
		public float y;

		@Override
		public String toString() {
			return ToStringBuilder.reflectionToString(this, ToStringStyle.SHORT_PREFIX_STYLE);
		}
	}

	public static class GeoLocation implements Serializable {
		private static final long serialVersionUID = 1L;
		public float longitude;
		public float latitude;
		public float radius;

		@Override
		public String toString() {
			return ToStringBuilder.reflectionToString(this, ToStringStyle.SHORT_PREFIX_STYLE);
		}
	}

	public static class Size implements Serializable {
		private static final long serialVersionUID = 1L;
		public float width;
		public float height;

		@Override
		public String toString() {
			return ToStringBuilder.reflectionToString(this, ToStringStyle.SHORT_PREFIX_STYLE);
		}
	}
}
