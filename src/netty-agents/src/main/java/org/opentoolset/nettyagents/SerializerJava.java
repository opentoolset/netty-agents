package org.opentoolset.nettyagents;

import java.io.Serializable;

import org.apache.commons.lang3.SerializationException;
import org.apache.commons.lang3.SerializationUtils;
import org.opentoolset.nettyagents.MessageWrapper.Serializer;

@Deprecated
public class SerializerJava implements Serializer {

	@Override
	public String serialize(Object object) {
		if (object instanceof Serializable) {
			Serializable serializable = (Serializable) object;
			byte[] serialized = SerializationUtils.serialize(serializable);
			return new String(serialized, Constants.DEFAULT_CHARSET);
		} else {
			Context.getLogger().error(String.format("Object is not serializable, object: %s", object), new SerializationException());
			return null;
		}
	}

	@Override
	public <T> T deserialize(String serialized, Class<T> classOfObj) {
		try {
			Object object = SerializationUtils.deserialize(serialized.getBytes(Constants.DEFAULT_CHARSET));
			if (classOfObj.isInstance(object)) {
				@SuppressWarnings("unchecked")
				T message = (T) object;
				return message;
			}
		} catch (Exception e) {
			Context.getLogger().error(e.getLocalizedMessage(), e);
		}

		return null;
	}
}
