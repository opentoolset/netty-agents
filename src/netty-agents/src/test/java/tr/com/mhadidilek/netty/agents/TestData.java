// ---
// Copyright 2020 Mustafa Hadi Dilek
// All rights reserved
// ---
package tr.com.mhadidilek.netty.agents;

import tr.com.mhadidilek.netty.AbstractMessage;
import tr.com.mhadidilek.netty.AbstractRequest;

public interface TestData {

	public static class SampleMessage extends AbstractMessage {

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
