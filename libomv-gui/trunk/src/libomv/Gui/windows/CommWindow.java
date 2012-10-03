/**
 * Copyright (c) 2009-2011, Frederick Martian
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * 
 * - Redistributions of source code must retain the above copyright notice, this
 *   list of conditions and the following disclaimer.
 * - Redistributions in binary form must reproduce the above copyright notice,
 *   this list of conditions and the following disclaimer in the
 *   documentation and/or other materials provided with the distribution.
 * - Neither the name of the libomv-java project nor the names of its
 *   contributors may be used to endorse or promote products derived from
 *   this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */
package libomv.Gui.windows;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.ContainerEvent;
import java.awt.event.ContainerListener;
import java.util.HashMap;

import javax.swing.JFrame;
import javax.swing.JTabbedPane;
import javax.swing.UIManager;
import javax.swing.border.LineBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import libomv.AgentManager.ChatSourceType;
import libomv.AgentManager.ChatType;
import libomv.AgentManager.InstantMessage;
import libomv.AgentManager.InstantMessageOnline;
import libomv.Gui.channels.AbstractChannel;
import libomv.Gui.channels.GroupChannel;
import libomv.Gui.channels.PrivateChannel;
import libomv.Gui.channels.LocalChannel;
import libomv.Gui.components.ButtonTabPane;
import libomv.Gui.components.OnlinePanel;
import libomv.types.UUID;

public class CommWindow extends JFrame
{
	private static final long serialVersionUID = 1L;
	
	public static final String cmdChat = "chat";

	private JTabbedPane jTpComm;
	private JTabbedPane jTpContacts;
	
	private MainControl _Main;
	private LocalChannel localChat;
	private HashMap<UUID, AbstractChannel> channels;
	
	public CommWindow(MainControl main)
	{
		super();
		
		_Main = main;
		
		setTitle("Communication");
		
		channels = new HashMap<UUID, AbstractChannel>();
		
		// Choose a sensible minimum size.
		setPreferredSize(new Dimension(360, 440));
		getContentPane().setLayout(new BorderLayout(0, 0));
		getContentPane().add(getJTpComm());

		//Display the window.
        pack();
        setVisible(true);
	}
	
	public boolean existsChannel(UUID uuid)
	{
		return channels.containsKey(uuid);
	}
	
	public void setFocus(String focus, UUID uuid)
	{
		if (focus.equals(cmdChat))
		{
			int index = 1;
			if (uuid != null && !uuid.equals(UUID.Zero))
				index = getJTpComm().indexOfComponent(channels.get(uuid));
			getJTpComm().setSelectedIndex(index);
		}
		else
		{
			getJTpComm().setSelectedIndex(0);
			if (focus.equals(OnlinePanel.cmdFriends))
			{
				getJTpContacts().setSelectedIndex(0);
			}
			else if (focus.equals(OnlinePanel.cmdGroups))
			{
				getJTpContacts().setSelectedIndex(1);
			}
		}
	}
	
	public void printAlertMessage(String alertMessage, String style)
	{
        if (alertMessage.toLowerCase().contains("autopilot canceled"))
        	return; //workaround the stupid autopilot alerts

        AbstractChannel channel = getLocalChannel();
        channel.receiveMessage(null, null, "Alert message", alertMessage, style != null ? style : AbstractChannel.STYLE_SYSTEM);
		highlightChannel(channel, true);
	}
	
	public void printChatMessage(ChatSourceType sourceType, UUID sourceID, String fromName, String message, ChatType type)
	{
        
        StringBuilder localMessage = new StringBuilder();
        boolean isEmote = message.toLowerCase().startsWith("/me ");

        if (!isEmote)
        {
        	switch (type)
        	{
				case Shout:
					localMessage.append(" shouts");
					break;
				case Whisper:
					localMessage.append(" whispers");
					break;
        	}
        	localMessage.append(": ");
            if (sourceType == ChatSourceType.Agent && !message.startsWith("/") && _Main.getGridClient().RLV.restrictionActive("recvchat", sourceID.toString()))
            	localMessage.append("...");
            else
            	localMessage.append(message);
		}
        else
        {
            if (sourceType == ChatSourceType.Agent && _Main.getGridClient().RLV.restrictionActive("recvemote", sourceID.toString()))
            	localMessage.append(" ...");
            else
            	localMessage.append(message.substring(3));       	
        }

        String style = null;
        switch (sourceType)
        {
            case Agent:
                style = (fromName.endsWith("Linden") ? AbstractChannel.STYLE_SYSTEM : AbstractChannel.STYLE_REGULAR);
                break;

            case Object:
                if (type == ChatType.OwnerSay)
                {
                    style = AbstractChannel.STYLE_OBJECT;
                }
                else
                {
                    style = AbstractChannel.STYLE_OBJECT;
                }
                break;
        }
        AbstractChannel channel = getLocalChannel();
        channel.receiveMessage(null, sourceID, fromName, localMessage.toString(), style);		
		highlightChannel(channel, true);
	}
	
