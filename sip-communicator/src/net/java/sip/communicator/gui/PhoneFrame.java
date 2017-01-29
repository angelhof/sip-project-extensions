/* ====================================================================
 * The Apache Software License, Version 1.1
 *
 * Copyright (c) 2000 The Apache Software Foundation.  All rights
 * reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in
 *    the documentation and/or other materials provided with the
 *    distribution.
 *
 * 3. The end-user documentation included with the redistribution,
 *    if any, must include the following acknowledgment:
 *       "This product includes software developed by the
 *        Apache Software Foundation (http://www.apache.org/)."
 *    Alternately, this acknowledgment may appear in the software itself,
 *    if and wherever such third-party acknowledgments normally appear.
 *
 * 4. The names "Apache" and "Apache Software Foundation" must
 *    not be used to endorse or promote products derived from this
 *    software without prior written permission. For written
 *    permission, please contact apache@apache.org.
 *
 * 5. Products derived from this software may not be called "Apache",
 *    nor may "Apache" appear in their name, without prior written
 *    permission of the Apache Software Foundation.
 *
 * THIS SOFTWARE IS PROVIDED ``AS IS'' AND ANY EXPRESSED OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED.  IN NO EVENT SHALL THE APACHE SOFTWARE FOUNDATION OR
 * ITS CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF
 * USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT
 * OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
 * SUCH DAMAGE.
 * ====================================================================
 *
 * This software consists of voluntary contributions made by many
 * individuals on behalf of the Apache Software Foundation.  For more
 * information on the Apache Software Foundation, please see
 * <http://www.apache.org/>.
 *
 * Portions of this software are based upon public domain software
 * originally written at the National Center for Supercomputing Applications,
 * University of Illinois, Urbana-Champaign.
 */
package net.java.sip.communicator.gui;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.border.*;

/**
 * <p>Title: SIP COMMUNICATOR</p>
 * <p>Description:JAIN-SIP Audio/Video phone application</p>
 * <p>Copyright: Copyright (c) 2003</p>
 * <p>Organisation: LSIIT laboratory (http://lsiit.u-strasbg.fr) </p>
 * <p>Network Research Team (http://www-r2.u-strasbg.fr))</p>
 * <p>Louis Pasteur University - Strasbourg - France</p>
 * @author Emil Ivov (http://www.emcho.com)
 * @version 1.1
 *
 */
