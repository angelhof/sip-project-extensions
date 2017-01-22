package net.java.sip.communicator.sip;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.Timer;

import javax.sip.ClientTransaction;
import javax.sip.InvalidArgumentException;
import javax.sip.TransactionUnavailableException;
import javax.sip.address.Address;
import javax.sip.address.SipURI;
import javax.sip.address.URI;
import javax.sip.header.CSeqHeader;
import javax.sip.header.CallIdHeader;
import javax.sip.header.ContactHeader;
import javax.sip.header.ExpiresHeader;
import javax.sip.header.FromHeader;
import javax.sip.header.MaxForwardsHeader;
import javax.sip.header.ToHeader;
import javax.sip.message.Request;
import javax.sip.message.Response;

import net.java.sip.communicator.common.Console;

public class CallForwardProcessing {
	
	private static final Console console =
	        Console.getConsole(CallForwardProcessing.class);
	private SipManager sipManCallback = null;
	private Request forwardRequest = null;
	private boolean forwardeeURI = false;

	CallForwardProcessing(SipManager sipManCallback)
    {
        this.sipManCallback = sipManCallback;
    }

    void setSipManagerCallBack(SipManager sipManCallback)
    {
        this.sipManCallback = sipManCallback;
    }
    
    void forwardOK(ClientTransaction clientTransaction, Response response)
    {
        try {
            console.logEntry();
            
            // Get the request that was accepted
            Request request = clientTransaction.getRequest();
            
            // Save the forwardee URI
            URI forwardeeURI = request.getRequestURI();
            
            // Fire forwarded ok to pass the information to all listeners
            sipManCallback.fireEnabledForward(forwardeeURI.toString());
            
        }
        finally {
            console.logExit();
        }
    }
    
    /**
     * Create URI from Address 
     * @param addressToForward
     * @return
     * @throws CommunicationsException
     */
    URI createUriFromAddress(String addressToForward) throws CommunicationsException
    {
    	URI requestURI;
    	try {
            requestURI = sipManCallback.addressFactory.createURI(addressToForward);
        }
        catch (ParseException ex) {
            console.error(addressToForward + " is not a legal SIP uri!", ex);
            throw new CommunicationsException(addressToForward +
                                              " is not a legal SIP uri!", ex);
        }
    	return requestURI;
    }
    
	
	synchronized void forward(String registrarAddress, int registrarPort,
            String registrarTransport, int expires, String addressToForward) throws
	   CommunicationsException
	{
	  try
	  {
	      console.logEntry();
	      console.debug("Address to forward: " + addressToForward);
	      
	      //From
	      FromHeader fromHeader = sipManCallback.getFromHeader();
	      Address fromAddress = fromHeader.getAddress();
	      
	      console.debug("From Header: " + fromHeader);
	      
	      /**
	       * TODO: GUI CALL Fix later
	       */
	      //sipManCallback.fireRegistering(fromAddress.toString());
	      
	      
	      //Request URI
          URI requestURI = createUriFromAddress(addressToForward);
	      console.debug("Request URI: " + requestURI);
	      
	      
	      //Call ID Header
	      CallIdHeader callIdHeader = sipManCallback.sipProvider.getNewCallId();
	      //CSeq Header
	      CSeqHeader cSeqHeader = null;
	      try {
	    	  // We used the UPDATE method of the SIP
	          cSeqHeader = sipManCallback.headerFactory.createCSeqHeader(1,
	              Request.UPDATE);
	      }
	      catch (ParseException ex) {
	          //Should never happen
	          console.error("Corrupt Sip Stack");
	          Console.showError("Corrupt Sip Stack");
	      }
	      catch (InvalidArgumentException ex) {
	          //Should never happen
	          console.error("The application is corrupt");
	          Console.showError("The application is corrupt!");
	      }
	      
	      console.debug("Call Id Header: " + callIdHeader);
	      console.debug("C Seq Header: " + cSeqHeader);
	      
	      
	      //To Header
	      ToHeader toHeader = null;
	      try {
	          toHeader = sipManCallback.headerFactory.createToHeader(fromAddress, null);
	      }
	      catch (ParseException ex) {
	          console.error("Could not create a To header for address:"
	                        + fromHeader.getAddress(),
	                        ex);
	          //throw was missing - reported by Eero Vaarnas
	          throw new CommunicationsException("Could not create a To header "
	                                      + "for address:"
	                                      + fromHeader.getAddress(),
	                                      ex);
	      }
	      //Via Headers
	      ArrayList viaHeaders = sipManCallback.getLocalViaHeaders();
	      //MaxForwardsHeader
	      MaxForwardsHeader maxForwardsHeader = sipManCallback.
	          getMaxForwardsHeader();
	      
	      console.debug(toHeader);
	      
	      
	      
	      /**
	       * Make the request and then add an expires and a contact header
	       */
	      Request request = null;
	      try {
	          request = sipManCallback.messageFactory.createRequest(requestURI,
	              "FORWARD ",
	              callIdHeader,
	              cSeqHeader, fromHeader, toHeader,
	              viaHeaders,
	              maxForwardsHeader);
	      }
	      catch (ParseException ex) {
	          console.error("Could not create the register request!", ex);
	          //throw was missing - reported by Eero Vaarnas
	          throw new CommunicationsException(
	              "Could not create the register request!",
	              ex);
	      }
	      
	      
	      //Expires Header
	      ExpiresHeader expHeader = null;
	      for (int retry = 0; retry < 2; retry++) {
	          try {
	              expHeader = sipManCallback.headerFactory.createExpiresHeader(
	                  expires);
	          }
	          catch (InvalidArgumentException ex) {
	              if (retry == 0) {
	                  expires = 3600;
	                  continue;
	              }
	              console.error(
	                  "Invalid registrations expiration parameter - "
	                  + expires,
	                  ex);
	              throw new CommunicationsException(
	                  "Invalid registrations expiration parameter - "
	                  + expires,
	                  ex);
	          }
	      }
	      request.addHeader(expHeader);
	      //Contact Header should contain IP - bug report - Eero Vaarnas
	      ContactHeader contactHeader = sipManCallback.
	          getRegistrationContactHeader();
	      request.addHeader(contactHeader);
	      
	      console.debug(request);
	      
	      
	      
	      //Transaction
	      ClientTransaction forwardTrans = null;
	      try {
	    	  forwardTrans = sipManCallback.sipProvider.getNewClientTransaction(
	              request);
	      }
	      catch (TransactionUnavailableException ex) {
	          console.error("Could not create a forward transaction!\n"
	                        + "Check that the Registrar address is correct!",
	                        ex);
	          //throw was missing - reported by Eero Vaarnas
	          throw new CommunicationsException(
	              "Could not create a forward transaction!\n"
	              + "Check that the Registrar address is correct!");
	      }
	      try {
	    	  forwardTrans.sendRequest();
	          if( console.isDebugEnabled() )
	              console.debug("sent request= " + request);
	          //[issue 2] Schedule re registrations
	          //bug reported by LynlvL@netscape.com
	          
	          //scheduleReRegistration( registrarAddress, registrarPort,
	          //            registrarTransport, expires);
	
	      }
	      //we sometimes get a null pointer exception here so catch them all
	      catch (Exception ex) {
	          console.error("Could not send out the forward request!", ex);
	          //throw was missing - reported by Eero Vaarnas
	          throw new CommunicationsException(
	              "Could not send out the forward request!", ex);
	      }
	      console.debug(forwardTrans);
	      this.forwardRequest = request;
	      
	  }
	  finally
	  {
	      console.logExit();
	  }
	
	}
}
