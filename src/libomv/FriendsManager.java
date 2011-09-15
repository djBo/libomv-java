/**
 * Copyright (c) 2007-2008, openmetaverse.org
 * Portions Copyright (c) 2009-2011, Frederick Martian
 * All rights reserved.
 *
 * - Redistribution and use in source and binary forms, with or without 
 *   modification, are permitted provided that the following conditions are met:
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
package libomv;

import java.io.UnsupportedEncodingException;
import java.util.Hashtable;
import java.util.Vector;

import libomv.AgentManager.InstantMessageCallbackArgs;
import libomv.AgentManager.InstantMessageDialog;
import libomv.AgentManager.InstantMessageOnline;
import libomv.LoginManager.BuddyListEntry;
import libomv.LoginManager.LoginProgressCallbackArgs;
import libomv.LoginManager.LoginResponseCallbackArgs;
import libomv.LoginManager.LoginStatus;
import libomv.assets.AssetItem.AssetType;
import libomv.inventory.InventoryException;
import libomv.packets.AcceptFriendshipPacket;
import libomv.packets.ChangeUserRightsPacket;
import libomv.packets.DeclineFriendshipPacket;
import libomv.packets.FindAgentPacket;
import libomv.packets.GenericMessagePacket;
import libomv.packets.GrantUserRightsPacket;
import libomv.packets.OfflineNotificationPacket;
import libomv.packets.OnlineNotificationPacket;
import libomv.packets.Packet;
import libomv.packets.PacketType;
import libomv.packets.TerminateFriendshipPacket;
import libomv.packets.TrackAgentPacket;
import libomv.packets.UUIDNameReplyPacket;
import libomv.types.PacketCallback;
import libomv.types.UUID;
import libomv.types.Vector3;
import libomv.utils.BitFlags;
import libomv.utils.CallbackArgs;
import libomv.utils.CallbackHandler;
import libomv.utils.CallbackHandlerQueue;
import libomv.utils.Helpers;

/** This class is used to add and remove avatars from your friends list and to
 *  manage their permission. */
public class FriendsManager implements PacketCallback
{
	public class FriendRights extends BitFlags <Byte>
	{
		/** The avatar has no rights */
		public static final byte None = 0;
        /** The avatar can see the online status of the target avatar */
		public static final byte CanSeeOnline = 1;
        /** The avatar can see the location of the target avatar on the map */
		public static final byte CanSeeOnMap = 2;
        /** The avatar can modify the ojects of the target avatar */
		public static final byte CanModifyObjects = 4;
		
		private final String[] _names = new String[]{"None", "SeeOnline", "SeeOnMap", "ModifyObjects"};

		@Override
		public Byte getValue() {
			return getByte();
		}

		public FriendRights()
		{
			super(0x7);
		}
		
		public String toString()
		{
			byte value = getValue();
			if (value > 0)
			{
				String names = new String("{");
			    for (int i = 0; i < _names.length; i++)
			    {
			    	if (isSet(1 << value))
			    	{
			    		names.concat(_names[i] + ", ");
			    	}
			    }
			    return names.substring(0, names.length() - 2) + "}";
			}	
			return "{" + _names[0] + "}";
		}
		
		public FriendRights(long value) {
			this();
			setValue(value);
		}
    }

	/** This class holds information about an avatar in the friends list.  There are two ways 
	 *  to interface to this class.  The first is through the set of boolean properties.  This is the typical
	 *  way clients of this class will use it.  The second interface is through two bitflag properties,
	 *  TheirFriendsRights and MyFriendsRights
	 */
	public class FriendInfo
	{
	    private UUID ID;
	    private String name;
	    private boolean isOnline;
	    private FriendRights myRights;
	    private FriendRights theirRights;

	    /* System ID of the avatar */
	    public final UUID getUUID()
	    {
	        return ID;
	    }

	    /* full name of the avatar */
	    public final String getName()
	    {
	        return name;
	    }
	    public final void setName(String name)
	    {
	    	this.name = name;
	    }

