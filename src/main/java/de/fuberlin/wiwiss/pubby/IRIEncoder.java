package de.fuberlin.wiwiss.pubby;

import java.io.UnsupportedEncodingException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Implements the IRI-to-URI and URI-to-IRI conversions defined in
 * RFC 3987.
 * 
 * TODO: This really needs some unit tests
 */
public class IRIEncoder {

	/**
	 * Converts a URI to an IRI by removing unnecessary percent-encoding
	 * of UTF-8 sequences.
	 */
	public static String toIRI(String uri) {
		StringBuffer decoded = new StringBuffer();
		Matcher matcher = percentEncoding.matcher(uri);
		while (matcher.find()) {
			matcher.appendReplacement(decoded, decode(matcher.group()));
		}
		matcher.appendTail(decoded);
		return decoded.toString();
	}
	private static final Pattern percentEncoding = Pattern.compile("(%[0-9a-fA-F][0-9a-fA-F])+");
	
	/**
	 * Converts an IRI to a URI by percent-encoding characters outside of
	 * the US-ASCII range.
	 */
	public static String toURI(String iri) {
		try {
			StringBuffer encoded = new StringBuffer();
			for (int i = 0; i < iri.length(); i++) {
				if ((int) iri.charAt(i) <= 128) {
					encoded.append(iri.charAt(i));
					continue;
				}
				for (byte b: iri.substring(i, i + 1).getBytes("utf-8")) {
					appendOctet(encoded, b);
				}
			}
			return encoded.toString();
		} catch (UnsupportedEncodingException ex) {
			// Can't happen
			return iri;
		}
	}
	private static char[] hexDigits = {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F'};
	
	private static String decode(String percentEncoded) {
		StringBuffer decoded = new StringBuffer();
		int[] octets = toBytes(percentEncoded);
		int i = 0;
		while (i < octets.length) {
			if (octets[i] <= 0x7F) {
				// US-ASCII character. Decode, except if it's one of
				// %, reserved, or not allowed in IRIs. In that case, re-encode.
				if (isUnreservedASCII((char) octets[i])) {
					decoded.append((char) octets[i]);
				} else {
					// FIXME: Strictly speaking, the spec says that the original
					// percent-encoding remains unchanged, meaning lower-case
					// hex digits would remain lower-case. We upper-case them
					// here by re-encoding.
					appendOctet(decoded, (byte) octets[i]);
				}
				i++;
				continue;
			}
			if (isContinuationOctet(octets[i])) {
				appendOctet(decoded, (byte) octets[i]);
				i++;
				continue;
			}
			int bytesInSequence = getBytesInSequence(octets[i]);
			if (i + bytesInSequence > octets.length) {
				// Not enough continuation bytes to complete the character.
				// Re-encode one byte, then let the main loop eat the rest.
				appendOctet(decoded, (byte) octets[i]);
				i++;
				continue;
			}
			// Next, check if the next n bytes are all continuation bytes.
			boolean enoughContinuationBytes = true;
			for (int j = 1; j < bytesInSequence; j++) {
				if (!isContinuationOctet(octets[i + j])) {
					// Nope
					enoughContinuationBytes = false;
					break;
				}
			}
			if (!enoughContinuationBytes) {
				// Re-encode one byte, and let the main loop eat the rest.
				appendOctet(decoded, (byte) octets[i]);
				i++;
				continue;
			}
			// UTF-8 encoding looks fine. Decode to one character.

			// FIXME: RFC 3987 says here:
			//   4. Re-percent-encode all octets produced in step 3 that in UTF-8
			//      represent characters that are not appropriate according to
			//      sections 2.2, 4.1, and 6.1.
			// This is about weird unicode characters that are inappropriate
			// in IRIs for various reasons. We ignore this currently.
			decoded.append(toCharacter(octets, i, bytesInSequence));
			i += bytesInSequence;
		}
		return decoded.toString();
	}

	private static boolean isContinuationOctet(int octet) {
		return (octet & 0xC0) == 0x80;
	}
	
	private static void appendOctet(StringBuffer sb, byte octet) {
		sb.append('%');
		sb.append(hexDigits[(octet >> 4) & 0x0F]);
		sb.append(hexDigits[octet & 0x0F]);
	}
	
	private static int getBytesInSequence(int octet) {
		// See table in http://en.wikipedia.org/wiki/UTF-8#Description
		if ((octet & 0x80) == 0) return 1;
		if ((octet & 0xC0) == 0x80) return 0;	// Continuation octet
		if ((octet & 0xE0) == 0xC0) return 2;
		if ((octet & 0xF0) == 0xE0) return 3;
		if ((octet & 0xF8) == 0xF0) return 4;
		if ((octet & 0xFC) == 0xF8) return 5;
		if ((octet & 0xFE) == 0xFC) return 6;
		return 0;	// Shouldn't happen
	}
	
	private static char toCharacter(int[] octets, int offset, int length) {
		byte[] bytes = new byte[length];
		for (int i = 0; i < length; i++) {
			bytes[i] = (byte) octets[offset + i];
		}
		try {
			return new String(bytes, "utf-8").charAt(0);
		} catch (UnsupportedEncodingException ex) {
			// Can't happen
			throw new RuntimeException(ex);
		}
	}
	
	private static boolean isUnreservedASCII(char c) {
		// unreserved = ALPHA / DIGIT / "-" / "." / "_" / "~"
		return (c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z') ||
				(c >= '0' && c <= '9') || c == '-' || c == '.' || c == '_' || c == '~';
	}
	
	private static int[] toBytes(String percentEncoded) {
		int length = percentEncoded.length() / 3;
		int[] result = new int[length];
		for (int i = 0; i < length; i++) {
			result[i] = toByte(percentEncoded.charAt(i * 3 + 1), percentEncoded.charAt(i * 3 + 2));
		}
		return result;
	}
	
	private static int toByte(char hex1, char hex2) {
		return (toByte(hex1) << 4) | toByte(hex2);
	}
	
	private static int toByte(char hex) {
		if (hex >= '0' && hex <= '9') {
			return hex - '0';
		}
		if (hex >= 'a' && hex <= 'f') {
			return hex - 'a' + 10;
		}
		if (hex >= 'A' && hex <= 'F') {
			return hex - 'A' + 10;
		}
		throw new IllegalArgumentException("Not a hex digit: " + hex);
	}
}
