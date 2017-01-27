package gov.nist.sip.proxy;


import java.util.*;

import javax.sip.*;
import javax.sip.message.*;
import javax.sip.header.*;
import javax.sip.address.*;
import gov.nist.sip.proxy.registrar.*;
import java.text.ParseException;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import gov.nist.sip.proxy.authentication.*;
import gov.nist.sip.proxy.presenceserver.*;
import gov.nist.sip.proxy.router.*;
import gov.nist.sip.proxy.utils.FileIOHelper;
import gov.nist.javax.sip.header.*;

//ifdef SIMULATION
/*
import sim.java.net.*;
//endif
 */


/** Proxy Entry point.
 *
 *@version  JAIN-SIP-1.1
 *
 *@author Olivier Deruelle <deruelle@nist.gov>  
 * M. Ranganathan <mranga@nist.gov> (convert to simulation) <br/>
 * Henrik Leion: Some changes in how SUBSCRIBE and NOTIFY  are processed. <br/>
 *<a href="{@docRoot}/uncopyright.html">This code is in the public domain.</a>
 *
 */
public class Proxy implements SipListener  {


	protected LinkedList listeningPoints;
	// Map the server transactions with the client transactions
	protected SipStack sipStack;
	protected SipProvider defaultProvider;

	protected MessageFactory messageFactory;
	protected HeaderFactory headerFactory;
	protected AddressFactory addressFactory;

	protected Configuration configuration;
	protected PresenceServer presenceServer;

	protected Registrar registrar;
	protected ProxyUtilities proxyUtilities;
	protected Authentication authentication;
	protected RequestForwarding requestForwarding;
	protected ResponseForwarding responseForwarding;


	public RequestForwarding getRequestForwarding() {
		return requestForwarding;
	}

	public ResponseForwarding getResponseForwarding() {
		return responseForwarding;
	}

	public AddressFactory getAddressFactory() {
		return addressFactory;
	}

	public MessageFactory getMessageFactory() {
		return messageFactory;
	}

	public HeaderFactory getHeaderFactory() {
		return headerFactory;
	}

	public Registrar getRegistrar() {
		return registrar;
	}


	public boolean isPresenceServer() {
		return configuration.enablePresenceServer;
	}

	public PresenceServer getPresenceServer() {
		return presenceServer;
	}

	public ProxyUtilities getProxyUtilities() {
		return proxyUtilities;
	}

	public SipStack getSipStack() {
		return sipStack;
	}

	public Configuration getConfiguration() {
		return configuration;
	}
	/** get the first allocated provider.
	 */
	public SipProvider getSipProvider() {
		return this.defaultProvider;
	}

	public Authentication getAuthentication() {
		return authentication;
	}

	public boolean managesDomain( String domainAddress ) {
		return   configuration.hasDomain(domainAddress) || 
				registrar.hasRegistration("sip:"+domainAddress);
	}


	/** Creates new Proxy */
	public Proxy(String confFile) throws Exception{

		this.listeningPoints = new LinkedList();
		if (confFile==null) {
			System.out.println
			("ERROR: Set the configuration file flag: " +
					"USE: -cf configuration_file_location.xml"  );
		}
		else {
			try {

				// First, let's parse the configuration file.
				ProxyConfigurationHandler handler=
						new ProxyConfigurationHandler(confFile);
				configuration=handler.getConfiguration();
				if (configuration==null ||
						!configuration.isValidConfiguration()) {
					System.out.println
					("ERROR: the configuration file is not correct!"+
							" Correct the errors first.");
					throw new Exception
					("ERROR: the configuration file is not correct!"+
							" Correct the errors first.");
				}
				else {

					proxyUtilities=new ProxyUtilities(this);
					presenceServer=new PresenceServer(this);
					registrar=new Registrar(this);
					requestForwarding=new RequestForwarding(this);
					responseForwarding=new ResponseForwarding(this);
				}
			}
			catch (Exception ex) {
				System.out.println
				("ERROR: exception raised while initializing the proxy");
				ex.printStackTrace();
				throw new Exception
				("ERROR: exception raised while initializing the proxy");
			}
		}
	}