	    /* True if the avatar is online */
	    public final boolean getIsOnline()
	    {
	        return isOnline;
	    }
	    public final void setIsOnline(boolean value)
	    {
	        isOnline = value;
	    }

	    /* True if the friend can see if I am online */
	    public final boolean getCanSeeMeOnline()
	    {
	        return myRights.isSet(FriendRights.CanSeeOnline);
	    }
	    public final void setCanSeeMeOnline(boolean value)
	    {
	        if (value)
	        {
	            myRights.set(FriendRights.CanSeeOnline);	
	        }
	        else
	        {
	            // if they can't see me online, then they also can't see me on the map
	            myRights.reset(FriendRights.CanSeeOnline | FriendRights.CanSeeOnMap);
	        }
	    }

	    /* True if the friend can see me on the map */
	    public final boolean getCanSeeMeOnMap()
	    {
	        return myRights.isSet(FriendRights.CanSeeOnMap);
	    }
	    public final void setCanSeeMeOnMap(boolean value)
	    {
	        myRights.setBoolean(FriendRights.CanSeeOnMap, value);	
	    }

	    /* True if the friend can modify my objects */
	    public final boolean getCanModifyMyObjects()
	    {
	        return myRights.isSet(FriendRights.CanModifyObjects);
	    }
	    public final void setCanModifyMyObjects(boolean value)
	    {
	        myRights.setBoolean(FriendRights.CanModifyObjects, value);	
	    }

	    /* True if I can see if my friend is online */
	    public final boolean getCanSeeThemOnline()
	    {
	        return theirRights.isSet(FriendRights.CanSeeOnline);	
	    }

	    /* True if I can see if my friend is on the map */
	    public final boolean getCanSeeThemOnMap()
	    {
	       	return theirRights.isSet(FriendRights.CanSeeOnMap);	
	    }

	    /* True if I can modify my friend's objects */
	    public final boolean getCanModifyTheirObjects()
	    {
	      	return theirRights.isSet(FriendRights.CanSeeOnline);		
	    }

	    /* My friend's rights represented as bitmapped flags */
	    public final FriendRights getTheirFriendRights()
	    {
	        return theirRights;
	    }
	    public final void setTheirFriendRights(FriendRights value)
	    {
	     	theirRights = value;
	    }
	    public final void setTheirFriendRights(int value)
	    {
	     	theirRights = new FriendRights(value);
	    }

	    /* My rights represented as bitmapped flags */
	    public final FriendRights getMyFriendRights()
	    {
	     	return myRights;
	    }
	    public final void setMyFriendRights(FriendRights value)
	    {
	        myRights = value;
	    }
	    public final void setMyFriendRights(int value)
	    {
	        myRights = new FriendRights(value);
	    }

	    /** Used internally when building the initial list of friends at login time
	     * 
	     *  @param id System ID of the avatar being prepesented
	     *  @param buddy_rights_given Rights the friend has to see you online and to modify your objects
	     *  @param buddy_rights_has Rights you have to see your friend online and to modify their objects 
	     */
	    public FriendInfo(UUID id, int buddy_rights_given, int buddy_rights_has)
	    {
	        ID = id;
	        this.theirRights = new FriendRights(buddy_rights_given);
	        this.myRights = new FriendRights(buddy_rights_has);
	    }

	    /** FriendInfo represented as a string
	     * 
	     *  @return A string reprentation of both my rights and my friends rights
	     */
	    public String toString()
	    {
	        return String.format("%f (Their Rights: %1x, My Rights: %1x)", getName(), getTheirFriendRights().toString(), getMyFriendRights().toString());
	    }
	}

	// #region callback handlers
	
	// Triggered whenever a friend comes online or goes offline
	public class FriendNotificationCallbackArgs extends CallbackArgs
	{
		private final UUID agentID;
		private final boolean online;
		
		public UUID getAgentID()
		{
			return agentID;
		}
		
		public boolean getOnline()
		{
			return online;
		}
		
