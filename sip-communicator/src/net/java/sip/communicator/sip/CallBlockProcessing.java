package net.java.sip.communicator.sip;

import java.text.ParseException;
import java.util.ArrayList;

import javax.sip.ClientTransaction;
import javax.sip.InvalidArgumentException;
import javax.sip.TransactionUnavailableException;
import javax.sip.address.Address;
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

public class CallBlockProcessing {
	private static final Console console =
	        Console.getConsole(CallBlockProcessing.class);
	private SipManager sipManCallback = null;
	private Request blockRequest = null;
	private boolean forwardeeURI = false;

	CallBlockProcessing(SipManager sipManCallback)
    {
        this.sipManCallback = sipManCallback;
    }

    void setSipManagerCallBack(SipManager sipManCallback)
    {
        this.sipManCallback = sipManCallback;
    }
    
    void blockOK(ClientTransaction clientTransaction, Response response)
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
    
    
    
	
	synchronized void block(String registrarAddress, int registrarPort,
            String registrarTransport, int expires, String addressToBlock) throws
	   CommunicationsException
	{
	  try
	  {
	      console.logEntry();
	      console.debug("Address to block: " + addressToBlock);
	      
	      //Get the From Header and address
	      FromHeader fromHeader = sipManCallback.getFromHeader();
	      Address fromAddress = fromHeader.getAddress();
	      console.debug("From Header: " + fromHeader);
	      
	      /**
	       * TODO: GUI CALL Fix later
	       */
	      //sipManCallback.fireRegistering(fromAddress.toString());
	      
	      //Make the blockee address in a URI
          URI requestURI = ProcessingUtilities.createUriFromAddress(addressToBlock, sipManCallback, console);
	      CallIdHeader callIdHeader = sipManCallback.sipProvider.getNewCallId();
	      CSeqHeader cSeqHeader = ProcessingUtilities.safeCSeqHeader(1, Request.UPDATE, sipManCallback, console);
	      ToHeader toHeader = ProcessingUtilities.headerFromAddress(fromAddress, sipManCallback, console);
	      ArrayList viaHeaders = sipManCallback.getLocalViaHeaders();
	      MaxForwardsHeader maxForwardsHeader = sipManCallback.
	          getMaxForwardsHeader();
	      
	      /**
	       * Make the request and then add an expires and a contact header
	       */
	      Request request = null;
	      try {
	          request = sipManCallback.messageFactory.createRequest(requestURI,
	              "BLOCK ",
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
	      ClientTransaction blockTrans = null;
	      try {
	    	  blockTrans = sipManCallback.sipProvider.getNewClientTransaction(
	              request);
	      }
	      catch (TransactionUnavailableException ex) {
	          console.error("Could not create a block transaction!\n"
	                        + "Check that the Registrar address is correct!",
	                        ex);
	          //throw was missing - reported by Eero Vaarnas
	          throw new CommunicationsException(
	              "Could not create a block transaction!\n"
	              + "Check that the Registrar address is correct!");
	      }
	      try {
	    	  blockTrans.sendRequest();
	          if( console.isDebugEnabled() )
	              console.debug("sent request= " + request);
	          //[issue 2] Schedule re registrations
	          //bug reported by LynlvL@netscape.com
	          
	          //scheduleReRegistration( registrarAddress, registrarPort,
	          //            registrarTransport, expires);
	
	      }
	      //we sometimes get a null pointer exception here so catch them all
	      catch (Exception ex) {
	          console.error("Could not send out the block request!", ex);
	          //throw was missing - reported by Eero Vaarnas
	          throw new CommunicationsException(
	              "Could not send out the block request!", ex);
	      }
	      console.debug(blockTrans);
	      this.blockRequest = request;
	      
	  }
	  finally
	  {
	      console.logExit();
	  }
	
	}
	
	synchronized void unblock(String registrarAddress, int registrarPort,
            String registrarTransport, int expires, String addressToBlock) throws
	   CommunicationsException
	{
	  try
	  {
	      console.logEntry();
	      console.debug("Address to block: " + addressToBlock);
	      
	      //Get the From Header and address
	      FromHeader fromHeader = sipManCallback.getFromHeader();
	      Address fromAddress = fromHeader.getAddress();
	      console.debug("From Header: " + fromHeader);
	      
	      /**
	       * TODO: GUI CALL Fix later
	       */
	      //sipManCallback.fireRegistering(fromAddress.toString());
	      
	      //Make the blockee address in a URI
          URI requestURI = ProcessingUtilities.createUriFromAddress(addressToBlock, sipManCallback, console);
	      CallIdHeader callIdHeader = sipManCallback.sipProvider.getNewCallId();
	      CSeqHeader cSeqHeader = ProcessingUtilities.safeCSeqHeader(1, Request.UPDATE, sipManCallback, console);
	      ToHeader toHeader = ProcessingUtilities.headerFromAddress(fromAddress, sipManCallback, console);
	      ArrayList viaHeaders = sipManCallback.getLocalViaHeaders();
	      MaxForwardsHeader maxForwardsHeader = sipManCallback.
	          getMaxForwardsHeader();
	      
	      /**
	       * Make the request and then add an expires and a contact header
	       */
	      Request request = null;
	      try {
	          request = sipManCallback.messageFactory.createRequest(requestURI,
	              "UNBLOCK ",
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
	      ClientTransaction blockTrans = null;
	      try {
	    	  blockTrans = sipManCallback.sipProvider.getNewClientTransaction(
	              request);
	      }
	      catch (TransactionUnavailableException ex) {
	          console.error("Could not create a block transaction!\n"
	                        + "Check that the Registrar address is correct!",
	                        ex);
	          //throw was missing - reported by Eero Vaarnas
	          throw new CommunicationsException(
	              "Could not create a block transaction!\n"
	              + "Check that the Registrar address is correct!");
	      }
	      try {
	    	  blockTrans.sendRequest();
	          if( console.isDebugEnabled() )
	              console.debug("sent request= " + request);
	          //[issue 2] Schedule re registrations
	          //bug reported by LynlvL@netscape.com
	          
	          //scheduleReRegistration( registrarAddress, registrarPort,
	          //            registrarTransport, expires);
	
	      }
	      //we sometimes get a null pointer exception here so catch them all
	      catch (Exception ex) {
	          console.error("Could not send out the block request!", ex);
	          //throw was missing - reported by Eero Vaarnas
	          throw new CommunicationsException(
	              "Could not send out the block request!", ex);
	      }
	      console.debug(blockTrans);
	      this.blockRequest = request;
	      
	  }
	  finally
	  {
	      console.logExit();
	  }
	
	}
}