	/** This is a listener method.
	 */ 
	public void processRequest(RequestEvent requestEvent) {
		
		ProxyDebug.println("Pare to stack gamiolh: "+Thread.currentThread().getStackTrace().toString());
		
		for (StackTraceElement ste : Thread.currentThread().getStackTrace()) {
			ProxyDebug.println(ste.toString());
		}
		
		Request request = requestEvent.getRequest();
        ProxyDebug.println("perase sto 0 To request in proxy klasomouno, request: "+request.toString());


		SipProvider sipProvider = (SipProvider) requestEvent.getSource();
		ServerTransaction serverTransaction=requestEvent.getServerTransaction();
		try {

			if (ProxyDebug.debug)
				ProxyDebug.println
				("\n****************************************************"+
						"\nRequest " + request.getMethod() +
						" received:\n"+request.toString());


			if (ProxyDebug.debug) 
				ProxyUtilities.printTransaction(serverTransaction);



			/*******************************************************************************/
			/***********************  PROXY BEHAVIOR    ************************************/
			/*******************************************************************************/

			/* RFC 3261: 16.2:
			 * For all new requests, including any with unknown methods, an element
			 * intending to proxy the request MUST:
			 *
			 * 1. Validate the request (Section 16.3)
			 *
			 * 2. Preprocess routing information (Section 16.4)
			 *
			 * 3. Determine target(s) for the request (Section 16.5)
			 *
			 * 4. Forward the request to each target (Section 16.6)
			 *
			 * 5. Process all responses (Section 16.7)
			 */

			/*******************************************************************************/
			/***************************** 1. Validate the request (Section 16.3) **********/
			/*******************************************************************************/

			/*
            Before an element can proxy a request, it MUST verify the message's
            validity
			 */

	        ProxyDebug.println("perase sto 1 To request in proxy klasomouno, request: "+request.toString());

			
			RequestValidation requestValidation=new RequestValidation(this);
			if ( !requestValidation.validateRequest
					(sipProvider,request,serverTransaction) ) {
				// An appropriate response has been sent back by the request
				// validation step, so we just return. The request has been
				// processed!
				if (ProxyDebug.debug)
					ProxyDebug.println
					("Proxy, processRequest(), the request has not been"+
							" validated, so the request is discarded "  +
							" (an error code has normally been"+
							" sent back)");
				return;
			}

			// Let's check if the ACK is for the proxy: if there is no Route
			// header: it is mandatory for the ACK to be forwarded
			if ( request.getMethod().equals(Request.ACK) ) {
				ListIterator routes = request.getHeaders(RouteHeader.NAME);
				if (routes==null  || !routes.hasNext()) {
					if (ProxyDebug.debug)
						ProxyDebug.println("Proxy, processRequest(), "+
								"the request is an ACK"+
								" targeted for the proxy, we ignore it");
					return;
				}
			}



			if (serverTransaction==null) {
				String method=request.getMethod();       
				// Methods that creates dialogs, so that can 
				// generate transactions
				if ( method.equals(Request.INVITE) ||
						method.equals(Request.SUBSCRIBE)
						) {
					try{
						serverTransaction=
								sipProvider.getNewServerTransaction(request);
						TransactionsMapping transactionsMapping=
								(TransactionsMapping) 
								serverTransaction.getDialog().getApplicationData();
						if (transactionsMapping == null) {
							transactionsMapping = 
									new TransactionsMapping(serverTransaction);
						}
					}
					catch(TransactionAlreadyExistsException e) {
						if (ProxyDebug.debug)
							ProxyDebug.println
							("Proxy, processRequest(), this request"+
									" is a retransmission, we drop it!");
					}
				}
			}

			/***************************************************************************/
			/****** 2. Preprocess routing information (Section 16.4) *******************/
			/***************************************************************************/
	        ProxyDebug.println("perase sto 2 To request in proxy klasomouno, request: "+request.toString());

			/*   The proxy MUST inspect the Request-URI of the request.  If the
            Request-URI of the request contains a value this proxy previously
            placed into a Record-Route header field (see Section 16.6 item 4),
            the proxy MUST replace the Request-URI in the request with the last
            value from the Route header field, and remove that value from the
            Route header field.  The proxy MUST then proceed as if it received
            this modified request.
             .....  (idem to below:)          
             16.12.  The proxy will inspect the URI in the topmost Route header
            field value.  If it indicates this proxy, the proxy removes it
            from the Route header field (this route node has been
            reached).
			 */

			ListIterator routes = request.getHeaders(RouteHeader.NAME);
			if (routes!=null) {
				if ( routes.hasNext() ) {
					RouteHeader  routeHeader = (RouteHeader) routes.next();
					Address routeAddress=routeHeader.getAddress();
					SipURI routeSipURI=(SipURI)routeAddress.getURI();

					String host = routeSipURI.getHost();
					int port = routeSipURI.getPort();

					if (sipStack.getIPAddress().equals(host) ) {
						Iterator lps=sipStack.getListeningPoints();
						while(lps!=null && lps.hasNext()) {
							ListeningPoint lp=(ListeningPoint)lps.next();
							if (lp.getPort()==port) {
								if (ProxyDebug.debug)
									ProxyDebug.println
									("Proxy, processRequest(),"+
											" we remove the first route form " +
											" the RouteHeader;"+
											" it matches the proxy");
								routes.remove();
								break;
							}
						}
					}
				}
			}

			/*
            If the Request-URI contains a maddr parameter, the proxy MUST check
            to see if its value is in the set of addresses or domains the proxy
            is configured to be responsible for.  If the Request-URI has a maddr
            parameter with a value the proxy is responsible for, and the request
            was received using the port and transport indicated (explicitly or by
            default) in the Request-URI, the proxy MUST strip the maddr and any
            non-default port or transport parameter and continue processing as if
            those values had not been present in the request.
			 */

			URI requestURI=request.getRequestURI();
			if (requestURI.isSipURI()) {
				SipURI requestSipURI=(SipURI)requestURI;
				if (requestSipURI.getMAddrParam()!=null ) {
					// The domain the proxy is configured to be responsible for is defined
					// by stack_domain parameter in the configuration file:
					if (configuration.hasDomain(requestSipURI.getMAddrParam())) {
						if (ProxyDebug.debug)
							ProxyDebug.println("Proxy, processRequest(),"+
									" The maddr contains a domain we are responsible for,"+
									" we remove the mAddr parameter from the original"+
									" request");
						// We have to strip the madr parameter:
						requestSipURI.removeParameter("maddr");
						// We have to strip the port parameter:
						if (requestSipURI.getPort()!=5060 && requestSipURI.getPort()!=-1) {
							requestSipURI.setPort(5060);
						}
						// We have to strip the transport parameter:
						requestSipURI.removeParameter("transport");
					}
					else {
						// The Maddr parameter is not a domain we have to take
						// care of, we pass this check...
					}
				}
				else {
					// No Maddr parameter, we pass this check...
				}
			}
			else {
				// No SipURI, so no Maddr parameter, we pass this check...
			}



			/******************************************************************************/
			/************* 3. Determine target(s) for the request (Section 16.5) **********/
			/*****************************************************************************/            
			/*
            The set of targets will either be predetermined by the contents of the 
            request or will be obtained from an abstract location service.  Each 
            target in the set is represented as a URI.
			 */

			Vector targetURIList=new Vector();
			URI targetURI;

			/* If the Request-URI of the request contains an maddr parameter, the
			 * Request-URI MUST be placed into the target set as the only target
			 * URI, and the proxy MUST proceed to Section 16.6.
			 */    

			if (requestURI.isSipURI()) {
				SipURI requestSipURI=(SipURI)requestURI;
				if (requestSipURI.getMAddrParam()!=null ) {
					targetURI=requestURI;
					targetURIList.addElement(targetURI);
					if (ProxyDebug.debug)
						ProxyDebug.println("Proxy, processRequest(),"+
								" the only target is the Request-URI (mAddr parameter)");

					// 4. Forward the request statefully:
					requestForwarding.forwardRequest(targetURIList,sipProvider,
							request,serverTransaction,true);

					return;
				}
			}

			/*
              If the domain of the Request-URI indicates a domain this element is
              not responsible for, the Request-URI MUST be placed into the target
              set as the only target, and the element MUST proceed to the task of
              Request Forwarding (Section 16.6).
			 */

			if (requestURI.isSipURI()) {
				SipURI requestSipURI=(SipURI)requestURI;
				if ( !configuration.hasDomain(requestSipURI.getHost() ) ) {
					if (ProxyDebug.debug)
						ProxyDebug.println("Proxy, processRequest(),"+
								" we are not responsible for the domain: Let's check if we have"+
								" a registration for this domain from another proxy");

					// We have to check if another proxy did not registered
					// to us, in this case we have to use the contacts provided
					// by the registered proxy to create the targets:
					if (registrar.hasDomainRegistered(request)) {
						targetURIList=registrar.getDomainContactsURI(request);
						if (targetURIList!=null && !targetURIList.isEmpty()) {
							if (ProxyDebug.debug) {
								ProxyDebug.println("Proxy, processRequest(), we have"+
										" a registration for this domain from another proxy");
							}
							// 4. Forward the request statefully:
							requestForwarding.forwardRequest(targetURIList,sipProvider,
									request,serverTransaction,true);
							return;

						}
						else {
							targetURIList=new Vector();
							ProxyDebug.println("Proxy, processRequest(),"+
									" we are not responsible for the domain: the only target"+
									" URI is given by the request-URI");
							targetURI=requestURI;
							targetURIList.addElement(targetURI);
						}
					}
					else {
						ProxyDebug.println("Proxy, processRequest(),"+
								" we are not responsible for the domain: the only target"+
								" URI is given by the request-URI");
						targetURI=requestURI;
						targetURIList.addElement(targetURI);
					}

					// 4. Forward the request statelessly:
					requestForwarding.forwardRequest(targetURIList,sipProvider,
							request,serverTransaction,false);

					return;
				}
				else {
					ProxyDebug.println("Proxy, processRequest(),"+
							" we are responsible for the domain... Let's find the contact...");
				}
			}
			
			/*
			 * We insert the listener for the UPDATE requests here 22-1-2017
			 */
	        ProxyDebug.println("perase to 3 To request in proxy , request: "+request.toString());
	        ProxyDebug.println("method for request  "+request.getMethod().toString());
			if (request.getMethod().equals("FORWARD") || request.getMethod().equals("UNFORWARD")){
		        ProxyDebug.println("UPDATE request in proxy , request: "+request.toString());
		        
		        //send the response to the user
		        
		        registrar.processUserForward(request,sipProvider,serverTransaction);    
        
		        
		        return;
			}
			
			
			/**
			 * If the request is options and the content says duration
			 */
			if (request.getMethod().equals("OPTIONS") && request.getContent().toString().contains("Duration")){
		        ProxyDebug.println("OPTION request in proxy , request: "+request.toString());
		        
		        //send the response to the user
		        
		        //registrar.processUserForward(request,sipProvider,serverTransaction);    
        
		        
		        return;
			}

			// we use a SIP registrar:
			if ( request.getMethod().equals(Request.REGISTER) ) {
				if (ProxyDebug.debug) 
					ProxyDebug.println("Incoming request Register");
				// we call the RegisterProcessing:
				registrar.processRegister
				(request,sipProvider,serverTransaction);               
				//Henrik: let the presenceserver do some processing too
				if ( isPresenceServer()) {
					presenceServer.processRegisterRequest
					(sipProvider, request, serverTransaction);
				}

				return;
			}



			/* If we receive a subscription targeted to a user that
			 * is publishing its state here, send to presence server
			 */
			if ( isPresenceServer() && 
					(request.getMethod().equals(Request.SUBSCRIBE))) {
				ProxyDebug.println("Incoming request Subscribe");

				if (presenceServer.isStateAgent(request)) {
					Request clonedRequest=(Request)request.clone();
					presenceServer.processSubscribeRequest(sipProvider,
							clonedRequest,
							serverTransaction);
				} else {
					// Do we know this guy?


					targetURIList = registrar.getContactsURI(request);
					if (targetURIList == null ) { 
						// If not respond that we dont know him.
						ProxyDebug.println
						("Proxy: received a Subscribe request to " +
								" a user in our domain that was not found, " +
								" responded 404");
						Response response=
								messageFactory.createResponse
								(Response.NOT_FOUND,request);
						if (serverTransaction!=null)
							serverTransaction.sendResponse(response);
						else 
							sipProvider.sendResponse(response);
						return;
					} else  { 
						ProxyDebug.println
						("Trying to forward subscribe to " 
								+ targetURIList.toString() + 
								"\n" + request.toString());
						requestForwarding.forwardRequest
						(targetURIList,sipProvider, 
								request,serverTransaction,false);

					}
				}
				return;
			}

			/** Received a Notify. 
			 *  TOADD: Check if it match active VirtualSubscriptions and update it
			 **/
			if ( isPresenceServer() && (request.getMethod().equals
					(Request.NOTIFY) )) { 
				System.out.println("Incoming request Notify");

				Response response=messageFactory.createResponse(481,request);
				response.setReasonPhrase("Subscription does not exist");
				if (serverTransaction!=null)
					serverTransaction.sendResponse(response);
				else 
					sipProvider.sendResponse(response);
				ProxyDebug.println ("Proxy: received a Notify request. Probably wrong, responded 481");
				return;
			}



			if ( isPresenceServer() && ( request.getMethod().equalsIgnoreCase("PUBLISH"))) {

				System.out.println("Incoming request Publish");

				ProxyDebug.println("Proxy: received a Publish request.");
				Request clonedRequest=(Request)request.clone();


				if (presenceServer.isStateAgent(clonedRequest)) {
					ProxyDebug.println("PresenceServer.isStateAgent");
				} else {
					ProxyDebug.println("PresenceServer is NOT StateAgent");
				}

				if (presenceServer.isStateAgent(clonedRequest)) {
					presenceServer.processPublishRequest(sipProvider,
							clonedRequest,
							serverTransaction);
				} else {
					Response response=messageFactory.createResponse(Response.NOT_FOUND,request);
					if (serverTransaction!=null)
						serverTransaction.sendResponse(response);
					else 
						sipProvider.sendResponse(response);
				}
				return;
			}





			// Forward to next hop but dont reply OK right away for the
			// BYE. Bye is end-to-end not hop by hop!
			if (request.getMethod().equals(Request.BYE) ) {
				if (serverTransaction == null) {
					if (ProxyDebug.debug)
						ProxyDebug.println
						("Proxy, null server transactin for BYE");
					return;
				}
				Dialog d = serverTransaction.getDialog();
				TransactionsMapping transactionsMapping = 
						(TransactionsMapping) d.getApplicationData();
				Dialog peerDialog = (Dialog) transactionsMapping.getPeerDialog
						(serverTransaction);
				Request clonedRequest = (Request) request.clone();
				FromHeader from = (FromHeader) 
						clonedRequest.getHeader(FromHeader.NAME);
				from.removeParameter("tag");
				ToHeader to = (ToHeader)
						clonedRequest.getHeader(ToHeader.NAME);
				to.removeParameter("tag");
				ViaHeader via = this.getStackViaHeader();
				clonedRequest.addHeader(via);
				if ( peerDialog.getState() != null ) {
					ClientTransaction newct = 
							sipProvider.getNewClientTransaction
							(clonedRequest);
					transactionsMapping.addMapping(serverTransaction,newct);
					peerDialog.sendRequest(newct);
					return;
				} else {
					// the peer dialog is not yet established so bail out.
					// this is a client error - client is sending BYE
					// before dialog establishment.
					if (ProxyDebug.debug) 
						ProxyDebug.println
						("Proxy, bad dialog state - BYE dropped");
					return;
				}
			}




			/*
              If the target set for the request has not been predetermined as
              described above, this implies that the element is responsible for the
              domain in the Request-URI, and the element MAY use whatever mechanism
              it desires to determine where to send the request.  
              ...
              When accessing the location service constructed by a registrar, the 
              Request-URI MUST first be canonicalized as described in Section 10.3
              before being used as an index.   
			 */
			if (requestURI.isSipURI()) {
				SipURI requestSipURI=(SipURI)requestURI;
				Iterator iterator=requestSipURI.getParameterNames();
				if (ProxyDebug.debug)
					ProxyDebug.println("Proxy, processRequest(), we canonicalized"+
							" the request-URI");
				while (iterator!=null && iterator.hasNext()) {
					String name=(String)iterator.next();
					requestSipURI.removeParameter(name);
				}
			}

			ProxyDebug.println("krakovia sipProvider: "+sipProvider.toString());
			ProxyDebug.println("krakovia request: "+request.toString());
			ProxyDebug.println("krakovia serverTransaction: "+request.getRequestURI());

			/**
			 * Changes request based on final forwardee
			 */
			Request newrequest = transformRequestBasedOnForwardings(request);
			if (newrequest == null){
				ProxyDebug.println
				("Proxy: User To forward not found online");
				Response response=
						messageFactory.createResponse
						(Response.NOT_FOUND,request);
				if (serverTransaction!=null)
					serverTransaction.sendResponse(response);
				else 
					sipProvider.sendResponse(response);
				return;
			}
			request = newrequest;
			// request = transformRequestBasedOnForwardings(request);
			
			if ( registrar.hasRegistration(request)  ) {

				targetURIList=registrar.getContactsURI(request);

				// We fork only INVITE
				if (targetURIList!=null && targetURIList.size()>1 
						&& !request.getMethod().equals("INVITE") ) {
					if (ProxyDebug.debug)
						ProxyDebug.println
						("Proxy, processRequest(), the request "+
								" to fork is not an INVITE, so we will process"+
								" it with the first target as the only target.");
					targetURI= (URI)targetURIList.firstElement();
					targetURIList=new Vector();
					targetURIList.addElement(targetURI);
					// 4. Forward the request statefully to the target:
					requestForwarding.forwardRequest(targetURIList,sipProvider,
							request,serverTransaction,true);
					return;
				}

				if (targetURIList!=null && !targetURIList.isEmpty()) {
					if (ProxyDebug.debug)
						ProxyDebug.println
						("Proxy, processRequest(), the target set"+
								" is the set of the contacts URI from the " +
								" location service");
					
					// 4. Forward the request statefully to each target Section 16.6.:
					ProxyDebug.println("krakovia target URI LIST: "+targetURIList.toString());
					ProxyDebug.println("krakovia sipProvider: "+sipProvider.toString());
					ProxyDebug.println("krakovia request: "+request.toString());
					ProxyDebug.println("krakovia serverTransaction: "+request.getRequestURI());

					requestForwarding.forwardRequest
					(targetURIList,sipProvider,
							request,serverTransaction,true);

					return;
				} else {
					// Let's continue and try the default hop.
				}
			}

			// The registrar cannot help to decide the targets, so let's use
			// our router: the default hop!
			ProxyDebug.println
			("Proxy, processRequest(), the registrar cannot help"+
					" to decide the targets, so let's use our router: the default hop");
			Router router=sipStack.getRouter();
			if (router!=null) {
				ProxyHop hop =(ProxyHop) router.getOutboundProxy();
				if (hop!=null ) {
					if (ProxyDebug.debug)
						ProxyDebug.println
						("Proxy, processRequest(), the target set"+
								" is the defaut hop: outbound proxy");

					// Bug fix contributed by Joe Provino
					String user = null;

					if (requestURI.isSipURI()) {
						SipURI requestSipURI=(SipURI)requestURI;
						user = requestSipURI.getUser();
					}

					SipURI hopURI=addressFactory.createSipURI
							(user,hop.getHost());
					hopURI.setTransportParam(hop.getTransport());
					hopURI.setPort(hop.getPort());
					targetURI=hopURI;
					targetURIList.addElement(targetURI);

					// 4. Forward the request statelessly to each target Section 16.6.:
					requestForwarding.forwardRequest(targetURIList,sipProvider,
							request,serverTransaction,false);

					return;
				}
			}

			/* If the target set remains empty after applying all of the above, the
               proxy MUST return an error response, which SHOULD be the 480
               (Temporarily Unavailable) response.
			 */
			Response response=messageFactory.createResponse
					(Response.TEMPORARILY_UNAVAILABLE,request);
			if (serverTransaction!=null)
				serverTransaction.sendResponse(response);
			else sipProvider.sendResponse(response);

			if (ProxyDebug.debug)
				ProxyDebug.println("Proxy, processRequest(), unable to set "+
						" the targets, 480 (Temporarily Unavailable) replied:\n"+
						response.toString() );

		}
		catch (Exception ex){
			try{
				if (ProxyDebug.debug) {
					ProxyDebug.println("Proxy, processRequest(), internal error, "+
							"exception raised:");
					ProxyDebug.logException(ex);
					ex.printStackTrace();
				}

				// This is an internal error:
				// Let's return a 500 SERVER_INTERNAL_ERROR
				Response response=messageFactory.createResponse
						(Response.SERVER_INTERNAL_ERROR,request);
				if (serverTransaction!=null)
					serverTransaction.sendResponse(response);
				else sipProvider.sendResponse(response);

				if (ProxyDebug.debug)
					ProxyDebug.println("Proxy, processRequest(),"+
							" 500 SERVER_INTERNAL_ERROR replied:\n"+
							response.toString());
			}
			catch (Exception e){
				e.printStackTrace();
			}
		}
	}

