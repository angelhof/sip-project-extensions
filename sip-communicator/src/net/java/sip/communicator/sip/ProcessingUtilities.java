package net.java.sip.communicator.sip;

import java.text.ParseException;

import javax.sip.InvalidArgumentException;
import javax.sip.address.Address;
import javax.sip.address.URI;
import javax.sip.header.CSeqHeader;
import javax.sip.header.ToHeader;
import javax.sip.message.Request;

import net.java.sip.communicator.common.Console;

public class ProcessingUtilities {
	
	/**
     * Create URI from Address 
     * @param addressToForward
     * @return
     * @throws CommunicationsException
     */
    static public URI createUriFromAddress(String addressToForward, SipManager sipManCallback, Console console) throws CommunicationsException
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
    
    /**
     * Create the CSeqHeader in a safe way
     * @param i
     * @param method
     * @param sipManCallback
     * @param console
     * @return
     * @throws CommunicationsException 
     */
    static public CSeqHeader safeCSeqHeader(int i, String method, SipManager sipManCallback, Console console) throws CommunicationsException
    {
    	CSeqHeader cSeqHeader;
    	try {
    		// We used the requested method of the SIP
		    cSeqHeader = sipManCallback.headerFactory.createCSeqHeader(i, method);
		}
		catch (ParseException ex) {
		    //Should never happen
		    console.error("Corrupt Sip Stack");
		    Console.showError("Corrupt Sip Stack");
		    throw new CommunicationsException("Corrupt Sip Stack", ex);
		}
		catch (InvalidArgumentException ex) {
		    //Should never happen
		    console.error("The application is corrupt");
		    Console.showError("The application is corrupt!");
		    throw new CommunicationsException("The application is corrupt", ex);
		}
    	return cSeqHeader;
    }
    
    static public ToHeader headerFromAddress(Address fromAddress, SipManager sipManCallback, Console console) throws CommunicationsException
	{
    	ToHeader toHeader;
    	try {
	        toHeader = sipManCallback.headerFactory.createToHeader(fromAddress, null);
	    }
	    catch (ParseException ex) {
	        console.error("Could not create a To header for address:"
	                      + fromAddress,
	                      ex);
	        //throw was missing - reported by Eero Vaarnas
	        throw new CommunicationsException("Could not create a To header "
	                                    + "for address:"
	                                    + fromAddress,
	                                    ex);
	    }
    	return toHeader;
    }
}
