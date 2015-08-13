package com.hdu.cfryan.service;

public interface IConnectionStatusCallback {
	public void connectionStatusChanged(int connectedState, String reason);
}