	private Request transformRequestBasedOnForwardings(Request request) throws ParseException {
		
		ProxyDebug.println("Proxy [entry]: transformRequestBasedOnForwardings");
		String key=registrar.getKey(request);
		ProxyDebug.println("Request key: " + key);
		ProxyDebug.println("Request: " + request.toString());
		
		String finalForwardee = registrar.findFinalForwardee(key);
		if (finalForwardee == null) {
			ProxyDebug.println("Proxy [transformRequestBasedOnForwardings Error]: null finalforwardee");		
			return null;
		}
		ProxyDebug.println("Trying to addressFactory.createSipURI finalForwardee: "+ finalForwardee );		

		SipURI finalforwardeeuri = stringToSipURI(finalForwardee);
	
		ProxyDebug.println("Found final forwardee: "+ finalforwardeeuri );	
		
		Request newrequest = (Request)request.clone();
		
		ProxyDebug.println("Request header: " + newrequest.getHeader("To"));
		
		ToHeader tempheader = (ToHeader) newrequest.getHeader(ToHeader.NAME);
		Address newaddressheader = addressFactory.createAddress(finalForwardee);
		tempheader.setAddress(newaddressheader);
		
		newrequest.setHeader(tempheader);
		newrequest.setRequestURI(finalforwardeeuri);
		ProxyDebug.println("New Transformed Request: " + newrequest.toString());

		ProxyDebug.println("Proxy [exit]: transformRequestBasedOnForwardings");		
		return newrequest;
	}
	
