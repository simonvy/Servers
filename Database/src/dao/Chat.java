package dao;


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
		try {
			Buddy buddy = session.findOne("findBuddyByName", Buddy.class, name);
			if (buddy == null) {
				buddy = new Buddy();
				buddy.setName(name);
				buddy.setPassword(password);
				session.persist(buddy);
			}
			if (buddy.getId() == 0) {
				buddy = session.findOne("findBuddyByName", Buddy.class, name);
			}
			if (!buddy.getPassword().equals(password)) {
				scene.call("loginFailed", clientid, name, "password incorrect");
			} else {
				scene.call("loginSucceed", clientid, buddy);
			}
		} finally {
			session.close();
		}
	}
	
	@Procedure
	public void loadMessage(NetSession scene, int clientid, Buddy buddy) {
		SqlSession session = getSqlSession();
		try {
			List<Message> messages = 
					session.find("findMessagesByBuddyId", Message.class, buddy.getId(), buddy.getId());
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
