/*
 * RegistrationTable.java
 *
 * Created on October 10, 2002, 11:19 PM
 */

package gov.nist.sip.proxy.registrar;

import javax.sip.*;
import javax.sip.message.*; 
import javax.sip.header.*;
import javax.sip.address.*;
import java.util.*;
//ifndef SIMULATION
//
import java.rmi.*;
import java.rmi.server.*;
//endif
//
import gov.nist.sip.proxy.*;

//ifdef SIMULATION
/*
import sim.java.util.*;
//endif
*/

/**
 *
 * @author  olivier
 * @version 1.0
 */
public class RegistrationsTable{
    
    protected Registrar registrar;
    protected Hashtable registrations;
    protected Hashtable expiresTaskTable;
    
    /** Creates new RegistrationTable */
    public RegistrationsTable(Registrar registrar) {
        this.registrar=registrar;
        registrations=new Hashtable();
        expiresTaskTable=new Hashtable();
    }
    
    public Hashtable getRegistrations() {
        return registrations;
    }
    
    public Hashtable getExpiresTaskTable() {
        return expiresTaskTable;
    }
    
    /*******************************************************************/
    /*************************** RMI REGISTRY    ***********************/
    
    public synchronized String getRegistryXMLTags() 
//ifndef SIMULATION
//
throws RemoteException 
//endif
//
  {
        StringBuffer retval = new StringBuffer("<REGISTRATIONS>");
        Collection values = registrations.values();
        Iterator it = values.iterator();
        while (it.hasNext()) {
            Registration registration= (Registration) it.next();
            retval.append(registration.getXMLTags());
        }
        retval.append("</REGISTRATIONS>");
        return retval.toString();
    }
 
    public synchronized Vector getRegistryBindings()
//ifndef SIMULATION
//
    throws RemoteException 
//endif
//
{
        Vector retval = new Vector();
        Collection values = registrations.values();
        Iterator it = values.iterator();
        while (it.hasNext()) {
            Registration registration= (Registration) it.next();
            ExportedBinding be = registration.exportBinding();
	    System.out.println("adding a binding " + be);
            if (be!=null)
                retval.add(be);
            
        }

        return retval;
    }
    
    public synchronized int getRegistrySize() 
//ifndef SIMULATION
//
throws RemoteException 
//endif
//
{
        Vector retval = new Vector();
        Collection values = registrations.values();
        return values.size();
    }
    
    
    /*************************************************************************/
    /*************************************************************************/
    
    
    
    public synchronized boolean hasRegistration(String key) {
        boolean res=registrations.containsKey(key.toLowerCase());
        if (res)
            ProxyDebug.println
            ("RegistrationsTable, hasRegistration(), Checking registration for \""
            +key.toLowerCase()+"\" : registered");
        else  {
            ProxyDebug.println
            ("RegistrationsTable, hasRegistration(), Checking registration for \""
            +key.toLowerCase()+"\" : not registered");
        }
        return  res;
    }
    
    
    protected void addRegistration(String key,Request request) throws Exception{
        Vector contacts=Registrar.getContactHeaders(request);
        
        int expiresTimeHeader=-1;
        
        Registration registration=new Registration();
        registration.key=key;
        
        ExpiresHeader expiresHeader=
	(ExpiresHeader)request.getHeader(ExpiresHeader.NAME); 
        if (expiresHeader!=null) {
            expiresTimeHeader=expiresHeader.getExpires();
            if (expiresTimeHeader > registrar.EXPIRES_TIME_MAX ||
                expiresTimeHeader < registrar.EXPIRES_TIME_MIN )
                expiresTimeHeader=registrar.EXPIRES_TIME_MAX;
        }
        else expiresTimeHeader=registrar.EXPIRES_TIME_MAX;
        
        for( int i=0; i<contacts.size();i++) {
            ContactHeader contactHeader=(ContactHeader)contacts.elementAt(i);
           
            if (contactHeader.getExpires()==-1 ) {
                contactHeader.setExpires(expiresTimeHeader);
            }
        
            registration.addContactHeader(contactHeader);
            startTimer(key,contactHeader.getExpires(),contactHeader);
        }
        
        ToHeader toHeader=(ToHeader)request.getHeader(ToHeader.NAME);
        Address toAddress=toHeader.getAddress();
        String displayName=toAddress.getDisplayName();
        if (displayName !=null) registration.setDisplayName(displayName);
	// Store the to and from headers for binding to the responder.
	registration.toHeader = toHeader;

	FromHeader fromHeader = (FromHeader)request.getHeader(FromHeader.NAME);
	registration.fromHeader = fromHeader;
        
	
	//set password
	String content = new String( request.getRawContent());
	String pass = content;
	try{
		pass = content.split("Password:")[1];
		pass = pass.replace("\n","");
		registration.setPassword(pass);
	}
	catch(Exception e){
		e.printStackTrace();
		 ProxyDebug.println("Add Registration Exception");
	}
        
		

		ProxyDebug.println("Registration Request: " + request.toString());
        registrations.put(key,registration);
        ProxyDebug.println
	("RegistrationsTable, addRegistration(), registration "+
        " added for the key: "+key);
   
        printRegistrations();
        updateGUI(registration,false);
    }
    

