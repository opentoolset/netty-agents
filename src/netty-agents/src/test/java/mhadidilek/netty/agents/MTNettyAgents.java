// ---
// Copyright 2020 netty-agents team
// All rights reserved
// ---
package mhadidilek.netty.agents;

import java.net.SocketAddress;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import io.netty.handler.ssl.util.SelfSignedCertificate;
import mhadidilek.netty.agents.TestData.SampleMessage;
import mhadidilek.netty.agents.TestData.SampleRequest;
import mhadidilek.netty.agents.TestData.SampleResponse;
import nettyagents.AbstractAgent.AbstractConfig.Peer;
import nettyagents.PeerContext;
import nettyagents.Utils;
import nettyagents.agents.ClientAgent;
import nettyagents.agents.ServerAgent;

public class MTNettyAgents {

	private static ServerAgent serverAgent;
	private static ClientAgent clientAgent;
	private static String serverPriKey;
	private static String serverCert;
	private static String clientPriKey;
	private static String clientCert;

	// ---

	@Test
	public void test() throws Exception {
		serverAgent.startup();
		clientAgent.startup();

		Map<SocketAddress, PeerContext> clients = null;
		while (true) {
			clients = serverAgent.getClients();
			if (!clients.isEmpty()) {
				break;
			} else {
				TimeUnit.SECONDS.sleep(1);
			}
		}

		while (true) {
			if (clientAgent.getServerContext().getChannelHandlerContext() != null) {
				break;
			} else {
				TimeUnit.SECONDS.sleep(1);
			}
		}

		PeerContext client = clients.values().iterator().next();
		System.out.printf("Client: %s\n", client);

		{
			// --- client to server:

			clientAgent.sendMessage(new SampleMessage("Sample message to server"));

			SampleResponse response = clientAgent.doRequest(new SampleRequest("Sample request to server"));
			System.out.printf("Response received from server: %s\n", response);
			Assert.assertNotNull(response);
		}

		{
			// --- server to client:

			serverAgent.sendMessage(new SampleMessage("Sample message to client"), client);

			SampleResponse response = serverAgent.doRequest(new SampleRequest("Sample request to client"), client);
			System.out.printf("Response received from client: %s\n", response);
			Assert.assertNotNull(response);
		}

		clientAgent.shutdown();
		serverAgent.shutdown();
	}

	// ---

	@BeforeClass
	public static void beforeClass() throws Exception {
		serverAgent = new ServerAgent();
		clientAgent = new ClientAgent();

		{
			SelfSignedCertificate cert = new SelfSignedCertificate();
			serverPriKey = Utils.base64Encode(cert.key().getEncoded());
			serverCert = Utils.base64Encode(cert.cert().getEncoded());

			serverAgent.getConfig().setPriKey(serverPriKey);
			serverAgent.getConfig().setCert(serverCert);
			serverAgent.setMessageHandler(SampleMessage.class, message -> handleMessageOnServer(message));
			serverAgent.setRequestHandler(SampleRequest.class, request -> handleRequestOnServer(request));

			clientAgent.getConfig().getTrustedPeers().add(new Peer(serverCert));
		}

		{
			SelfSignedCertificate cert = new SelfSignedCertificate();
			clientPriKey = Utils.base64Encode(cert.key().getEncoded());
			clientCert = Utils.base64Encode(cert.cert().getEncoded());

			clientAgent.getConfig().setPriKey(clientPriKey);
			clientAgent.getConfig().setCert(clientCert);
			clientAgent.setMessageHandler(SampleMessage.class, message -> handleMessageOnClient(message));
			clientAgent.setRequestHandler(SampleRequest.class, request -> handleRequestOnClient(request));

			serverAgent.getConfig().getTrustedPeers().add(new Peer(clientCert));
		}
	}

	private static void handleMessageOnServer(SampleMessage message) {
		System.out.printf("Message received on server: %s\n", message);
	}

	private static SampleResponse handleRequestOnServer(SampleRequest request) {
		System.out.printf("Request received on server: %s\n", request);
		SampleResponse response = new SampleResponse("Sample response from server");
		System.out.printf("Response sending on server: %s\n", response);
		return response;
	}

	private static void handleMessageOnClient(SampleMessage message) {
		System.out.printf("Message received on client: %s\n", message);
	}

	private static SampleResponse handleRequestOnClient(SampleRequest request) {
		System.out.printf("Request received on client: %s\n", request);
		SampleResponse response = new SampleResponse("Sample response from client");
		System.out.printf("Response sending on client: %s\n", response);
		return response;
	}
}
