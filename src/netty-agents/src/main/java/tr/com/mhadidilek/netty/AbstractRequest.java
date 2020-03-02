// ---
// Copyright 2020 Mustafa Hadi Dilek
// All rights reserved
// ---
package tr.com.mhadidilek.netty;

import org.apache.commons.lang3.builder.ReflectionToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

public abstract class AbstractRequest<T extends AbstractMessage> extends AbstractMessage {

	public abstract Class<T> getResponseClass();

	@Override
	public String toString() {
		return ReflectionToStringBuilder.toString(this, ToStringStyle.SHORT_PREFIX_STYLE);
	}
}