		public FriendNotificationCallbackArgs(UUID agentID, boolean online)
		{
			this.agentID = agentID;
			this.online = online;
		}
	}

	public abstract class FriendNotificationCallback extends CallbackHandler<FriendNotificationCallbackArgs>
	{
		public abstract void callback(FriendNotificationCallbackArgs params);
	}
	
	public final CallbackHandlerQueue<FriendNotificationCallbackArgs> OnFriendNotification = new CallbackHandlerQueue<FriendNotificationCallbackArgs>();
	

	// Triggered when a friends rights changed
	public class FriendRightsCallbackArgs extends CallbackArgs
	{
		private final FriendInfo friendInfo;
		
		public FriendInfo getFriendInfo()
		{
			return friendInfo;
		}
		
		public FriendRightsCallbackArgs(FriendInfo friendInfo)
		{
			this.friendInfo = friendInfo;
		}
	}
	
	public abstract class FriendRightCallback extends CallbackHandler<FriendRightsCallbackArgs>
	{
		public abstract void callback(FriendRightsCallbackArgs params);
	}

	public final CallbackHandlerQueue<FriendRightsCallbackArgs> OnFriendRights = new CallbackHandlerQueue<FriendRightsCallbackArgs>();


	// Triggered when a map request for a friend is answered
	public class FriendFoundReplyCallbackArgs extends CallbackArgs
	{
		private final UUID preyID;
		private final long regionHandle;
		private final Vector3 vector3;
		
		public UUID getPreyID()
		{
			return preyID;
		}
		
		public long getRegionHandle()
		{
			return regionHandle;
		}
		
		public Vector3 getVector3()
		{
			return vector3;
		}

		public FriendFoundReplyCallbackArgs(UUID preyID, long regionHandle, Vector3 vector3)
		{
			this.preyID = preyID;
			this.regionHandle = regionHandle;
			this.vector3 = vector3;
		}
	}

	public abstract class FriendFoundReplyCallback extends CallbackHandler<FriendFoundReplyCallbackArgs>
	{
		public abstract void callback(FriendFoundReplyCallbackArgs params);
	}

	public CallbackHandlerQueue<FriendFoundReplyCallbackArgs> OnFriendFoundReply = new CallbackHandlerQueue<FriendFoundReplyCallbackArgs>();


	/* Triggered when friend rights packet is received */
	public class FriendshipOfferedCallbackArgs extends CallbackArgs
	{
		private final UUID friendID;
		private final String name;
		private final UUID sessionID;
		
		public UUID getFriendID()
		{
			return friendID;
		}
		
		public String getName()
		{
			return name;
		}
		
		public UUID getSessionID()
		{
			return sessionID;
		}

		public FriendshipOfferedCallbackArgs(UUID friendID, String name, UUID sessionID)
		{
			this.friendID = friendID;
			this.name = name;
			this.sessionID = sessionID;
		}
	}

	public abstract class FriendshipOfferedCallback extends CallbackHandler<FriendshipOfferedCallbackArgs>
	{
		public abstract void callback(FriendshipOfferedCallbackArgs params);
	}

	public CallbackHandlerQueue<FriendshipOfferedCallbackArgs> OnFriendshipOffered = new CallbackHandlerQueue<FriendshipOfferedCallbackArgs>();

	
	/* Triggered when friend rights packet is received */
	public class FriendshipResponseCallbackArgs extends CallbackArgs
	{
		private final UUID agentID;
		private final String name;
		private final boolean accepted;
		
		public UUID getAgentID()
		{
			return agentID;
		}
		
		public String getName()
		{
			return name;
		}
		
		public boolean getAccepted()
		{
			return accepted;
		}

		public FriendshipResponseCallbackArgs(UUID agentID, String name, boolean accepted)
		{
			this.agentID = agentID;
			this.name = name;
			this.accepted = accepted;
		}
	}

	public abstract class FriendshipResponseCallback extends CallbackHandler<FriendshipResponseCallbackArgs>
	{
		public abstract void callback(FriendshipResponseCallbackArgs params);
	}