	// Method for a better URI manipulation with a given string
	
	public SipURI stringToSipURI(String stringos){
		SipURI newSipURIresult = null;
		try{
			String[] parts = stringos.split("@");
			String sipuser = parts[0];
			String[] sipuserparts = sipuser.split(":");
			String username = sipuserparts[1];
			
			String ipandport = parts[1];
			String[] ipandportparts = ipandport.split(":");
			String ipaddress = ipandportparts[0];
			String portaddress = ipandportparts[1];

			
			newSipURIresult = addressFactory.createSipURI(username,ipaddress);
			newSipURIresult.setPort( Integer.parseInt(portaddress));
			
		}
		catch(Exception ex){
			ProxyDebug.println("Error: Couldn't transform String to URI");
		}
		return newSipURIresult;
		
	}

	/** This is a listener method.
	 */
	public void processResponse(ResponseEvent responseEvent) {
		try{
			
			ProxyDebug.println("Pare to stack gamiolh: "+Thread.currentThread().getStackTrace().toString());
			
			for (StackTraceElement ste : Thread.currentThread().getStackTrace()) {
				ProxyDebug.println(ste.toString());
			}
					
			Response response = responseEvent.getResponse();

			
	        ProxyDebug.println("perase sto 0 To response in proxy klasomouno, request: "+response.toString());



			SipProvider sipProvider = (SipProvider) responseEvent.getSource();
			ClientTransaction clientTransaction=responseEvent.getClientTransaction();

			ProxyDebug.println
			("\n***************************************************************"+
					"\n***************************************************************"+
					"\nResponse "+response.getStatusCode() + " "+response.getReasonPhrase()
					+" received:\n"+response.toString() );
			ProxyDebug.println("Processing Response in progress");

			if (ProxyDebug.debug)
				ProxyUtilities.printTransaction(clientTransaction);

			//Henrik - added handling of responses addressed to server
			//If we use a presenceserver, and if statuscode was OK...
			CSeqHeader cseqHeader = (CSeqHeader)response.getHeader(CSeqHeader.NAME); 

			if (cseqHeader.getMethod().equals("SUBSCRIBE")) {
				presenceServer.processSubscribeResponse((Response)response.clone(), clientTransaction);
			} else if (cseqHeader.getMethod().equals("NOTIFY")) {
				//presenceServer.processNotifyResponse((Response)response.clone(), clientTransaction);
			} 

			responseForwarding.forwardResponse(sipProvider, response,clientTransaction);

		} catch (Exception ex) {
			if (ProxyDebug.debug) {
				ProxyDebug.println("Proxy, processResponse(), internal error, "+
						"exception raised:");
				ProxyDebug.logException(ex);
			}
		}
	}


