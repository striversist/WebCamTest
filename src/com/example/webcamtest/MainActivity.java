package com.example.webcamtest;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.List;

import android.graphics.ImageFormat;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.hardware.Camera;
import android.hardware.Camera.Parameters;
import android.hardware.Camera.PictureCallback;
import android.hardware.Camera.PreviewCallback;
import android.hardware.Camera.Size;
import android.media.MediaRecorder;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.IBinder;
import android.os.RemoteException;
import android.annotation.TargetApi;
import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.res.Configuration;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;

public class MainActivity extends Activity implements ServiceConnection 
{
	private static final String TAG = "MainActivity";
	private SurfaceView mSurfaceView;
	private SurfaceHolder mSurfaceHolder;
	private Camera mCamera;
	private int mCurrentCameraIndex;
	private boolean mIsFlashOn;
	private MediaRecorder mRecorder;
	private IRemoteRequest mRemoteRequest;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) 
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		
		loadLibrary();
		bindAidlService(this);
		
		mSurfaceView = (SurfaceView) findViewById(R.id.surface_view);
		mRecorder = new MediaRecorder();
		mCurrentCameraIndex = 0;
		mIsFlashOn = false;
		start();
		
		Log.d(TAG, "process id=" + android.os.Process.myPid());
	}
	
	@Override
	protected void onDestroy()
	{
		super.onDestroy();
		stop();
		unBindAidlService(this);
	}

	public void onClick(View view)
	{
		switch (view.getId()) 
		{
			case R.id.take_picture_btn:
				takePicture();
				break;
			case R.id.start_record:
				startRecord();
				break;
			case R.id.stop_record:
				stopRecord();
				break;
			case R.id.start_webcam:
				startWebCam();
				break;
			case R.id.toggle_camera_btn:
				toggleCamera();
				break;
			case R.id.toggle_flash_btn:
				toggleFlash();
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
		if (mRecorder != null)
		{
			mRecorder.reset();
			mRecorder.release();
			mRecorder = null;
		}
	}
	
	private void preview()
	{
		preview(0);
	}
	
	@TargetApi(Build.VERSION_CODES.GINGERBREAD)
	private void preview(int cameraIndex)
	{
		if (mCamera == null)
		{
			if (android.os.Build.VERSION.SDK_INT > 8)
				mCamera = Camera.open(cameraIndex);
			else
				mCamera = Camera.open();
			mCurrentCameraIndex = cameraIndex;
		}

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
		
		params.setPreviewSize(480, 320);
		params.setPreviewFrameRate(10);
		
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
		
		mCamera.setPreviewCallback(new PreviewCallback() 
		{
			@Override
			public void onPreviewFrame(byte[] data, Camera camera) 
			{
//				Log.d(TAG, "onPreviewFrame: data.length=" + data.length);
				if (data.length == 0)
					return;

                Camera.Parameters parameters = camera.getParameters();  
                int imageFormat = parameters.getPreviewFormat();
                if(imageFormat == ImageFormat.NV21)
                {
                    int w = parameters.getPreviewSize().width;
                    int h = parameters.getPreviewSize().height;
                    YuvImage img = new YuvImage(data, ImageFormat.NV21, w, h, null);                
                    try 
                    {
                    	ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
                        img.compressToJpeg(new Rect(0, 0, img.getWidth(), img.getHeight()), 80, outputStream);
                        
                        String picString = outputStream.toString();
//                        Log.d(TAG, "preview: picString.length=" + picString.length());
                        Log.i("autopic", "ok!");
                    } 
                    catch (Exception e) 
                    {  
                        Log.i("autopic", e.toString());  
                    }  
                }
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
	
	private void takePicture()
	{
		mCamera.takePicture(null, null, 
			new PictureCallback() 
			{
				@Override
				public void onPictureTaken(byte[] data, Camera camera) 
				{
					Log.d(TAG, "post picture length: " + data.length);
				}
			},
			new PictureCallback() 
			{
				@Override
				public void onPictureTaken(byte[] data, Camera camera) 
				{
					Log.d(TAG, "jpg picture length: " + data.length);
				}
			});
	}
	
	private void startRecord()
	{
		if (mCamera == null)
			return;
		
		mCamera.stopPreview();		// 需要先停止camera的preview，才能再开始下面recorder的preview。否则录像结果是花屏。
		mCamera.unlock();
		
		// 以下设置有先后顺序
		mRecorder.setCamera(mCamera);
		mRecorder.setVideoSource(MediaRecorder.VideoSource.CAMERA);
		mRecorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
		mRecorder.setOutputFile(Environment.getExternalStorageDirectory().getPath() + File.separator + "test.3gp");
		
		mRecorder.setVideoFrameRate(10);
		mRecorder.setVideoSize(320, 240);
		mRecorder.setVideoEncoder(MediaRecorder.VideoEncoder.H264);

		mRecorder.setPreviewDisplay(mSurfaceHolder.getSurface());	// 在surfaceview中显示
		
		try 
		{
			mRecorder.prepare();
			mRecorder.start();	// Recording is now started
		} 
		catch (IllegalStateException e) 
		{
			e.printStackTrace();
		} 
		catch (IOException e) 
		{
			e.printStackTrace();
		}
	}
	
	private void stopRecord()
	{
		mRecorder.stop();
		mRecorder.reset();   // You can reuse the object by going back to setAudioSource() step
//		mRecorder.release(); // Now the object cannot be reused
		
		preview();
	}
	
	protected void loadLibrary() 
	{
		System.loadLibrary("basenet");
	}
	
	private void startWebCam()
	{
		stop();
		if (mRemoteRequest == null)
			return;
		
		try
		{
			mRemoteRequest.startWebCam();
		} 
		catch (RemoteException e) 
		{
			e.printStackTrace();
		}
	}
	
	private void toggleCamera()
	{
		stop();
		
		if (mCurrentCameraIndex == 0)
			preview(1);
		else
			preview(0);
	}
	
	private void toggleFlash()
	{
		if (mCamera == null)
			return;
		
		Parameters params = mCamera.getParameters();
		if (mIsFlashOn)
		{
			params.setFlashMode(Parameters.FLASH_MODE_OFF);
			mIsFlashOn = false;
		}
		else
		{
			params.setFlashMode(Parameters.FLASH_MODE_TORCH);
			mIsFlashOn = true;
		}
		mCamera.setParameters(params);
	}
	
	@Override
	public void onServiceConnected(ComponentName name, IBinder service) 
	{
		mRemoteRequest = IRemoteRequest.Stub.asInterface(service);
	}

	@Override
	public void onServiceDisconnected(ComponentName name) 
	{
	}
	
	private void bindAidlService(Context context) 
	{
        Intent serviceIntent = new Intent(context, TestAidlService.class);
        context.startService(serviceIntent);
        context.bindService(serviceIntent, this, Context.BIND_AUTO_CREATE);
    }
	
	private void unBindAidlService(Context context) 
	{
        context.unbindService(this);
        mRemoteRequest = null;
    }
}