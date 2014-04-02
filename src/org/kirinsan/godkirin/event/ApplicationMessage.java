package org.kirinsan.godkirin.event;

import java.io.Serializable;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

public class ApplicationMessage implements Serializable {
	private static final long serialVersionUID = 1L;
	public String fromDeviceId;
	public String toDeviceId;

	@Override
	public String toString() {
		return ToStringBuilder.reflectionToString(this, ToStringStyle.MULTI_LINE_STYLE);
	}
}
