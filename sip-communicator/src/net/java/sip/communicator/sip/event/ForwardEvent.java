package net.java.sip.communicator.sip.event;

import java.util.EventObject;

public class ForwardEvent
	extends EventObject{

	public ForwardEvent(String blockAddress){
		
		
		super(blockAddress);
		
	}

	public String getReason(){
		
		return (String) getSource();
	}
}