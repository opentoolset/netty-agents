package nettyagents;

import java.util.Base64;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

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
}
