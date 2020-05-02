// ---
// Copyright 2020 netty-agents team
// All rights reserved
// ---
package org.opentoolset.nettyagents;

import java.util.UUID;

import org.apache.commons.lang3.builder.ReflectionToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

public class MessageWrapper {

	public interface Serializer {

		String serialize(Object obj);

		<T> T deserialize(String serialized, Class<T> classOfObj);
	}

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
		messageWrapper.serializedMessage = getSerializer().serialize(message);
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
		AbstractMessage message = getSerializer().deserialize(this.serializedMessage, this.classOfMessage);
		return message;
	}

	public <T extends AbstractMessage> T deserializeMessage(Class<T> classOfMessage) {
		T message = getSerializer().deserialize(this.serializedMessage, classOfMessage);
		return message;
	}

	@Override
	public String toString() {
		return ReflectionToStringBuilder.toString(this, ToStringStyle.SHORT_PREFIX_STYLE);
	}

	// ---

	public static String serialize(MessageWrapper messageWrapper) {
		String serialized = getOuterSerializer().serialize(messageWrapper);
		return serialized;
	}

	public static MessageWrapper deserialize(String serializedMessageWrapper) {
		MessageWrapper messageWrapper = getOuterSerializer().deserialize(serializedMessageWrapper, MessageWrapper.class);
		return messageWrapper;
	}

	// ---

	private static Serializer getSerializer() {
		return Context.getSerializer();
	}

	private static Serializer getOuterSerializer() {
		return Context.getOuterSerializer();
	}
}
