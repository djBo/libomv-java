/**
 * Copyright (c) 2009-2011, Frederick Martian
 * All rights reserved.
 *
 * - Redistribution and use in source and binary forms, with or without
 *   modification, are permitted provided that the following conditions are met:
 *
 * - Redistributions of source code must retain the above copyright notice, this
 *   list of conditions and the following disclaimer.
 * - Neither the name of the openmetaverse.org nor the names
 *   of its contributors may be used to endorse or promote products derived from
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
package libomv.Gui.components.list;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.Insets;

import java.util.Comparator;
import java.util.Enumeration;

import javax.swing.DefaultListModel;
import javax.swing.ImageIcon;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JList;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.ListCellRenderer;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;

import libomv.FriendsManager.FriendInfo;
import libomv.FriendsManager.FriendNotificationCallbackArgs;
import libomv.FriendsManager.FriendRightsCallbackArgs;
import libomv.FriendsManager.FriendshipResponseCallbackArgs;
import libomv.FriendsManager.FriendshipTerminatedCallbackArgs;
import libomv.GridClient;
import libomv.Gui.components.list.SortedListModel.SortOrder;
import libomv.types.UUID;
import libomv.types.Vector3;
import libomv.utils.Callback;
import libomv.utils.Logger;
import libomv.utils.Logger.LogLevel;

// List to display the friends
public class FriendList extends JScrollPane
{
	private static final long serialVersionUID = 1L;

	private static ImageIcon offline = null;
	private static ImageIcon online = null;

	private GridClient _Client;

	private JList jLFriendsList;

	/**
	 * Constructs a list to display
	 */
	public FriendList(GridClient client)
	{
		super();
		this._Client = client;

		// install friend change event handlers
		_Client.Friends.OnFriendRights.add(new FriendRightsChanged());
		_Client.Friends.OnFriendNotification.add(new FriendNotification());
		_Client.Friends.OnFriendshipResponse.add(new FriendshipResponse());
		_Client.Friends.OnFriendshipTerminated.add(new FriendshipTerminated());

		// Choose a sensible minimum size.
		setPreferredSize(new Dimension(200, 0));
		// Add the friends list to the viewport.
		getViewport().add(getJFriendsList());
	}

	private final JList getJFriendsList()
	{
		if (jLFriendsList == null)
		{
			jLFriendsList = new JList(new SortedListModel(new DefaultListModel(), SortOrder.ASCENDING, FriendComparator));
			// install a mouse handler
			jLFriendsList.addMouseListener(new MouseAdapter()
			{
				@Override
				public void mouseClicked(MouseEvent e)
				{
					// Select the item first.
					jLFriendsList.setSelectedIndex(jLFriendsList.locationToIndex(e.getPoint()));

					// If an index is selected...
					if (jLFriendsList.getSelectedIndex() >= 0)
					{
						// If the left mouse button was pressed
						if (SwingUtilities.isLeftMouseButton(e))
						{
							// Look for a double click.
							if (e.getClickCount() >= 2)
							{
								// Get the associated agent.
								FriendInfo friend = (FriendInfo) jLFriendsList.getSelectedValue();
								// Only allow creation of a chat window if the avatar name is resolved.
								if (friend.getName() != null && !friend.getName().isEmpty())
								{
									// TODO: Create a private chat
								}
							}
						}
						// If the right mouse button was pressed...
						else if (SwingUtilities.isRightMouseButton(e))
						{
							FriendPopupMenu fpm = new FriendPopupMenu(_Client, (FriendInfo) (jLFriendsList.getSelectedValue()));
							fpm.show(jLFriendsList, e.getX(), e.getY());
						}
					}
				}
			});
			
			// Initialize the list with the values from the friends manager
			DefaultListModel model = (DefaultListModel) ((SortedListModel) jLFriendsList.getModel()).getUnsortedModel();
			model.copyInto(_Client.Friends.FriendList.values().toArray());
			// create Renderer and display
			jLFriendsList.setCellRenderer(new FriendListRow());
			// only allow single selections.
			jLFriendsList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		}
		return jLFriendsList;
	}

	private final static Comparator<Object> FriendComparator = new Comparator<Object>()
	{
		@Override
		public int compare(Object arg1, Object arg2)
		{
			if (arg1 instanceof FriendInfo && arg1 instanceof FriendInfo)
			{
				FriendInfo info1 = (FriendInfo) arg1;
				FriendInfo info2 = (FriendInfo) arg2;
				if (info1.getIsOnline() == (info2.getIsOnline()))
				{
					return info1.getName().compareToIgnoreCase(info2.getName());
				}
				if (info1.getIsOnline())
					return 1;
				return -1;
			}
			return arg1.equals(arg2) ? 0 : 1;
		}
	};

	private class FriendRightsChanged implements Callback<FriendRightsCallbackArgs>
	{
		@Override
		public void callback(FriendRightsCallbackArgs e)
		{
			changeFriend(e.getFriendInfo());
		}
	}

	private class FriendNotification implements Callback<FriendNotificationCallbackArgs>
	{
		@Override
		public void callback(FriendNotificationCallbackArgs e)
		{
			FriendInfo info = _Client.Friends.FriendList.get(e.getAgentID());
			if (info != null)
			{
				addFriend(info);
			}
		}
	}

	private class FriendshipResponse implements Callback<FriendshipResponseCallbackArgs>
	{
		@Override
		public void callback(FriendshipResponseCallbackArgs e)
		{
			FriendInfo info =  _Client.Friends.FriendList.get(e.getAgentID());
			if (info != null)
			{
				if (e.getAccepted())
				{
					addFriend(info);
				}
				else
				{
					removeFriend(info);
				}
			}
		}
	}

	private class FriendshipTerminated implements Callback<FriendshipTerminatedCallbackArgs>
	{
		@Override
		public void callback(FriendshipTerminatedCallbackArgs e)
		{
			FriendInfo info = findFriend(e.getOtherID());
			if (info != null)
			{
				removeFriend(info);
			}
		}
	}
	
	/**
	 * Find the entry based on the friends UUID
	 * 
	 * @param id The UUID of the friend
	 * @return returns the FriendInfo if found, null otherwise
	 */
	public FriendInfo findFriend(UUID id)
	{
		DefaultListModel model = (DefaultListModel) ((SortedListModel) getJFriendsList().getModel()).getUnsortedModel();
		for (Enumeration<?> e = model.elements(); e.hasMoreElements();)
		{
			FriendInfo info = (FriendInfo) e.nextElement();
			if (info.getID().equals(id))
			{
				return info;
			}
		}
		return null;
	}
	
	/**
	 * Add a friend to the list
	 * 
	 * @param info The friend info to add to the list
	 * @return true if te friend was added, false if it was replaced
	 */
	public boolean addFriend(FriendInfo info)
	{
		DefaultListModel model = (DefaultListModel) ((SortedListModel) getJFriendsList().getModel()).getUnsortedModel();
		int idx = model.indexOf(info);
		if (idx < 0)
			model.add(model.size(), info);
		else
			model.set(idx, info);
		return idx < 0;
	}

	/**
	 * Remove a friend from the list
	 * 
	 * @param info The friend info to remove from the list
	 * @return true if the friend info was successful removed, false if the friend could not be found, 
	 */
	public boolean removeFriend(FriendInfo info)
	{
		DefaultListModel model = (DefaultListModel) ((SortedListModel) getJFriendsList().getModel()).getUnsortedModel();
		int idx = model.indexOf(info);
		if (idx < 0)
			return false;
		model.remove(idx);
		return true;
	}
	
	/**
	 * Change friend info in the list
	 * 
	 * @param info The friend info to change
	 * @return true if the friend info was successfull changed, false otherwise
	 */
	public boolean changeFriend(FriendInfo info)
	{
		DefaultListModel model = (DefaultListModel) ((SortedListModel) getJFriendsList().getModel()).getUnsortedModel();
		int idx = model.indexOf(info);
		if (idx >= 0)
			model.set(idx, info);
		return idx >= 0;
	}

	private class FriendListRow extends JPanel implements ListCellRenderer
	{
		private static final long serialVersionUID = 1L;

		JLabel jlblName, jlblTyping, jlblVoice;
		JCheckBox jckCanSeeMe, jckCanMapMe, jckCanEditMe, jckCanSeeThem, jckCanMapThem, jckCanEditThem;

		public FriendListRow()
		{
			GridBagLayout gridBagLayout = new GridBagLayout();
			gridBagLayout.columnWidths = new int[] { 180, 16, 16, 0, 0, 0, 0, 0, 0, 0 };
			gridBagLayout.rowHeights = new int[] { 0, 0 };
			gridBagLayout.columnWeights = new double[] { 1.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, Double.MIN_VALUE };
			gridBagLayout.rowWeights = new double[] { 0.0, Double.MIN_VALUE };
			setLayout(gridBagLayout);

			jlblName = new JLabel(offline);
			GridBagConstraints gridBagConstraints = new GridBagConstraints();
			gridBagConstraints.anchor = GridBagConstraints.WEST;
			gridBagConstraints.insets = new Insets(0, 0, 0, 5);
			gridBagConstraints.gridx = 0;
			gridBagConstraints.gridy = 0;
			add(jlblName, gridBagConstraints);

			jlblTyping = new JLabel();
			gridBagConstraints = new GridBagConstraints();
			gridBagConstraints.insets = new Insets(0, 0, 0, 5);
			gridBagConstraints.gridx = 1;
			gridBagConstraints.gridy = 0;
			add(jlblTyping, gridBagConstraints);

			jlblVoice = new JLabel("");
			gridBagConstraints = new GridBagConstraints();
			gridBagConstraints.insets = new Insets(0, 0, 0, 5);
			gridBagConstraints.gridx = 2;
			gridBagConstraints.gridy = 0;
			add(jlblVoice, gridBagConstraints);

			jckCanSeeMe = new JCheckBox();
			gridBagConstraints = new GridBagConstraints();
			gridBagConstraints.insets = new Insets(0, 0, 0, 5);
			gridBagConstraints.gridx = 3;
			gridBagConstraints.gridy = 0;
			add(jckCanSeeMe, gridBagConstraints);

			jckCanMapMe = new JCheckBox();
			gridBagConstraints = new GridBagConstraints();
			gridBagConstraints.insets = new Insets(0, 0, 0, 5);
			gridBagConstraints.gridx = 4;
			gridBagConstraints.gridy = 0;
			add(jckCanMapMe, gridBagConstraints);

			jckCanEditMe = new JCheckBox();
			gridBagConstraints = new GridBagConstraints();
			gridBagConstraints.insets = new Insets(0, 0, 0, 5);
			gridBagConstraints.gridx = 5;
			gridBagConstraints.gridy = 0;
			add(jckCanEditMe, gridBagConstraints);

			jckCanSeeThem = new JCheckBox();
			gridBagConstraints = new GridBagConstraints();
			gridBagConstraints.insets = new Insets(0, 0, 0, 5);
			gridBagConstraints.gridx = 6;
			gridBagConstraints.gridy = 0;
			add(jckCanSeeThem, gridBagConstraints);

			jckCanMapThem = new JCheckBox();
			gridBagConstraints = new GridBagConstraints();
			gridBagConstraints.insets = new Insets(0, 0, 0, 5);
			gridBagConstraints.gridx = 7;
			gridBagConstraints.gridy = 0;
			add(jckCanMapThem, gridBagConstraints);

			jckCanEditThem = new JCheckBox();
			gridBagConstraints = new GridBagConstraints();
			gridBagConstraints.gridx = 8;
			gridBagConstraints.gridy = 0;
			add(jckCanEditThem, gridBagConstraints);
		}

		@Override
		public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected,
				boolean cellHasFocus)
		{
			FriendInfo info = (FriendInfo) value;

			String name = info.getName();
			if (name == null || name.isEmpty())
				name = "loading...";
			jlblName.setText(name);
			jlblName.setIcon(info.getIsOnline() ? online : offline);
			jckCanSeeMe.setSelected(info.getCanSeeMeOnline());
			jckCanMapMe.setSelected(info.getCanSeeMeOnMap());
			jckCanEditMe.setSelected(info.getCanModifyMyObjects());
			jckCanSeeThem.setSelected(info.getCanSeeThemOnline());
			jckCanMapThem.setSelected(info.getCanSeeThemOnMap());
			jckCanEditThem.setSelected(info.getCanModifyTheirObjects());

			Color background, foreground;
			if (isSelected)
			{
				background = list.getSelectionBackground();
				foreground = list.getSelectionForeground();
			}
			else
			{
				background = list.getBackground();
				foreground = list.getForeground();
			}

			jlblName.setBackground(background);
			jlblName.setForeground(foreground);
			jckCanSeeMe.setBackground(background);
			jckCanSeeMe.setForeground(foreground);
			jckCanMapMe.setBackground(background);
			jckCanMapMe.setForeground(foreground);
			jckCanEditMe.setBackground(background);
			jckCanEditMe.setForeground(foreground);
			jckCanSeeThem.setBackground(background);
			jckCanSeeThem.setForeground(foreground);
			jckCanMapThem.setBackground(background);
			jckCanMapThem.setForeground(foreground);

			setEnabled(list.isEnabled());
			setFont(list.getFont());
			return this;
		}
	}

	private class FriendPopupMenu extends JPopupMenu
	{
		private static final long serialVersionUID = 1L;
		// The friend associated with the menu
		private FriendInfo _Info;
		// The client to use to communicate with the grid
		private GridClient _Client;

		// The menu item used to send the agent a message
		private JMenuItem jmiSendMessage;
		// The menu item used to request the avatar's profile
		private JMenuItem jmiProfile;
		// The menu item used to send money to another avatar
		private JMenuItem jmiMoneyTransfer;
		// The menu item used to offer the agent teleportation
		private JMenuItem jmiOfferTeleport;
		// The menu item used to remove the agent as a friend
		private JMenuItem jmiRemoveAsFriend;
		// The menu item used to teleport to the agent
		private JMenuItem jmiTeleportTo;
		// The menu item used to autopilot to an agent
		private JMenuItem jmiAutopilotTo;

		/**
		 * Constructor
		 * 
		 * @param client
		 *            The client to use to communicate with the grid
		 * @param info
		 *            The friend to generate the menu for.
		 */
		public FriendPopupMenu(GridClient client, FriendInfo info)
		{
			super();
			this._Info = info;
			this._Client = client;

			// Send message
			add(getJmiSendMessage());
			// Add the profile menu item
			add(getJmiProfile());
			// Send money transfer
			add(getJmiMoneyTransfer());
			if (_Info.getIsOnline())
			{
				// Offer teleport.
				add(getJmiOfferTeleport());
				if (_Client.Network.getCurrentSim().getAvatarPositions().containsKey(_Info.getID()))
				{
					add(new JPopupMenu.Separator());
					// Allow teleporting to the agent
					add(getJmiTeleportTo());
					// Allow autopiloting to the agent
					add(getJmiAutopilotTo());
					add(new JPopupMenu.Separator());
				}

			}
			// Allow removing as a friend
			add(getJmiRemoveAsFriend());
		}

		/**
		 * Get the send message menu item
		 * 
		 * @return The "send message" menu item
		 */
		private JMenuItem getJmiSendMessage()
		{
			if (jmiSendMessage == null)
			{
				jmiSendMessage = new JMenuItem("Send message");
				// Add an ActionListener
				jmiSendMessage.addActionListener(new ActionListener()
				{
					/**
					 * Called when an action is performed
					 * 
					 * @param e
					 *            The ActionEvent
					 */
					@Override
					public void actionPerformed(ActionEvent e)
					{
						// TODO: Open a private chat with the friend
					}
				});
			}
			if (_Info.getName() == null || _Info.getName().isEmpty())
				jmiSendMessage.setEnabled(false);
			return jmiSendMessage;
		}

		/**
		 * Get the profile menu item
		 * 
		 * @return The "profile" menu item
		 */
		private JMenuItem getJmiProfile()
		{
			if (jmiProfile == null)
			{
				jmiProfile = new JMenuItem("Profile ..");
				// add an ActionListener
				jmiProfile.addActionListener(new ActionListener()
				{
					/**
					 * Called when an action is performed
					 * 
					 * @param e
					 *            The ActionEvent
					 */
					@Override
					public void actionPerformed(ActionEvent e)
					{
						// TODO: open avatar profile dialog
					}
				});
			}
			return jmiProfile;
		}

		/**
		 * Get the money transfer menu item
		 * 
		 * @return The "pay" menu item
		 */
		private JMenuItem getJmiMoneyTransfer()
		{
			if (jmiMoneyTransfer == null)
			{
				jmiMoneyTransfer = new JMenuItem("Pay ..");
				// add an ActionListener
				jmiMoneyTransfer.addActionListener(new ActionListener()
				{
					/**
					 * Called when an action is performed
					 * 
					 * @param e
					 *            The ActionEvent
					 */
					@Override
					public void actionPerformed(ActionEvent e)
					{
						// TODO: open a money transfer dialog
					}
				});
			}
			return jmiMoneyTransfer;
		}

		/**
		 * Get the teleport offer menu item
		 * 
		 * @return The teleport offer menu item.
		 */
		private JMenuItem getJmiOfferTeleport()
		{
			if (jmiOfferTeleport == null)
			{
				jmiOfferTeleport = new JMenuItem("Offer Teleport ..");
				// Add an action listener.
				jmiOfferTeleport.addActionListener(new ActionListener()
				{
					/**
					 * Called when an action is performed
					 * 
					 * @param e
					 *            The ActionEvent
					 */
					@Override
					public void actionPerformed(ActionEvent e)
					{

						// Offer teleportation.
						try
						{
							_Client.Self.SendTeleportLure(_Info.getID());
						}
						catch (Exception ex)
						{
							Logger.Log("SendTeleportLure failed", LogLevel.Error, _Client, ex);
						}
					}
				});
			}
			return jmiOfferTeleport;
		}

		/**
		 * Get the remove as friend menu item
		 * 
		 * @return The remove as friend menu item
		 */
		private JMenuItem getJmiRemoveAsFriend()
		{
			if (jmiRemoveAsFriend == null)
			{
				jmiRemoveAsFriend = new JMenuItem("Remove ..");
				// Add an action listener
				jmiRemoveAsFriend.addActionListener(new ActionListener()
				{
					/**
					 * Called when an action is performed
					 * 
					 * @param e
					 *            The ActionEvent
					 */
					@Override
					public void actionPerformed(ActionEvent e)
					{
						// Terminate the friendship
						try
						{
							_Client.Friends.TerminateFriendship(_Info.getID());
							removeFriend(_Info);
						}
						catch (Exception ex)
						{
							Logger.Log("TerminateFriendship failed", LogLevel.Error, _Client, ex);
						}
					}
				});
			}
			return jmiRemoveAsFriend;
		}

		/**
		 * Get the teleport to menu item
		 * 
		 * @return The teleport to menu item
		 */
		private JMenuItem getJmiTeleportTo()
		{
			if (jmiTeleportTo == null)
			{
				jmiTeleportTo = new JMenuItem("Teleport to");
				// Add an ActionListener
				jmiTeleportTo.addActionListener(new ActionListener()
				{
					/**
					 * Called when an action is performed
					 * 
					 * @param e
					 *            The ActionEvent
					 */
					@Override
					public void actionPerformed(ActionEvent e)
					{
						// Teleport
						try
						{
							Vector3 pos = _Client.Network.getCurrentSim().getAvatarPositions().get(_Info.getID());
							_Client.Self.Teleport(_Client.Network.getCurrentSim().Name, pos);
						}
						catch (Exception ex)
						{
							Logger.Log("Teleporting to " + _Info.getName() + " failed", LogLevel.Error, _Client, ex);
						}
					}
				});
			}
			return jmiTeleportTo;
		}

		/**
		 * Get the autopilot to menu item. If it has not been initialised, it is
		 * initialised upon first call.
		 * 
		 * @return The autopilot to menu item.
		 */
		private JMenuItem getJmiAutopilotTo()
		{
			if (jmiAutopilotTo == null)
			{
				jmiAutopilotTo = new JMenuItem("Autopilot to");
				// Add an ActionListener.
				jmiAutopilotTo.addActionListener(new ActionListener()
				{
					/**
					 * Called when an action is performed.
					 * 
					 * @param e
					 *            The ActionEvent.
					 */
					@Override
					public void actionPerformed(ActionEvent e)
					{
						// Autopilot
						try
						{
							Vector3 pos = _Client.Network.getCurrentSim().getAvatarPositions().get(_Info.getID());
							_Client.Self.AutoPilotLocal((int) pos.X, (int) pos.Y, pos.Y);
						}
						catch (Exception ex)
						{
							Logger.Log("Autopiloting to " + _Info.getName() + " failed", LogLevel.Error, _Client, ex);
						}
					}
				});
			}
			return jmiAutopilotTo;
		}
	}
}
