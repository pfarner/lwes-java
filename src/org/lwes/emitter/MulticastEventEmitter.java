package org.lwes.emitter;

import java.io.InputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;

import org.lwes.Event;
import org.lwes.EventFactory;
import org.lwes.EventSystemException;
import org.lwes.util.Log;

/**
 * MulticastEventEmitter emits events to multicast groups on the network.
 * 
 * @author      Michael P. Lum
 * @author      Anthony Molinaro
 * @version     %I%, %G%
 * @since       0.0.1
 */
public class MulticastEventEmitter implements EventEmitter {
	/* an EventFactory */
	private EventFactory factory = new EventFactory();
	
	/* the actual multicast socket being used */
	private MulticastSocket socket = null;
	
	/* the multicast address */
	private InetAddress address = null;
	
	/* the multicast port */
	private int port = 9191;
	
	/* the multicast interface */
	private InetAddress iface = null;
	
	/* the multicast time-to-live */
	private int ttl = 31;
	
	/* a lock variable to synchronize events */
	private Object lock = new Object();
	
	/**
	 * Default constructor.
	 */
	public MulticastEventEmitter() {
	}
	
	/**
	 * Sets the multicast address for this emitter.
	 * 
	 * @param address the multicast address
	 * 
	 */
	public void setMulticastAddress(InetAddress address) {
		this.address = address;
	}
	
	/**
	 * Gets the multicast address for this emitter.
	 * 
	 * @return the address
	 */
	public InetAddress getMulticastAddress() {
		return this.address;
	}

	/**
	 * Sets the multicast port for this emitter.
	 * 
	 * @param address the multicast port
	 * 
	 */
	public void setMulticastPort(int port) {
		this.port = port;
	}
	
	/**
	 * Gets the multicast port for this emitter.
	 * 
	 * @return the multicast port
	 */
	public int getMulticastPort() {
		return this.port;
	}
	
	/**
	 * Sets the network interface for this emitter.
	 * 
	 * @param address the network interface
	 * 
	 */
	public void setInterface(InetAddress iface) {
		this.iface = iface;
	}
	
	/**
	 * Gets the network interface for this emitter.
	 * 
	 * @return the interface address
	 */
	public InetAddress getInterface() {
		return this.iface;
	}
	
	/**
	 * Sets the multicast time-to-live for this emitter.
	 * 
	 * @param ttl the time to live
	 * 
	 */
	public void setTimeToLive(int ttl) {
		this.ttl = ttl;
	}
	
	/**
	 * Gets the multicast time-to-live for this emitter.
	 * 
	 * @return the time to live
	 */
	public int getTimeToLive() {
		return this.ttl;
	}
	
	/**
	 * Sets the ESF file used for event validation.
	 * @param esfFilePath the path of the ESF file
	 */
	public void setESFFilePath(String esfFilePath) {
		if(factory != null) {
			factory.setESFFilePath(esfFilePath);
		}
	}
	
	/**
	 * Gets the ESF file used for event validation
	 * @return the ESF file path
	 */
	public String getESFFilePath() {
		if(factory != null) {
			return factory.getESFFilePath();
		} else {
			return null;
		}
	}
	
	/**
	 * Sets an InputStream to be used for event validation.
	 * @param esfInputStream an InputStream used for event validation
	 */
	public void setESFInputStream(InputStream esfInputStream) {
		if(factory != null) {
			factory.setESFInputStream(esfInputStream);
		}
	}
	
	/**
	 * Gets the InputStream being used for event validation.
	 * @return the InputStream of the ESF validator
	 */
	public InputStream getESFInputStream() {
		if(factory != null) {
			return factory.getESFInputStream();
		} else {
			return null;
		}
	}
	
	/**
	 * Initializes the emitter.
	 */
	public void initialize() throws IOException {
		try {
			factory.initialize();
			socket = new MulticastSocket();
		
			if(iface != null) {
				socket.setInterface(iface);
			}
		
			socket.setTimeToLive(ttl);
		} catch(IOException ie) {
			throw ie;
		} catch(Exception e) {
			Log.error("Unable to initialize MulticastEventEmitter", e);
		}
	}

	/**
	 * Shuts down the emitter.
	 */
	public void shutdown() throws IOException {
		socket.close();
	}

	/**
	 * Creates a new event named <tt>eventName</tt>.
	 * @param eventName the name of the event to be created
	 * @return a new Event
	 * @exception EventSystemException if there is a problem creating the event
	 */
	public Event createEvent(String eventName) throws EventSystemException {
		return createEvent(eventName, true);
	}
	
	/**
	 * Creates a new event named <tt>eventName</tt>.
	 * @param eventName the name of the event to be created
	 * @param validate whether or not to validate the event against the EventTemplateDB
	 * @return a new Event
	 * @exception EventSystemException if there is a problem creating the event
	 */
	public Event createEvent(String eventName, boolean validate) throws EventSystemException {
		if(factory != null) {
			return factory.createEvent(eventName, validate);
		} else {
			throw new EventSystemException("EventFactory not initialized");
		}
	}	
	
	/**
	 * Emits the event to the network.
	 * 
	 * @param event the event to emit
	 * @exception IOException throws an IOException is there is a network error.
	 */
	public void emit(Event event) throws IOException {
		byte[] msg = event.serialize();
		
		synchronized(lock) {
			emit(msg);
		}
	}

	/**
	 * Emits a byte array to the network.
	 * 
	 * @param bytes the byte array to emit
	 * @exception IOException throws an IOException if there is a network error.
	 */
	protected void emit(byte[] bytes) throws IOException {
		/* don't bother with empty arrays */
		if(bytes == null) return;

		/* construct a datagram */
		DatagramPacket dp = new DatagramPacket(bytes, bytes.length, address, port);
		socket.send(dp);
	}
}