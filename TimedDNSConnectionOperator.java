package org.archive.wayback.accesscontrol.robotstxt;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

import org.apache.http.conn.scheme.SchemeRegistry;
import org.apache.http.impl.conn.DefaultClientConnectionOperator;

class TimedDNSConnectionOperator extends DefaultClientConnectionOperator
{
	private final static Logger LOGGER = 
		Logger.getLogger(TimedDNSConnectionOperator.class.getName());
	
	private ExecutorService executor;
	private int dnsTimeoutMS;

	public TimedDNSConnectionOperator(SchemeRegistry schemes, int poolSize, int dnsTimeoutMS) {
		super(schemes);
		
		executor = Executors.newFixedThreadPool(poolSize);
		this.dnsTimeoutMS = dnsTimeoutMS;
	}

	@Override
	protected InetAddress[] resolveHostname(String host)
			throws UnknownHostException {
		
		Future<InetAddress[]> future = executor.submit(new InetLookup(host));
		
		try {
			return future.get(dnsTimeoutMS, TimeUnit.MILLISECONDS);
		} catch (ExecutionException e) {
			future.cancel(true);
			LOGGER.warning("DNS TIMEOUT: " + host);
			throw (UnknownHostException)e.getCause();
		} catch (Exception e) {
			future.cancel(true);
			LOGGER.warning("DNS TIMEOUT OTHER EXCEPTION: " + host + " " + e);
			throw new UnknownHostException(host);
		}
	}
	
	private class InetLookup implements Callable<InetAddress[]>
	{
		String host;
		
		InetLookup(String host)
		{
			this.host = host;
		}
		
		@Override
		public InetAddress[] call() throws Exception {
			return new InetAddress[]{InetAddress.getByName(host)};
		}	
	}
}