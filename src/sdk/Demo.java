package sdk;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * @title Demo
 * @author JJC
 * @createDate 2017年7月20日
 * @version 1.0
 * @details 
 */
public class Demo {
	private static final String METHOD_GET = "GET";
	private static final String METHOD_POST = "POST";
	private static final String URL = "http://api.gsdata.cn/weixin/v1/tests";
	private static final String APPKEY = "6Z003el1dHU8eKtjIle8mJyYxpPDoJNr";
	private static final String APPID = "19";
	public static void main(String[] args) {
		//请求参数
		Map<String , Object> params = new HashMap<String , Object>();
		params.put("time", new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()));
		params.put("s", System.currentTimeMillis());
		//初始化SDK
		SDK sdkGet = new SDK(METHOD_GET, URL , APPKEY , APPID);
		String result = null;
		try { 
			//发送测试GET请求
			//result = sdkGet.send(params,false);
			//System.out.println(result);
			
			//初始化SDK
			SDK sdkPost = new SDK(METHOD_POST, URL , APPKEY , APPID);
			//发送测试POST请求
			//若发送json格式数据，请设置true
			result = sdkPost.send(params,false);
			System.out.println(result);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
