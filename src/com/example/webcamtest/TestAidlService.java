package com.example.webcamtest;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;

public class TestAidlService extends Service {

    public static final String TAG = "TestAidlService";

        
    private final IRemoteRequest.Stub mBinder = new IRemoteRequest.Stub() {
        
    	private boolean mRunning;
//        MyHttpAdapter mHttpAdapter;
        String mIpInfo = null;
        @Override
        public String startLocalService() throws RemoteException {
        	Log.i("test", "aidl -> stopLocalService");
//            if (mHttpAdapter == null) {
//                loadLibrary();
//                Log.i("test", "aidl -> startLocalService Start");
//                mHttpAdapter = new MyHttpAdapter();
//                mIpInfo = mHttpAdapter.Start(getApplicationContext());
//            }
        	
        	if (!mRunning)
        	{
        		loadLibrary();
        		mIpInfo = WebControlManager.getInstance().start(getApplicationContext());
        	}
            
            return mIpInfo;
        }
        
        @Override
        public void stopLocalService() throws RemoteException {
        	Log.i("test", "aidl -> stopLocalService");
        	mIpInfo = null;
//        	if (mHttpAdapter != null) {
//        		Log.w("test", "aidl -> mHttpAdapter Stop");
//        		mHttpAdapter.Stop();
//        		mHttpAdapter.Destroy();
//        		mHttpAdapter = null;
//        	}
            
        	if (mRunning)
        	{
        		WebControlManager.getInstance().stop();
        		mRunning = false;
        	}
        }
        
        @Override
        public String getIpInfo() throws RemoteException {
//            if (mHttpAdapter != null) {
//                return mIpInfo;
//            }
        	if (mRunning)
        		return mIpInfo;
            return null;
        }
        
        protected void loadLibrary() {
            System.loadLibrary("basenet");
        }

		@Override
		public void startWebCam() throws RemoteException 
		{
			Log.d(TAG, "process id=" + android.os.Process.myPid());
			String ipString = startLocalService();
			Intent intent = new Intent(TestAidlService.this, WebCamActivity.class);
			intent.putExtra("ip_address", ipString);
			intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
			startActivity(intent);
		}
    };

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    @Override
    public void onCreate() {
        super.onCreate();

    }

    @Override
    public void onStart(Intent intent, int startId) {
        
    }

    @Override
    public void onDestroy() {
        
    }     
   

}