	/** JAIN Listener method.
	 */
	public void processTimeout(TimeoutEvent timeOutEvent) {
		ProxyDebug.println("TimeoutEvent received");
		SipProvider sipProvider = (SipProvider)timeOutEvent.getSource();
		TransactionsMapping transactionsMapping = null;
		if (timeOutEvent.isServerTransaction()) {
			ServerTransaction serverTransaction  =
					timeOutEvent.getServerTransaction();
			Dialog dialog = serverTransaction.getDialog();
			if (dialog != null) {
				transactionsMapping = 
						(TransactionsMapping) dialog.getApplicationData();
				transactionsMapping.removeMapping(serverTransaction);
			}
		} else {
			ClientTransaction clientTransaction =
					timeOutEvent.getClientTransaction();
			Dialog dialog = clientTransaction.getDialog();
			ServerTransaction st = null;
			if (dialog != null) {
				transactionsMapping = 
						(TransactionsMapping) dialog.getApplicationData();
				if (transactionsMapping != null)  {
					st = transactionsMapping.getServerTransaction
							(clientTransaction);
				}
				if (st==null) {
					ProxyDebug.println
					("ERROR, Unable to retrieve the server transaction,"+
							" cannot process timeout!");
					return;
				}
			} else {
				ProxyDebug.println
				("ERROR, Unable to retrieve the transaction Mapping,"+
						" cannot process timeout!");
				return;
			}
			Request request = st.getRequest();
			// This removes the given mapping from the table but not
			// necessarily the whole thing.
			transactionsMapping.removeMapping(clientTransaction);
			if (!transactionsMapping.hasMapping(st)) {
				// No more mappings left in the transaction table.
				try {
					Response response = messageFactory.createResponse
							(Response.REQUEST_TIMEOUT, request);
					st.sendResponse(response);
				} catch (ParseException ex) {
					ex.printStackTrace();
				} catch (SipException ex1) {
					ex1.printStackTrace();
				}
			}
		}


	}