	public void printInstantMessage(InstantMessage message)
	{
		AbstractChannel channel = channels.get(message.FromAgentID);
		if (channel == null && (message.Message == null || message.Message.isEmpty()))
		{
			// if the channel doesn't exist yet and we don't have a real message to report, return now
			return;
		}
		
		if (message.GroupIM)
		{
			if (channel == null)
			{
				channel = new GroupChannel(_Main, message.FromAgentName, message.FromAgentID, message.IMSessionID);
				addChannel(channel);
			}
		}
		else
		{
			if (channel == null)
			{
				channel = new PrivateChannel(_Main, message.FromAgentName, message.FromAgentID, message.IMSessionID);
				addChannel(channel);
			}
		}
		String style = null;
		if (message.Offline == InstantMessageOnline.Offline)
			style = AbstractChannel.STYLE_OFFLINE; 
		channel.receiveMessage(message.Timestamp, message.FromAgentID, message.FromAgentName, message.Message, style);
		highlightChannel(channel, true);
	}
	
	private LocalChannel getLocalChannel()
	{
		if (localChat == null)
		{
			localChat = new LocalChannel(_Main);
		}
		return localChat;
	}
	
	private JTabbedPane getJTpComm()
	{
		if (jTpComm == null)
		{
			jTpComm = new JTabbedPane(JTabbedPane.BOTTOM);
			jTpComm.setTabLayoutPolicy(JTabbedPane.SCROLL_TAB_LAYOUT);
			jTpComm.setBorder(new LineBorder(new Color(0, 0, 0)));

			jTpComm.add("Contacts", getJTpContacts());
			jTpComm.add("Local Chat", getLocalChannel());
			// Install container listener so we can detect tab panes removed through the tab close button
			jTpComm.addContainerListener(new ContainerListener()
			{
				@Override
				public void componentAdded(ContainerEvent e)
				{
					// Nothing to do now
				}

				@Override
				public void componentRemoved(ContainerEvent e)
				{
					Component comp = e.getComponent();
					if (comp instanceof AbstractChannel)
					{
						channels.remove(((AbstractChannel)comp).getUUID());
					}
				}
				
			});
			jTpComm.addChangeListener(new ChangeListener()
			{
				@Override
				public void stateChanged(ChangeEvent e)
				{
					Component comp = getJTpComm().getSelectedComponent();
					if (comp instanceof AbstractChannel)
					{
						highlightChannel((AbstractChannel)comp, false);
					}
				}
			});
		}
		return jTpComm;
	}
	
    private JTabbedPane getJTpContacts()
	{
		if (jTpContacts == null)
		{
			jTpContacts = new JTabbedPane(JTabbedPane.TOP);
			jTpContacts.setTabLayoutPolicy(JTabbedPane.SCROLL_TAB_LAYOUT);
			jTpContacts.setBorder(new LineBorder(new Color(0, 0, 0)));

			jTpContacts.add("Friends", _Main.getFriendList());
			jTpContacts.add("Groups", _Main.getGroupList());
		}
		return jTpContacts;
	}

	public boolean addChannel(AbstractChannel channel)
	{
		if (channels.put(channel.getUUID(), channel) == null)
		{
			getJTpComm().add(channel.getName(), channel);
			getJTpComm().setTabComponentAt(getJTpComm().indexOfComponent(channel), new ButtonTabPane(getJTpComm()));
			getJTpComm().setTabLayoutPolicy(JTabbedPane.SCROLL_TAB_LAYOUT);
			return true;
		}
		return false;
	}
	
	public void removeChannel(AbstractChannel channel)
	{
		getJTpComm().remove(channel);
	}
	
	public void highlightChannel(AbstractChannel channel, boolean highlight)
	{
		getJTpComm().getTabComponentAt(getJTpComm().indexOfComponent(channel)).setBackground(highlight ? Color.orange : UIManager.getColor("TabbedPane.background"));
	}
}
