// ---
// Copyright 2020 netty-agents team
// All rights reserved
// ---
package mhadidilek.netty.agents;

import java.net.SocketAddress;
import java.security.InvalidKeyException;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.TimeUnit;

import org.junit.Assert;
import org.junit.Test;

import io.netty.handler.ssl.util.SelfSignedCertificate;
import mhadidilek.netty.agents.TestData.SampleMessage;
import mhadidilek.netty.agents.TestData.SampleRequest;
import mhadidilek.netty.agents.TestData.SampleResponse;
import nettyagents.Context;
import nettyagents.PeerContext;
import nettyagents.Utils;
import nettyagents.agents.ClientAgent;
import nettyagents.agents.ServerAgent;

public class MTNettyAgents {

	@Test
	public void testWithNoTLS() throws Exception {
		System.out.println("Testing with TLS ...");

		ServerAgent serverAgent = new ServerAgent();
		ClientAgent clientAgent = new ClientAgent();

		Context.tlsEnabled = false;
		doStartups(serverAgent, clientAgent);
		doAgentOperations(serverAgent, clientAgent);
	}

	@Test
	public void testWithTLS() throws Exception {
		System.out.println("Testing with TLS ...");

		ServerAgent serverAgent = new ServerAgent();
		ClientAgent clientAgent = new ClientAgent();

		doTLSConfigs(serverAgent, clientAgent);

		{
			X509Certificate serverCert = serverAgent.getConfig().getCert();
			String serverFingerprint = Utils.getFingerprintAsHex(serverCert);
			clientAgent.getContext().getTrustedCerts().put(serverFingerprint, serverCert);
		}

		{
			X509Certificate clientCert = clientAgent.getConfig().getCert();
			String clientFingerprint = Utils.getFingerprintAsHex(clientCert);
			serverAgent.getContext().getTrustedCerts().put(clientFingerprint, clientCert);
		}

		doStartups(serverAgent, clientAgent);
		doAgentOperations(serverAgent, clientAgent);
	}

	@Test
	public void testWithTLSAndPeerIdentificationMode() throws Exception {
		System.out.println("Testing with TLS and peer identification mode...");

		ServerAgent serverAgent = new ServerAgent();
		ClientAgent clientAgent = new ClientAgent();

		doTLSConfigs(serverAgent, clientAgent);

		serverAgent.startPeerIdentificationMode();
		clientAgent.startPeerIdentificationMode();

		doStartups(serverAgent, clientAgent);

		for (Entry<SocketAddress, PeerContext> entry : serverAgent.getClients().entrySet()) {
			SocketAddress socketAddress = entry.getKey();
			PeerContext client = entry.getValue();

			X509Certificate clientCert = client.getCert();
			String clientFingerprint = Utils.getFingerprintAsHex(clientCert);
			System.out.printf("Remote socket: %s - Client fingerprint: %s\n", socketAddress, clientFingerprint);

			{
				client.setTrusted(true);
				serverAgent.getContext().getTrustedCerts().put(clientFingerprint, clientCert);
			}
		}

		X509Certificate serverCert = clientAgent.getServer().getCert();
		String serverFingerprint = Utils.getFingerprintAsHex(serverCert);
		System.out.printf("Server fingerprint: %s\n", serverFingerprint);

		{
			clientAgent.getServer().setTrusted(true);
			clientAgent.getContext().getTrustedCerts().put(serverFingerprint, serverCert);
		}

		doAgentOperations(serverAgent, clientAgent);

//		serverAgent.stopPeerIdentificationMode();
//		clientAgent.stopPeerIdentificationMode();

//		doAgentOperations(serverAgent, clientAgent);
	}

	private void doTLSConfigs(ServerAgent serverAgent, ClientAgent clientAgent) throws CertificateException, CertificateEncodingException, InvalidKeyException {
		{ // --- on server side
			SelfSignedCertificate cert = new SelfSignedCertificate();
			String serverPriKey = Utils.base64Encode(cert.key().getEncoded());
			String serverCert = Utils.base64Encode(cert.cert().getEncoded());

			serverAgent.getConfig().setPriKey(serverPriKey);
			serverAgent.getConfig().setCert(serverCert);
		}

		{ // --- on client side
			SelfSignedCertificate cert = new SelfSignedCertificate();
			String clientPriKey = Utils.base64Encode(cert.key().getEncoded());
			String clientCert = Utils.base64Encode(cert.cert().getEncoded());

			clientAgent.getConfig().setPriKey(clientPriKey);
			clientAgent.getConfig().setCert(clientCert);
		}
	}

	// ---

	private void doAgentOperations(ServerAgent serverAgent, ClientAgent clientAgent) throws InterruptedException {
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
			if (clientAgent.getServer().getChannelHandlerContext() != null) {
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

		TimeUnit.SECONDS.sleep(1);
	}

	private void doStartups(ServerAgent serverAgent, ClientAgent clientAgent) throws InterruptedException {
		{ // --- on server side
			serverAgent.setMessageHandler(SampleMessage.class, message -> handleMessageOnServer(message));
			serverAgent.setRequestHandler(SampleRequest.class, request -> handleRequestOnServer(request));
			serverAgent.startup();
		}

		{ // --- on client side
			clientAgent.setMessageHandler(SampleMessage.class, message -> handleMessageOnClient(message));
			clientAgent.setRequestHandler(SampleRequest.class, request -> handleRequestOnClient(request));
			clientAgent.startup();
		}

		TimeUnit.SECONDS.sleep(1);
	}

	// ---

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