	/***********************  Methods for         ***************
	 *    starting and stopping the proxy          		*
	 ************************************************************/

	/** Start the proxy, this method has to be called after the init method
	 * throws Exception that which can be caught by the upper application
	 */
	public void start() throws Exception {
		if (configuration!=null
				&& configuration.isValidConfiguration()) {
			Properties properties=new Properties();
			// LOGGING property:

			if (configuration.enableDebug) {
				ProxyDebug.debug=true;
				ProxyDebug.setProxyOutputFile(configuration.outputProxy);
				ProxyDebug.println("DEBUG properties set!");
				if (configuration.badMessageLogFile!=null)
					properties.setProperty("gov.nist.javax.sip.BAD_MESSAGE_LOG",
							configuration.badMessageLogFile);
				if (configuration.debugLogFile!=null) {
					properties.setProperty("gov.nist.javax.sip.DEBUG_LOG",
							configuration.debugLogFile);

				}
				if (configuration.serverLogFile!=null)
					properties.setProperty("gov.nist.javax.sip.SERVER_LOG",
							configuration.serverLogFile);
				if (configuration.debugLogFile != null)
					properties.setProperty("gov.nist.javax.sip.TRACE_LEVEL",
							"32");
				else
					if (configuration.serverLogFile != null)
						properties.setProperty("gov.nist.javax.sip.TRACE_LEVEL",
								"16");
					else
						properties.setProperty("gov.nist.javax.sip.TRACE_LEVEL",
								"0");


			}
			else {
				System.out.println("DEBUG properties not set!");
			}
			registrar.setExpiresTime(configuration.expiresTime);

			// STOP TIME
			if (configuration.stopTime!=null) {
				try {
					long stopTime=Long.parseLong(configuration.stopTime);
					StopProxy stopProxy=new StopProxy(this);
					Timer timer=new Timer();
					timer.schedule(stopProxy,stopTime);
				}
				catch(Exception e) {
					e.printStackTrace();
				}
			}

			sipStack = null;

			SipFactory sipFactory = SipFactory.getInstance();
			sipFactory.setPathName("gov.nist");

			headerFactory = sipFactory.createHeaderFactory();
			addressFactory = sipFactory.createAddressFactory();
			messageFactory = sipFactory.createMessageFactory();


			// Create SipStack object

			properties.setProperty("javax.sip.IP_ADDRESS",
					configuration.stackIPAddress);

			// We have to add the IP address of the proxy for the domain:
			configuration.domainList.addElement(configuration.stackIPAddress);
			ProxyDebug.println("The proxy is responsible for the domain:"+configuration.stackIPAddress);

			properties.setProperty("javax.sip.STACK_NAME",
					configuration.stackName);
			if (configuration.check(configuration.outboundProxy))
				properties.setProperty("javax.sip.OUTBOUND_PROXY",
						configuration.outboundProxy);
			if (configuration.check(configuration.routerPath))
				properties.setProperty("javax.sip.ROUTER_PATH",
						configuration.routerPath);
			if (configuration.check(configuration.extensionMethods))
				properties.setProperty("javax.sip.EXTENSION_METHODS",
						configuration.extensionMethods);
			// This has to be hardcoded to true. for the proxy.
			properties.setProperty("javax.sip.RETRANSMISSION_FILTER",
					"on");

			if (configuration.check(configuration.maxConnections) ) 
				properties.setProperty("gov.nist.javax.sip.MAX_CONNECTIONS",
						configuration.maxConnections);
			if (configuration.check(configuration.maxServerTransactions) ) 
				properties.setProperty("gov.nist.javax.sip.MAX_SERVER_TRANSACTIONS",
						configuration.maxServerTransactions);
			if (configuration.check(configuration.threadPoolSize) ) 
				properties.setProperty("gov.nist.javax.sip.THREAD_POOL_SIZE",
						configuration.threadPoolSize);

			if (configuration.domainList!=null)
				for ( int j=0;j<configuration.domainList.size();j++) {
					String domain=(String)configuration.domainList.elementAt(j);
					ProxyDebug.println("Here is one domain to take care of:"+domain);
				}
			else ProxyDebug.println("No domain to take care of...");

			if (configuration.accessLogViaRMI) {
				properties.setProperty("gov.nist.javax.sip.ACCESS_LOG_VIA_RMI",
						"true");

				properties.setProperty("gov.nist.javax.sip.RMI_PORT",
						configuration.logRMIPort);

				if (configuration.check(configuration.logLifetime) )
					properties.setProperty("gov.nist.javax.sip.LOG_LIFETIME",
							configuration.logLifetime);

			}

			sipStack = sipFactory.createSipStack(properties);




			// Authentication part:
			if (configuration.enableAuthentication) {
				authentication =new Authentication(this);
				try{

					Class authMethodClass =
							Class.forName(configuration.classFile);
					AuthenticationMethod authMethod
					= (AuthenticationMethod)
					authMethodClass.newInstance();
					authMethod.initialize(configuration.passwordsFile);

					authentication.setAuthenticationMethod(authMethod);

				}
				catch(Exception e) {
					ProxyDebug.println
					("ERROR, authentication process stopped, exception raised:");
					e.printStackTrace();
				}
			}

			// We create the Listening points:
			Vector lps=configuration.getListeningPoints();

			for ( int i=0;lps!=null && i<lps.size();i++) {
				Association a=(Association)lps.elementAt(i);
				try{
					System.out.println("transport  " + a.transport);
					System.out.println("port  " +
							Integer.valueOf(a.port).intValue());
					ListeningPoint lp=sipStack.createListeningPoint
							(Integer.valueOf(a.port).intValue(),
									a.transport);
					this.listeningPoints.add(lp);
					SipProvider sipProvider = sipStack.createSipProvider(lp);
					if (this.defaultProvider == null) 
						this.defaultProvider = sipProvider;
					sipProvider.addSipListener( this );
				}
				catch(Exception e) {
					e.printStackTrace();
					ProxyDebug.println
					("ERROR: listening point not created ");
				}
			}
			// Export the registry for polling by the responder.

			if (configuration.exportRegistry )
				// initialize the registrar for  RMI.
				this.registrar.initRMIBindings();


			// Parse static configuration for registrar.
			if (configuration.enableRegistrations) {
				String value=configuration.registrationsFile;
				ProxyDebug.println("Parsing the XML registrations file: "+value);
				if (value==null || value.trim().equals("")) {
					ProxyDebug.println("You have to set the registrations file...");
				} else {
					registrar.parseXMLregistrations(value);
				}
			}
			else ProxyDebug.println("No registrations to parse...");

			// Register to proxies if any:
			registrar.registerToProxies();

		}
		else {
			System.out.println("ERROR: the configuration file is not correct!"+
					" Correct the errors first.");
		}
	}

