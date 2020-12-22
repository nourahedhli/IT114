package server;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Room implements AutoCloseable {
	private static SocketServer server;// used to refer to accessible server functions
	private String name;
	private final static Logger log = Logger.getLogger(Room.class.getName());

	// Commands
	private final static String COMMAND_TRIGGER = "/";
	private final static String CREATE_ROOM = "createroom";
	private final static String JOIN_ROOM = "joinroom";
	private final static String ROLL = "roll";
	private final static String FLIP = "flip";
	private final static String MUTE = "mute";
	private final static String UNMUTE = "unmute";
	private final static String PM = "pm";

	public Room(String name) {
		this.name = name;
	}

	public static void setServer(SocketServer server) {
		Room.server = server;
	}

	public String getName() {
		return name;
	}

	private List<ServerThread> clients = new ArrayList<ServerThread>();

	protected synchronized void addClient(ServerThread client) {
		client.setCurrentRoom(this);
		if (clients.indexOf(client) > -1) {
			log.log(Level.INFO, "Attempting to add a client that already exists");
		} else {
			clients.add(client);
			if (client.getClientName() != null) {
				client.sendClearList();
				sendConnectionStatus(client, true, "joined the room " + getName());
				updateClientList(client);
			}
		}
	}

	private void updateClientList(ServerThread client) {
		Iterator<ServerThread> iter = clients.iterator();
		while (iter.hasNext()) {
			ServerThread c = iter.next();
			if (c != client) {
				boolean messageSent = client.sendConnectionStatus(c.getClientName(), true, null);
			}
		}
	}

	protected synchronized void removeClient(ServerThread client) {
		clients.remove(client);
		if (clients.size() > 0) {
			// sendMessage(client, "left the room");
			sendConnectionStatus(client, false, "left the room " + getName());
		} else {
			cleanupEmptyRoom();
		}
	}

	private void cleanupEmptyRoom() {
		// If name is null it's already been closed. And don't close the Lobby
		if (name == null || name.equalsIgnoreCase(SocketServer.LOBBY)) {
			return;
		}
		try {
			log.log(Level.INFO, "Closing empty room: " + name);
			close();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	protected void joinRoom(String room, ServerThread client) {
		server.joinRoom(room, client);
	}

	protected void joinLobby(ServerThread client) {
		server.joinLobby(client);
	}

	protected void createRoom(String room, ServerThread client) {
		if (server.createNewRoom(room)) {
			sendMessage(client, "Created a new room");
			joinRoom(room, client);
		}
	}

	private String processCommands(String message, ServerThread client) {

		String response = null;

		try {
			if (message.indexOf(COMMAND_TRIGGER) > -1) {
				String[] comm = message.split(COMMAND_TRIGGER);
				log.log(Level.INFO, message);
				String part1 = comm[1];
				String[] comm2 = part1.split(" ");
				String command = comm2[0];
				if (command != null) {
					command = command.toLowerCase();
				}
				String roomName;
				switch (command) {

				case CREATE_ROOM:
					roomName = comm2[1];
					createRoom(roomName, client);
					break;
				case JOIN_ROOM:
					roomName = comm2[1];
					joinRoom(roomName, client);
					break;

				case ROLL:
					String num = Integer.toString((int) ((Math.random() * 6) + 1));
					//sendMessage(client, "<b style=color:red>The number you rolled is:  </b>"  + num);
					//sendMessage(client, "The number you rolled is: "  + num);
					//sendMessage(client, "The number you rolled is : "  + num);
					response =  "<font color=red>The number you rolled is :  </font>"  + num;
					break;
				case FLIP:
					int ranflip = (int) (Math.random() * 2);
					if (ranflip == 0) {
						String msg = "<b style=color:red>Heads </b>" ;
						//String msg = "<b style=color:red>Heads </b>" ;
						response =  "<font color=red> Heads  </font>" ;
					} else {
						//String msg1 = "<b style=color:red>Tails</b>" ;
						response =  "<font color=red> Tails  </font>"  ;
					}

					break;

				case MUTE:
					
					String[] splitMsg = message.split(" ");
					//message should look something like: /mute Bob
					String mutedClient = splitMsg[1];
					client.mutedList.add(mutedClient);
					
					try {
			    		String fileName = client.getClientName()+"MuteFile.txt";
						File f = new File(fileName);
						FileWriter fw = new FileWriter(fileName);
						
						if(f.createNewFile()) {
							System.out.println("Created "+client.getClientName()+" mute file.");
							for(String clientName: client.mutedList) {
								fw.write(clientName + " ");
							}
							fw.close();
						}else {
							for(String clientName: client.mutedList) {
								fw.write(clientName + " ");
							}
							fw.close();
						}
			
					}catch (IOException e) {
						e.printStackTrace();
					}
					

					// send to client who is muted
					Iterator<ServerThread> iter = clients.iterator();
					while (iter.hasNext()) {
						ServerThread c = iter.next();
						if (c.getClientName().equalsIgnoreCase(mutedClient)
								|| c.getClientName().equalsIgnoreCase(client.getClientName())) {
							sendMessage(client, "muted " + mutedClient);
						}}

					

					break;

				case UNMUTE:
					String[] splitmsg = message.split(" ");

					String unmutedClient = splitmsg[1];
					unmutedClient = unmutedClient.trim();
					// for loop to look for the name
					for (String name : client.mutedList) {
						if (name.equalsIgnoreCase(unmutedClient)) {
							// removing the name or basically not mute
							client.mutedList.remove(unmutedClient);

							
							try {
					    		String fileName = client.getClientName()+"MuteFile.txt";
								File f = new File(fileName);
								FileWriter fw = new FileWriter(fileName);
								
								if(f.createNewFile()) {
									System.out.println("Created "+client.getClientName()+" mute file.");
									for(String clientName: client.mutedList) {
										fw.write(clientName + " ");
									}
									fw.close();
								}else {
									for(String clientName: client.mutedList) {
										fw.write(clientName + " ");
									}
									if(client.mutedList.isEmpty()) {
										fw.write("");
									}
									fw.close();
								}
							}catch (IOException e) {
								e.printStackTrace();
							}
							
							// send to client who is muted

							Iterator<ServerThread> iter2 = clients.iterator();
							while (iter2.hasNext()) {
								ServerThread c = iter2.next();
								if (c.getClientName().equalsIgnoreCase(unmutedClient)
										|| c.getClientName().equalsIgnoreCase(client.getClientName())) {
									sendMessage(client, "unmuted " + unmutedClient);
								}

							}

							break;
						}
					}
				case PM:

					String clientName = "";
					clientName = clientName.trim().toLowerCase();
					List<String> clients = new ArrayList<String>();

					if (message.indexOf("@") > -1) {
						String[] words = message.split(" ");
						for (String word : words) {
							if ((word.charAt(0) == '@') && (word.charAt(1) != '@')) {
								clients.add(word.substring(1).trim().toLowerCase());
							}

						}
						sendPM(client, clients, message);
						break;
					}
				}

			}
			// not a command BOLD Italic and underline

			else {

				String alteredMessage = message;
				if (alteredMessage.indexOf("@") > -1) {
					String[] s1 = alteredMessage.split("@");
					String m = "";
					for (int i = 0; i < s1.length; i++) {
						if (i % 2 == 0) {

							m += s1[i];
						} else {
							m += "<b>" + s1[i] + "</b>";

						}

					}
					alteredMessage = m;

				}

				if (alteredMessage.indexOf("*") > -1) {

					String[] s1 = alteredMessage.split("\\*");
					String m = "";
					for (int i = 0; i < s1.length; i++) {
						if (i % 2 == 0) {

							m += s1[i];
						} else {
							m += "<i>" + s1[i] + "</i>";

						}

					}
					alteredMessage = m;

				}

				if (alteredMessage.indexOf("~") > -1) {

					String[] s1 = alteredMessage.split("~");
					String m = "";
					for (int i = 0; i < s1.length; i++) {
						if (i % 2 == 0) {

							m += s1[i];
						} else {
							m += "<u>" + s1[i] + "</u>";

						}

					}

					alteredMessage = m;

				}

				response = alteredMessage;
				System.out.println(alteredMessage);
			}

		}

		catch (Exception e) {
			e.printStackTrace();
		}
		return response;
	}

	// TODO changed from string to ServerThread
	protected void sendConnectionStatus(ServerThread client, boolean isConnect, String message) {
		Iterator<ServerThread> iter = clients.iterator();
		while (iter.hasNext()) {
			ServerThread c = iter.next();
			boolean messageSent = c.sendConnectionStatus(client.getClientName(), isConnect, message);
			if (!messageSent) {
				iter.remove();
				log.log(Level.INFO, "Removed client " + c.getId());
			}
		}
	}

	/***
	 * Takes a sender and a message and broadcasts the message to all clients in
	 * this room. Client is mostly passed for command purposes but we can also use
	 * it to extract other client info.
	 * 
	 * @param sender  The client sending the message
	 * @param message The message to broadcast inside the room
	 */
	protected synchronized void sendMessage(ServerThread sender, String message) {
		// no broadcast
		log.log(Level.INFO, getName() + ": Sending message to " + clients.size() + " clients");
		String resp = processCommands(message, sender);
		if (resp == null) {

			return;
		}
		message = resp;

		Iterator<ServerThread> iter = clients.iterator();
		while (iter.hasNext()) {
			ServerThread client = iter.next();
			if (!client.isMuted(sender.getClientName())) {
				boolean messageSent = client.send(sender.getClientName(), message);
				if (!messageSent) {
					iter.remove();
					log.log(Level.INFO, "Removed client " + client.getId());
				}
			}
		}
	}

	protected synchronized void sendPM(ServerThread sender, List<String> dest, String message) {

		Iterator<ServerThread> iter = clients.iterator();
		while (iter.hasNext()) {
			ServerThread client = iter.next();
			if (dest.contains(client.getClientName().toLowerCase())) {
				boolean messageSent = client.send(sender.getClientName(), message);

				if (!messageSent) {
					iter.remove();
				}
				break;
			}

		}

	}

	protected synchronized void sendPrivateMessage(ServerThread sender, String message) {

		Iterator<ServerThread> iter = clients.iterator();
		while (iter.hasNext()) {
			ServerThread client = iter.next();
			if (!client.isMuted(sender.getClientName())) {
				boolean messageSent = client.send(sender.getClientName(), message);
				if (!messageSent) {
					iter.remove();
					log.log(Level.INFO, "Removed client " + client.getId());
				}
			}
		}
	}

	/***
	 * Will attempt to migrate any remaining clients to the Lobby room. Will then
	 * set references to null and should be eligible for garbage collection
	 */
	@Override
	public void close() throws Exception {
		int clientCount = clients.size();
		if (clientCount > 0) {
			log.log(Level.INFO, "Migrating " + clients.size() + " to Lobby");
			Iterator<ServerThread> iter = clients.iterator();
			Room lobby = server.getLobby();
			while (iter.hasNext()) {
				ServerThread client = iter.next();
				lobby.addClient(client);
				iter.remove();
			}
			log.log(Level.INFO, "Done Migrating " + clients.size() + " to Lobby");
		}
		server.cleanupRoom(this);
		name = null;
		// should be eligible for garbage collection now
	}

}
