import java.sql.SQLException;

import common.Context;
import common.net.Server;

import dao.Chat;
import database.SqlSessionFactory;

public class Main {

	public static void main(String[] args) throws SQLException {
		
		Server server = Context.instance().register(Server.class);
		SqlSessionFactory ssf = Context.instance().register(SqlSessionFactory.class);
		
		// database access methods
		server.registerProcedures(Chat.class);
		
		try {
			ssf.setupDefault();
			server.start(6669);
			
			System.out.println("> Database started!");
			
			synchronized(server) {
				server.wait();
			}
			
		} catch (InterruptedException e) {
			e.printStackTrace(System.err);
		} finally {
			server.stop();
			ssf.close();
			System.out.println("> Database stopped!");
		}
	}

}