	public CallbackHandlerQueue<FriendshipResponseCallbackArgs> OnFriendshipResponse = new CallbackHandlerQueue<FriendshipResponseCallbackArgs>();

	
	/* Triggered when friend rights packet is received */
	public class FriendshipTerminatedCallbackArgs extends CallbackArgs
	{
		private final UUID otherID;
		private final String name;
		
		public UUID getOtherID()
		{
			return otherID;
		}
		
		public String getName()
		{
			return name;
		}
		
		public FriendshipTerminatedCallbackArgs(UUID otherID, String name)
		{
			this.otherID = otherID;
			this.name = name;
		}
	}

	public abstract class FriendshipTerminatedCallback extends CallbackHandler<FriendshipTerminatedCallbackArgs>
	{
		public abstract void callback(FriendshipTerminatedCallbackArgs params);
	}

	public CallbackHandlerQueue<FriendshipTerminatedCallbackArgs> OnFriendshipTerminated = new CallbackHandlerQueue<FriendshipTerminatedCallbackArgs>();
	// #endregion callback handlers

	private GridClient Client;

	/**
	 * A dictionary of key/value pairs containing known friends of this avatar.
     * 
     * he Key is the {@link UUID} of the friend, the value is a {@link FriendInfo} object
     * that contains detailed information including permissions you have and have given to the friend
     */
    public Hashtable<UUID, FriendInfo> FriendList = new Hashtable<UUID, FriendInfo>();

    /**
     * A Dictionary of key/value pairs containing current pending friendship offers.
     * 
     * The key is the {@link UUID} of the avatar making the request, 
     * the value is the {@link UUID} of the request which is used to accept
     * or decline the friendship offer
     */
    public Hashtable<UUID, UUID> FriendRequests = new Hashtable<UUID, UUID>();

    /**
     * Internal constructor
     * @param client A reference to the ClientManager Object
     */
    public FriendsManager(GridClient client)
    {
        Client = client;

        Client.Self.OnInstantMessage.add(new Self_OnInstantMessage());

        Client.Login.OnLoginProgress.add(new Network_OnConnect(), false);
        Client.Login.RegisterLoginResponseCallback(new Network_OnLoginResponse(), new String[] { "buddy-list" }, false);

        Client.Network.RegisterCallback(PacketType.OnlineNotification, this);
        Client.Network.RegisterCallback(PacketType.OfflineNotification, this);
        Client.Network.RegisterCallback(PacketType.ChangeUserRights, this);
        Client.Network.RegisterCallback(PacketType.TerminateFriendship, this);
        Client.Network.RegisterCallback(PacketType.FindAgent, this);
        Client.Network.RegisterCallback(PacketType.UUIDNameReply, this);

    }

	public void packetCallback(Packet packet, Simulator simulator) throws Exception
	{
        switch (packet.getType()) {
           case OnlineNotification:
           case OfflineNotification:
        	   FriendNotificationHandler(packet, simulator);
	           break;
           case ChangeUserRights:
        	   ChangeUserRightsHandler(packet, simulator);
        	   break;
           case TerminateFriendship:
        	   TerminateFriendshipHandler(packet, simulator);
        	   break;
           case FindAgent:
        	   OnFindAgentReplyHandler(packet, simulator);
        	   break;
           case UUIDNameReply:
	           UUIDNameReplyHandler(packet, simulator);
	           break;
        }
    }
    
