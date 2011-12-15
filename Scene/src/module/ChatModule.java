package module;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import net.SceneServer;

import common.Context;
import common.net.NetSession;
import common.net.Procedure;
import entites.Buddy;
import entites.Message;

public class ChatModule {

	private Map<String, Buddy> buddies;
	private Set<String> occupiedName;
	
	public ChatModule() {
		this.buddies = new HashMap<String, Buddy>();
		this.occupiedName = new HashSet<String>();
	}
	
	@Procedure
	public void login(NetSession client, String name, String password) {
		// if this user already login
		if (buddies.containsKey(name)) {
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
		buddies.put(buddy.getName(), buddy);
		
		Buddy[] allBuddies = new Buddy[buddies.size() - 1];
		int i = 0;
		for(Buddy b : buddies.values()) {
			if (b != buddy) {
				Buddy copyBuddy = new Buddy();
				copyBuddy.setId(b.getId());
				copyBuddy.setName(b.getName());
				copyBuddy.setPassword("");
				allBuddies[i++] = copyBuddy;
			}
		}
		client.call("loginSucceed", buddy, allBuddies);
	}
	
	@Procedure
	public void loadMessage(NetSession client, Buddy buddy) {
		// validation
		if (!buddies.containsKey(buddy.getName())) {
			return;
		}
		NetSession database = getDbSession();
		if (database != null) {
			database.call("loadMessage", client.getId(), buddy.getId());
		}
	}
	
	@Procedure
	public void messageLoaded(NetSession client, List<Message> messages) {
		client.call("setMessages", (Object[])messages.toArray());
	}
	
	private NetSession getDbSession() {
		SceneServer server = Context.instance().get(SceneServer.class);
		return server.getDbSession();
	}
}
