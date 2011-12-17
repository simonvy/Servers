package common.net;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;

class AsynchronousProcedureQueue {
	
	private Map<String, Pair> knownProcedures = new HashMap<String, Pair>();
	private ExecutorService executor;
	
	public void initExecutors(int nExecutors) {
		if (nExecutors > 0) {
			this.executor = Executors.newFixedThreadPool(nExecutors);
		}
	}
	
	public void registerProcedures(Class<?> clazz) {
		Object host = null;
		for (Method method : clazz.getMethods()) {
			Procedure rc = method.getAnnotation(Procedure.class);
			if (rc == null) {
				continue;
			}
			
			String funcName = method.getName();
			if (rc.name() != null && rc.name().length() > 0) {
				funcName = rc.name();
			}
			// if method of the same name is already registered, throw an exception
			if (knownProcedures.containsKey(funcName)) {
				throw new IllegalStateException("procedure [" + funcName + "] is already registered.");
			}
			
			if (host == null) {
				try {
					// constructor with no params is used to instance the host.
					host = clazz.newInstance();
				} catch (Exception e) {
					throw new IllegalStateException(e);
				}
			}
			
			Pair p = new Pair();
			p.method = method;
			p.host = host;
			knownProcedures.put(funcName, p);
		}
	}
	
	public void invokeProcedure(NetSession session, APC apc) {
		if (this.executor == null) {
			return;
		}
		if (apc != null && validateProcedure(apc)) {
			// set the session to the first parameter of the apc.
			apc.getParameters()[0] = session;
			
			ProcedureTask task = new ProcedureTask();
			task.apc = apc;
			task.p = knownProcedures.get(apc.getFunctionName());
			try {
				if (!this.executor.isShutdown()) {
					this.executor.execute(task);
				}
			} catch(RejectedExecutionException e) {
				e.printStackTrace(System.err);
			}
		}
	}
	
	public void shutdown() {
		if (this.executor == null) {
			return;
		}
		this.executor.shutdown();
		while(!this.executor.isShutdown()) {
			try {
				this.executor.awaitTermination(Integer.MAX_VALUE, TimeUnit.SECONDS);
			} catch (InterruptedException e) {
				e.printStackTrace(System.err);
			}
		}
	}
	
	private boolean validateProcedure(APC apc) {
		String funcName = apc.getFunctionName();
		if (funcName == null || funcName.length() == 0) {
			System.err.println("> procedure name is empty.");
			return false;
		}
		
		if (!knownProcedures.containsKey(funcName)) {
			System.err.println("> procedure " + funcName + " is not registered.");
			return false;
		}
		
		Pair p = knownProcedures.get(funcName);
		Class<?>[] paramTypes = p.method.getParameterTypes();
		Object[] params = apc.getParameters();
		
		if (params == null || params.length != paramTypes.length) {
			System.err.println(funcName + " #params != " + paramTypes.length);
			return false;
		}
		
		return true;
	}
	
	private class ProcedureTask implements Runnable {
		APC apc;
		Pair p;
		
		@Override
		public void run() {
			try {
				p.method.invoke(p.host, apc.getParameters());
			} catch(Exception e) {
				e.printStackTrace(System.err);
			}
		}
	} 
	
	private class Pair {
		Object host;
		Method method;
	}
}