    /**
     * Accept a friendship request
     * 
     * @param fromAgentID agentID of avatatar to form friendship with
     * @param imSessionID imSessionID of the friendship request message
     * @throws Exception 
     * @throws InventoryException 
     */
    public final void AcceptFriendship(UUID fromAgentID, UUID imSessionID) throws Exception, InventoryException
    {
    	if (Client.Inventory == null)
    		throw new InventoryException("Inventory not instantiated. Need to lookup CallingCard folder in oreder to accept a friendship request.");

    	UUID callingCardFolder = Client.Inventory.FindFolderForType(AssetType.CallingCard);

        AcceptFriendshipPacket request = new AcceptFriendshipPacket();
        request.AgentData.AgentID = Client.Self.getAgentID();
        request.AgentData.SessionID = Client.Self.getSessionID();
        request.TransactionBlock.TransactionID = imSessionID;
        request.FolderData = new AcceptFriendshipPacket.FolderDataBlock[1];
        request.FolderData[0] = request.new FolderDataBlock();
        request.FolderData[0].FolderID = callingCardFolder;

        Client.Network.SendPacket(request);

        FriendInfo friend = new FriendInfo(fromAgentID, FriendRights.CanSeeOnline, FriendRights.CanSeeOnline);

        if (!FriendList.containsKey(fromAgentID))
        {
            FriendList.put(friend.getUUID(), friend);
        }

        if (FriendRequests.containsKey(fromAgentID))
        {
            FriendRequests.remove(fromAgentID);
        }
        Client.Avatars.RequestAvatarName(fromAgentID, null);
    }

    /**
     * Decline a friendship request
     *
     * @param fromAgentID {@link UUID} of friend
     * @param imSessionID imSessionID of the friendship request message
     * @throws Exception 
     */
    public final void DeclineFriendship(UUID fromAgentID, UUID imSessionID) throws Exception
    {
        DeclineFriendshipPacket request = new DeclineFriendshipPacket();
        request.AgentData.AgentID = Client.Self.getAgentID();
        request.AgentData.SessionID = Client.Self.getSessionID();
        request.TransactionBlock.TransactionID = imSessionID;
        Client.Network.SendPacket(request);

        if (FriendRequests.containsKey(fromAgentID))
        {
             FriendRequests.remove(fromAgentID);
        }
    }

    /**
     * Overload: Offer friendship to an avatar.
     *  
     * @param agentID System ID of the avatar you are offering friendship to
     * @throws Exception 
     */
    public final void OfferFriendship(UUID agentID) throws Exception
    {
        OfferFriendship(agentID, "Do you want to be my friend?");
    }

    /**
     * Offer friendship to an avatar.
     * 
     * @param agentID System ID of the avatar you are offering friendship to
     * @param message A message to send with the request
     * @throws Exception 
     */
    public final void OfferFriendship(UUID agentID, String message) throws Exception
    {
        Client.Self.InstantMessage(Client.Self.getName(), agentID, message, UUID.GenerateUUID(),
        		InstantMessageDialog.FriendshipOffered, InstantMessageOnline.Offline,
        		Client.Self.getSimPosition(), Client.Network.getCurrentSim().ID, null);
    }

    /**
     * Terminate a friendship with an avatar
     * 
     * @param agentID System ID of the avatar you are terminating the friendship with
     * @throws Exception 
     */
    public final void TerminateFriendship(UUID agentID) throws Exception
    {
        if (FriendList.containsKey(agentID))
        {
            TerminateFriendshipPacket request = new TerminateFriendshipPacket();
            request.AgentData.AgentID = Client.Self.getAgentID();
            request.AgentData.SessionID = Client.Self.getSessionID();
            request.ExBlock.OtherID = agentID;

            Client.Network.SendPacket(request);

            FriendList.remove(agentID);
        }
    }

    /**
     * Process an incoming packet and raise the appropriate events
     * 
     * @param sender The sender
     * @param e The EventArgs object containing the packet data
     */
    private void TerminateFriendshipHandler(Packet packet, Simulator simulator)
    {
        TerminateFriendshipPacket itsOver = (TerminateFriendshipPacket)packet;
        FriendInfo friend = FriendList.remove(itsOver.ExBlock.OtherID);

        OnFriendshipTerminated.dispatch(new FriendshipTerminatedCallbackArgs(itsOver.ExBlock.OtherID, friend != null ? friend.getName() : null));
    }

