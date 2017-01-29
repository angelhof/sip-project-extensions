/*
 * Registrar.java
 *
 * Created on June 27, 2002, 11:16 AM
 */

package gov.nist.sip.proxy.registrar;

import gov.nist.sip.proxy.*;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.rmi.*;
import java.rmi.server.*;
import javax.sip.*;
import javax.sip.message.*; 
import javax.sip.header.*;
import javax.sip.address.*;
import java.util.*;
import java.net.URLEncoder;
import gov.nist.sip.proxy.presenceserver.*;
import gov.nist.sip.proxy.gui.*;
import org.jgraph.JGraph;
import org.jgraph.graph.DefaultEdge;
import org.jgrapht.DirectedGraph;
import org.jgrapht.ListenableGraph;
import org.jgrapht.alg.CycleDetector;
import org.jgrapht.ext.JGraphModelAdapter;
import org.jgrapht.graph.DefaultDirectedGraph;
import org.jgrapht.graph.ListenableDirectedGraph;

//ifdef SIMULATION
/*
import sim.java.net.*;
//endif
 */


/**
 *
 * @author  deruelle
 * @version 1.0
 */
public class Registrar  
//ifndef SIMULATION
//
extends UnicastRemoteObject 
//endif
//
implements RegistrarAccess {

	protected  RegistrationsTable registrationsTable;
	protected  RegistrationsList gui;
	protected  Proxy proxy;

	// in seconds
	public static int EXPIRES_TIME_MIN=1;
	public static int EXPIRES_TIME_MAX=3600;

	protected String xmlRegistrationsFile;
	protected Vector threads;
	/**
	 *  Creates new Registrar
	 */
	public Registrar(Proxy proxy) throws RemoteException {
		this.proxy = proxy;
		registrationsTable=new RegistrationsTable(this);
	}

	public void registerToProxies() {
		try{
			Configuration configuration=proxy.getConfiguration();
			Vector proxyToRegisterWithList=configuration.proxyToRegisterWithList;
			if (proxyToRegisterWithList!=null) {
				threads=new Vector();
				if (ProxyDebug.debug) {
					ProxyDebug.println
					("Registrar, registerToProxies(), we have to register to "
							+proxyToRegisterWithList.size()+" proxies");
				}
				for( int i=0;i<proxyToRegisterWithList.size();i++) {
					Domain domain=
							(Domain)proxyToRegisterWithList.elementAt(i);
					if (domain.hostName!=null) {
						RegistrationDomainThread rr=
								new RegistrationDomainThread(proxy,domain);
						//ifdef 		SIMULATION
						/*
                        new SimThread(rr).start();
//else
						 */
						new Thread(rr).start();
						//endif
						//
						threads.addElement(rr);
					}
				}
			}
		}
		catch(Exception e) {
			if (ProxyDebug.debug)  {
				ProxyDebug.println
				("ERROR, Registrar, registerToProxies(), exception  raised:");
			}
			e.printStackTrace();
		}
	}

	public void setRegistrationsList(RegistrationsList registrationsList) {
		this.gui=registrationsList;
	}

	public void parseXMLregistrations(String file) {
		try{
			xmlRegistrationsFile=file;

			XMLRegistrationsParser xmlRegistrationsParser=new
					XMLRegistrationsParser(xmlRegistrationsFile,proxy);
			Registrations registrations=xmlRegistrationsParser.getRegistrations();
			if (registrations==null) return;
			Vector registrationList=registrations.registrationList;
			if (registrationList!=null) {
				if (ProxyDebug.debug) {
					ProxyDebug.println("Registrar, parseXMLregistrations(), Uploading of "
							+registrationList.size()+" registrations");
				}
				for( int i=0;i<registrationList.size();i++) {
					Registration registration=
							(Registration)registrationList.elementAt(i);
					registrationsTable.addRegistration(registration);

					//Henrik Leion: Add registrations to presenceServer 
					//  (Assuming we are PA for all of them).
					PresenceServer presenceServer = proxy.getPresenceServer();
					presenceServer.processUploadedRegistration(registration);

				}
			}


		}
		catch(Exception e) {
			if (ProxyDebug.debug)  {
				ProxyDebug.println
				("ERROR, Registrar, Registrar(), exception  raised during"+
						" parsing of the static registrations:");
			}
			e.printStackTrace();
		}
	}


	public void clean() {
		if (threads==null) return;
		for( int i=0;i<threads.size();i++) {
			RegistrationDomainThread rr=(RegistrationDomainThread)threads.elementAt(i);
			rr.STOP=true;
		}
	}


	public static void writeFile(String outFile, String text) {
		// we read this file to obtain the options
		try{
			FileWriter fileWriter = new FileWriter(outFile,false);
			PrintWriter pw = new PrintWriter(fileWriter,true);

			if (text==null) {
				pw.println();
			}
			else
			{
				pw.println(text);
			}

			pw.close();
			fileWriter.close();
		}
		catch(Exception e) {
			e.printStackTrace();
		}
	}

	public void setExpiresTime(int expiresTime) {
		EXPIRES_TIME_MAX=expiresTime;
	}


	public void writeXMLRegistrations() {
		String registrationsTags=registrationsTable.getXMLTags();
		writeFile(xmlRegistrationsFile,registrationsTags);
	}


	public RegistrationsTable getRegistrationsTable() {
		return registrationsTable;
	}

	public Registration getRegistration(String key) {
		return (Registration) registrationsTable.getRegistrations().get(key);
	}


	/****************************************************************************/
	/*************************** RMI REGISTRY    ********************************/

	public String getRegistryXMLTags() throws RemoteException {
		return registrationsTable.getRegistryXMLTags();
	}



	public synchronized Vector getRegistryBindings() throws RemoteException {
		return registrationsTable.getRegistryBindings();
	}

	public synchronized int getRegistrySize() throws RemoteException {
		return registrationsTable.getRegistrySize();
	}

	// Need to add more registry query functions here.
	public void initRMIBindings() {
		String name = null;
		try {
			Configuration configuration=proxy.getConfiguration();
			if (configuration.accessLogViaRMI) {
				SipStack sipStack=proxy.getSipStack();
				Iterator it  =sipStack.getListeningPoints();
				ListeningPoint lp = (ListeningPoint) it.next();
				String stackIPAddress = sipStack.getIPAddress();
				name = "//" + stackIPAddress + ":" + 0 +  "/" +
						sipStack.getStackName()
				+ "/" + "test.jainproxy.Registrar";
				if (ProxyDebug.debug)  {
					ProxyDebug.println("Exporting Registration Table " + name);
				}
				Naming.rebind(name,this);
			}
			else {
				if (ProxyDebug.debug)
					ProxyDebug.println
					("We don't export the registrations because RMI is disabled.");
			}
		} 
		catch (Exception ex) {
			if (ProxyDebug.debug) {
				ProxyDebug.println
				("Problem trying to export the Registration Table: " + name);
			}
			ex.printStackTrace();
		}
	}

	/******************************************************************************/
	/******************************************************************************/


	/** Process the register message: add, remove, update the bindings
	 *  and manage also the expiration time.
	 *  @param Request Register message to set
	 *  @return int status code of the process of the Register.
	 */
	public
	synchronized void processRegister(Request request, SipProvider sipProvider,
			ServerTransaction serverTransaction ) {
		try{
			MessageFactory messageFactory=proxy.getMessageFactory();

			String key=getKey(request);

			// Add the key if it is a new user:
			if (ProxyDebug.debug){
				ProxyDebug.println
				("Registrar, processRegister(), key: \""+key+"\"");
			}
			if (key==null){
				if (ProxyDebug.debug) {
					ProxyDebug.println
					("Registrar, processRegister(), key is null"+
							" 400 INVALID REQUEST replied");
				}
				Response response=messageFactory.createResponse
						(Response.BAD_REQUEST,request);
				if (serverTransaction!=null)
					serverTransaction.sendResponse(response);
				else sipProvider.sendResponse(response);
				return ;
			}

			// RFC 3261: 10.3:
			/*  6. The registrar checks whether the request contains the Contact
         header field.  If not, it skips to the last step.  If the
         Contact header field is present, the registrar checks if there
         is one Contact field value that contains the special value "*"
         and an Expires field.  If the request has additional Contact
         fields or an expiration time other than zero, the request is
         invalid, and the server MUST return a 400 (Invalid Request) and
         skip the remaining steps.  If not, the registrar checks whether
         the Call-ID agrees with the value stored for each binding.  If
         not, it MUST remove the binding.  If it does agree, it MUST
         remove the binding only if the CSeq in the request is higher
         than the value stored for that binding.  Otherwise, the update
         MUST be aborted and the request fails.
			 */
			
			
			if ( !hasContactHeaders(request) ) {
				Vector contactHeaders=getContactHeaders(key);
				Response response=messageFactory.createResponse
						(Response.OK,request);
				if ( contactHeaders!=null ) {
					for (int i = 0 ; i < contactHeaders.size(); i++) {
						ContactHeader contact = (ContactHeader)
								contactHeaders.elementAt(i);
						response.addHeader(contact);
					}
				}

				if (serverTransaction!=null)
					serverTransaction.sendResponse(response);
				else sipProvider.sendResponse(response);
				if (ProxyDebug.debug) {
					ProxyDebug.println
					("Registrar, processRegister(), response sent:"+response.toString());
				}
				
				// if registrations table has changed we should update the registrations.xml file
				this.writeXMLRegistrations();
				
				return;
			}


			// bug report by Alistair Coles
			if ( hasStar(request) ) {
				Vector contactHeaders=getContactHeaders(key);
				if (contactHeaders.size()>1) {
					if (ProxyDebug.debug) {
						ProxyDebug.println
						("Registrar, processRegister(), more than one contact header"+
								" is present at the same time as a wild card."+
								" 400 INVALID REQUEST replied");
					}
					Response response=messageFactory.createResponse
							(Response.BAD_REQUEST,request);
					if (serverTransaction!=null)
						serverTransaction.sendResponse(response);
					else sipProvider.sendResponse(response);
					if (ProxyDebug.debug) {
						ProxyDebug.println
						("Registrar, processRegister(), response sent:");
						ProxyDebug.print(response.toString());
					}
					return ;
				}

				if ( !hasExpiresZero(request) ) {
					if (ProxyDebug.debug) {
						ProxyDebug.println
						("Registrar, processRegister(), expires time different from"+
								" 0 with a wild card."+
								" 400 INVALID REQUEST replied");
					}
					Response response=messageFactory.createResponse
							(Response.BAD_REQUEST,request);

					if (serverTransaction!=null)
						serverTransaction.sendResponse(response);
					else sipProvider.sendResponse(response); 
					if (ProxyDebug.debug) {
						ProxyDebug.println
						("Registrar, processRegister(), response sent:");
						ProxyDebug.print(response.toString());
					}
					return ;
				}

				if (ProxyDebug.debug) {
					ProxyDebug.println
					("Registrar, processRegister(), (* and expires=0) "+
							" we remove the registration!!");
				}
				registrationsTable.removeRegistration(key);

				Response response=messageFactory.createResponse
						(Response.OK,request);

				if (serverTransaction!=null)
					serverTransaction.sendResponse(response);
				else sipProvider.sendResponse(response); 

				if (ProxyDebug.debug) {
					ProxyDebug.println
					("Registrar, processRegister(), response sent:");
					ProxyDebug.print(response.toString());
				}
				return;
			}
			
			
			
			/*
			 * 29-1-2017
			 * Check if the username and password matches to a current registration
			 * If he username is the same and the passsword is not equal then we send 
			 * BAD Responese to the sender
			 */
			
			String content = new String( request.getRawContent());
			String pass = content;
			String username = getKey(request);
						
			ProxyDebug.println("User: "+username+" Content of request: "+pass + " me tou kwsta ta skoulikia: " + content);

			
			try{
				pass = content.split("Password:")[1];
				pass = pass.split("\n")[0];
				
			}
			catch(Exception e){
				e.printStackTrace();
				ProxyDebug.println("Could not get proper password for User: "+username);
			}
			
			ProxyDebug.println("User: "+username+" trying to connect with password: "+pass);


			if ( registrationsTable.hasRegistration(key) ) {
				
				
				Hashtable registrations = registrationsTable.getRegistrations();
				Registration registration=(Registration) registrations.get(username);
				
				ProxyDebug.println("Given Password: "+pass+" Saved Password: "+registration.getPassword());
				if (!registration.getPassword().equals(pass)){
					
					// username same and password not the same :(
					
					if (ProxyDebug.debug) {
						ProxyDebug.println
						("Registrar, processRegister(), username same and password not the same!");
					}
					Response response=messageFactory.createResponse
							(Response.UNAUTHORIZED,request);
					if (serverTransaction!=null)
						serverTransaction.sendResponse(response);
					else sipProvider.sendResponse(response);
					if (ProxyDebug.debug) {
						ProxyDebug.println
						("Registrar, processRegister(), UNAUTHORIZED response sent:");
						ProxyDebug.print(response.toString());
					}
					return ;
					
				}
				
				
				
				
				registrationsTable.updateRegistration(key,request);
				
				// if registrations table has changed we should update the registrations.xml file
				this.writeXMLRegistrations();

				if (  proxy.getConfiguration().rfc2543Compatible && 
						key.indexOf(":5060") < 0 ) {
					//
					// Hack for Cisco IP Phone which registers incorrectly
					// by not specifying :5060.
					//
					key += ":5060";

					System.out.println("CISCO IP PHONE FIX:  "
							+ "Updating proper registration for " 
							+ key);

					registrationsTable.updateRegistration(key, request);
				}

				Vector contactHeaders=getContactHeaders(key);
				Response response=
						messageFactory.createResponse(Response.OK,request);
				try{
					if ( hasExpiresZero(request) ) {
						response.addHeader(request.getHeader(ExpiresHeader.NAME));
					}
				}
				catch(Exception e){
					e.printStackTrace();
				}
				if ( contactHeaders!=null ) {
					for (int i = 0; i < contactHeaders.size(); i++) {
						ContactHeader contact = (ContactHeader)
								contactHeaders.elementAt(i);
						response.addHeader(contact);
					}
				}

				if (serverTransaction!=null)
					serverTransaction.sendResponse(response);
				else sipProvider.sendResponse(response); 

				if (ProxyDebug.debug)  {
					ProxyDebug.println
					("Registrar, processRegister(), response sent:");
					ProxyDebug.print(response.toString());
				}
			}
			else {
				// Let's check the Expires header:
				if ( hasExpiresZero(request) ) {
					// This message is lost....
					proxy.getPresenceServer();
					ProxyDebug.println("Registrar, processRegister(), "+
							"we don't have any record for this REGISTER.");
					Response response=messageFactory.createResponse
							(Response.OK,request);

					if (serverTransaction!=null)
						serverTransaction.sendResponse(response);
					else sipProvider.sendResponse(response); 
					if (ProxyDebug.debug) {
						ProxyDebug.println
						("Registrar, processRegister(), response sent:");
						ProxyDebug.print(response.toString());
					}
					return;
				}

				registrationsTable.addRegistration(key,request);
				
				// if registrations table has changed we should update the registrations.xml file
				this.writeXMLRegistrations();

				if (proxy.getConfiguration().rfc2543Compatible &&
						key.indexOf(":5060") < 0) {
					//
					// Hack for Cisco IP Phone which registers incorrectly
					// by not specifying :5060.
					//
					key += ":5060";

					System.out.println("CISCO IP PHONE FIX:  "
							+ "adding proper registration for " + key);

					registrationsTable.addRegistration(key, request);
					
					// if registrations table has changed we should update the registrations.xml file
					this.writeXMLRegistrations();
				}

				// we have to forward SUBSCRIBE if the presence server
				// is enabled:

				if (proxy.isPresenceServer()) {
					PresenceServer presenceServer=
							proxy.getPresenceServer();
					ProxyDebug.println("Registrar, processRegister(), "+
							"  we have to check if we have some SUBSCRIBE stored.");
				}

				Vector contactHeaders=getContactHeaders(key);
				Response response=
						messageFactory.createResponse(Response.OK,request);
				if ( contactHeaders!=null ) {
					for (int i = 0; i < contactHeaders.size(); i++) {
						ContactHeader contact = (ContactHeader)
								contactHeaders.elementAt(i);
						response.addHeader(contact);
					}
				}

				if (serverTransaction!=null)
					serverTransaction.sendResponse(response);
				else sipProvider.sendResponse(response); 

				if (ProxyDebug.debug)  {
					ProxyDebug.println
					("Registrar, processRegister(), response sent:");
					ProxyDebug.print(response.toString());
				}
			}
		}  catch (IOException ex) {
			if (ProxyDebug.debug) {
				ProxyDebug.println("Registrar exception raised:");
				ProxyDebug.logException(ex);
			}
		} catch (SipException ex) {
			if (ProxyDebug.debug) {
				ProxyDebug.println("Registrar exception raised:");
				ProxyDebug.logException(ex);
			}
		} catch(Exception ex) {
			if (ProxyDebug.debug) {
				ProxyDebug.println
				("Registrar, processRegister(), internal error, "+
						"exception raised:");
				ProxyDebug.logException(ex);
			}
		}
	}

	public static URI getCleanUri(URI uri) {
		if (uri instanceof SipURI) {
			SipURI sipURI=(SipURI)uri.clone();

			Iterator iterator=sipURI.getParameterNames();
			while (iterator!=null && iterator.hasNext()) {
				String name=(String)iterator.next();
				sipURI.removeParameter(name);
			}
			return  sipURI;
		}
		else return  uri;
	}

	/** The key is built following this rule:
	 * The registrar extracts the address-of-record from the To header
	 * field of the request. The URI
	 * MUST then be converted to a canonical form.  To do that, all
	 * URI parameters MUST be removed (including the user-param), and
	 * any escaped characters MUST be converted to their unescaped
	 * form.  The result serves as an index into the list of bindings
	 */
	public String getKey(Request request) {
		// Let's see if we already have a binding for this request:
		try{
			ToHeader toHeader=(ToHeader)request.getHeader(ToHeader.NAME);
			Address address=null;
			address  = toHeader.getAddress();

			javax.sip.address.URI  cleanedUri;
			if (address==null) {
				cleanedUri= getCleanUri(request.getRequestURI());
			}
			else {
				// We have to build the key, all
				// URI parameters MUST be removed:
				cleanedUri = getCleanUri(address.getURI());
			}
			String  keyresult=cleanedUri.toString();

			return keyresult.toLowerCase();
		} catch(Exception ex) {
			if (ProxyDebug.debug) {
				ProxyDebug.println("Registrar, hasDomainRegistered(), internal error, "+
						"exception raised:");
				ProxyDebug.logException(ex);
			}
			return null;
		}
	}

	public boolean hasRegistration(String key) {
		return registrationsTable.hasRegistration(key);
	}

	public boolean hasDomainRegistered(Request request) {
		try{
			URI uri=request.getRequestURI();
			URI cleanedURI=getCleanUri(uri);

			if (! (cleanedURI instanceof SipURI) ) return false;

			// We have to check the host part:
				String host=((SipURI)cleanedURI).getHost();

				return hasRegistration("sip:"+host );
		}
		catch (Exception ex) {
			if (ProxyDebug.debug) {
				ProxyDebug.println("Registrar, hasDomainRegistered(), internal error, "+
						"exception raised:");
				ProxyDebug.logException(ex);
			}
			return false;
		}
	}


	public boolean hasDomainRegistered(URI uri) {
		try{
			URI cleanedURI=getCleanUri(uri);

			if (! (cleanedURI instanceof SipURI) ) return false;

			// We have to check the host part:
				String host=((SipURI)cleanedURI).getHost();

				return hasRegistration("sip:"+host );
		}
		catch (Exception ex) {
			if (ProxyDebug.debug) {
				ProxyDebug.println("Registrar, hasDomainRegistered(), internal error, "+
						"exception raised:");
				ProxyDebug.logException(ex);
			}
			return false;
		}

	}


	public Vector getDomainContactsURI(Request request) {
		try{
			URI uri=request.getRequestURI();
			URI cleanedURI=getCleanUri(uri);

			if (! (cleanedURI instanceof SipURI) ) return null;

			// We have to check the host part:
				String host=((SipURI)cleanedURI).getHost();

				Vector contacts=getContactHeaders("sip:"+ host );
				if (contacts==null) return null;
				Vector results=new Vector();
				for (int i=0;i<contacts.size();i++) {
					ContactHeader contact = (ContactHeader)
							contacts.elementAt(i);
					Address address=contact.getAddress();
					uri=address.getURI();
					cleanedURI=getCleanUri(uri);
					results.addElement(cleanedURI);
				}
				return results;
		}
		catch (Exception ex) {
			if (ProxyDebug.debug) {
				ProxyDebug.println("Registrar, getDomainContacts(), internal error, "+
						"exception raised:");
				ProxyDebug.logException(ex);
			}
			return null;
		}
	}


	public boolean hasRegistration(Request request)   {
		try{
			String key = getKey(request);
			return hasRegistration(key);
		}
		catch (Exception ex) {
			if (ProxyDebug.debug) {
				ProxyDebug.println("Registrar, hasRegistration(), internal error, "+
						"exception raised:");
				ProxyDebug.logException(ex);
			}
			return false;
		}
	}
	/*
	 * The result is a list of URI that we kept from a registration related
	 * to the ToHeader URI from this request.
	 */
	public Vector getContactsURI(Request request) {
		try{
			String key=getKey(request);
			Vector contacts=getContactHeaders(key);
			if (contacts==null) return null;
			Vector results=new Vector();
			for (int i=0;i<contacts.size();i++) {
				ContactHeader contact = (ContactHeader)
						contacts.elementAt(i);
				Address address=contact.getAddress();
				URI uri=address.getURI();
				URI cleanedURI=getCleanUri(uri);
				results.addElement(cleanedURI);
			}
			return results;
		}
		catch (Exception ex) {
			if (ProxyDebug.debug) {
				ProxyDebug.println
				("Registrar, getContactsURI(), internal error, exception raised:");
				ProxyDebug.logException(ex);
			}
			return null;
		}
	}
	/*
	 * Matches a Sip URI "sip:user@domain" with a list of Contacts
	 * @param key The sip URI found in the To-header of a request
	 * @author Henrik Leion
	 */
	public Vector getContactsURI(String key) {
		try{
			Vector contacts=getContactHeaders(key);
			if (contacts==null) return null;
			Vector results=new Vector();
			for (int i=0;i<contacts.size();i++) {
				ContactHeader contact = (ContactHeader)
						contacts.elementAt(i);
				Address address=contact.getAddress();
				URI uri=address.getURI();
				URI cleanedURI=getCleanUri(uri);
				results.addElement(cleanedURI);
			}
			return results;
		}
		catch (Exception ex) {
			if (ProxyDebug.debug) {
				ProxyDebug.println
				("Registrar, getContactsURI(), internal error, exception raised:");
				ProxyDebug.logException(ex);
			}
			return null;
		}
	}

	public boolean hasContactHeaders(Request request) {
		ListIterator list=(ListIterator)request.getHeaders(ContactHeader.NAME);
		return list!=null;
	}

	private boolean hasStar(Request request) throws Exception{
		ListIterator list=(ListIterator)request.getHeaders(ContactHeader.NAME);

		if (list==null) return false;
		while( list.hasNext() ) {
			ContactHeader contactHeader=(ContactHeader)list.next();
			if (contactHeader.getAddress().isWildcard()  ) {
				return true;
			}
		}
		return false;
	}

	private boolean hasExpiresZero(Request request) {
		try{
			ExpiresHeader expiresHeader=
					(ExpiresHeader)request.getHeader(ExpiresHeader.NAME);
			if (expiresHeader==null) {
				ProxyDebug.println
				("Registrar, hasExpiresZero(), the REGISTER does not have an Expires Header");
				return false;
			}
			else
			{
				ProxyDebug.println
				("Registrar, hasExpiresZero(), the REGISTER has an Expires Header with"+
						" expires time:" +expiresHeader.getExpires());
				return expiresHeader.getExpires()==0;
			}
		}
		catch(Exception e){
			if (ProxyDebug.debug) {
				ProxyDebug.println
				("Registrar, hasExpiresZero(), internal error, exception raised:");
				ProxyDebug.logException(e);
			}
			return false;
		}
	}


	public Vector getContactHeaders(String key) {
		return registrationsTable.getContactHeaders(key);
	}

	public static Vector getContactHeaders(Request request){
		Vector contacts =new Vector();
		try{
			ListIterator list=
					(ListIterator)request.getHeaders(ContactHeader.NAME);

			if (list==null) return contacts;
			while( list.hasNext() ) {
				ContactHeader contactHeader=(ContactHeader)list.next();
				contacts.addElement(contactHeader);
			}

			// We will sort out the contacts following the "q" parameter

			return contacts;
		}
		catch(Exception e){
			if (ProxyDebug.debug) {
				ProxyDebug.println
				("Registrar, getContactHeaders(), internal error, exception raised:");
				ProxyDebug.logException(e);
			}
			return contacts;
		}
	}

	protected void printRegistrations(){
		registrationsTable.printRegistrations();
	}


	
	
	/*
	 * Add the response to the user who wants to perform forwarding 22-1-2017
	 */
	public synchronized void processUserForward(Request request, SipProvider sipProvider,
			ServerTransaction serverTransaction ) {
		try{
			MessageFactory messageFactory=proxy.getMessageFactory();

			String key=getKey(request);

			// Add the key if it is a new user:
			if (ProxyDebug.debug){
				ProxyDebug.println
				("Registrar, processUserForward(), key: \""+key+"\"");
			}
			if (key==null){
				if (ProxyDebug.debug) {
					ProxyDebug.println
					("Registrar, processUserForward(), key is null"+
							" 400 INVALID REQUEST replied");
				}
				Response response=messageFactory.createResponse
						(Response.BAD_REQUEST,request);
				if (serverTransaction!=null)
					serverTransaction.sendResponse(response);
				else sipProvider.sendResponse(response);
				return ;
			}


			if ( registrationsTable.hasRegistration(key) ) {
				ProxyDebug.println("Content request sou einai: "+request.getContent());
				ProxyDebug.println(" to RequestURI sou einai: "+request.getRequestURI());
				ProxyDebug.println("To key tha einai: "+key.toString());
				
				String ForwardTo = null;
				boolean hasCycles = false;
				String Sender = key.toString();
				if(request.getMethod().equals("FORWARD")){
					ForwardTo = request.getRequestURI().toString();
					hasCycles = foundCycleInRegistrationsGraph(Sender, ForwardTo);
				}
				
				
				// Here we should validate if the update in the registration table by checking the graph
				
				
				if(!hasCycles){
					boolean updateresult = registrationsTable.updateForwardRegistration(Sender, ForwardTo); 
					
					if (updateresult){
						//if we are here everything went as planned
						Response response=
								messageFactory.createResponse(Response.OK,request);
						
	
						if (serverTransaction!=null)
							serverTransaction.sendResponse(response);
						else sipProvider.sendResponse(response); 
	
						if (ProxyDebug.debug)  {
							ProxyDebug.println
							("Registrar, processUserForward(), response sent:");
							ProxyDebug.print(response.toString());
						}
						
						// if registrations table has changed we should update the registrations.xml file
						this.writeXMLRegistrations();
						
						return;
						
					}
					
				}
		
			}
			
			
			if (ProxyDebug.debug) {
				ProxyDebug.println
				("No valid User To forward");
			}
			Response response=messageFactory.createResponse
					(Response.BAD_REQUEST,request);
			if (serverTransaction!=null)
				serverTransaction.sendResponse(response);
			else sipProvider.sendResponse(response);
			if (ProxyDebug.debug) {
				ProxyDebug.println
				("Registrar, processUserForward(), response sent:");
				ProxyDebug.print(response.toString());
			}
			return ;

		
		} catch (SipException ex) {
			if (ProxyDebug.debug) {
				ProxyDebug.println("Registrar.processUserForward exception raised:");
				ProxyDebug.logException(ex);
			}
		} catch(Exception ex) {
			if (ProxyDebug.debug) {
				ProxyDebug.println
				("Registrar, processUserForward(), internal error, "+
						"exception raised:");
				ProxyDebug.logException(ex);
			}
		}
	}
	
	
	/*
	 * Add the response to the user who wants to perform Block 25-1-2017
	 */
	public synchronized void processUserBlocking(Request request, SipProvider sipProvider,
			ServerTransaction serverTransaction ) {
		try{
			MessageFactory messageFactory=proxy.getMessageFactory();

			String key=getKey(request);

			// Add the key if it is a new user:
			if (ProxyDebug.debug){
				ProxyDebug.println
				("Registrar, processUserBlocking(), key: \""+key+"\"");
			}
			if (key==null){
				if (ProxyDebug.debug) {
					ProxyDebug.println
					("Registrar, processUserBlocking(), key is null"+
							" 400 INVALID REQUEST replied");
				}
				Response response=messageFactory.createResponse
						(Response.BAD_REQUEST,request);
				if (serverTransaction!=null)
					serverTransaction.sendResponse(response);
				else sipProvider.sendResponse(response);
				return ;
			}


			if ( registrationsTable.hasRegistration(key) ) {
				ProxyDebug.println("Content of request is: "+request.getContent());
				ProxyDebug.println("URI of request is: "+request.getRequestURI());
				ProxyDebug.println("Key is: "+key.toString());
				
				boolean updateresult = false;
				
				String BlockID = null;
				String Sender = key.toString();
				if(request.getMethod().equals("BLOCK")){
					BlockID = request.getRequestURI().toString();
					updateresult = registrationsTable.insertToBlockedUsersListRegistration
							(Sender, BlockID);
				}
				
				if(request.getMethod().equals("UNBLOCK")){
					BlockID = request.getRequestURI().toString();
					updateresult = registrationsTable.deleteFromBlockedUsersListRegistration
							(Sender, BlockID);
				}
				
				
				
				
				
				if (updateresult){
					//if we are here everything went as planned
					Response response=
							messageFactory.createResponse(Response.OK,request);
					

					if (serverTransaction!=null)
						serverTransaction.sendResponse(response);
					else sipProvider.sendResponse(response); 

					if (ProxyDebug.debug)  {
						ProxyDebug.println
						("Registrar, processUserBlocking(), response sent:");
						ProxyDebug.print(response.toString());
					}
					// if registrations table has changed we should update the registrations.xml file
					this.writeXMLRegistrations();
					
					return;
					
				}
					
		
			}
			
			
			if (ProxyDebug.debug) {
				ProxyDebug.println
				("No valid User To Block");
			}
			Response response=messageFactory.createResponse
					(Response.BAD_REQUEST,request);
			if (serverTransaction!=null)
				serverTransaction.sendResponse(response);
			else sipProvider.sendResponse(response);
			if (ProxyDebug.debug) {
				ProxyDebug.println
				("Registrar, processUserBlocking(), response sent:");
				ProxyDebug.print(response.toString());
			}
			return ;

		
		} catch (SipException ex) {
			if (ProxyDebug.debug) {
				ProxyDebug.println("Registrar.processUserBlocking exception raised:");
				ProxyDebug.logException(ex);
			}
		} catch(Exception ex) {
			if (ProxyDebug.debug) {
				ProxyDebug.println
				("Registrar, processUserBlocking(), internal error, "+
						"exception raised:");
				ProxyDebug.logException(ex);
			}
		}
	}
	
	public boolean foundCycleInRegistrationsGraph(String newV1, String newV2){
		// check if the functionality does not allow autoregression
		ProxyDebug.println("[Registrations Graph here] Prior To initilization!");

		//intialize the graph object
		DirectedGraph<String, DefaultEdge> g = new DefaultDirectedGraph<String, DefaultEdge>(DefaultEdge.class);

		ProxyDebug.println("[Registrations Graph here] Initialized Graph!");

		Iterator iterator=registrationsTable.getRegistrations().keySet().iterator();
		
		ProxyDebug.println("[Registrations Graph here] Initialized Iterator!");

		while (iterator!=null && iterator.hasNext()) {
			//ProxyDebug.println("[Registrations Graph Iteration] In!");

			Registration registration=(Registration)registrationsTable.getRegistrations().get(iterator.next());
			//ProxyDebug.println("[Registrations Graph Iteration] Got Registration! "+registration.toString());

			String vertexToAdd = registration.getKey();
			//ProxyDebug.println("[Registrations Graph Iteration] Vertex To add! "+vertexToAdd);

			g.addVertex(vertexToAdd);
			//ProxyDebug.println("[Registrations Graph Iteration] Vertex Added! ");

		}
		ProxyDebug.println("[Registrations Graph here] Vertexes Done!");

		
		boolean sameEdgeTwice = false;
		
		iterator=registrationsTable.getRegistrations().keySet().iterator();
		while (iterator!=null && iterator.hasNext()) {
			Registration registration=(Registration)registrationsTable.getRegistrations().get(iterator.next());
			String v1 = registration.getKey();
			String v2 = registration.getForwardToUser();
			ProxyDebug.println("[Edge Graph iteration] Checking Edge now: "+ v1 + "|" +v2);

			if (v2 != null){
				if (v1.equals(newV1) && v2.equals(newV2)){
					ProxyDebug.println("[Edge Graph iteration] edge already found: "+ v1 + "|" +v2);
					sameEdgeTwice = true;
				}
				
				ProxyDebug.println("[Edge Graph iteration] Inserting Edge now: "+ v1 + "|" +v2);
				g.addEdge(v1,v2);
			}
		}
		//ProxyDebug.println("[Registrations Graph here] foundCycleInRegistrationsGraph: "+ g);
		
		//if the edge does not already exists then add it in the graph and check if there is a circle
		if (!sameEdgeTwice){
			ProxyDebug.println("[Edge Graph iteration] The edge has NOT been found, Add it now: "+ newV1 + "|" +newV2);
			g.addEdge(newV1, newV2);
		}
		
		ProxyDebug.println("[Registrations Graph here] Built in visualization: "+ g);
	    //JGraph jgraph = new JGraph( new JGraphModelAdapter( g ) );
		
		CycleDetector<String, DefaultEdge> cycleDetector = new CycleDetector<String, DefaultEdge>(g);
		ProxyDebug.println("[Registrations Graph here] Cycle Detector: ");

		boolean result = cycleDetector.detectCycles();
		ProxyDebug.println("[Registrations Graph here] Cycle Detector result: "+ result);

		
		return result;
	}
	
	public String findFinalForwardee(String key){
		Hashtable registrations=registrationsTable.getRegistrations();
        Registration currentRegistration=(Registration)registrations.get(key);

		String currentForwardee = currentRegistration.getForwardToUser();
		
		
		while (currentForwardee!=null){
			ProxyDebug.println("mouniasa 0: "+currentForwardee);

			currentRegistration = (Registration)registrations.get(currentForwardee);
			
			if (currentRegistration == null){
				return null;
			}
			
			ProxyDebug.println("mouniasa 1: "+currentRegistration);

			
			currentForwardee = currentRegistration.getForwardToUser();
			
			ProxyDebug.println("Iteration current forwardee: "+currentForwardee);
		}
		
		return currentRegistration.getKey();
	}
	
	public boolean foundInBlockedUsersList(String caller, String callee){
		boolean result = registrationsTable.inBlockedUsersListRegistration(caller,callee);
		return result;
	}
	
	/*
	 * process the OPTION request
	 */
	
	public void processUserBilling(Request request, SipProvider sipProvider,
			ServerTransaction serverTransaction ){
		try{
			MessageFactory messageFactory=proxy.getMessageFactory();

			String key=request.getHeader(FromHeader.NAME).toString();

			// Add the key if it is a new user:
			if (ProxyDebug.debug){
				ProxyDebug.println
				("Registrar, processUserBilling(), key: \""+key+"\"");
			}
			if (key==null){
				if (ProxyDebug.debug) {
					ProxyDebug.println
					("Registrar, processUserBilling(), key is null"+
							" 400 INVALID REQUEST replied");
				}
				Response response=messageFactory.createResponse
						(Response.BAD_REQUEST,request);
				if (serverTransaction!=null)
					serverTransaction.sendResponse(response);
				else sipProvider.sendResponse(response);
				return ;
			}


			if ( registrationsTable.hasRegistration(key) ) {
				ProxyDebug.println("Content of request is: "+request.getContent());
				ProxyDebug.println("URI of request is: "+request.getRequestURI());
				ProxyDebug.println("Key is: "+key.toString());
				
				boolean updateresult = false;
				
				String BlockID = null;
				String Sender = key.toString();
				
					
		
			}
			
			
			if (ProxyDebug.debug) {
				ProxyDebug.println
				("No valid User To Charge the Bill");
			}
			Response response=messageFactory.createResponse
					(Response.BAD_REQUEST,request);
			if (serverTransaction!=null)
				serverTransaction.sendResponse(response);
			else sipProvider.sendResponse(response);
			if (ProxyDebug.debug) {
				ProxyDebug.println
				("Registrar, processUserBilling(), response sent:");
				ProxyDebug.print(response.toString());
			}
			return ;

		
		} catch (SipException ex) {
			if (ProxyDebug.debug) {
				ProxyDebug.println("Registrar.processUserBilling exception raised:");
				ProxyDebug.logException(ex);
			}
		} catch(Exception ex) {
			if (ProxyDebug.debug) {
				ProxyDebug.println
				("Registrar, processUserBilling(), internal error, "+
						"exception raised:");
				ProxyDebug.logException(ex);
			}
		}
	}

	
	
	//Function for debug purposes for a simple proxy response sender
	/* 
	public synchronized void processUserForward2(Request request, SipProvider sipProvider,
			ServerTransaction serverTransaction ) {
        ProxyDebug.println("Kalispera paw gia na steilw");

		try{
			MessageFactory messageFactory=proxy.getMessageFactory();
			String key=getKey(request);

				Vector contactHeaders=getContactHeaders(key);
				Response response=
				messageFactory.createResponse(Response.OK,request);
				try{
					if ( hasExpiresZero(request) ) {
						response.addHeader(request.getHeader(ExpiresHeader.NAME));
					}
				}
				catch(Exception e){
					e.printStackTrace();
				}
				if ( contactHeaders!=null ) {
					for (int i = 0; i < contactHeaders.size(); i++) {
						ContactHeader contact = (ContactHeader)
								contactHeaders.elementAt(i);
						response.addHeader(contact);
					}
				}

		        ProxyDebug.println("Ligo prin steilw");

				if (serverTransaction!=null)
					serverTransaction.sendResponse(response);
				else sipProvider.sendResponse(response); 

				if (ProxyDebug.debug)  {
					ProxyDebug.println
					("Response to FOrward klasopatata, response sent:");
					ProxyDebug.print(response.toString());
				}
				return;
			}
		catch (SipException ex) {
			if (ProxyDebug.debug) {
				ProxyDebug.println("Registrar exception raised:");
				ProxyDebug.logException(ex);
			}
		} catch(Exception ex) {
			if (ProxyDebug.debug) {
				ProxyDebug.println
				("Registrar, processRegister(), internal error, "+
						"exception raised:");
				ProxyDebug.logException(ex);
			}
		}
	}
	*/

}
