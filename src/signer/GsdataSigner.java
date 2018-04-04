package signer;

import com.google.common.base.Joiner;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.text.SimpleDateFormat;
import java.util.*;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * @title GsdataSigner
 * @author JJC
 * @createDate 2017年7月1日
 * @version 1.0
 * @details
 */
public class GsdataSigner {
	private static final String ALGORITHM = "GSDATA-HMAC-SHA256";
	private static final String DEFAULT_ENCODING = "UTF-8";
	private static final SimpleDateFormat SDF = new SimpleDateFormat("yyyyMMdd'T'HHmmss'Z'");
	private static final SimpleDateFormat HAX_SDF = new SimpleDateFormat("yyyyMMdd");

	private static BitSet URI_UNRESERVED_CHARACTERS = new BitSet();
	private static String[] PERCENT_ENCODED_STRINGS = new String[256];
	private static final Set<String> defaultHeadersToSign = Sets.newHashSet();
	private static final Joiner headerJoiner = Joiner.on('\n');
	private static final Joiner signedHeaderStringJoiner = Joiner.on(';');
	private static final Joiner queryStringJoiner = Joiner.on('&');

	static {
		GsdataSigner.defaultHeadersToSign.add(Headers.HOST.toLowerCase());
		// GsdataSigner.defaultHeadersToSign.add(Headers.CONTENT_TYPE.toLowerCase());
		GsdataSigner.defaultHeadersToSign.add(Headers.X_GSDATA_DATE.toLowerCase());
		// GsdataSigner.defaultHeadersToSign.add(Headers.USER_AGENT.toLowerCase());
		
		SDF.setTimeZone(TimeZone.getTimeZone("UTC"));
		HAX_SDF.setTimeZone(TimeZone.getTimeZone("UTC"));
		for (int i = 'a'; i <= 'z'; i++) {
			URI_UNRESERVED_CHARACTERS.set(i);
		}
		for (int i = 'A'; i <= 'Z'; i++) {
			URI_UNRESERVED_CHARACTERS.set(i);
		}
		for (int i = '0'; i <= '9'; i++) {
			URI_UNRESERVED_CHARACTERS.set(i);
		}
		URI_UNRESERVED_CHARACTERS.set('-');
		URI_UNRESERVED_CHARACTERS.set('.');
		URI_UNRESERVED_CHARACTERS.set('_');
		URI_UNRESERVED_CHARACTERS.set('~');

		for (int i = 0; i < PERCENT_ENCODED_STRINGS.length; ++i) {
			PERCENT_ENCODED_STRINGS[i] = String.format("%%%02X", i);
		}

	}

	// 签名算法
	public void sign(InternalRequest request) throws Exception {
		checkNotNull(request, "request should not be null.");
		Date timestamp = new Date();
		String dataTime = SDF.format(timestamp);
		request.addHeader(Headers.HOST, "api.gsdata.cn");
		//request.addHeader(Headers.CONTENT_TYPE,
		//"application/x-www-form-urlencoded; charset=utf-8");
		request.addHeader(Headers.X_GSDATA_DATE, dataTime);
		// request.addHeader(Headers.USER_AGENT, "GSDATA-v1.0.0-SDK");

		// 1.请求类型"20170703T035143Z"
		// 2.用签名协议格式化URL。
		String canonicalURI = this.getCanonicalURIPath(request.getUri().getPath());
		// 使用签名协议格式化查询字符串。
		String canonicalQueryString = getCanonicalQueryString(request.getParameters(), true);
		// 请求头标题签名排序。
		SortedMap<String, String> headersToSign = getHeadersToSign(request.getHeaders(), defaultHeadersToSign);
		// 基于签名协议格式化请求头标题。
		String canonicalHeader = getCanonicalHeaders(headersToSign);
		// 规范请求标头
		String signedHeaders = "";
		if (defaultHeadersToSign != null) {
			signedHeaders = signedHeaderStringJoiner.join(headersToSign.keySet());
			signedHeaders = signedHeaders.trim().toLowerCase();
		}
		// 编码负载文本
		String requestPayload = encode((request.getContent() == null ? "".getBytes(DEFAULT_ENCODING)
				: request.getContent().getBytes(DEFAULT_ENCODING)));

		String canonicalRequest = request.getHttpMethod() + "\n" + canonicalURI + "\n" + canonicalQueryString + "\n"
				+ canonicalHeader + "\n" + signedHeaders + "\n" + requestPayload;

		// 签名使用SHA-256算法。
		String hashedCanonicalRequest = encode(canonicalRequest.getBytes(DEFAULT_ENCODING));
		String data = HAX_SDF.format(timestamp);
		String stringToSign = ALGORITHM + "\n" + dataTime + "\n" + hashedCanonicalRequest;
		byte[] signKey = getSignatureKey(request.getAppkey(), data, canonicalURI);
		String signature = base16(HmacSHA256(stringToSign, signKey));
		String authorization = ALGORITHM + " AppKey=" + request.getAppId() + ", SignedHeaders=" + signedHeaders
				+ ", Signature=" + signature;
		request.addHeader(Headers.AUTHORIZATION, authorization);
	}