public class PhoneFrame
    extends JFrame
{
    BorderLayout borderLayout1 = new BorderLayout();
    JSplitPane splitPane = new JSplitPane();
    TitledBorder titledBorder1;
    Border border1;
//    BorderLayout borderLayout2 = new BorderLayout();
    Border border2;
    Border border3;
    Border border4;
    Border border5;
    JPanel controlPanel = new JPanel();
    JScrollPane participantsScroll = new JScrollPane();
    JPanel videoPane = new JPanel();
    BorderLayout borderLayout2 = new BorderLayout();
    JPanel callControlButtons = new JPanel();
    JButton answerButton = new JButton();
    JButton hangupButton = new JButton();
    GridLayout gridLayout1 = new GridLayout();
    Border border6;
    Border border7;
    Border border71;
    Border border8;
    JTable participantsTable = new JTable(30, 3);
    JMenuBar jMenuBar1 = new net.java.sip.communicator.gui.MenuBar();
    Border border9;
    JPanel statusPanel = new JPanel();
    JLabel registrationLabel = new JLabel();
    BorderLayout borderLayout5 = new BorderLayout();
    Border border10;
    JLabel registrationAddressLabel = new JLabel();
    Border border11;
    Border border12;
    Border border13;
    Border border14;
    BoxLayout boxLayout21 = new BoxLayout(videoPane, BoxLayout.Y_AXIS);
    Border border15;
    public String frwed = "None yet";

    private GuiManager guiManCallback = null;
    BorderLayout borderLayout4 = new BorderLayout();
    BorderLayout borderLayout41 = new BorderLayout();
    
    /**
     * Dial GUI 
     */
    JPanel dialPanel = new JPanel();
    JButton dialButton = new JButton();
    JComboBox contactBox = new JComboBox();
    
    /**
     * Call Forward GUI 
     */
    JPanel forwardPanel = new JPanel();
    JButton forwardButton = new JButton();
    JButton disableForwardButton = new JButton();
    JComboBox forwardBox = new JComboBox();
    
    //BorderLayout borderLayoutDialForward = new BorderLayout();
    //JPanel dialAndForwardPanel = new JPanel();
    
    /**
     * Block & Unblock GUI
     */
    public JPanel blockPanel = new JPanel();
    JButton blockButton = new JButton();
    
    String[] blockedpeople = {"Blocked People"};
    public JComboBox unblockButton = new JComboBox(blockedpeople);
    
    JPanel forwardl = new JPanel();
    public JLabel frwl = new JLabel();
    Font myFont = new Font("Serif", Font.ITALIC | Font.BOLD, 12);
    
    BorderLayout borderLayoutDialForwardBlock = new BorderLayout();
    JPanel dialForwardBlockPanel = new JPanel();
    
    public PhoneFrame(GuiManager guiManCallback) //throws HeadlessException
    {
        try {

            this.guiManCallback = guiManCallback;

            jbInit();
            Toolkit toolkit = Toolkit.getDefaultToolkit();
            pack();
            int x = (toolkit.getScreenSize().width - getWidth()) / 2;
            int y = (toolkit.getScreenSize().height - getHeight()) / 2;
            setLocation(x, y);
            participantsTable.sizeColumnsToFit(0);
            participantsTable.setAutoResizeMode(JTable.AUTO_RESIZE_SUBSEQUENT_COLUMNS);
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    private void setupDialAndForwardPanel() throws Exception{
    	borderLayoutDialForwardBlock.setHgap(10);
    	dialForwardBlockPanel.setLayout(borderLayoutDialForwardBlock);
    	dialForwardBlockPanel.setLayout(borderLayoutDialForwardBlock);
    	dialForwardBlockPanel.setBorder(border8);
    	dialForwardBlockPanel.setBorder(border8);
        
        borderLayout4.setHgap(10);
        dialPanel.setLayout(borderLayout4);
        dialPanel.setBorder(border7);
        dialButton.setEnabled(false);
        dialButton.setMnemonic('D');
        dialButton.setText("Dial");
        
        contactBox.setBorder(null);
        contactBox.setDebugGraphicsOptions(0);
        contactBox.setActionMap(null);
        contactBox.setEditable(true);
        
        borderLayout41.setHgap(10);
        forwardPanel.setLayout(borderLayout41);
        forwardPanel.setBorder(border71);
        
        borderLayout41.setHgap(10);
        forwardl.setLayout(borderLayout41);
        forwardl.setBorder(border71);
        
        forwardButton.setEnabled(false);
        forwardButton.setMnemonic('F');
        forwardButton.setText("Forward");
        disableForwardButton.setEnabled(false);
        disableForwardButton.setMnemonic('D');
        disableForwardButton.setText("Disable Forward");
        
        
        frwl.setText("Forward to: "+frwed);
        frwl.setFont(myFont);
        
        blockButton.setEnabled(false);
        blockButton.setMnemonic('B');
        blockButton.setText("Block");
        unblockButton.setEnabled(false);
        //unblockButton.setRenderer(new MyComboBoxRenderer("Unblock"));
        unblockButton.setSelectedIndex(0);
        //unblockButton.addIt
        
        this.getContentPane().add(dialForwardBlockPanel, BorderLayout.NORTH);
        dialForwardBlockPanel.add(dialPanel, BorderLayout.NORTH);
        
        dialForwardBlockPanel.add(forwardPanel, BorderLayout.CENTER);
        dialForwardBlockPanel.add(blockPanel, BorderLayout.SOUTH);
        
        dialPanel.add(dialButton, BorderLayout.EAST);
        dialPanel.add(contactBox, BorderLayout.CENTER); 
        
        forwardPanel.add(forwardButton, BorderLayout.EAST);
        forwardPanel.add(disableForwardButton, BorderLayout.WEST);
        forwardPanel.add(frwl, BorderLayout.SOUTH);
        
        blockPanel.add(blockButton, BorderLayout.EAST);
        blockPanel.add(unblockButton, BorderLayout.WEST);
        
        
    }

    private void jbInit() throws Exception
    {
        titledBorder1 = new TitledBorder("");
        border1 = BorderFactory.createCompoundBorder(BorderFactory.
            createBevelBorder(BevelBorder.LOWERED),//, Color.white, Color.white,
                              //new Color(109, 109, 110), new Color(156, 156, 158)),
            BorderFactory.createEmptyBorder(5, 5, 5, 5));
        border2 = BorderFactory.createCompoundBorder(BorderFactory.
            createBevelBorder(BevelBorder.LOWERED), BorderFactory.createEmptyBorder(5, 5, 5, 5));
        border3 = BorderFactory.createCompoundBorder(BorderFactory.
                                    createBevelBorder(BevelBorder.LOWERED),
                                    BorderFactory.createEmptyBorder(5, 5, 5, 5));
        border4 = BorderFactory.createEmptyBorder(5, 5, 5, 5);
        border5 = BorderFactory.createEmptyBorder(5, 5, 5, 5);
        border6 = BorderFactory.createEmptyBorder(5, 5, 5, 5);
        border7 = BorderFactory.createEmptyBorder(5, 5, 5, 5);
        border71 = BorderFactory.createEmptyBorder(5, 5, 5, 5);
        border8 = BorderFactory.createEmptyBorder(5, 5, 5, 5);
        border9 = BorderFactory.createEmptyBorder(10, 10, 10, 10);
        
        border14 = BorderFactory.createEmptyBorder(4, 0, 0, 0);
        this.getContentPane().setLayout(borderLayout1);
        splitPane.setOrientation(JSplitPane.VERTICAL_SPLIT);
        splitPane.setBorder(null);
        splitPane.setMinimumSize(new Dimension(260, 300));
        splitPane.setPreferredSize(new Dimension(260, 300));
//    splitPane.setLastDividerLocation(600);
//        splitPane.setLastDividerLocation(400);
        splitPane.setOneTouchExpandable(true);
        this.setJMenuBar(jMenuBar1);
        this.setResizable(true);
        this.setState(Frame.NORMAL);
        if(GuiManager.isThisSipphoneAnywhere)
            this.setTitle("SIPphone Anywhere");
        else
            this.setTitle("SIP COMMUNICATOR");
        participantsScroll.setAutoscrolls(false);
        participantsScroll.setBorder(border5);
        videoPane.setLayout(boxLayout21);
        controlPanel.setLayout(borderLayout2);
        answerButton.setEnabled(false);
        answerButton.setMnemonic('A');
        answerButton.setText("Answer");
        hangupButton.setEnabled(false);
        hangupButton.setMnemonic('H');
        hangupButton.setText("Hangup");
        callControlButtons.setLayout(gridLayout1);
        gridLayout1.setHgap(10);
        gridLayout1.setVgap(10);
        callControlButtons.setBorder(border6);
        borderLayout2.setHgap(10);
        borderLayout2.setVgap(0);
        videoPane.setBorder(border8);
//        videoPane.setPreferredSize(new Dimension(200, 10));
        registrationLabel.setText("Not Registered");
        statusPanel.setLayout(borderLayout5);
//        participantsTable.setMinimumSize(new Dimension(45, 300));
        
        
        
        this.getContentPane().add(splitPane, BorderLayout.CENTER);
        splitPane.add(controlPanel, JSplitPane.BOTTOM);
        splitPane.add(videoPane, JSplitPane.TOP);
        this.getContentPane().add(statusPanel, BorderLayout.SOUTH);

        if(GuiManager.isThisSipphoneAnywhere)
        {
            JPanel spaButtons = new JPanel(new GridLayout(1, 2, 15, 15));
            spaButtons.setBorder(BorderFactory.createEmptyBorder(6,0,3,0));

            spaButtons.add(new JButton(guiManCallback.voiceMailAction));
            spaButtons.add(new JButton(guiManCallback.mySipphoneAction));

            this.dialPanel.add(spaButtons, BorderLayout.SOUTH);

        }

        controlPanel.add(callControlButtons, BorderLayout.SOUTH);
        callControlButtons.add(answerButton, null);
        callControlButtons.add(hangupButton, null);
        controlPanel.add(participantsScroll,  BorderLayout.CENTER);
        participantsScroll.setViewportView(participantsTable);
        statusPanel.add(registrationLabel, BorderLayout.WEST);
        statusPanel.add(registrationAddressLabel, BorderLayout.CENTER);
        
        setupDialAndForwardPanel(); 
        
//        splitPane.setDividerLocation(200);
    }

    //exit is handled by SipCommunicator.shutDown()
    //report by Stacy Clark sipphone.com
//    protected void processWindowEvent(WindowEvent evt)
//    {
//        super.processWindowEvent(evt);
//        if (evt.getID() == WindowEvent.WINDOW_CLOSING) {
//            System.exit(0);
//        }
//    }
    

}