    /**
     * Change the rights of a friend avatar.
     * 
     * @param friendID the {@link UUID} of the friend
     * @param rights the new rights to give the friend
     * @throws Exception 
     *  
     * This method will implicitly set the rights to those passed in the rights parameter.
     */
    public final void GrantRights(UUID friendID, byte rights) throws Exception
    {
        GrantUserRightsPacket request = new GrantUserRightsPacket();
        request.AgentData.AgentID = Client.Self.getAgentID();
        request.AgentData.SessionID = Client.Self.getSessionID();
        request.Rights = new GrantUserRightsPacket.RightsBlock[1];
        request.Rights[0] = request.new RightsBlock();
        request.Rights[0].AgentRelated = friendID;
        request.Rights[0].RelatedRights = (int)rights;

        Client.Network.SendPacket(request);
    }

    /**
     * Use to map a friends location on the grid.
     * 
     * @param friendID Friends UUID to find
     * @throws Exception 
     * 
     * {@link E:OnFriendFound}
     */
    public final void MapFriend(UUID friendID) throws Exception
    {
        FindAgentPacket stalk = new FindAgentPacket();
        stalk.AgentBlock.Hunter = Client.Self.getAgentID();
        stalk.AgentBlock.Prey = friendID;
        stalk.AgentBlock.SpaceIP = 0; // Will be filled in by the simulator
        stalk.LocationBlock = new FindAgentPacket.LocationBlockBlock[1];
        stalk.LocationBlock[0] = stalk.new LocationBlockBlock();
        stalk.LocationBlock[0].GlobalX = 0.0; // Filled in by the simulator
        stalk.LocationBlock[0].GlobalY = 0.0;

        Client.Network.SendPacket(stalk);
    }

    /**
     * Use to track a friends movement on the grid
     * 
     * @param friendID Friends Key
     * @throws Exception 
     */
    public final void TrackFriend(UUID friendID) throws Exception
    {
        TrackAgentPacket stalk = new TrackAgentPacket();
        stalk.AgentData.AgentID = Client.Self.getAgentID();
        stalk.AgentData.SessionID = Client.Self.getSessionID();
        stalk.TargetData.PreyID = friendID;

        Client.Network.SendPacket(stalk);
    }

    /**
     * Ask for a notification of friend's online status
     * 
     * @param friendID Friend's UUID
     * @throws Exception 
     */
    public final void RequestOnlineNotification(UUID friendID) throws Exception
    {
        GenericMessagePacket gmp = new GenericMessagePacket();
        gmp.AgentData.AgentID = Client.Self.getAgentID();
        gmp.AgentData.SessionID = Client.Self.getSessionID();
        gmp.AgentData.TransactionID = UUID.Zero;

        gmp.MethodData.setMethod(Helpers.StringToBytes("requestonlinenotification"));
        gmp.MethodData.Invoice = UUID.Zero;
        gmp.ParamList = new GenericMessagePacket.ParamListBlock[1];
        gmp.ParamList[0] = gmp.new ParamListBlock();
        gmp.ParamList[0].setParameter(Helpers.StringToBytes(friendID.toString()));

        Client.Network.SendPacket(gmp);
    }