	private String getCanonicalURIPath(String path) {
		if (path == null) {
			return "/";
		} else if (path.startsWith("/")) {
			return normalizePath(path);
		} else {
			return "/" + normalizePath(path);
		}
	}

	private static String normalizePath(String path) {
		return normalize(path).replace("%2F", "/");
	}

	private static String normalize(String value) {
		try {
			StringBuilder builder = new StringBuilder();
			for (byte b : value.getBytes(DEFAULT_ENCODING)) {
				if (URI_UNRESERVED_CHARACTERS.get(b & 0xFF)) {
					builder.append((char) b);
				} else {
					builder.append(PERCENT_ENCODED_STRINGS[b & 0xFF]);
				}
			}
			return builder.toString();
		} catch (UnsupportedEncodingException e) {
			throw new RuntimeException(e);
		}
	}

	private String getCanonicalHeaders(SortedMap<String, String> headers) {
		if (headers.isEmpty()) {
			return "";
		}

		List<String> headerStrings = Lists.newArrayList();
		for (Map.Entry<String, String> entry : headers.entrySet()) {
			String key = entry.getKey();
			if (key == null) {
				continue;
			}
			String value = entry.getValue();
			if (value == null) {
				value = "";
			}
			headerStrings.add(normalize(key.trim().toLowerCase()) + ':' + normalize(value.trim()));
		}
		Collections.sort(headerStrings);

		return headerJoiner.join(headerStrings);
	}

	public static String getCanonicalQueryString(Map<String, Object> parameters, boolean forSignature) {
		if (parameters.isEmpty()) {
			return "";
		}

		List<String> parameterStrings = Lists.newArrayList();
		for (Map.Entry<String, Object> entry : parameters.entrySet()) {
			if (forSignature && Headers.AUTHORIZATION.equalsIgnoreCase(entry.getKey())) {
				continue;
			}
			String key = entry.getKey();
			checkNotNull(key, "parameter key should not be null");
			String value = entry.getValue().toString();
			if (value == null) {
				if (forSignature) {
					parameterStrings.add(normalize(key) + '=');
				} else {
					parameterStrings.add(normalize(key));
				}
			} else {
				parameterStrings.add(normalize(key) + '=' + normalize(value));
			}
		}
		Collections.sort(parameterStrings);

		return queryStringJoiner.join(parameterStrings);
	}

	/**
	 * 字符串 SHA 加密
	 * 
	 * @param strSourceText
	 * @return
	 * @throws Exception
	 */
	private static String encode(final byte[] content) throws Exception {
		// 返回值
		String strResult = null;

		// 是否是有效字符串
		if (content != null) {

			// SHA 加密开始
			// 创建加密对象 并传入加密类型
			MessageDigest messageDigest = MessageDigest.getInstance("SHA-256");
			// 传入要加密的字符串
			messageDigest.update(content);
			// 得到 byte 类型结果
			byte byteBuffer[] = messageDigest.digest();
			strResult = base16(byteBuffer);
		}

		return strResult;
	}

	/**
	 * 对字节数据进行Base16编码。
	 * 
	 * @param src
	 *            源字节数组
	 * @return 编码后的字符串
	 */
	private static String base16(byte src[]) throws Exception {
		StringBuffer strbuf = new StringBuffer(src.length * 2);
		int i;

		for (i = 0; i < src.length; i++) {
			if (((int) src[i] & 0xff) < 0x10) {
				strbuf.append("0");
			}
			strbuf.append(Long.toString((int) src[i] & 0xff, 16));
		}

		return strbuf.toString();
	}

	private SortedMap<String, String> getHeadersToSign(Map<String, String> headers, Set<String> headersToSign) {
		SortedMap<String, String> ret = Maps.newTreeMap();
		if (headersToSign != null) {
			Set<String> tempSet = Sets.newHashSet();
			for (String header : headersToSign) {
				tempSet.add(header.trim().toLowerCase());
			}
			headersToSign = tempSet;
		}
		for (Map.Entry<String, String> entry : headers.entrySet()) {
			String key = entry.getKey();
			if (entry.getValue() != null && !entry.getValue().isEmpty()) {
				if ((headersToSign == null && isDefaultHeaderToSign(key)) || (headersToSign != null
						&& headersToSign.contains(key.toLowerCase()) && !Headers.AUTHORIZATION.equalsIgnoreCase(key))) {
					ret.put(key, entry.getValue());
				}
			}
		}
		return ret;
	}

	private boolean isDefaultHeaderToSign(String header) {
		header = header.trim().toLowerCase();
		return defaultHeadersToSign.contains(header);
	}

	private static byte[] HmacSHA256(String data, byte[] key) throws Exception {
		String algorithm = "HmacSHA256";
		Mac mac = Mac.getInstance(algorithm);
		mac.init(new SecretKeySpec(key, algorithm));
		return mac.doFinal(data.getBytes(DEFAULT_ENCODING));
	}

	private static byte[] getSignatureKey(String key, String dateStamp, String serviceName) throws Exception {
		byte[] kSecret = ("GSDATA" + key).getBytes(DEFAULT_ENCODING);
		byte[] kDate = HmacSHA256(dateStamp, kSecret);
		byte[] kService = HmacSHA256(serviceName, kDate);
		byte[] kSigning = HmacSHA256("gsdata_request", kService);
		return kSigning;
	}

}
