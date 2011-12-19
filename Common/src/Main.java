import common.Context;
import common.net.Server;

public class Main {

	public static void main(String[] args) {
		Server server = Context.instance().register(Server.class);
		try {
			server.start(6668, 1);
			System.out.println("> Server started.");
			
			synchronized(server) {
				server.wait();
			}
		} catch (Exception e) {
			e.printStackTrace(System.err);
		} finally {
			server.shutdown();
			System.out.println("> Server stopped.");
		}
	}
}
