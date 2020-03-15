// ---
// Copyright 2020 netty-agents team
// All rights reserved
// ---
package nettyagents;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.cert.CertPath;
import java.security.cert.CertPathValidator;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.PKIXCertPathValidatorResult;
import java.security.cert.PKIXParameters;
import java.security.cert.TrustAnchor;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.Base64;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

import nettyagents.AbstractAgent.AbstractConfig.Peer;

public class Utils {

	public static boolean waitUntil(Supplier<Boolean> tester, int timeoutSec) {
		try {
			for (int i = 0; i < timeoutSec; i++) {
				if (tester.get()) {
					break;
				}
				TimeUnit.SECONDS.sleep(1);
			}
		} catch (InterruptedException e) {
			Context.getLogger().warn(e.getLocalizedMessage(), e);
		}

		return tester.get();
	}

	public static String base64Encode(byte[] bytes) {
		return Base64.getEncoder().encodeToString(bytes);
	}

	public static byte[] base64Decode(String str) {
		return Base64.getDecoder().decode(str);
	}

	public static void verifyCertChain(X509Certificate[] peerCertChain, List<Peer> trustedPeers) throws CertificateException {
		CertificateFactory certFactory = CertificateFactory.getInstance("X.509");
		X509Certificate certPEM = peerCertChain[0];
		CertPath certPath = certFactory.generateCertPath(Arrays.asList(certPEM));

		try {
			CertPathValidator certPathValidator = CertPathValidator.getInstance("PKIX");

			for (Peer trustedPeer : trustedPeers) {
				try {
					Set<TrustAnchor> trustAnchors = new HashSet<>();
					TrustAnchor trustAnchor = buildTrustAnchor(trustedPeer.getCert());
					trustAnchors.add(trustAnchor);

					PKIXParameters params = new PKIXParameters(trustAnchors);
					params.setRevocationEnabled(false);

					PKIXCertPathValidatorResult result = (PKIXCertPathValidatorResult) certPathValidator.validate(certPath, params);
					@SuppressWarnings("unused")
					PublicKey publicKey = result.getPublicKey();
					// Context.getLogger().info("Certificate verified. Public key: {}", publicKey);
					return;
				} catch (GeneralSecurityException | IOException e) {
					// Context.getLogger().debug(e.getLocalizedMessage(), e);
				}
			}
		} catch (NoSuchAlgorithmException e) {
			Context.getLogger().error(e.getLocalizedMessage(), e);
		}

		throw new CertificateException("Certificate couldn't be verified by custom trust manager...");
	}

	// ---

	private static TrustAnchor buildTrustAnchor(String peerCertStr) throws GeneralSecurityException, IOException {
		byte[] peerCertBytes = Utils.base64Decode(peerCertStr);
		@SuppressWarnings("restriction")
		sun.security.x509.X509CertImpl peerCert = new sun.security.x509.X509CertImpl(peerCertBytes);
		TrustAnchor trustAnchor = new TrustAnchor(peerCert, null);
		return trustAnchor;
	}
}
