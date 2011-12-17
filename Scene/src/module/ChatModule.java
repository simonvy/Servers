package module;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import net.DatabaseClient;

import common.Context;
import common.net.NetSession;
import common.net.Procedure;
import entites.Buddy;
import entites.Message;

public class ChatModule {

	private Map<String, NetSession> sessions;
	private Set<String> occupiedName;
	
	public ChatModule() {
		this.sessions = new HashMap<String, NetSession>();
		this.occupiedName = new HashSet<String>();
	}
	
	@Procedure
	public void login(NetSession client, String name, String password) {
		// if this user already login
		if (sessions.containsKey(name)) {
			client.call("loginFailed", "already login.");
			return;
		}
		
		NetSession database = getDbSession();
		if (database != null) {
			this.occupiedName.add(name);
			database.call("login", client.getId(), name, password);
		} else {
			client.call("loginFailed", "server internal error.");
		}
	}
	
	@Procedure
	public void loginFailed(NetSession client, String name, String message) {
		this.occupiedName.remove(name);
		client.call("loginFailed", message);
	}
	
	@Procedure
	public void loginSucceed(NetSession client, Buddy buddy) {
		this.occupiedName.remove(buddy.getName());
		
		Buddy copyBuddy = new Buddy();
		copyBuddy.setId(buddy.getId());
		copyBuddy.setName(buddy.getName());
		copyBuddy.setPassword("");
		notifyAll("addBuddy", copyBuddy);
		
		this.sessions.put(buddy.getName(), client);
		client.attachment = buddy;
		
		List<Buddy> allBuddies = new ArrayList<Buddy>();
		for(NetSession session : sessions.values()) {
			if (session != client) {
				if (session.attachment != null) {
					Buddy b = (Buddy) session.attachment;
					copyBuddy = new Buddy();
					copyBuddy.setId(b.getId());
					copyBuddy.setName(b.getName());
					copyBuddy.setPassword("");
					allBuddies.add(copyBuddy);
				}
			}
		}
		client.call("loginSucceed", buddy, (Object)allBuddies.toArray());
	}
	
	@Procedure
	public void loadMessage(NetSession client, Buddy buddy) {
		// validation
		if (!sessions.containsKey(buddy.getName())) {
			return;
		}
		NetSession database = getDbSession();
		if (database != null) {
			database.call("loadMessage", client.getId(), buddy.getId());
		}
	}
	
	@Procedure
	public void messageLoaded(NetSession client, List<Message> messages) {
		client.call("setMessages", (Object)messages.toArray());
	}
	
	@Procedure
	public void talk(NetSession client, Buddy sender, Buddy receiver, String content) {
		sender.setPassword("");
		receiver.setPassword("");
		
		Message message = new Message();
		message.setSender(sender.getId());
		message.setReceiver(receiver.getId());
		message.setText(content);
		
		// 1. send message back to from/to
		NetSession fromSession = sessions.get(sender.getName());
		NetSession toSession = sessions.get(receiver.getName());
		if (fromSession != null) {
			fromSession.call("addMessage", message);
		}
		if (toSession != null) {
			toSession.call("addMessage", message);
		}
		
		// 2. add message to database
		NetSession database = getDbSession();
		if (database != null) {
			database.call("saveMessage", message);
		}
	}
	
	@Procedure
	public void disconnected(NetSession client) {
		if (client.attachment != null) {
			Buddy buddy = (Buddy)client.attachment;
			this.sessions.remove(buddy.getName());
			client.attachment = null;
			buddy.setPassword("");
			notifyAll("removeBuddy", buddy);
		}
	}
	
	private void notifyAll(String funcName, Object...params) {
		for(NetSession session : sessions.values()) {
			session.call(funcName, params);
		}
	}
	
	private NetSession getDbSession() {
		DatabaseClient database = Context.instance().get(DatabaseClient.class);
		return database.getDbSession();
	}
}
