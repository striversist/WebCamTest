package com.example.webcamtest;

import java.io.IOException;
import java.util.List;

import android.os.Bundle;
import android.app.Activity;
import android.content.res.Configuration;
import android.graphics.ImageFormat;
import android.hardware.Camera;
import android.hardware.Camera.PreviewCallback;
import android.hardware.Camera.Size;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.TextView;

public class WebCamActivity extends Activity
{
	private static final String TAG = "WebCamActivity";
	private SurfaceView mSurfaceView;
	private SurfaceHolder mSurfaceHolder;
	private TextView mIpTextView;
	private Camera mCamera;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) 
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_webcam);
		
		mSurfaceView = (SurfaceView) findViewById(R.id.surface_view);
		mIpTextView = (TextView) findViewById(R.id.ip_tv);
		String ipString = getIntent().getStringExtra("ip_address");
		if (ipString != null)
			mIpTextView.setText("IP: " + ipString);
		
		start();
		Log.d(TAG, "process id=" + android.os.Process.myPid());
	}
	
	@Override
	protected void onDestroy()
	{
		super.onDestroy();
		stop();
	}

	public void onClick(View view)
	{
		switch (view.getId()) 
		{
			case R.id.start_service_btn:
				break;
			case R.id.stop_service_btn:
				break;
		}
	}
	
	private void start()
	{
		mSurfaceView.getHolder().addCallback(new SurfaceHolder.Callback() 
		{
			@Override
			public void surfaceDestroyed(SurfaceHolder holder) 
			{
				stop();
			}
			
			@Override
			public void surfaceCreated(SurfaceHolder holder) 
			{
				mSurfaceHolder = holder;
				preview();
			}
			
			@Override
			public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) 
			{
			}
		});
	}
	
	private void stop()
	{
		if (mCamera != null)
		{
			mCamera.stopPreview();
			mCamera.release();
			mCamera = null;
		}
	}
	
	private void preview()
	{
		if (mCamera == null)
			mCamera = Camera.open();

		if (mCamera == null)
		{
			Log.e(TAG, "camera open failed");
			return;
		}
		
		Camera.Parameters params = mCamera.getParameters();
		
		List<Size> previewSizes = mCamera.getParameters().getSupportedPreviewSizes();
		for (int i=0; i<previewSizes.size(); ++i)
		{
			Log.d(TAG, "preview: supported size.width=" + previewSizes.get(i).width + ", size.height=" + previewSizes.get(i).height);
		}
		List<Integer> previewFormats = mCamera.getParameters().getSupportedPreviewFormats();
		for (int i=0; i<previewFormats.size(); ++i)
		{
			Log.d(TAG, "preview: supported format=" + previewFormats.get(i).toString());
		}
		List<Integer> previewFrameRates = mCamera.getParameters().getSupportedPreviewFrameRates();
		for (int i=0; i<previewFrameRates.size(); ++i)
		{
			Log.d(TAG, "preview: supported framerate=" + previewFrameRates.get(i).toString());
		}
		
		params.setPreviewSize(800, 480);
//		params.setPreviewFrameRate(10);
		
		// 横竖屏镜头自动调整
		if (this.getResources().getConfiguration().orientation != Configuration.ORIENTATION_LANDSCAPE) 
		{
			params.set("orientation", "portrait");
			params.set("rotation", 90); 		// 镜头角度转90度（默认摄像头是横拍） 
			mCamera.setDisplayOrientation(90);  // 在2.2以上可以使用
		} 
		else	// 如果是横屏
		{
			params.set("orientation", "landscape");
			mCamera.setDisplayOrientation(0); 	// 在2.2以上可以使用
		}
		
		mCamera.setParameters(params);
		
		int bitsPerPixel = ImageFormat.getBitsPerPixel(params.getPreviewFormat());
		Camera.Size camSize = params.getPreviewSize();
		int frameSize = camSize.width * camSize.height * bitsPerPixel / 8;
		mCamera.addCallbackBuffer(new byte[frameSize]);
		mCamera.addCallbackBuffer(new byte[frameSize]);
				
		mCamera.setPreviewCallbackWithBuffer(new PreviewCallback() 
		{
			@Override
			public void onPreviewFrame(byte[] data, Camera camera) 
			{
				if (data == null || data.length == 0)
					return;

                WebControlManager.getInstance().addFrame(data, camera);
                mCamera.addCallbackBuffer(data);
			}
		});
		
		try 
		{
			mCamera.setPreviewDisplay(mSurfaceView.getHolder());
			mCamera.startPreview();
		}
		catch (IOException e) 
		{
			e.printStackTrace();
		}
	}
	
}
