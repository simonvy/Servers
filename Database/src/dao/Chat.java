package dao;

import java.util.ArrayList;
import java.util.List;

import common.Context;
import common.net.NetSession;
import common.net.Procedure;
import database.SqlSession;
import database.SqlSessionFactory;
import entites.Buddy;
import entites.Message;

public class Chat {

	@Procedure
	public void login(NetSession scene, int clientid, String name, String password) {
		// if name is not exist create one
		SqlSession session = getSqlSession();
		if (session == null) {
			scene.call("loginFailed", clientid, name, "database internal error.");
			return;
		}
		try {
			Buddy buddy = session.findOne("findBuddyByName", Buddy.class, name);
			if (buddy == null) {
				buddy = new Buddy();
				buddy.setName(name);
				buddy.setPassword(password);
				session.persist(buddy);
				buddy = session.findOne("findBuddyByName", Buddy.class, name);
			}
			if (buddy != null) {
				if (buddy.getPassword().equals(password)) {
					scene.call("loginSucceed", clientid, buddy);
				} else {
					scene.call("loginFailed", clientid, name, "name or password incorrect.");
				}
			}
		} finally {
			session.close();
		}
	}
	
	@Procedure
	public void loadMessage(NetSession scene, int clientid, int buddyId) {
		SqlSession session = getSqlSession();
		if (session == null) {
			scene.call("messageLoaded", clientid, new ArrayList<Message>(0));
			return;
		}
		try {
			List<Message> messages = 
					session.find("findMessagesByBuddyId", Message.class, buddyId, buddyId);
			scene.call("messageLoaded", clientid, messages);
		} finally {
			session.close();
		}
	}
	
	public SqlSession getSqlSession() {
		SqlSessionFactory factory = Context.instance().get(SqlSessionFactory.class);
		return factory.openSession();
	}
}
