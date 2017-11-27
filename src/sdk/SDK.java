package sdk;

import com.alibaba.fastjson.JSONArray;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.apache.http.NameValuePair;
import signer.GsdataSigner;
import signer.Headers;
import signer.InternalRequest;

import java.io.IOException;
import java.net.URI;
import java.nio.charset.Charset;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.ArrayList;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * @title SDK
 * @author JJC
 * @createDate 2017年7月1日
 * @version 1.0
 * @details
 */
public class SDK {
	private String method;
	private String url;
	private String appKey;
	private String appId;

	public SDK(String method, String url, String appKey, String appId) {
		this.method = method;
		this.url = url;
		this.appKey = appKey;
		this.appId = appId;
	}

	public String send(Map<String, Object> params, boolean isJson) throws Exception {
		checkNotNull(method, "method should not be null.");
		checkNotNull(url, "method should not be null.");
		method = method.toUpperCase();
		InternalRequest request = new InternalRequest(method, new URI(url));
		request.setAppkey(appKey);
		request.setAppId(appId);
		String result = null;
		switch (method) {
		case "GET":
			request.setParameters(params);
			result = getSend(request, params);
			break;

		case "POST":
			String json = null;
            request.addHeader("Accept","application/json");
			if (params != null) {
				if (isJson) {
                    request.addHeader(Headers.CONTENT_TYPE,"application/json; charset=utf-8");
					json = JSONArray.toJSONString(params);
					request.setContent(json);
				} else {
                    String param = "";
                    for (String entry : params.keySet()) {
                        param += entry + "=" + params.get(entry) + "&";
                    }
                    param = param.substring(0, param.length() - 1);
                    request.setContent(param);
                    request.addHeader(Headers.CONTENT_TYPE,"application/x-www-form-urlencoded; charset=utf-8");
					//param = param.substring(0, param.length() - 1);
					//request.setContent(json);
				}

			}
			result = postSend(request,isJson);
			break;

		default:
			throw new RuntimeException("请求模式不存在");
		}
		return result;
	}

	private static String postSend(InternalRequest request, boolean isJson) throws Exception {
		new GsdataSigner().sign(request);
		return post(request.getUri().toString(), request.getContent(), request.getHeaders(), "UTF-8",isJson);
	}

	private static String getSend(InternalRequest request, Map<String, Object> params) throws Exception {
		new GsdataSigner().sign(request);
		String URL = null;
		String param = GsdataSigner.getCanonicalQueryString(params, true);
		if ("".equals(param)) {
			URL = request.getUri().toString();
		} else {
			URL = request.getUri().toString() + "?" + param;
		}
		return get(URL, "UTF-8", request.getHeaders());
	}

    /**
     *
     * @param URL       请求地址
     * @param charset   字符集
     * @param headers   自定义header 头信息
     * @return
     */
	private static String get(final String URL, final String charset, final Map<String, String> headers) {
		if (URL == null) {
			throw new NullPointerException("[URL is not null]");
		}
		CloseableHttpClient httpClient = HttpClients.createDefault();

		String html = null;
		CloseableHttpResponse response = null;
		HttpGet httpGet = new HttpGet(URL);
		for (Entry<String, String> e : headers.entrySet()) {
			httpGet.addHeader(e.getKey(), e.getValue());
		}
		try {

			// 重试
			boolean isRetry = true;
			int retryRequestCount = 0;
			long cycleSleepTime = 3000;
			int cycleRetryTimes = 2;
			int responseCode = 200;
			do {
				// 执行get请求
				response = httpClient.execute(httpGet);
				responseCode = response.getStatusLine().getStatusCode();
				// 获取响应实体
				HttpEntity entity = response.getEntity();
				if (entity != null) {
					html = EntityUtils.toString(entity, charset);
				}
				// 重试
				if (responseCode != 200 || "".equals(html.trim())) {
					Thread.sleep(cycleSleepTime);
					retryRequestCount++;
				}
			} while (isRetry && responseCode != 200 && retryRequestCount < cycleRetryTimes);

		} catch (Exception e) {
			return null;
		} finally {
			try {
				if (response != null) {
					response.close();
				}
				if (httpClient != null) {
					httpClient.close();
				}
			} catch (IOException e) {
			}
		}
		return html;
	}

	/**
	 * 
	 * @param URL
	 *            请求地址
	 * @param params
	 *            参数列表
	 * @return
	 */
	private static String post(final String URL, final String params, final Map<String, String> headers,
			final String charset,boolean isJson) {
		if (URL == null) {
			throw new NullPointerException("[URL is not null]");
		}
		CloseableHttpClient httpClient = null;
		CloseableHttpResponse response = null;
		String html = null;
		try {
			// 创建默认的httpClient实例
			httpClient = HttpClients.createDefault();
			// 创建httpPost
			HttpPost httpPost = new HttpPost(URL);
			// 添加请求头
			for (Entry<String, String> e : headers.entrySet()) {
				httpPost.addHeader(e.getKey(), e.getValue());
			}
			// 如果发送json字符串就直接发送，否就把params 转换成list 发送
            httpPost.setEntity(new StringEntity(params, Charset.forName("UTF-8")));
            /*if(isJson){
                httpPost.setEntity(new StringEntity(params, Charset.forName("UTF-8")));
            }else{
                List<NameValuePair> formparams = new ArrayList<NameValuePair>();

				//httpPost.setEntity(new UrlEncodedFormEntity(params),Charset.forName("UTF-8"));
			}*/

			boolean isRetry = true;
			int retryRequestCount = 0;
			long cycleSleepTime = 3000;
			int cycleRetryTimes = 2;
			int responseCode = 200;
			do {
				response = httpClient.execute(httpPost);
				responseCode = response.getStatusLine().getStatusCode();

				HttpEntity entity = response.getEntity();
				if (entity != null) {
					html = EntityUtils.toString(entity, "UTF-8");
				}
				// 重试
				if (responseCode != 200 || "".equals(html.trim())) {
					Thread.sleep(cycleSleepTime);
					retryRequestCount++;
				}
			} while (isRetry && responseCode != 200 && retryRequestCount < cycleRetryTimes);
		} catch (Exception e) {
		} finally {
			try {
				if (response != null) {
					response.close();
				}
				if (httpClient != null) {
					httpClient.close();
				}
			} catch (IOException e) {
			}
		}
		return html;
	}
}