    protected void addRegistration(Registration registration) throws Exception{
        Vector contacts=registration.getContactsList();
	// ok to have empty contact list. This just means that the
	// registration is known to the registrar but contact info 
	// is not available.
        if (contacts==null ) {
            throw new Exception
		("contact list is empty, registration not added!");
        }
        
        String key=registration.getKey();
        if (key==null) 
		throw new Exception("key is null, registration not added!");
       
        for( int i=0; i<contacts.size();i++) {
            ContactHeader contactHeader=(ContactHeader)contacts.elementAt(i);
            if (contactHeader.getExpires()==-1 ) {
                contactHeader.setExpires(registrar.EXPIRES_TIME_MAX);
            }
           
            startTimer(key,contactHeader.getExpires(),contactHeader);
            
        }
        
        registrations.put(key,registration);
        ProxyDebug.println
	("RegistrationsTable, addRegistration(), registration "+
        " added for the key: "+key);
        
        //initialize forward and block values 
        //registration.setBlockedUsersList(null);
        //registration.setForwardToUser(null);
       
        
        
        printRegistrations();
        
        updateGUI(registration,false);
    }
    
    
    public synchronized void removeRegistration(String key) {
        ProxyDebug.println("RegistrationsTable, removeRegistration(), "+
        " registration removed"+
        " for the key: "+key);
        Registration registration=(Registration)registrations.get(key);
        updateGUI(registration,true);
        registrations.remove(key);
        printRegistrations();
        //updateGUI(registration,true);
    }
    
     public void removeContact(String key,ContactHeader contactHeader) {
        ProxyDebug.println("RegistrationsTable, removeContact(), "+
        " contact removed for the key: "+key);
        Registration registration=(Registration)registrations.get(key);
        if (registration!=null) {
            registration.removeContactHeader(contactHeader);
            printRegistrations();
            if ( !registration.hasContacts()) {
                ProxyDebug.println("RegistrationsTable, removeContact(), the registration: "+
                key+
                " does not contain any contacts, we remove it");
                removeRegistration(key);
            }
        }
    }
     
    //Update REgistration does not update all values but ONLY CONTACT LIST!!!!!!!!
    public void updateRegistration(String key,Request request) throws Exception {
        ProxyDebug.println("RegistrationsTable, updateRegistration(), registration updated"+
        " for the key: "+key);
        
        Vector contacts=Registrar.getContactHeaders(request);
        Registration registration=(Registration)registrations.get(key);
        
        int expiresTime=registrar.EXPIRES_TIME_MAX;
        for( int i=0; i<contacts.size();i++) {
            ContactHeader contactHeader=(ContactHeader)contacts.elementAt(i);
           
            if (contactHeader.getExpires()!=-1 ) {
                expiresTime=contactHeader.getExpires();
            }
            else {
                ExpiresHeader expiresHeader=(ExpiresHeader)request.getHeader(ExpiresHeader.NAME);
                if (expiresHeader!=null) {
                    expiresTime=expiresHeader.getExpires();
                }
            }
            if (expiresTime==0) {
                removeContact(key,contactHeader);
            }
            else {
                if (expiresTime > registrar.EXPIRES_TIME_MAX ||
                expiresTime < registrar.EXPIRES_TIME_MIN)
                    expiresTime=registrar.EXPIRES_TIME_MAX;
                contactHeader.setExpires(expiresTime);
                
                if (registration.hasContactHeader(contactHeader))
                    registration.updateContactHeader(contactHeader);
                else
                    registration.addContactHeader(contactHeader);
                
                
                startTimer(key,expiresTime,contactHeader);
                expiresTime=registrar.EXPIRES_TIME_MAX;
                
            }
           
        }

        printRegistrations();
       
    }
    
    
    public Vector getContactHeaders(String key) {
        Registration registration=(Registration)registrations.get(key);
        if (registration==null) return null;
        else return registration.getContactsList();
    }
    
    
    
    public void startTimer
	(String key,int  expiresTime,ContactHeader contactHeader) {
        // we kill the precedent timer related to this key if there is one:
        Address address=contactHeader.getAddress();
        javax.sip.address.URI cleanedUri=Registrar.getCleanUri(address.getURI() );
        String contactURI=cleanedUri.toString();

//ifdef SIMULATION
/*
	SimTimer oldTimer;
//else
*/
	Timer oldTimer;
//endif
//
        
        
//ifndef SIMULATION
//
	synchronized(expiresTaskTable) {
           oldTimer=(Timer)expiresTaskTable.get(contactURI);
	}
//else
/*
	synchronized(expiresTaskTable) {
	   oldTimer = (SimTimer)expiresTaskTable.get(contactURI);
	}
//endif
*/
        if (oldTimer !=null) {
            ProxyDebug.println
		("RegistrationsTable, startTimer(), An old timer has "+
            " been stopped for the contact: "+contactURI);
            oldTimer.cancel();
        }
        
        // Let's start a timer for this contact...
        ExpiresTask expiresTask=new ExpiresTask(key,contactHeader,this);
//ifndef SIMULATION
//
        Timer timer=new Timer();
        timer.schedule(expiresTask,expiresTime*1000);
//else
/*
	SimTimer timer = new SimTimer();
        timer.schedule(expiresTask,expiresTime*1000);
//endif
*/
	synchronized (expiresTaskTable) {
           expiresTaskTable.put(contactURI,timer);
	}
        ProxyDebug.println("RegistrationsTable, startTimer(), timer started "+
        " for the contact: "+contactURI+" , expiresTime:"+expiresTime);
    }
    

