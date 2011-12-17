import java.sql.SQLException;

import common.Context;
import common.net.Server;

import dao.Chat;
import database.SqlSessionFactory;

public class Main {

	public static void main(String[] args) {
		
		Server server = Context.instance().register(Server.class);
		SqlSessionFactory ssf = Context.instance().register(SqlSessionFactory.class);
		
		// database access methods
		server.registerProcedures(Chat.class);
		
		try {
			ssf.setupDefault();
			server.start(6669, 1);
			
			System.out.println("> Database started.");
			
			synchronized(server) {
				server.wait();
			}
			
		} catch (Exception e) {
			e.printStackTrace(System.err);
		} finally {
			server.shutdown();
			try {
				ssf.close();
			} catch (SQLException e) {
				e.printStackTrace(System.err);
			}
			System.out.println("> Database stopped.");
		}
	}

}