    /**
     * Process an incoming packet and raise the appropriate events
     * 
     * @param packet The received packet data
     * @param simulator The simulator for which the even the packet data is
     */
	private void FriendNotificationHandler(Packet packet, Simulator simulator) throws Exception
	{
        Vector<UUID> requestids = new Vector<UUID>();
		FriendInfo friend = null;
		UUID agentID = null;
        boolean doNotify = false;

        if (packet.getType() == PacketType.OnlineNotification)
        {
        	OnlineNotificationPacket notification = (OnlineNotificationPacket) packet;
        	for (OnlineNotificationPacket.AgentBlockBlock block : notification.AgentBlock)
	        {
        		agentID = block.AgentID;
    			synchronized (FriendList)
		        {
			        if (!FriendList.containsKey(agentID))
			        {
				        // Mark this friend for a name request
				        requestids.addElement(agentID);
						friend = new FriendInfo(agentID, FriendRights.CanSeeOnline, FriendRights.CanSeeOnline);
				        FriendList.put(agentID, friend);
			        }
			        else
			        {
			            friend = FriendList.get(agentID);
			        }
		        }
    			doNotify = !friend.getIsOnline(); 
                friend.setIsOnline(true);
	        }
        }
        else if (packet.getType() == PacketType.OfflineNotification)
        {
        	OfflineNotificationPacket notification = (OfflineNotificationPacket) packet;
        	for (OfflineNotificationPacket.AgentBlockBlock block : notification.AgentBlock)
	        {
        		agentID = block.AgentID;
    			synchronized (FriendList)
    			{
		            if (!FriendList.containsKey(agentID))
		            {
				        // Mark this friend for a name request
				        requestids.addElement(agentID);

				        friend = new FriendInfo(agentID, FriendRights.CanSeeOnline, FriendRights.CanSeeOnline);
				        FriendList.put(agentID, friend);
			        }
		            else
		            {
		            	friend = FriendList.get(agentID);
		            }
    			}
    			doNotify = friend.getIsOnline();
                friend.setIsOnline(false);
	        }
        }

        // Only notify when there was a change in online status
        if (doNotify)
	        OnFriendNotification.dispatch(new FriendNotificationCallbackArgs(agentID, friend.getIsOnline()));

        if (requestids.size() > 0)
        {
        	Client.Avatars.RequestAvatarNames(requestids, null);
        }
    }
	
    /** Process an incoming packet and raise the appropriate events
     * 
     *  @param packet The received packet data
     *  @param simulator The simulator for which the even the packet data is
     */
    private void ChangeUserRightsHandler(Packet packet, Simulator simulator) throws Exception
    {
        if (packet.getType() == PacketType.ChangeUserRights)
        {
            FriendInfo friend;
            ChangeUserRightsPacket rights = (ChangeUserRightsPacket)packet;

            for (ChangeUserRightsPacket.RightsBlock block : rights.Rights)
            {
                if (FriendList.containsKey(block.AgentRelated))
                {
                    friend = FriendList.get(block.AgentRelated);
                    friend.setTheirFriendRights(block.RelatedRights);
                    
                    OnFriendRights.dispatch(new FriendRightsCallbackArgs(friend));
                }
                else if (block.AgentRelated.equals(Client.Self.getAgentID()))
                {
                    if (FriendList.containsKey(rights.AgentData.AgentID))
                    {
                        friend = FriendList.get(rights.AgentData.AgentID);
                        friend.setMyFriendRights(block.RelatedRights);
                        
                        OnFriendRights.dispatch(new FriendRightsCallbackArgs(friend));
                    }
                }
            }
        }
    }

    /**
     * Process an incoming packet and raise the appropriate events
     * 
     * @param packet The received packet data
     * @param simulator The simulator for which the packet data is
     */
    private void OnFindAgentReplyHandler(Packet packet, Simulator simulator) throws Exception
    {
        if (OnFriendFoundReply.count() > 0)
        {
            FindAgentPacket reply = (FindAgentPacket)packet;

            UUID prey = reply.AgentBlock.Prey;
            float values[] = new float[2]; 
            long regionHandle = Helpers.GlobalPosToRegionHandle((float)reply.LocationBlock[0].GlobalX, (float)reply.LocationBlock[0].GlobalY, values);

            OnFriendFoundReply.dispatch(new FriendFoundReplyCallbackArgs(prey, regionHandle, new Vector3(values[0], values[1], 0f)));
        }
    }

	/**
	 * Process an incoming UUIDNameReply Packet and insert Full Names into the
	 * FriendList Dictionary
	 *  
	 * @param packet Incoming Packet to process</param>
	 * @param simulator Unused
	 */
	private void UUIDNameReplyHandler(Packet packet, Simulator simulator) throws Exception
	{
		UUIDNameReplyPacket reply = (UUIDNameReplyPacket) packet;

		synchronized (FriendList)
		{
			for (UUIDNameReplyPacket.UUIDNameBlockBlock block : reply.UUIDNameBlock)
			{
				FriendInfo friend;
				
				if (!FriendList.containsKey(block.ID))
				{
					friend = new FriendInfo(block.ID, FriendRights.CanSeeOnline, FriendRights.CanSeeOnline);
					FriendList.put(block.ID, friend);
				}
				else
				{
					friend = FriendList.get(block.ID);
				}
				friend.setName(Helpers.BytesToString(block.getFirstName()) + " " + Helpers.BytesToString(block.getLastName()));
			}
		}
	}

