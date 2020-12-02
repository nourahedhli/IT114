package client;

import java.awt.Point;

public interface Event {
	void onClientConnect(String clientName, String message);

	void onClientDisconnect(String clientName, String message);

	void onMessageReceive(String clientName, String message);

	void onChangeRoom();
	
	

	

	void onGetRoom(String roomName);

	void onResize(Point p);

	
}