	/** Stop the proxy, this method has to be called after the start method
	 * throws Exception that which can be caught by the upper application
	 */
	public void stop()  throws Exception {
		if (sipStack==null) return;     
		this.presenceServer.stop();

		Iterator sipProviders=sipStack.getSipProviders();
		if (sipProviders!=null) {
			while( sipProviders.hasNext()) {
				SipProvider sp=(SipProvider)sipProviders.next();                    
				sp.removeSipListener(this);
				sipStack.deleteSipProvider(sp);
				sipProviders=sipStack.getSipProviders();
				System.out.println("One sip Provider removed!");
			}
		}
		else {
			ProxyDebug.println("WARNING, STOP_PROXY, The proxy " +
					" has no sip Provider to remove!");
		}

		Iterator listeningPoints=sipStack.getListeningPoints();
		if (listeningPoints!=null) {
			while( listeningPoints.hasNext()) {
				ListeningPoint lp=(ListeningPoint)listeningPoints.next();
				sipStack.deleteListeningPoint(lp);
				listeningPoints=sipStack.getListeningPoints();
				System.out.println("One listening point removed!");
			}
		}
		else {
			ProxyDebug.println("WARNING, STOP_PROXY, The proxy " +
					" has no listening points to remove!");
		}        
		registrar.clean();
	}

