// ---
// Copyright 2020 netty-agents team
// All rights reserved
// ---
package org.opentoolset.nettyagents;

import org.opentoolset.nettyagents.AbstractMessage;
import org.opentoolset.nettyagents.AbstractRequest;

public interface TestData {

	public static class SampleMessage extends AbstractMessage {

		private static final long serialVersionUID = 1L;

		public String text;

		public SampleMessage() {
			// Required for deserialization
		}

		public SampleMessage(String text) {
			this();
			this.text = text;
		}

		public String getText() {
			return text;
		}
	}

	public static class SampleRequest extends AbstractRequest<SampleResponse> {

		private static final long serialVersionUID = 1L;

		private String text;

		public SampleRequest() {
			// Required for deserialization
		}

		public SampleRequest(String text) {
			this();
			this.text = text;
		}

		@Override
		public Class<SampleResponse> getResponseClass() {
			return SampleResponse.class;
		}

		public String getText() {
			return text;
		}
	}

	public static class SampleResponse extends AbstractMessage {

		private static final long serialVersionUID = 1L;

		private String text;

		public SampleResponse() {
			// Required for deserialization
		}

		public SampleResponse(String text) {
			this();
			this.text = text;
		}

		public String getText() {
			return text;
		}
	}
}