    protected void printRegistrations() {
        ProxyDebug.println("*********  Registration record *****************");
        ProxyDebug.println();
        for (Enumeration e = registrations.keys() ; e.hasMoreElements() ;) {
            String keyTable=(String)e.nextElement();
            Registration registration=(Registration)registrations.get(keyTable);
            ProxyDebug.println("registered user: \""+keyTable+"\"");
            ProxyDebug.println("Forward To User: \""+registration.getForwardToUser()+"\"");
            
            
            //ProxyDebug.println("Blocked Users List: \""+registration.getBlockedUsersList().toString()+"\"");
            Vector BlockedList = registration.getBlockedUsersList();
            ProxyDebug.println("Blocked Users List: ");
        	// check if this block is inside the list
            if (BlockedList != null){
            	Iterator itr = BlockedList.iterator();
            	while (itr.hasNext()){
            		String CurrentBlockedUser = itr.next().toString();
                    ProxyDebug.println("	Blocked User: "+CurrentBlockedUser);
            	}           
            }
        	
            ProxyDebug.println();
        }
        ProxyDebug.println("************************************************");
        ProxyDebug.println();
    }
    
    public String getXMLTags() {
	StringBuffer retval = new StringBuffer();

        retval.append("<?xml version='1.0' encoding='us-ascii'?> \n");
	retval.append("<REGISTRATIONS> \n");
        for (Enumeration e = registrations.keys() ; e.hasMoreElements() ;) {
            String keyTable=(String)e.nextElement();
            Registration registration=(Registration)registrations.get(keyTable);
            retval.append(registration.getXMLTags());
        }
        retval.append("</REGISTRATIONS> \n");
	return retval.toString();
    }
    
    public void updateGUI(Registration registration,boolean toRemove) {
        if (registrar.gui!=null) {
            registrar.gui.updateRegistration(registration,toRemove);
        }
        else {
              ProxyDebug.println("DEBUG, not gui to update");
        }
    }
    
    
    // Implement some functions in order to update the registration tables for Forward and Block Requests
    public boolean updateForwardRegistration(String key, String NewUserToForward){
    	ProxyDebug.println("RegistrationsTable, updateForwardRegistration(), registration updated"+
    	        " for the key: "+key);
    	
    	boolean result = true;
    	
    	String newDesignatedUserToForward = NewUserToForward;
        Registration registration=(Registration)registrations.get(key);
        
    	// TODO check if this Forward should be done (according to graph)
    	registration.setForwardToUser(newDesignatedUserToForward);

        printRegistrations();
        //see if GUI is mandatory to be updated
        return result;
    }
    
    public boolean insertToBlockedUsersListRegistration(String key, String NewBlockedUser){
    	
    	String newDesignatedBlockUser = NewBlockedUser;
        Registration registration=(Registration)registrations.get(key);   	
        return registration.insertToBlockedUsersListRegistration(NewBlockedUser);
    }
    
    
    public boolean deleteFromBlockedUsersListRegistration(String key, String NewBlockedUser){
    	ProxyDebug.println("RegistrationsTable, deleteFromBlockedUsersListRegistration(), "
    			+ "registration updated"+" for the key: "+key);
    	
    	String newDesignatedBlockUser = NewBlockedUser;
        Registration registration=(Registration)registrations.get(key);
        return registration.deleteFromBlockedUsersListRegistration(NewBlockedUser);
    }
    
    public boolean inBlockedUsersListRegistration(String key, String blockedUser){
    	Registration registration=(Registration)registrations.get(key);
        ProxyDebug.println("inBlockedUsersListRegistration registered user: "+key+"\"");    
        
        //ProxyDebug.println("Blocked Users List: \""+registration.getBlockedUsersList().toString()+"\"");
        Vector BlockedList = registration.getBlockedUsersList();
        
    	// check if this blocked is inside the list
        if (BlockedList != null){
        	Iterator itr = BlockedList.iterator();
        	while (itr.hasNext()){
        		String CurrentBlockedUser = itr.next().toString();
                ProxyDebug.println("	Blocked User: "+CurrentBlockedUser);
        		if (CurrentBlockedUser.equals(blockedUser)){
                    ProxyDebug.println("FOUND HIM!!  "+CurrentBlockedUser);
        			return true;
        		}
        	}           
        }
        return false;
    }
    
    
}