	/** Exit the proxy,
	 * throws Exception that which can be caught by the upper application
	 */
	public void exit()  throws Exception {
		Iterator sipProviders=sipStack.getSipProviders();
		if (sipProviders!=null) {
			while( sipProviders.hasNext()) {
				SipProvider sp=(SipProvider)sipProviders.next();                    
				sp.removeSipListener(this);
				sipStack.deleteSipProvider(sp);
				sipProviders=sipStack.getSipProviders();
				System.out.println("One sip Provider removed!");
			}
		}
		else {
			ProxyDebug.println("WARNING, STOP_PROXY, The proxy " +
					" has no sip Provider to remove!");
		}

		Iterator listeningPoints=sipStack.getListeningPoints();
		if (listeningPoints!=null) {
			while( listeningPoints.hasNext()) {
				ListeningPoint lp=(ListeningPoint)listeningPoints.next();
				sipStack.deleteListeningPoint(lp);
				listeningPoints=sipStack.getListeningPoints();
				System.out.println("One listening point removed!");
			}
		}
		else {
			ProxyDebug.println("WARNING, STOP_PROXY, The proxy " +
					" has no listening points to remove!");
		}        
		ProxyDebug.println("Proxy exit.........................");
		configuration.listeningPoints.clear();
		registrar.clean();
	}

	public ViaHeader getStackViaHeader() {
		try {
			ListeningPoint lp = 
					(ListeningPoint)sipStack.getListeningPoints().next();
			String host = sipStack.getIPAddress();
			int port = lp.getPort();
			String transport = lp.getTransport();
			// branch id is assigned by the transaction layer.
			return  headerFactory.createViaHeader
					(host,port,transport,null);
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}

	}

	public ContactHeader getStackContactHeader() {
		try {
			ListeningPoint lp = 
					(ListeningPoint)sipStack.getListeningPoints().next();
			String host = sipStack.getIPAddress();
			int port = lp.getPort();
			String transport = lp.getTransport();

			SipURI sipURI=addressFactory.createSipURI(null,host);
			sipURI.setPort(port);
			sipURI.setTransportParam(transport);
			Address contactAddress=addressFactory.createAddress(sipURI);

			return headerFactory.createContactHeader(contactAddress);
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}

	}


	/*************************************************************************/
	/************ The main method: to launch the proxy          *************/
	/************************************************************************/


	public static void main(String args[]) {
		try{
			// the Proxy:
			if (args.length == 0)  {
				System.out.println("Config file missing!");
				System.exit(0);
			}

			System.out.println("Using configuration file " + args[1]);
			String confFile= (String) args[1];
			Proxy proxy=new Proxy(confFile);
			proxy.start();
			ProxyDebug.println("Proxy ready to work");
		}
		catch(Exception e) {
			System.out.println
			("ERROR: Set the configuration file flag: " +
					"USE: -cf configuration_file_location.xml"  );
			System.out.println("ERROR, the proxy can not be started, " +
					" exception raised:\n");
			e.printStackTrace();
		}
	}

}

