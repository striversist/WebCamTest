package com.example.webcamtest;

import android.util.Log;

import com.tencent.qlauncher.httpserver.HttpAdapter;
import com.tencent.qlauncher.httpserver.HttpRequest;
import com.tencent.qlauncher.httpserver.HttpResponse;

public class MyHttpAdapter extends HttpAdapter {
	protected void OnHttpRequest(byte[] req, String current)	{	
		
	
		HttpRequest hreq = new HttpRequest(req);
	
		Log.d("HttpAdapter", "OnHttpRequest, url:" + hreq.GetRequestUrl() + ", current:" + current);
	
		HttpResponse hrsp = new HttpResponse();
		
		hrsp.SetHeader("Connection", "close");
		
		byte[] b = {'a','b','c'};
		
		hrsp.SetContent(b);
		
		SendHttpResponse(hrsp.Encode(), current);
		
		hreq.Destroy();
		hrsp.Destroy();

	}
}
