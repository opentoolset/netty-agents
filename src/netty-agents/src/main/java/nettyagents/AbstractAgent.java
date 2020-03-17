// ---
// Copyright 2020 netty-agents team
// All rights reserved
// ---
package nettyagents;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.InvalidKeyException;
import java.security.PrivateKey;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.security.interfaces.RSAPrivateKey;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

import javax.net.ssl.X509TrustManager;

import org.slf4j.Logger;

import io.netty.handler.ssl.SslContext;

public abstract class AbstractAgent {

	protected static Logger logger = Context.getLogger();

	private Context context = new Context();

	private SslContext sslContext;

	// ---

	protected abstract AbstractConfig getConfig();

	// ---

	public <TReq extends AbstractRequest<TResp>, TResp extends AbstractMessage> void setRequestHandler(Class<TReq> classOfRequest, Function<TReq, TResp> function) {
		this.context.getMessageReceiver().setRequestHandler(classOfRequest, function);
	}

	public <T extends AbstractMessage> void setMessageHandler(Class<T> classOfMessage, Consumer<T> consumer) {
		this.context.getMessageReceiver().setMessageHandler(classOfMessage, consumer);
	}

	public void startPeerIdentificationMode() {
		this.context.setTrustNegotiationMode(true);
	}

	public void stopPeerIdentificationMode() {
		this.context.setTrustNegotiationMode(false);
	}

	// ---

	public Context getContext() {
		return context;
	}

	protected void startup() {
	}

	protected SslContext getSslContext() {
		return sslContext;
	}

	protected void setSslContext(SslContext sslContext) {
		this.sslContext = sslContext;
	}

	// ---

	public static class AbstractConfig {

		private PrivateKey priKey;
		private X509Certificate cert;

		// ---

		public PrivateKey getPriKey() {
			return priKey;
		}

		public X509Certificate getCert() {
			return cert;
		}

		// ---

		public AbstractConfig setPriKey(String priKeyStr) throws InvalidKeyException {
			return setPriKey(Utils.buildPriKey(priKeyStr));
		}

		public AbstractConfig setPriKey(byte[] priKeyBytes) throws InvalidKeyException {
			RSAPrivateKey priKey = Utils.buildPriKey(priKeyBytes);
			return setPriKey(priKey);
		}

		public AbstractConfig setPriKey(PrivateKey priKey) {
			this.priKey = priKey;
			return this;
		}

		public AbstractConfig setCert(String certStr) throws CertificateException {
			return setCert(Utils.buildCert(certStr));
		}

		public AbstractConfig setCert(byte[] certBytes) throws CertificateException {
			X509Certificate cert = Utils.buildCert(certBytes);
			return setCert(cert);
		}

		public AbstractConfig setCert(X509Certificate cert) {
			this.cert = cert;
			return this;
		}
	}

	// ---

	public static class TrustManager implements X509TrustManager {

		private Supplier<Context> supplier;

		public TrustManager(Supplier<Context> supplier) throws GeneralSecurityException, IOException {
			this.supplier = supplier;
		}

		@Override
		public void checkClientTrusted(X509Certificate[] peerCertChain, String authType) throws CertificateException {
			if (!supplier.get().isTrustNegotiationMode()) {
				Utils.verifyCertChain(peerCertChain, supplier.get().getTrustedCerts());
			}
		}

		@Override
		public void checkServerTrusted(X509Certificate[] peerCertChain, String authType) throws CertificateException {
			if (!supplier.get().isTrustNegotiationMode()) {
				Utils.verifyCertChain(peerCertChain, supplier.get().getTrustedCerts());
			}
		}

		@Override
		public X509Certificate[] getAcceptedIssuers() {
			return new X509Certificate[0];
		}
	}
}
