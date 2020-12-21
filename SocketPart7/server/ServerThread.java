package server;
import java.util.*;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;



public class ServerThread extends Thread {
    private Socket client;
    private ObjectInputStream in;// from client
    private ObjectOutputStream out;// to client
    private boolean isRunning = false;
    private Room currentRoom;// what room we are in, should be lobby by default
    private String clientName;
    private final static Logger log = Logger.getLogger(ServerThread.class.getName());

    public List <String> mutedList = new ArrayList<String>();

    public boolean isMuted(String clientName) {
    	for(String name: mutedList) {
    		if (name.equals(clientName)){
    			return true;
    		}
    	}
    	return false;
    }
    
    protected synchronized void createMuteFile(String name) {
    	try {
    		String fileName = name+"MuteFile.txt";
			File f = new File(fileName);
			f.createNewFile();
			FileWriter fw = new FileWriter(fileName);
			
			if(!mutedList.isEmpty()) {
				System.out.println(mutedList.toString());
				for(String clientName: mutedList) {
					//System.out.println(clientName);
					fw.write(clientName + " ");
				}
			}
			fw.close();

		} catch (IOException e) {
			e.printStackTrace();
		}
    }
    
    //will try to read from the muted file and add the names to the muted list
    protected synchronized void readMuteFile(String name) {
    	try {
    		String fileName = name+"MuteFile.txt";
    		File f = new File(fileName);
    		Scanner scan = new Scanner(f);
    		while (scan.hasNextLine()) {
				String[] mutedClients = scan.nextLine().split(" ");
				for(String client : mutedClients) {
					mutedList.add(client);
				}
    		}
    		scan.close();
				
    	}catch (FileNotFoundException e) {
			e.printStackTrace();
		}
    }
    
    public String getClientName() {
	return clientName;
    }

    protected synchronized Room getCurrentRoom() {
	return currentRoom;
    }

    protected synchronized void setCurrentRoom(Room room) {
	if (room != null) {
	    currentRoom = room;
	}
	else {
	    log.log(Level.INFO, "Passed in room was null, this shouldn't happen");
	}
    }

    public ServerThread(Socket myClient, Room room) throws IOException {
	this.client = myClient;
	this.currentRoom = room;
	out = new ObjectOutputStream(client.getOutputStream());
	in = new ObjectInputStream(client.getInputStream());
    }

    /***
     * Sends the message to the client represented by this ServerThread
     * 
     * @param message
     * @return
     */
    @Deprecated
    protected boolean send(String message) {
	// added a boolean so we can see if the send was successful
	try {
	    out.writeObject(message);
	    return true;
	}
	catch (IOException e) {
	    log.log(Level.INFO, "Error sending message to client (most likely disconnected)");
	    e.printStackTrace();
	    cleanup();
	    return false;
	}
    }

    /***
     * Replacement for send(message) that takes the client name and message and
     * converts it into a payload
     * 
     * @param clientName
     * @param message
     * @return
     */
    protected boolean send(String clientName, String message) {
	Payload payload = new Payload();
	payload.setPayloadType(PayloadType.MESSAGE);
	payload.setClientName(clientName);
	payload.setMessage(message);

	return sendPayload(payload);
    }

    protected boolean sendConnectionStatus(String clientName, boolean isConnect, String message) {
	Payload payload = new Payload();
	if (isConnect) {
	    payload.setPayloadType(PayloadType.CONNECT);
	    payload.setMessage(message);
	}
	else {
	    payload.setPayloadType(PayloadType.DISCONNECT);
	    payload.setMessage(message);
	}
	payload.setClientName(clientName);
	return sendPayload(payload);
    }

    protected boolean sendClearList() {
	Payload payload = new Payload();
	payload.setPayloadType(PayloadType.CLEAR_PLAYERS);
	return sendPayload(payload);
    }

    private boolean sendPayload(Payload p) {
	try {
	    out.writeObject(p);
	    return true;
	}
	catch (IOException e) {
	    log.log(Level.INFO, "Error sending message to client (most likely disconnected)");
	    e.printStackTrace();
	    cleanup();
	    return false;
	}
    }

    /***
     * Process payloads we receive from our client
     * 
     * @param p
     */
    private void processPayload(Payload p) {
	switch (p.getPayloadType()) {
	case CONNECT:
	    // here we'll fetch a clientName from our client
	    String n = p.getClientName();
	    if (n != null) {
		clientName = n;
		readMuteFile(n);
		log.log(Level.INFO, "Set our name to " + clientName);
		if (currentRoom != null) {
		    currentRoom.joinLobby(this);
		}
	    }
	    break;
	case DISCONNECT:
	    isRunning = false;// this will break the while loop in run() and clean everything up
	    break;
	case MESSAGE:
	    currentRoom.sendMessage(this, p.getMessage());
	    break;
	case CLEAR_PLAYERS:
	    // we currently don't need to do anything since the UI/Client won't be sending
	    // this
	    break;
	default:
	    log.log(Level.INFO, "Unhandled payload on server: " + p);
	    break;
	}
    }

    @Override
    public void run() {
	try {
	    isRunning = true;
	    Payload fromClient;
	    while (isRunning && // flag to let us easily control the loop
		    !client.isClosed() // breaks the loop if our connection closes
		    && (fromClient = (Payload) in.readObject()) != null // reads an object from inputStream (null would
									// likely mean a disconnect)
	    ) {
		System.out.println("Received from client: " + fromClient);
		processPayload(fromClient);
	    } // close while loop
	}
	catch (Exception e) {
	    // happens when client disconnects
	    e.printStackTrace();
	    log.log(Level.INFO, "Client Disconnected");
	}
	finally {
	    isRunning = false;
	    log.log(Level.INFO, "Cleaning up connection for ServerThread");
	    cleanup();
	}
    }

    private void cleanup() {
	if (currentRoom != null) {
	    log.log(Level.INFO, getName() + " removing self from room " + currentRoom.getName());
	    currentRoom.removeClient(this);
	}
	if (in != null) {
	    try {
		in.close();
	    }
	    catch (IOException e) {
		log.log(Level.INFO, "Input already closed");
	    }
	}
	if (out != null) {
	    try {
		out.close();
	    }
	    catch (IOException e) {
		log.log(Level.INFO, "Client already closed");
	    }
	}
	if (client != null && !client.isClosed()) {
	    try {
		client.shutdownInput();
	    }
	    catch (IOException e) {
		log.log(Level.INFO, "Socket/Input already closed");
	    }
	    try {
		client.shutdownOutput();
	    }
	    catch (IOException e) {
		log.log(Level.INFO, "Socket/Output already closed");
	    }
	    try {
		client.close();
	    }
	    catch (IOException e) {
		log.log(Level.INFO, "Client already closed");
	    }
	}
    }
}
