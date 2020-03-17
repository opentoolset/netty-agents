// ---
// Copyright 2020 netty-agents team
// All rights reserved
// ---
package org.opentoolset.nettyagents;

import java.io.Serializable;
import java.util.UUID;

import org.apache.commons.lang3.builder.ReflectionToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

public class MessageWrapper implements Serializable {

	private static final long serialVersionUID = 7776235450857746832L;

	public interface Serializer {

		String serialize(Object obj);

		<T> T deserialize(String serialized, Class<T> classOfObj);
	}

	public transient static Serializer serializer = new SerializerJson();

	private transient static Serializer outerSerializer = new SerializerJson();

	private Class<? extends AbstractMessage> classOfMessage;
	private String serializedMessage;

	private String id;
	private String correlationId;

	// ---

	public MessageWrapper() {
	}

	public static <T extends AbstractMessage> MessageWrapper create(T message) {
		MessageWrapper messageWrapper = new MessageWrapper();
		messageWrapper.classOfMessage = message.getClass();
		messageWrapper.serializedMessage = serializer.serialize(message);
		return messageWrapper;
	}

	public static <T extends AbstractRequest<?>> MessageWrapper createRequest(T message) {
		MessageWrapper messageWrapper = create(message);
		messageWrapper.id = UUID.randomUUID().toString();
		return messageWrapper;
	}

	public static <T extends AbstractMessage> MessageWrapper createResponse(T message, String correlationId) {
		MessageWrapper messageWrapper = create(message);
		messageWrapper.correlationId = correlationId;
		return messageWrapper;
	}

	// --- Getters:

	public String getId() {
		return id;
	}

	public String getCorrelationId() {
		return correlationId;
	}

	public Class<? extends AbstractMessage> getClassOfMessage() {
		return classOfMessage;
	}

	public String getSerializedMessage() {
		return serializedMessage;
	}

	// --- Helper methods:

	public String serialize() {
		return serialize(this);
	}

	public AbstractMessage deserializeMessage() {
		AbstractMessage message = serializer.deserialize(this.serializedMessage, this.classOfMessage);
		return message;
	}

	public <T extends AbstractMessage> T deserializeMessage(Class<T> classOfMessage) {
		T message = serializer.deserialize(this.serializedMessage, classOfMessage);
		return message;
	}

	@Override
	public String toString() {
		return ReflectionToStringBuilder.toString(this, ToStringStyle.SHORT_PREFIX_STYLE);
	}

	// ---

	public static String serialize(MessageWrapper messageWrapper) {
		String serialized = outerSerializer.serialize(messageWrapper);
		return serialized;
	}

	public static MessageWrapper deserialize(String serializedMessageWrapper) {
		MessageWrapper messageWrapper = outerSerializer.deserialize(serializedMessageWrapper, MessageWrapper.class);
		return messageWrapper;
	}
}
