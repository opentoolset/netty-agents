// ---
// Copyright 2020 netty-agents team
// All rights reserved
// ---
package org.opentoolset.nettyagents;

public interface TestData {

	public static class SampleMessage extends AbstractMessage {

		private String text;

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

		private int number;

		public SampleRequest() {
			// Required for deserialization
		}

		public SampleRequest(String text, int number) {
			this();
			this.text = text;
			this.number = number;
		}

		@Override
		public Class<SampleResponse> getResponseClass() {
			return SampleResponse.class;
		}

		public String getText() {
			return text;
		}

		public int getNumber() {
			return number;
		}
	}

	public static class SampleResponse extends AbstractMessage {

		private String text;

		private int number;

		public SampleResponse() {
			// Required for deserialization
		}

		public SampleResponse(String text, int number) {
			this();
			this.text = text;
			this.number = number;
		}

		public String getText() {
			return text;
		}

		public int getNumber() {
			return number;
		}
	}
}
