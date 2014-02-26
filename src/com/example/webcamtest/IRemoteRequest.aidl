package com.example.webcamtest;


interface IRemoteRequest {

	void startWebCam();    

    String startLocalService();
    

    void stopLocalService();    
    
    String getIpInfo();
    
}