	private class Self_OnInstantMessage extends CallbackHandler<InstantMessageCallbackArgs>
	{
		@Override
		public void callback(InstantMessageCallbackArgs e)
		{
	    	UUID friendID = e.getIM().FromAgentID;
	    	String name = e.getIM().FromAgentName;
	    	
	    	switch (e.getIM().Dialog)
	    	{
	    	    case FriendshipOffered:
	                if (OnFriendshipOffered != null)
	                { 
	                	UUID sessionID = e.getIM().IMSessionID;
	                    FriendRequests.put(friendID, sessionID);
	                    OnFriendshipOffered.dispatch(new FriendshipOfferedCallbackArgs(friendID, name, sessionID));
	                }
	                break;
	    	    case FriendshipAccepted:
	                FriendInfo friend = new FriendInfo(friendID, FriendRights.CanSeeOnline, FriendRights.CanSeeOnline);
	                friend.setName(name);
	                synchronized (FriendList)
	                {
	                    FriendList.put(friendID, friend);
	                }
	                if (OnFriendshipResponse != null)
	                {
	                    OnFriendshipResponse.dispatch(new FriendshipResponseCallbackArgs(friendID, name, true));
	                }
					try {
						RequestOnlineNotification(friendID);
					}
					catch (UnsupportedEncodingException ex)
					{
					}
					catch (Exception ex)
					{
					}
	                break;
	    	    case FriendshipDeclined:
	                if (OnFriendshipResponse != null)
	                {
	                    OnFriendshipResponse.dispatch(new FriendshipResponseCallbackArgs(friendID, name, false));
	                }
	        }
	    }
	}

    /**
     * Raised after login
     * 
     * @param sender
     * @param e
     */
    private class Network_OnConnect extends CallbackHandler<LoginProgressCallbackArgs>
    {		
        public void callback(LoginProgressCallbackArgs e)
        {
            if (e.getStatus() == LoginStatus.Success)
            {
                Vector<UUID> names = new Vector<UUID>();

                if (FriendList.size() > 0)
                {
                    for (FriendInfo kvp : FriendList.values())
                    {
                        if (kvp.getName().isEmpty())
                        {
                            names.add(kvp.getUUID());
                        }
                    }
                    try
                    {
						Client.Avatars.RequestAvatarNames(names, null);
					}
                    catch (Exception e1)
					{
					}
                }
            }
        }
    }

    /**
     * Populate FriendList {@link InternalDictionary} with data from the login reply
     * 
     * @param loginSuccess true if login was successful
     * @param redirect true if login request is requiring a redirect
     * @param message A string containing the response to the login request
     * @param reason A {@link LoginResponseData} object containing the decoded
     * @param replyData reply from the login server
     */
    private class Network_OnLoginResponse extends CallbackHandler<LoginResponseCallbackArgs>
    {		
        public void callback(LoginResponseCallbackArgs e) 
        // Network_OnLoginResponse(boolean loginSuccess, boolean redirect, String message, String reason, LoginResponseData replyData)
        {
            if (e.getSuccess() && e.getReply().BuddyList != null)
            {
                for (BuddyListEntry buddy : e.getReply().BuddyList)
                {
                    UUID bubid = UUID.Parse(buddy.buddy_id);
                    synchronized (FriendList)
                    {
                        if (!FriendList.containsKey(bubid))
                        {
                            FriendList.put(bubid, new FriendInfo(bubid, buddy.buddy_rights_given, buddy.buddy_rights_has));
                        }
                    }
                }
            }
        }
    }
}
