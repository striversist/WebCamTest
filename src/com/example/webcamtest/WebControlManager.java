package com.example.webcamtest;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.List;

import com.tencent.qlauncher.httpserver.HttpAdapter;
import com.tencent.qlauncher.httpserver.HttpRequest;
import com.tencent.qlauncher.httpserver.HttpResponse;

import android.content.Context;
import android.graphics.ImageFormat;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.hardware.Camera;
import android.os.Handler.Callback;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.util.Log;

public class WebControlManager extends HttpAdapter
{
	private static WebControlManager sInstance;
	private boolean mRunning;
	private List<byte[]> mImageList;
	private final int MAX_LIST_ITEM = 3;
	private EncoderThread mEncoderThread;
	private ResponseThread mResponseThread;
	private Handler mEncoderHandler;
	private Handler mResponseHandler;
	
	public static WebControlManager getInstance()
	{
		if (sInstance == null)
			sInstance = new WebControlManager();
		return sInstance;
	}
	
	private WebControlManager()
	{
		mImageList = new ArrayList<byte[]>(MAX_LIST_ITEM);
		mEncoderThread = new EncoderThread("encoder");
		mEncoderThread.start();
		mEncoderHandler = new Handler(mEncoderThread.getLooper(), mEncoderThread);
		
		mResponseThread = new ResponseThread("response");
		mResponseThread.start();
		mResponseHandler = new Handler(mResponseThread.getLooper(), mResponseThread);
	}
	
	public String start(Context context)
	{
		String result = "";
		if(!mRunning)
		{
			mRunning = true;
			result = Start(context);
		}
		
		return result;
	}
	
	public void stop()
	{
		mRunning = false;
		Stop();
		Destroy();
	}
	
	public void addFrame(byte[] data, Camera camera)
	{
		Camera.Parameters parameters = camera.getParameters();
		if (parameters.getPreviewFormat() == ImageFormat.NV21)
		{
			int w = parameters.getPreviewSize().width;  
			int h = parameters.getPreviewSize().height;
			mEncoderHandler.obtainMessage(EncoderThread.MSG_VIDEO_FRAME, w, h, data).sendToTarget();
		}
	}
	
	protected void OnHttpRequest(byte[] req, String current)	
	{	
		HttpRequest hreq = new HttpRequest(req);
		Log.d("HttpAdapter", "OnHttpRequest, url:" + hreq.GetRequestUrl() + ", current:" + current);
		hreq.Destroy();

		mResponseHandler.obtainMessage(ResponseThread.MSG_VIDEO_RESPONSE, current).sendToTarget();	
	}
	
	private class EncoderThread extends HandlerThread implements Callback
	{
		public static final int MSG_VIDEO_FRAME = 0;
		public EncoderThread(String name) 
		{
			super(name);
		}

		@Override
		public boolean handleMessage(Message msg) 
		{
			switch (msg.what) 
			{
				case MSG_VIDEO_FRAME:
					int w = msg.arg1;
					int h = msg.arg2;
	                YuvImage img = new YuvImage((byte[]) msg.obj, ImageFormat.NV21, w, h, null);
	                ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
		            img.compressToJpeg(new Rect(0, 0, img.getWidth(), img.getHeight()), 80, outputStream);
		            byte[] buffer = outputStream.toByteArray();
	                synchronized (mImageList) 
	                {
	                	if (mImageList.size() == MAX_LIST_ITEM)
		                	mImageList.remove(0);
		                mImageList.add(buffer);
					}
					break;
			}
			
			return true;
		}
	}
	
	private class ResponseThread extends HandlerThread implements Callback
	{
		public static final int MSG_VIDEO_RESPONSE = 0;
		public ResponseThread(String name) 
		{
			super(name);
		}

		@Override
		public boolean handleMessage(Message msg) 
		{
			switch (msg.what) 
			{
				case MSG_VIDEO_RESPONSE:
					byte[] data = null;
					synchronized (mImageList) 
					{
						if (!mImageList.isEmpty())
						{
							data = mImageList.remove(0);
						}
					}
					if (data == null && msg.arg1 < 2)
					{
						Message newMsg = mResponseHandler.obtainMessage(msg.what, msg.arg1, msg.arg2, msg.obj);
						newMsg.arg1 += 1;	// Delay times
						mResponseHandler.sendMessageDelayed(newMsg, 80);
					}
					else
					{
						String current = (String) msg.obj;
						HttpResponse hrsp = new HttpResponse();
						hrsp.SetContent(data);
				        hrsp.SetHeader("Connection", "Keep-Alive");
				        hrsp.SetHeader("Content-Length", String.valueOf(data.length));
				        hrsp.SetHeader("Content-Type", "image/jpeg");
				        hrsp.SetHeader("Cache-Control", "no-cache");
				        Log.d("HttpAdapter", "Set header OK, list.size=" + mImageList.size() + ", jpg length=" + String.valueOf(data.length));
						
						SendHttpResponse(hrsp.Encode(), current);
						hrsp.Destroy();
					}
					break;
			}
			return true;
		}
		
	}
}
