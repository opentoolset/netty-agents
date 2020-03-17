package org.opentoolset.nettyagents;

import java.io.IOException;

import org.opentoolset.nettyagents.MessageWrapper.Serializer;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

public class SerializerJson implements Serializer {

	@Override
	public String serialize(Object obj) {
		try {
			ObjectMapper mapper = new ObjectMapper();
			mapper.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);
			String serialized = mapper.writeValueAsString(obj);
			return serialized;
		} catch (JsonProcessingException e) {
			Context.getLogger().error(e.getLocalizedMessage(), e);
			return null;
		}
	}

	@Override
	public <T> T deserialize(String serialized, Class<T> classOfObj) {
		try {
			ObjectMapper mapper = new ObjectMapper();
			mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
			T message = mapper.readValue(serialized, classOfObj);
			return message;
		} catch (IOException e) {
			Context.getLogger().error(e.getLocalizedMessage(), e);
			return null;
		}
	}
}
