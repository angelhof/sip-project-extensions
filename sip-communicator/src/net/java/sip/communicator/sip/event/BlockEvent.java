package net.java.sip.communicator.sip.event;

import java.util.EventObject;

public class BlockEvent
	extends EventObject{

	public BlockEvent(String blockAddress){
		
		
		super(blockAddress);
		
	}

	public String getReason(){
		
		return (String) getSource();
	}
}