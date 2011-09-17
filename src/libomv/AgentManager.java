/**
 * Copyright (c) 2006, Second Life Reverse Engineering Team
 * Portions Copyright (c) 2006, Lateral Arts Limited
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
import java.net.InetAddress;
import java.net.URI;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Map.Entry;
import java.util.Timer;
import java.util.TimerTask;

import org.apache.http.nio.concurrent.FutureCallback;
import org.apache.http.nio.reactor.IOReactorException;

import libomv.DirectoryManager.ClassifiedCategories;
import libomv.DirectoryManager.ClassifiedFlags;
import libomv.GridManager.GridLayerType;
import libomv.GridManager.GridRegion;
import libomv.GroupManager.ChatSessionMember;
import libomv.GroupManager.GroupPowers;
import libomv.LoginManager.LoginResponseCallbackArgs;
import libomv.LoginManager.LoginResponseData;
import libomv.NetworkManager.DisconnectedCallbackArgs;
import libomv.StructuredData.OSD;
import libomv.StructuredData.OSD.OSDFormat;
import libomv.StructuredData.OSDMap;
import libomv.assets.AssetGesture;
import libomv.assets.AssetGesture.GestureStep;
import libomv.assets.AssetGesture.GestureStepAnimation;
import libomv.assets.AssetGesture.GestureStepChat;
import libomv.assets.AssetGesture.GestureStepWait;
import libomv.assets.AssetItem;
import libomv.assets.AssetItem.AssetType;
import libomv.assets.AssetManager.AssetDownload;
import libomv.assets.AssetManager.AssetReceivedCallback;
import libomv.capabilities.CapsCallback;
import libomv.capabilities.CapsClient;
import libomv.capabilities.CapsMessage.AttachmentResourcesMessage;
import libomv.capabilities.CapsMessage.CapsEventType;
import libomv.capabilities.CapsMessage.ChatSessionAcceptInvitation;
import libomv.capabilities.CapsMessage.ChatSessionRequestStartConference;
import libomv.capabilities.IMessage;
import libomv.packets.ActivateGesturesPacket;
import libomv.packets.AgentAnimationPacket;
import libomv.packets.AgentHeightWidthPacket;
import libomv.packets.AgentMovementCompletePacket;
import libomv.packets.AgentUpdatePacket;
import libomv.packets.ChatFromSimulatorPacket;
import libomv.packets.ChatFromViewerPacket;
import libomv.packets.ClassifiedDeletePacket;
import libomv.packets.ClassifiedInfoUpdatePacket;
import libomv.packets.CompleteAgentMovementPacket;
import libomv.packets.DeactivateGesturesPacket;
import libomv.packets.HealthMessagePacket;
import libomv.packets.ImprovedInstantMessagePacket;
import libomv.packets.MoneyBalanceReplyPacket;
import libomv.packets.MoneyTransferRequestPacket;
import libomv.packets.Packet;
import libomv.packets.PacketType;
import libomv.packets.PickDeletePacket;
import libomv.packets.PickInfoUpdatePacket;
import libomv.packets.RetrieveInstantMessagesPacket;
import libomv.packets.ScriptAnswerYesPacket;
import libomv.packets.ScriptDialogReplyPacket;
import libomv.packets.ScriptSensorRequestPacket;
import libomv.packets.SetAlwaysRunPacket;
import libomv.packets.StartLurePacket;
import libomv.packets.TeleportFailedPacket;
import libomv.packets.TeleportFinishPacket;
import libomv.packets.TeleportLandmarkRequestPacket;
import libomv.packets.TeleportLocalPacket;
import libomv.packets.TeleportLocationRequestPacket;
import libomv.packets.TeleportProgressPacket;
import libomv.packets.TeleportStartPacket;
import libomv.packets.ViewerEffectPacket;
import libomv.primitives.Primitive;
import libomv.types.Color4;
import libomv.types.Quaternion;
import libomv.types.UUID;
import libomv.types.Vector3;
import libomv.types.PacketCallback;
import libomv.types.Vector3d;
import libomv.types.Vector4;
import libomv.utils.CallbackArgs;
import libomv.utils.CallbackHandler;
import libomv.utils.CallbackHandlerQueue;
import libomv.utils.Helpers;
import libomv.utils.Logger;
import libomv.utils.Logger.LogLevel;
import libomv.utils.TimeoutEvent;

// Class to hold Client Avatar's data
public class AgentManager implements PacketCallback, CapsCallback {
	
	public class TeleportCallbackArgs extends CallbackArgs {
		String message;
		TeleportStatus status;
		int flags;
		
		public String getMessage() {
			return message;
		}
		public TeleportStatus getStatus() {
			return status;
		}
		
		public TeleportCallbackArgs(String message, TeleportStatus status, int flags) {
			this.message = message;
			this.status = status;
			this.flags = flags;
		}
	}
	
	public class ChatCallbackArgs extends CallbackArgs {
		private String message, fromName;
		private byte audible, type, sourcetype;
		private UUID id;

		public String getMessage() {
			return message;
		}
		public String getFromName() {
			return fromName;
		}
		public byte getAudible() {
			return audible;
		}
		public byte getType() {
			return type;
		}
		public byte getSourceType() {
			return sourcetype;
		}
		public UUID getID() {
			return id;
		}
		
		public ChatCallbackArgs(String message, byte audible, byte type, byte sourcetype, String fromName, UUID id) {
			this.message = message;
			this.fromName = fromName;
			this.audible = audible;
			this.type = type;
			this.sourcetype = sourcetype;
			this.id = id;
		}
	}
	
    /* The date received from an ImprovedInstantMessage */
	public class InstantMessageCallbackArgs
	{
	    private final InstantMessage m_IM;
	    private final Simulator m_Simulator;

	    /* Get the InstantMessage object */
	    public final InstantMessage getIM()
	    {
	        return m_IM;
	    }
	    
	    /* Get the simulator where the InstantMessage origniated */
	    public final Simulator getSimulator()
	    {
	        return m_Simulator;
	    }

	    /** Construct a new instance of the InstantMessageEventArgs object
	     * 
	     *  @param im the InstantMessage object
	     *  @param simulator the simulator where the InstantMessage origniated
	     */
	    public InstantMessageCallbackArgs(InstantMessage im, Simulator simulator)
	    {
	        this.m_IM = im;
	        this.m_Simulator = simulator;
	    }
	}

    /* The date received from an ImprovedInstantMessage */
	public class BalanceCallbackArgs
	{
	    private final int balance;

	    /* Get the balance value */
	    public final int getBalance()
	    {
	        return balance;
	    }
	    
	    /** Construct a new instance of the BalanceCallbackArgs object
	     * 
	     *  @param balance the InstantMessage object
	     */
	    public BalanceCallbackArgs(int balance)
	    {
	        this.balance = balance;
	    }
	}

	public class AttachmentResourcesCallbackArgs
	{
		AttachmentResourcesMessage info;
		boolean success;
		
		public AttachmentResourcesCallbackArgs(boolean success, AttachmentResourcesMessage info)
		{
			this.info = info;
			this.success = success;
		}
	}
	
	/** Permission request flags, asked when a script wants to control an Avatar */
    public static class ScriptPermission 
    {
        /* Placeholder for empty values, shouldn't ever see this */
        public static final int None = 0;
        /* Script wants ability to take money from you */
        public static final int Debit = 1 << 1;
        /* Script wants to take camera controls for you */
        public static final int TakeControls = 1 << 2;
        /* Script wants to remap avatars controls */
        public static final int RemapControls = 1 << 3;
        /* Script wants to trigger avatar animations
         * This function is not implemented on the grid */
        public static final int TriggerAnimation = 1 << 4;
        /* Script wants to attach or detach the prim or primset to your avatar */
        public static final int Attach = 1 << 5;
        /* Script wants permission to release ownership
         * This function is not implemented on the grid
         * The concept of "public" objects does not exist anymore. */
        public static final int ReleaseOwnership = 1 << 6;
        /* Script wants ability to link/delink with other prims */
        public static final int ChangeLinks = 1 << 7;
        /* Script wants permission to change joints
         * This function is not implemented on the grid */
        public static final int ChangeJoints = 1 << 8;
        /* Script wants permissions to change permissions
         * This function is not implemented on the grid */
        public static final int ChangePermissions = 1 << 9;
        /* Script wants to track avatars camera position and rotation */
        public static final int TrackCamera = 1 << 10;
        /* Script wants to control your camera */
        public static final int ControlCamera = 1 << 11;
        
        public static int setValue(int value)
        {
        	return (value & _mask);
        }
        
        public static int getValue(int value)
        {
        	return (value & _mask);
        }

        private static final int _mask = 0xFFF;
    }

    /** Special commands used in Instant Messages */
    public enum InstantMessageDialog 
    {
        /* Indicates a regular IM from another agent */
        MessageFromAgent,			// 0
        /* Simple notification box with an OK button */
        MessageBox,					// 1
        /* Used to show a countdown notification with an OK button, deprecated now */
        Deprecated_MessageBoxCountdown,        // 2
        /* You've been invited to join a group. */
        GroupInvitation,			// 3
        /* Inventory offer */
        InventoryOffered,			// 4
        /* Accepted inventory offer */
        InventoryAccepted,			// 5
        /* Declined inventory offer */
        InventoryDeclined,			// 6
        /* Group vote */
        GroupVote,					// 7
        /* A message to everyone in the agent's group, no longer used */
        Deprecated_GroupMessage,    // 8
        /* An object is offering its inventory */
        TaskInventoryOffered,		// 9
        /* Accept an inventory offer from an object */
        TaskInventoryAccepted,		// 10
        /* Decline an inventory offer from an object */
        TaskInventoryDeclined,		// 11
        /* Unknown */
        NewUserDefault,				// 12
        /* Start a session, or add users to a session */
        SessionAdd,					// 13
        /* Start a session, but don't prune offline users */
        SessionOfflineAdd,			// 14
        /* Start a session with your group */
        SessionGroupStart,			// 15
        /* Start a session without a calling card (finder or objects) */
        SessionCardlessStart,		// 16
        /* Send a message to a session */
        SessionSend,				// 17
        /* Leave a session */
        SessionDrop,				// 18
        /* Indicates that the IM is from an object */
        MessageFromObject,			// 19
        /* Sent an IM to a busy user, this is the auto response */
        BusyAutoResponse,			// 20
        /* Shows the message in the console and chat history */
        ConsoleAndChatHistory,		// 21
        /* Send a teleport lure */
        RequestTeleport,			// 22
        /* Response sent to the agent which inititiated a teleport invitation */
        AcceptTeleport,				// 23
        /* Response sent to the agent which inititiated a teleport invitation */
        DenyTeleport,				// 24
        /* Only useful if you have Linden permissions */
        GodLikeRequestTeleport,		// 25
        /* A placeholder type for future expansion, currently not used */
        CurrentlyUnused,			// 26
        /* Notification of a new group election, this is depreciated */
        Deprecated_GroupElection,	// 27
        /* IM to tell the user to go to an URL */
        GotoUrl,					// 28
        /* IM for help */
        Session911Start,			// 29
        /* IM sent automatically on call for help, sends a lure to each Helper reached */
        Lure911,					// 30
        /* Like an IM but won't go to email */
        FromTaskAsAlert,			// 31
        /* IM from a group officer to all group members */
        GroupNotice,				// 32
        /* Unknown */
        GroupNoticeInventoryAccepted, // 33
        /* Unknown */
        GroupNoticeInventoryDeclined, // 34
        /* Accept a group invitation */
        GroupInvitationAccept,		// 35
        /* Decline a group invitation */
        GroupInvitationDecline,		// 36
        /* Unknown */
        GroupNoticeRequested,		// 37
        /* An avatar is offering you friendship */
        FriendshipOffered,			// 38
        /* An avatar has accepted your friendship offer */
        FriendshipAccepted,			// 39
        /* An avatar has declined your friendship offer */
        FriendshipDeclined,			// 40
        /* Indicates that a user has started typing */
        StartTyping,				// 41
        /* Indicates that a user has stopped typing */
        StopTyping;					// 42
        
        public static InstantMessageDialog setValue(int value)
        {
        	return values()[value];
        }
 
        public byte getValue()
        {
        	return (byte)ordinal();
        }
    }

    /** Flag in Instant Messages, whether the IM should be delivered to offline avatars as well */
    public enum InstantMessageOnline
    {
        /* Only deliver to online avatars */
        Online,   // 0
        /* If the avatar is offline the message will be held until
         * they login next, and possibly forwarded to their e-mail account */
        Offline; // 1

        public static InstantMessageOnline setValue(int value)
        {
        	return values()[value];
        }
 
        public byte getValue()
        {
        	return (byte)ordinal();
        }
    }

    /* Conversion type to denote Chat Packet types in an easier-to-understand format */
    public enum ChatType 
    {
        /* Whisper (5m radius) */
        Whisper(0),
        /* Normal chat (10/20m radius), what the official viewer typically sends */
        Normal(1),
        /* Shouting! (100m radius) */
        Shout(2),
        /* Say chat (10/20m radius) - The official viewer will
         * print "[4:15] You say, hey" instead of "[4:15] You: hey"</summary>
         */
        //Say = 3,
        /* Event message when an Avatar has begun to type */
        StartTyping(4),
        /* Event message when an Avatar has stopped typing */
        StopTyping(5),
        /* Send the message to the debug channel */
        Debug(6),
        /* Event message when an object uses llOwnerSay */
        OwnerSay(8),
        /* Special value to support llRegionSay, never sent to the client */
        RegionSay(255);

        public static ChatType setValue(byte value)
        {
        	for (ChatType e : values())
        	{
        		if (e.val == value)
        			return e;
        	}
        	return Normal;
        }
 
        public int getValue()
        {
        	return val;
        }
        
        private int val;
        private ChatType(int value)
        {
            val = value;
        }
    }

    /* Identifies the source of a chat message */
    public enum ChatSourceType 
    {
        /* Chat from the grid or simulator */
        System,
        /* Chat from another avatar */
        Agent,
        /* Chat from an object */
        Object;

        public static ChatSourceType setValue(byte value)
        {
        	return values()[value];
        }
 
        public byte getValue()
        {
        	return (byte)ordinal();
        }
    }

    /*  */
    public enum ChatAudibleLevel 
    {
        /*  */
        Not(-1),
        /*  */
        Barely(0),
        /*  */
        Fully(1);

        public static ChatAudibleLevel setValue(byte value)
        {
        	for (ChatAudibleLevel e : values())
        	{
        		if (e.val == value)
        			return e;
        	}
        	return Barely;
        }

        public int getValue() {
        	return val;
        }

        private int val;
        private ChatAudibleLevel(int value) {
            val = value;
        }
   }

    /* Effect type used in ViewerEffect packets */
    public enum EffectType 
    {
        /* */
        Text,				// 0
        /* */
        Icon,				// 1
        /* */
        Connector,			// 2
        /* */
        FlexibleObject,		// 3
        /* */
        AnimalControls,		// 4
        /* */
        AnimationObject,	// 5
        /* */
        Cloth,				// 6
        /* Project a beam from a source to a destination, such as
         * the one used when editing an object */
        Beam,				// 7
        /* */
        Glow,				// 8
        /* */
        Point,				// 9
        /* */
        Trail,				// 10
        /* Create a swirl of particles around an object */
        Sphere,				// 11
        /* */
        Spiral,				// 12
        /* */
        Edit,				// 13
        /* Cause an avatar to look at an object */
        LookAt,				// 14
        /* Cause an avatar to point at an object */
        PointAt;			// 15

        public static EffectType setValue(byte value)
        {
        	return values()[value];
        }
 
        public byte getValue()
        {
        	return (byte)ordinal();
        }
     }

    /* The action an avatar is doing when looking at something, used in 
     * ViewerEffect packets for the LookAt effect */
    public enum LookAtType 
    {
        /* */
        None,
        /* */
        Idle,
        /* */
        AutoListen,
        /* */
        FreeLook,
        /* */
        Respond,
        /* */
        Hover,
        /* Deprecated */
        Conversation,
        /* */
        Select,
        /* */
        Focus,
        /* */
        Mouselook,
        /* */
        Clear;

        public static LookAtType setValue(byte value)
        {
        	return values()[value];
        }
 
        public byte getValue()
        {
        	return (byte)ordinal();
        }
    }

    /* The action an avatar is doing when pointing at something, used in
     * ViewerEffect packets for the PointAt effect */
    public enum PointAtType 
    {
        /* */
        None,
        /* */
        Select,
        /* */
        Grab,
        /* */
        Clear;

        public static PointAtType setValue(byte value)
        {
        	return values()[value];
        }
 
        public byte getValue()
        {
        	return (byte)ordinal();
        }
    }

    // Money transaction types
    public enum MoneyTransactionType 
    {
        /* */
        None(0),
        /* */
        FailSimulatorTimeout(1),
        /* */
        FailDataserverTimeout(2),
        /* */
        ObjectClaim(1000),
        /* */
        LandClaim(1001),
        /* */
        GroupCreate(1002),
        /* */
        ObjectPublicClaim(1003),
        /* */
        GroupJoin(1004),
        /* */
        TeleportCharge(1100),
        /* */
        UploadCharge(1101),
        /* */
        LandAuction(1102),
        /* */
        ClassifiedCharge(1103),
        /* */
        ObjectTax(2000),
        /* */
        LandTax(2001),
        /* */
        LightTax(2002),
        /* */
        ParcelDirFee(2003),
        /* */
        GroupTax(2004),
        /* */
        ClassifiedRenew(2005),
        /* */
        GiveInventory(3000),
        /* */
        ObjectSale(5000),
        /* */
        Gift(5001),
        /* */
        LandSale(5002),
        /* */
        ReferBonus(5003),
        /* */
        InventorySale(5004),
        /* */
        RefundPurchase(5005),
        /* */
        LandPassSale(5006),
        /* */
        DwellBonus(5007),
        /* */
        PayObject(5008),
        /* */
        ObjectPays(5009),
        /* */
        GroupLandDeed(6001),
        /* */
        GroupObjectDeed(6002),
        /* */
        GroupLiability(6003),
        /* */
        GroupDividend(6004),
        /* */
        GroupMembershipDues(6005),
        /* */
        ObjectRelease(8000),
        /* */
        LandRelease(8001),
        /* */
        ObjectDelete(8002),
        /* */
        ObjectPublicDecay(8003),
        /* */
        ObjectPublicDelete(8004),
        /* */
        LindenAdjustment(9000),
        /* */
        LindenGrant(9001),
        /* */
        LindenPenalty(9002),
        /* */
        EventFee(9003),
        /* */
        EventPrize(9004),
        /* */
        StipendBasic(10000),
        /* */
        StipendDeveloper(10001),
        /* */
        StipendAlways(10002),
        /* */
        StipendDaily(10003),
        /* */
        StipendRating(10004),
        /* */
        StipendDelta(10005);

        public static MoneyTransactionType setValue(byte value)
        {
        	for (MoneyTransactionType e : values())
        	{
        		if (e.val == value)
        			return e;
        	}
        	return None;
        }

        public int getValue() {
        	return val;
        }
        
        private int val;
        private MoneyTransactionType(int val) {
            this.val = val;
        }
    }
    /*  */
    public static class TransactionFlags 
    {
        /* */
        public static final byte None = 0x0;
        /* */
        public static final byte SourceGroup = 0x1;
        /* */
        public static final byte DestGroup = 0x2;
        /* */
        public static final byte OwnerGroup = 0x4;
        /* */
        public static final byte SimultaneousContribution = 0x8;
        /* */
        public static final byte ContributionRemoval = 0x10;

        public static byte setValue(int value)
        {
        	return (byte)(value & _mask);
        }
        
        public static byte getValue(byte value)
        {
        	return (byte)(value & _mask);
        }

        private static final byte _mask = 0x1F;
    }

    public enum MeanCollisionType 
    {
        /* */
        None,
        /* */
        Bump,
        /* */
        LLPushObject,
        /* */
        SelectedObjectCollide,
        /* */
        ScriptedObjectCollide,
        /* */
        PhysicalObjectCollide;

        public static MeanCollisionType setValue(byte value)
        {
        	return values()[value];
        }
 
        public byte getValue()
        {
        	return (byte)ordinal();
        }
    }

    /* Flags sent when a script takes or releases a control
     * 
     * NOTE: (need to verify) These might be a subset of the ControlFlags enum in Movement,
     */
    public static class ScriptControlChange 
    {
        /* No Flags set */
        public static final int None = 0;
        /* Forward (W or up Arrow) */
        public static final int Forward = 0x1;
        /* Back (S or down arrow) */
        public static final int Back = 0x2;
        /* Move left (shift+A or left arrow) */
        public static final int Left = 0x4;
        /* Move right (shift+D or right arrow) */
        public static final int Right = 0x8;
        /* Up (E or PgUp) */
        public static final int Up = 0x10;
        /* Down (C or PgDown) */
        public static final int Down = 0x20;
        /* Rotate left (A or left arrow) */
        public static final int RotateLeft = 0x100;
        /* Rotate right (D or right arrow) */
        public static final int RotateRight = 0x200;
        /* Left Mouse Button */
        public static final int LeftButton = 0x10000000;
        /* Left Mouse button in MouseLook */
        public static final int MouseLookLeftButton = 0x40000000;

        public static int setValue(int value)
        {
        	return value & _mask;
        }
        
        public static int getValue(int value) {
           	return value & _mask;
        }
        
        private static final int _mask = 0x5000033F;
    }

    /* Currently only used to hide your group title */
    public enum AgentFlags 
    {
        /* No flags set */
        None,
        /* Hide your group title */
        HideTitle;

        public static AgentFlags setValue(byte value)
        {
        	return values()[value];
        }
 
        public byte getValue()
        {
        	return (byte)ordinal();
        }
    }

    /* Action state of the avatar, which can currently be typing and editing */
    public static class AgentState
    {
        /* */
        public static final byte None = 0x00;
        /* */
        public static final byte Typing = 0x04;
        /* */
        public static final byte Editing = 0x10;
        
        public static byte setValue(int value) {
        	return (byte)(value & _mask);
        }

        public static byte getValue(byte value) {
        	return (byte)(value & _mask);
        }
        
        private static final byte _mask = 0x14;
    }

    /* Current teleport status */
    public enum TeleportStatus
    {
        /* Unknown status */
        None,
        /* Teleport initialized */
        Start,
        /* Teleport in progress */
        Progress,
        /* Teleport failed */
        Failed,
        /* Teleport completed */
        Finished,
        /* Teleport cancelled */
        Cancelled;

        public static TeleportStatus setValue(byte value)
        {
        	return values()[value];
        }
 
        public byte getValue()
        {
        	return (byte)ordinal();
        }
    }

    /* */
    public static class TeleportFlags
    {
        /* No flags set, or teleport failed */
    	public static final int Default = 0;
        /* Set when newbie leaves help island for first time */
    	public static final int SetHomeToTarget = 1 << 0;
        /* */
    	public static final int SetLastToTarget = 1 << 1;
        /* Via Lure */
    	public static final int ViaLure = 1 << 2;
        /* Via Landmark */
    	public static final int ViaLandmark = 1 << 3;
        /* Via Location */
    	public static final int ViaLocation = 1 << 4;
        /* Via Home */
    	public static final int ViaHome = 1 << 5;
        /* Via Telehub */
    	public static final int ViaTelehub = 1 << 6;
        /* Via Login */
    	public static final int ViaLogin = 1 << 7;
        /* Linden Summoned */
    	public static final int ViaGodlikeLure = 1 << 8;
        /* Linden Forced me */
    	public static final int Godlike = 1 << 9;
        /* */
    	public static final int NineOneOne = 1 << 10;
        /* Agent Teleported Home via Script */
    	public static final int DisableCancel = 1 << 11;
        /* */
    	public static final int ViaRegionID = 1 << 12;
        /* */
    	public static final int IsFlying = 1 << 13;
        /* */
    	public static final int ResetHome = 1 << 14;
        /* forced to new location for example when avatar is banned or ejected */
    	public static final int ForceRedirect = 1 << 15;
        /* Teleport Finished via a Lure */
    	public static final int FinishedViaLure = 1 << 26;
        /* Finished, Sim Changed */
    	public static final int FinishedViaNewSim = 1 << 28;
        /* Finished, Same Sim */
    	public static final int FinishedViaSameSim = 1 << 29;
    	
		public static int setValue(int value) {
			return (value & _mask);
		}

		public static int getValue(int value) {
			return (value & _mask);
		}

		private static final int _mask = 0x3400FFFF;
    }

    /* */
    public enum TeleportLureFlags
    {
        /* */
        NormalLure,
        /* */
        GodlikeLure,
        /* */
        GodlikePursuit;

        public static TeleportLureFlags setValue(byte value)
        {
        	return values()[value];
        }
 
        public byte getValue()
        {
        	return (byte)ordinal();
        }
    }

    /* */
    public static class ScriptSensorTypeFlags
    {
        /* */
    	public static final byte Agent = 1;
        /* */
    	public static final byte Active = 2;
        /* */
    	public static final byte Passive = 4;
        /* */
    	public static final byte Scripted = 8;

        public static byte setValue(int value) {
        	return (byte)(value & _mask);
        }

        public static byte getValue(byte value) {
        	return (byte)(value & _mask);
        }
        
        private static final byte _mask = 0xF;
}

    /* Instant Message */
    public final class InstantMessage
    {
        /* Key of sender */
        public UUID FromAgentID;
        /* Name of sender */
        public String FromAgentName;
        /* Key of destination avatar */
        public UUID ToAgentID;
        /* ID of originating estate */

        public int ParentEstateID;
        /* Key of originating region */
        public UUID RegionID;
        /* Coordinates in originating region */
        public Vector3 Position;
        /* Instant message type */
        public InstantMessageDialog Dialog;
        /* Group IM session toggle */
        public boolean GroupIM;
        /* Key of IM session, for Group Messages, the groups UUID */
        public UUID IMSessionID;
        /* Timestamp of the instant message */
        public Date Timestamp;
        /* Instant message text */
        public String Message;
        /* Whether this message is held for offline avatars */
        public InstantMessageOnline Offline;
        /* Context specific packed data */
        public byte[] BinaryBucket;

        /* Print the struct data as a string
         * 
         * @return A string containing the field name, and field value */
        @Override
		public String toString()
        {
            return Helpers.StructToString(this);
        }
    }

	public CallbackHandlerQueue<ChatCallbackArgs> OnChat = new CallbackHandlerQueue<ChatCallbackArgs>();
	public CallbackHandlerQueue<InstantMessageCallbackArgs> OnInstantMessage = new CallbackHandlerQueue<InstantMessageCallbackArgs>();
	public CallbackHandlerQueue<TeleportCallbackArgs> OnTeleport = new CallbackHandlerQueue<TeleportCallbackArgs>();
	public CallbackHandlerQueue<BalanceCallbackArgs> OnBalanceUpdated = new CallbackHandlerQueue<BalanceCallbackArgs>();

    private UUID agentID;
    // A temporary UUID assigned to this session, used for secure transactions
    private UUID sessionID;
    private UUID secureSessionID;
    private String startLocation = Helpers.EmptyString;
    private String agentAccess = Helpers.EmptyString;
	// Position avatar client will goto when login to 'home' or during
	// teleport request to 'home' region.
    private Vector3 homePosition;
	// LookAt point saved/restored with HomePosition
    private Vector3 homeLookAt;
    private String firstName = Helpers.EmptyString;
    private String lastName = Helpers.EmptyString;
    private String fullName;
	private TimeoutEvent<TeleportStatus> teleportTimeout;

    private int heightWidthGenCounter;
    private HashMap<UUID, AssetGesture> gestureCache = new HashMap<UUID, AssetGesture>();

    private float health;
    private int balance;
    private UUID activeGroup;
    private GroupPowers activeGroupPowers;

	// Your (client) Avatar ID, local to Region/sim
    private long localID;
	// Current position of avatar
    private Vector3 relativePosition;
	// Current rotation of avatar
    private Quaternion relativeRotation = Quaternion.Identity;
    private Vector4 collisionPlane;
    private Vector3 velocity;
    private Vector3 acceleration;
    private Vector3 angularVelocity;
    private long sittingOn;

    /* Your (client) avatars <see cref="UUID"/>
       "client", "agent", and "avatar" all represent the same thing */
    public final UUID getAgentID()
    {
        return agentID;
    }
    public final void setAgentID(UUID uuid)
    {
        agentID = uuid;
    }
    /* Temporary {@link UUID} assigned to this session, used for 
       verifying our identity in packets */
    public final UUID getSessionID()
    {
        return sessionID;
    }
    public final void setSessionID(UUID uuid)
    {
        sessionID = uuid;
    }
    /* Shared secret {@link UUID} that is never sent over the wire */
    public final UUID getSecureSessionID()
    {
        return secureSessionID;
    }
    /* Your (client) avatar ID, local to the current region/sim */
    public final long getLocalID()
    {
        return localID;
    }
    public final void setLocalID(long id)
    {
        localID = id;
    }
    /* Where the avatar started at login. Can be "last", "home" 
       or a login {@link T:OpenMetaverse.URI} */
    public final String getStartLocation()
    {
        return startLocation;
    }
    /* The access level of this agent, usually M or PG */
    public final String getAgentAccess()
    {
        return agentAccess;
    }
    /* The CollisionPlane of Agent */
    public final Vector4 getCollisionPlane()
    {
        return collisionPlane;
    }
    public final void setCollisionPlane(Vector4 val)
    {
        collisionPlane = val;
    }
    /* An {@link Vector3} representing the velocity of our agent */
    public final Vector3 getVelocity()
    {
        return velocity;
    }
	public void setVelocity(Vector3 val) {
		velocity = val;	
	}
    /* An {@link Vector3} representing the acceleration of our agent */
    public final Vector3 getAcceleration()
    {
        return acceleration;
    }
	public void setAcceleration(Vector3 val) {
		acceleration = val;
	}
    /* A {@link Vector3} which specifies the angular speed, and axis about which an Avatar is rotating. */
    public final Vector3 getAngularVelocity()
    {
        return angularVelocity;
    }
	public void setAngularVelocity(Vector3 val) {
		angularVelocity = val;
	}
    /* Position avatar client will goto when login to 'home' or during
    /* teleport request to 'home' region. */
    public final Vector3 getHomePosition()
    {
        return homePosition;
    }
    /* LookAt point saved/restored with HomePosition */
    public final Vector3 getHomeLookAt()
    {
        return homeLookAt;
    }
    /* Avatar First Name (i.e. Philip) */
    public final String getFirstName()
    {
        return firstName;
    }
    /* Avatar Last Name (i.e. Linden) */
    public final String getLastName()
    {
        return lastName;
    }
    /* Avatar Full Name (i.e. Philip Linden) */
    public final String getName()
    {
        // This is a fairly common request, so assume the name doesn't change mid-session and cache the result
        if (fullName == null || fullName.length() < 2)
        {
            fullName = String.format("%s %s", firstName, lastName);
        }
        return fullName;
    }
    public final void setName(String firstName, String lastName)
    {
    	this.firstName = firstName;
    	this.lastName = lastName;
    }
    /* Gets the health of the agent */
    public final float getHealth()
    {
        return health;
    }
    /* Gets the current balance of the agent
*/
    public final int getBalance()
    {
        return balance;
    }
    /* Gets the local ID of the prim the agent is sitting on,
       zero if the avatar is not currently sitting */
    public final long getSittingOn()
    {
        return sittingOn;
    }
    public final void setSittingOn(long val)
    {
        sittingOn = val;
    }
    /* Gets the {@link UUID} of the agents active group. */
    public final UUID getActiveGroup()
    {
        return activeGroup;
    }
    /* Gets the Agents powers in the currently active group */
    public final GroupPowers getActiveGroupPowers()
    {
        return activeGroupPowers;
    }
    /* Current position of the agent as a relative offset from
       the simulator, or the parent object if we are sitting on something */
    public final Vector3 getRelativePosition()
    {
        return relativePosition;
    }
    public final void setRelativePosition(Vector3 value)
    {
        relativePosition = value;
    }
    /* Current rotation of the agent as a relative rotation from
       the simulator, or the parent object if we are sitting on something */
    public final Quaternion getRelativeRotation()
    {
        return relativeRotation;
    }
    public final void setRelativeRotation(Quaternion value)
    {
        relativeRotation = value;
    }
    /* Current position of the agent in the simulator */
    public final Vector3 getSimPosition()
    {
            // simple case, agent not seated
        if (sittingOn == 0)
        {
            return relativePosition;
        }

        // a bit more complicatated, agent sitting on a prim
        Primitive p = null;
        Vector3 fullPosition = relativePosition;

        Simulator sim = _Client.Network.getCurrentSim();
        if (sim.getObjectsPrimitives().containsKey(sittingOn))
        {
          	p = sim.getObjectsPrimitives().get(sittingOn);
            fullPosition.add(p.Position);
        }

        // go up the hiearchy trying to find the root prim
        while (p != null && p.ParentID != 0)
        {
            if (sim.getObjectsAvatars().containsKey(p.ParentID))
            {
            	p = sim.getObjectsAvatars().get(p.ParentID);
                fullPosition.add(p.Position);
            }
            else if (sim.getObjectsPrimitives().containsKey(p.ParentID))
            {
              	p = sim.getObjectsPrimitives().get(p.ParentID);
                fullPosition.add(p.Position);
            }
        }

        if (p != null) // we found the root prim
        {
            return fullPosition;
        }

        // Didn't find the seat's root prim, try returning coarse loaction
        if (sim.getAvatarPositions().containsKey(agentID))
        {
            return sim.getAvatarPositions().get(agentID);
        }

        Logger.Log("Failed to determine agents sim position", LogLevel.Warning, _Client);
        return relativePosition;
    }
    /** 
     * A {@link Quaternion} representing the agents current rotation
     */
    public final Quaternion getSimRotation()
    {
        if (sittingOn != 0)
        {
            Primitive parent;
            if (_Client.Network.getCurrentSim() != null && _Client.Network.getCurrentSim().getObjectsPrimitives().containsKey(sittingOn))
            {
            	parent = _Client.Network.getCurrentSim().getObjectsPrimitives().get(sittingOn);
                return Quaternion.multiply(relativeRotation, parent.Rotation);
            }

            Logger.Log("Currently sitting on object " + sittingOn + " which is not tracked, SimRotation will be inaccurate", LogLevel.Warning, _Client);
            return relativeRotation;

        }
        return relativeRotation;
    }
    /**
     *  Returns the global grid position of the avatar
     */
    public final Vector3d getGlobalPosition()
    {
        if (_Client.Network.getCurrentSim() != null)
        {
            int globals[] = new int[2];
            Helpers.LongToUInts(_Client.Network.getCurrentSim().getHandle(), globals);
            Vector3 pos = getSimPosition();

            return new Vector3d(globals[0] + pos.X, globals[1] + pos.Y, pos.Z);
        }
        return Vector3d.Zero;
    }

    /* Reference to the GridClient instance */
    private final GridClient _Client;
    /* Used for movement and camera tracking */
    private final AgentMovement _Movement;

    /* Currently playing animations for the agent. Can be used to check the current movement status such as
     * walking, hovering, aiming, etc. by checking against system animations found in the Animations class*/
    public HashMap<UUID, Integer> SignaledAnimations = new HashMap<UUID, Integer>();
    /* Dictionary containing current Group Chat sessions and members */
    public HashMap<UUID, ArrayList<ChatSessionMember>> GroupChatSessions = new HashMap<UUID, ArrayList<ChatSessionMember>>();

    private int HeightWidthGenCounter;

    private class Network_OnLoginResponse extends CallbackHandler<LoginResponseCallbackArgs>
    {
    	@Override
		public void callback(LoginResponseCallbackArgs e)
        {
            if (e.getSuccess())
            {
                _Movement.ResetTimer();

                LoginResponseData reply = e.getReply();
                agentID = reply.AgentID;
                sessionID = reply.SessionID;
                secureSessionID = reply.SecureSessionID;
                firstName = reply.FirstName;
                lastName = reply.LastName;
                startLocation = reply.StartLocation;
                agentAccess = reply.AgentAccess;
                _Movement.Camera.LookDirection(reply.LookAt);
                homePosition = reply.HomePosition;
                homeLookAt = reply.HomeLookAt;
            }
        }
    }

    private class Network_OnDisconnected extends CallbackHandler<DisconnectedCallbackArgs>
    {
    	@Override
		public void callback(DisconnectedCallbackArgs e)
    	{
            // Null out the cached fullName since it can change after logging
            // in again (with a different account name or different login
            // server but using the same GridClient object
            fullName = null;
    	}
    }

    // 'CallBack Central' - Setup callbacks for packets related to our avatar
	// <param name="client"></param>
	public AgentManager(GridClient client) {
		_Client = client;

		_Movement = new AgentMovement(client);
		teleportTimeout = new TimeoutEvent<TeleportStatus>();

        _Client.Network.OnDisconnected.add(new Network_OnDisconnected(), false);
        // Login
        _Client.Login.RegisterLoginResponseCallback(new Network_OnLoginResponse(), null, false);

        // Coarse location callback
		_Client.Network.RegisterCallback(PacketType.CoarseLocationUpdate, this);

		// Teleport callbacks
		_Client.Network.RegisterCallback(PacketType.TeleportStart, this);
		_Client.Network.RegisterCallback(PacketType.TeleportProgress, this);
		_Client.Network.RegisterCallback(PacketType.TeleportFailed, this);
		_Client.Network.RegisterCallback(PacketType.TeleportFinish, this);
        _Client.Network.RegisterCallback(PacketType.TeleportCancel, this);
        _Client.Network.RegisterCallback(PacketType.TeleportLocal, this);
        // these come in via the EventQueue
        _Client.Network.RegisterCallback(CapsEventType.TeleportFailed, this);
        _Client.Network.RegisterCallback(CapsEventType.TeleportFinish, this);

		// Instant Message callback
		_Client.Network.RegisterCallback(PacketType.ImprovedInstantMessage, this);
		// Chat callback
		_Client.Network.RegisterCallback(PacketType.ChatFromSimulator, this);
        // Script dialog callback
        _Client.Network.RegisterCallback(PacketType.ScriptDialog, this);
        // Script question callback
        _Client.Network.RegisterCallback(PacketType.ScriptQuestion, this);
        // Script URL callback
        _Client.Network.RegisterCallback(PacketType.LoadURL, this);
        // Movement complete callback
        _Client.Network.RegisterCallback(PacketType.AgentMovementComplete, this);
        // Health callback
        _Client.Network.RegisterCallback(PacketType.HealthMessage, this);
        // Money callback
        _Client.Network.RegisterCallback(PacketType.MoneyBalanceReply, this);
        //Agent update callback
        _Client.Network.RegisterCallback(PacketType.AgentDataUpdate, this);
        // Animation callback
        _Client.Network.RegisterCallback(PacketType.AvatarAnimation, this);
        // Object colliding into our agent callback
        _Client.Network.RegisterCallback(PacketType.MeanCollisionAlert, this);
        // Region Crossing
        _Client.Network.RegisterCallback(PacketType.CrossedRegion, this);
        _Client.Network.RegisterCallback(CapsEventType.CrossedRegion, this);
        // CAPS callbacks
        _Client.Network.RegisterCallback(CapsEventType.EstablishAgentCommunication, this);
        // Incoming Group Chat
        _Client.Network.RegisterCallback(CapsEventType.ChatterBoxInvitation, this);
        // Outgoing Group Chat Reply
        _Client.Network.RegisterCallback(CapsEventType.ChatterBoxSessionEventReply, this);
        _Client.Network.RegisterCallback(CapsEventType.ChatterBoxSessionStartReply, this);
        _Client.Network.RegisterCallback(CapsEventType.ChatterBoxSessionAgentListUpdates, this);
        // Alert Messages
        _Client.Network.RegisterCallback(PacketType.AlertMessage, this);
        // script control change messages, ie: when an in-world LSL script wants to take control of your agent.
        _Client.Network.RegisterCallback(PacketType.ScriptControlChange, this);
        // Camera Constraint (probably needs to move to AgentManagerCamera TODO:
        _Client.Network.RegisterCallback(PacketType.CameraConstraint, this);
        _Client.Network.RegisterCallback(PacketType.ScriptSensorReply, this);
        _Client.Network.RegisterCallback(PacketType.AvatarSitResponse, this);
	}

	@Override
	public void capsCallback(IMessage message, Simulator simulator) throws Exception
	{
		switch (message.getType())
		{
			case TeleportFailed:
				break;
		    case TeleportFinish:
				break;
		    case CrossedRegion:
				break;
		    case EstablishAgentCommunication:
				break;
		    case ChatterBoxInvitation:
				break;
		    case ChatterBoxSessionEventReply:
				break;
		    case ChatterBoxSessionStartReply:
				break;
		    case ChatterBoxSessionAgentListUpdates:
				break;
		}
	}

	@Override
	public void packetCallback(Packet packet, Simulator simulator)throws Exception
	{
		switch (packet.getType())
		{
			case TeleportStart:
			case TeleportProgress:
			case TeleportFailed:
			case TeleportFinish:
			case TeleportCancel:
			case TeleportLocal:
				TeleportHandler(packet, simulator);
				break;
			case MoneyBalanceReply:
				BalanceHandler(packet, simulator);
				break;
			case ImprovedInstantMessage:
				InstantMessageHandler(packet, simulator);
				break;
			case ChatFromSimulator:
				ChatHandler(packet, simulator);
				break;
			case CoarseLocationUpdate:
				CoarseLocationHandler(packet, simulator);
				break;
			case AgentMovementComplete:
				MovementCompleteHandler(packet, simulator);
				break;
			case HealthMessage:
				HealthHandler(packet, simulator);
				break;
		}
	}

    ///#region Chat and instant messages

    /**
     * Send a text message from the Agent to the Simulator
     * 
     * @param message A <see cref="string"/> containing the message
     * @param channel The channel to send the message on, 0 is the public channel. Channels above 0
     *                can be used however only scripts listening on the specified channel will see the message
     * @param type Denotes the type of message being sent, shout, whisper, etc.
     */
	public void Chat(String message, int channel, ChatType type) throws Exception {
		ChatFromViewerPacket chat = new ChatFromViewerPacket();
		chat.AgentData.AgentID = this.agentID;
		chat.AgentData.SessionID = this.sessionID;
		chat.ChatData.Channel = channel;
		chat.ChatData.setMessage(Helpers.StringToField(message));
		chat.ChatData.Type = (byte)type.getValue();

		_Client.Network.SendPacket(chat);
	}
	
	/**
     * Request any instant messages sent while the client was offline to be resent.
     * @throws Exception 
     */
    public final void RetrieveInstantMessages() throws Exception
    {
        RetrieveInstantMessagesPacket p = new RetrieveInstantMessagesPacket();
        p.AgentData.AgentID = _Client.Self.getAgentID();
        p.AgentData.SessionID = _Client.Self.getSessionID();
        _Client.Network.SendPacket(p);
    }

    /**
     * Send an Instant Message to another Avatar
     * 
     * @param target The recipients <see cref="UUID"/>
     * @param message A <see cref="string"/> containing the message to send
     */
	public void InstantMessage(UUID target, String message) throws Exception {
        InstantMessage(getName(), target, message, UUID.Zero, InstantMessageDialog.MessageFromAgent,
        		       InstantMessageOnline.Offline, null, null, null);
	}

    /**
     * Send an Instant Message to an existing group chat or conference chat
     * 
     * @param target The recipients <see cref="UUID"/>
     * @param message A <see cref="string"/> containing the message to send
     * @param imSessionID IM session ID (to differentiate between IM windows)
     */
	public void InstantMessage(UUID target, String message, UUID imSessionID) throws Exception {
        InstantMessage(getName(), target, message, imSessionID, InstantMessageDialog.MessageFromAgent,
        		       InstantMessageOnline.Offline, null, null, null);
	}
        
    /**
     * Send an Instant Message
     * 
     * @param fromName The name this IM will show up as being from
     * @param target Key of Avatar
     * @param message Text message being sent
     * @param imSessionID IM session ID (to differentiate between IM windows)
     * @param conferenceIDs IDs of sessions for a conference
     */
	public void InstantMessage(String fromName, UUID target, String message, UUID imSessionID, UUID[] conferenceIDs) throws Exception {
		byte[] binaryBucket = null;
		
		if (conferenceIDs != null && conferenceIDs.length > 0) {
			binaryBucket = new byte[16 * conferenceIDs.length];

			for (int i = 0; i < conferenceIDs.length; ++i) {
				System.arraycopy(conferenceIDs[i].getData(), 0, binaryBucket, i * 16, 16);
			}
		}

		// Send the message
		InstantMessage(fromName, target, message, imSessionID, InstantMessageDialog.MessageFromAgent,
				       InstantMessageOnline.Offline, null, null, binaryBucket);
	}

    /** 
     * Send an Instant Message
     * 
     * @param fromName The name this IM will show up as being from
     * @param target Key of Avatar
     * @param message Text message being sent
     * @param imSessionID IM session ID (to differentiate between IM windows)
     * @param dialog Type of instant message to send
     * @param offline Whether to IM offline avatars as well
     * @param position Senders Position
     * @param regionID RegionID Sender is In
     * @param binaryBucket Packed binary data that is specific to the dialog type
     * 
     * @throws Exception 
     * @throws UnsupportedEncodingException 
     */
    public final void InstantMessage(String fromName, UUID target, String message, UUID imSessionID,
    		                         InstantMessageDialog dialog, InstantMessageOnline offline, Vector3 position,
    		                         UUID regionID, byte[] binaryBucket) throws Exception
    {
        if (!target.equals(UUID.Zero))
        {
            ImprovedInstantMessagePacket im = new ImprovedInstantMessagePacket();

            if (imSessionID == null || imSessionID.equals(UUID.Zero) || imSessionID.equals(getAgentID()))
            {
                imSessionID = getAgentID().equals(target) ? getAgentID() : UUID.XOr(target, getAgentID());
            }

            im.AgentData.AgentID = getAgentID();
            im.AgentData.SessionID = getSessionID();

            im.MessageBlock.Dialog = dialog.getValue();;
            im.MessageBlock.setFromAgentName(Helpers.StringToBytes(fromName));
            im.MessageBlock.FromGroup = false;
            im.MessageBlock.ID = imSessionID;
            im.MessageBlock.setMessage(Helpers.StringToBytes(message));
            im.MessageBlock.Offline = offline.getValue();
            im.MessageBlock.ToAgentID = target;

            // These fields are mandatory, even if we don't have valid values for them
            if (position == null)
            	position = getSimPosition();
            im.MessageBlock.Position = position;

            // Allow region id to be correctly set by caller or fetched from _Client.
            if (regionID == null)
            	regionID = _Client.Network.getCurrentSim().RegionID;
            im.MessageBlock.RegionID = regionID;

            if (binaryBucket != null)
            {
                im.MessageBlock.setBinaryBucket(binaryBucket);
            }
            else
            {
                im.MessageBlock.setBinaryBucket(Helpers.EmptyBytes);
            }

            // Send the message
            _Client.Network.SendPacket(im);
        }
        else
        {
            Logger.Log(String.format("Suppressing instant message \"%s\" to UUID.Zero", message), LogLevel.Error, _Client);
        }
    }

    /** 
     * Send an Instant Message to a group
     * 
     * @param groupID {@link UUID} of the group to send message to
     * @param message Text Message being sent.
     * @throws Exception 
     * @throws UnsupportedEncodingException 
     */
    public final void InstantMessageGroup(UUID groupID, String message) throws UnsupportedEncodingException, Exception
    {
        InstantMessageGroup(getName(), groupID, message);
    }

    /**
     * Send an Instant Message to a group the agent is a member of
     * 
     * @param fromName The name this IM will show up as being from
     * @param groupID {@link UUID} of the group to send message to
     * @param message Text message being sent
     * @throws Exception 
     * @throws UnsupportedEncodingException 
     */
    public final void InstantMessageGroup(String fromName, UUID groupID, String message) throws UnsupportedEncodingException, Exception
    {
        synchronized (GroupChatSessions)
        {
            if (GroupChatSessions.containsKey(groupID))
            {
                InstantMessage(fromName, groupID, message, groupID, InstantMessageDialog.SessionSend,
                		       InstantMessageOnline.Online, Vector3.Zero, UUID.Zero, Helpers.StringToBytes("\0"));
            }
            else
            {
                Logger.Log("No Active group chat session appears to exist, use RequestJoinGroupChat() to join one", LogLevel.Error, _Client);
            }
        }
    }

    /**
     * Send a request to join a group chat session
     * 
     * @param groupID {@link UUID} of Group to leave
     */
    public final void RequestJoinGroupChat(UUID groupID) throws Exception
    {
        InstantMessage(getName(), groupID, Helpers.EmptyString, groupID, InstantMessageDialog.SessionGroupStart,
 		       InstantMessageOnline.Online, null, UUID.Zero, null);
    }

    /**
     * Exit a group chat session. This will stop further Group chat messages
     * from being sent until session is rejoined.
     * 
     * @param groupID {@link UUID} of Group chat session to leave
     * @throws Exception 
     */
    public final void RequestLeaveGroupChat(UUID groupID) throws Exception
    {
        InstantMessage(getName(), groupID, Helpers.EmptyString, groupID, InstantMessageDialog.SessionDrop,
  		               InstantMessageOnline.Online, Vector3.Zero, UUID.Zero, null);

        synchronized (GroupChatSessions)
        {
            if (GroupChatSessions.containsKey(groupID))
            {
                GroupChatSessions.remove(groupID);
            }
        }
    }

    /**
     * Reply to script dialog questions.
     * 
     * @param channel Channel initial request came on
	 * @param buttonIndex Index of button you're "clicking"
     * @param buttonlabel Label of button you're "clicking"
     * @param objectID {@link UUID} of Object that sent the dialog request
     * {@link OnScriptDialog}
     * 
     * @throws Exception 
     */
    public final void ReplyToScriptDialog(int channel, int buttonIndex, String buttonlabel, UUID objectID) throws Exception
    {
        ScriptDialogReplyPacket reply = new ScriptDialogReplyPacket();

        reply.AgentData.AgentID = _Client.Self.getAgentID();
        reply.AgentData.SessionID = _Client.Self.getSessionID();

        reply.Data.ButtonIndex = buttonIndex;
        reply.Data.setButtonLabel(Helpers.StringToBytes(buttonlabel));
        reply.Data.ChatChannel = channel;
        reply.Data.ObjectID = objectID;

        _Client.Network.SendPacket(reply);
    }

    /**
     * Accept invite for to a chatterbox session
     * 
     * @param session_id {@link UUID} of session to accept invite to
     * @throws Exception 
     */
    public final void ChatterBoxAcceptInvite(UUID session_id) throws Exception
    {
        URI uri = _Client.Network.getCapabilityURI("ChatSessionRequest");
        if (uri != null)
        {
            ChatSessionAcceptInvitation acceptInvite = _Client.Messages.new ChatSessionAcceptInvitation();
 
            acceptInvite.SessionID = session_id;

            CapsClient request = new CapsClient(uri);
            request.BeginGetResponse(acceptInvite.Serialize(), OSDFormat.Xml, _Client.Settings.CAPS_TIMEOUT);

            synchronized (GroupChatSessions)
            {
                if (!GroupChatSessions.containsKey(session_id))
                {
                    GroupChatSessions.put(session_id, new ArrayList<ChatSessionMember>());
                }
            }
        }
        else
        {
            throw new Exception("ChatSessionRequest capability is not currently available");
        }

    }

    /**
     * Start a friends conference
     * 
     * @param participants {@link UUID} List of UUIDs to start a conference with
     * @param tmp_session_id the temportary session ID returned in the <see cref="OnJoinedGroupChat"/> callback>
     * @throws Exception 
     */
    public final void StartIMConference(UUID[] participants, UUID tmp_session_id) throws Exception
    {
        URI url = _Client.Network.getCapabilityURI("ChatSessionRequest");
        if (url != null)
        {
            ChatSessionRequestStartConference startConference = _Client.Messages.new ChatSessionRequestStartConference();
            
            startConference.AgentsBlock = new UUID[participants.length];
            for (int i = 0; i < participants.length; i++)
            {
                startConference.AgentsBlock[i] = participants[i];
            }
            startConference.SessionID = tmp_session_id;

            CapsClient request = new CapsClient(url);
            request.BeginGetResponse(startConference.Serialize(), OSDFormat.Xml, _Client.Settings.CAPS_TIMEOUT);
        }
        else
        {
            throw new Exception("ChatSessionRequest capability is not currently available");
        }
    }
    // #endregion
    
    ///#region Viewer Effects

    /** 
     * Start a particle stream between an agent and an object
     * 
     * @param sourceAvatar {@link UUID} Key of the source agent
     * @param targetObject {@link UUID} Key of the target object
     * @param globalOffset
     * @param type The type from the {@link T:PointAtType} enum
     * @param effectID A unique {@link UUID} for this effect
     * @throws Exception 
*/
    public final void PointAtEffect(UUID sourceAvatar, UUID targetObject, Vector3d globalOffset, PointAtType type, UUID effectID) throws Exception
    {
        ViewerEffectPacket effect = new ViewerEffectPacket();

        effect.AgentData.AgentID = _Client.Self.getAgentID();
        effect.AgentData.SessionID = _Client.Self.getSessionID();

        effect.Effect = new ViewerEffectPacket.EffectBlock[1];
        effect.Effect[0] = effect.new EffectBlock();
        effect.Effect[0].AgentID = _Client.Self.getAgentID();
        effect.Effect[0].Color = new byte[4];
        effect.Effect[0].Duration = (type == PointAtType.Clear) ? 0.0f : Float.MAX_VALUE / 4.0f;
        effect.Effect[0].ID = effectID;
        effect.Effect[0].Type = EffectType.PointAt.getValue();

        byte[] typeData = new byte[57];
        if (!sourceAvatar.equals(UUID.Zero))
        {
            sourceAvatar.ToBytes(typeData, 0);
        }
        if (!targetObject.equals(UUID.Zero))
        {
            targetObject.ToBytes(typeData, 16);
        }
        globalOffset.ToBytes(typeData, 32);
        typeData[56] = type.getValue();

        effect.Effect[0].setTypeData(typeData);

        _Client.Network.SendPacket(effect);
    }

    /** 
     * Start a particle stream between an agent and an object
     * 
     * @param sourceAvatar {@link UUID} Key of the source agent
     * @param targetObject {@link UUID} Key of the target object
     * @param globalOffset A {@link Vector3d} representing the beams offset from the source
     * @param type A {@link T:PointAtType} which sets the avatars lookat animation
     * @param effectID {@link UUID} of the Effect
     * @throws Exception 
     */
    public final void LookAtEffect(UUID sourceAvatar, UUID targetObject, Vector3d globalOffset, LookAtType type, UUID effectID) throws Exception
    {
        ViewerEffectPacket effect = new ViewerEffectPacket();

        effect.AgentData.AgentID = _Client.Self.getAgentID();
        effect.AgentData.SessionID = _Client.Self.getSessionID();

        float duration;

        switch (type)
        {
            case Clear:
                duration = 2.0f;
                break;
            case Hover:
                duration = 1.0f;
                break;
            case FreeLook:
                duration = 2.0f;
                break;
            case Idle:
                duration = 3.0f;
                break;
            case AutoListen:
            case Respond:
                duration = 4.0f;
                break;
            case None:
            case Select:
            case Focus:
            case Mouselook:
                duration = Float.MAX_VALUE / 2.0f;
                break;
            default:
                duration = 0.0f;
                break;
        }

        effect.Effect = new ViewerEffectPacket.EffectBlock[1];
        effect.Effect[0] = effect.new EffectBlock();
        effect.Effect[0].AgentID = _Client.Self.getAgentID();
        effect.Effect[0].Color = new byte[4];
        effect.Effect[0].Duration = duration;
        effect.Effect[0].ID = effectID;
        effect.Effect[0].Type = EffectType.LookAt.getValue();

        byte[] typeData = new byte[57];
        sourceAvatar.ToBytes(typeData, 0);
        targetObject.ToBytes(typeData, 16);
        globalOffset.ToBytes(typeData, 32);
        typeData[56] = type.getValue();

        effect.Effect[0].setTypeData(typeData);

        _Client.Network.SendPacket(effect);
    }

    /**
     * Create a particle beam between an avatar and an primitive 
     * 
     * @param sourceAvatar The ID of source avatar
     * @param targetObject The ID of the target primitive
     * @param globalOffset global offset
     * @param color A <see cref="Color4"/> object containing the combined red, green, blue and alpha 
     * color values of particle beam
     * @param duration a float representing the duration the parcicle beam will last
     * @param effectID A Unique ID for the beam
     * {@link ViewerEffectPacket}
     * @throws Exception 
     */
    public final void BeamEffect(UUID sourceAvatar, UUID targetObject, Vector3d globalOffset, Color4 color, float duration, UUID effectID) throws Exception
    {
        ViewerEffectPacket effect = new ViewerEffectPacket();

        effect.AgentData.AgentID = _Client.Self.getAgentID();
        effect.AgentData.SessionID = _Client.Self.getSessionID();

        effect.Effect = new ViewerEffectPacket.EffectBlock[1];
        effect.Effect[0] = effect.new EffectBlock();
        effect.Effect[0].AgentID = _Client.Self.getAgentID();
        effect.Effect[0].Color = color.GetBytes();
        effect.Effect[0].Duration = duration;
        effect.Effect[0].ID = effectID;
        effect.Effect[0].Type = EffectType.Beam.getValue();

        byte[] typeData = new byte[56];
        sourceAvatar.ToBytes(typeData, 0);
        targetObject.ToBytes(typeData, 16);
        globalOffset.ToBytes(typeData, 32);

        effect.Effect[0].setTypeData(typeData);

        _Client.Network.SendPacket(effect);
    }

    /** 
     * Create a particle swirl around a target position using a {@link ViewerEffectPacket} packet
     * 
     * @param globalOffset global offset
     * @param color A <see cref="Color4"/> object containing the combined red, green, blue and alpha 
     * color values of particle beam
     * @param duration a float representing the duration the parcicle beam will last
     * @param effectID A Unique ID for the beam
     * @throws Exception 
     */
    public final void SphereEffect(Vector3d globalOffset, Color4 color, float duration, UUID effectID) throws Exception
    {
        ViewerEffectPacket effect = new ViewerEffectPacket();

        effect.AgentData.AgentID = _Client.Self.getAgentID();
        effect.AgentData.SessionID = _Client.Self.getSessionID();

        effect.Effect = new ViewerEffectPacket.EffectBlock[1];
        effect.Effect[0] = effect.new EffectBlock();
        effect.Effect[0].AgentID = _Client.Self.getAgentID();
        effect.Effect[0].Color = color.GetBytes();
        effect.Effect[0].Duration = duration;
        effect.Effect[0].ID = effectID;
        effect.Effect[0].Type = EffectType.Sphere.getValue();

        byte[] typeData = new byte[56];
        UUID.Zero.ToBytes(typeData, 0);
        UUID.Zero.ToBytes(typeData, 16);
        globalOffset.ToBytes(typeData, 32);

        effect.Effect[0].setTypeData(typeData);

        _Client.Network.SendPacket(effect);
    }
    ///#endregion Viewer Effects
    

    /**
     * Set the height and the width of your avatar. This is used to scale
     * 
     * @param height New height of the avatar
     * @param width >New width of the avatar
     * @throws Exception
     */
	public void SetHeightWidth(short height, short width) throws Exception {
		AgentHeightWidthPacket heightwidth = new AgentHeightWidthPacket();
		heightwidth.AgentData.AgentID = this.agentID;
		heightwidth.AgentData.SessionID = this.sessionID;
		heightwidth.AgentData.CircuitCode = _Client.Network.getCircuitCode();
		heightwidth.HeightWidthBlock.GenCounter = HeightWidthGenCounter++;
		heightwidth.HeightWidthBlock.Height = height;
		heightwidth.HeightWidthBlock.Width = width;
        heightwidth.HeightWidthBlock.GenCounter = heightWidthGenCounter++;

		_Client.Network.SendPacket(heightwidth);
	}

    /** 
     * Give Money to destination Avatar
     * 
     * @param target UUID of the Target Avatar
     * @param amount Amount in L$
     * @throws Exception 
     */
    public final void GiveAvatarMoney(UUID target, int amount) throws Exception
    {
        GiveMoney(target, amount, Helpers.EmptyString, MoneyTransactionType.Gift, TransactionFlags.None);
    }

    /** 
     * Give Money to destination Avatar
     * 
     * @param target UUID of the Target Avatar
     * @param amount Amount in L$
     * @param description Description that will show up in the recipients transaction history
     * @throws Exception 
     */
    public final void GiveAvatarMoney(UUID target, int amount, String description) throws Exception
    {
        GiveMoney(target, amount, description, MoneyTransactionType.Gift, TransactionFlags.None);
    }

    /** 
     * Give L$ to an object
     * 
     * @param target object {@link UUID} to give money to
     * @param amount amount of L$ to give
     * @param objectName name of object
     * @throws Exception 
     */
    public final void GiveObjectMoney(UUID target, int amount, String objectName) throws Exception
    {
        GiveMoney(target, amount, objectName, MoneyTransactionType.PayObject, TransactionFlags.None);
    }

    /** 
     * Give L$ to a group
     * 
     * @param target group {@link UUID} to give money to
     * @param amount amount of L$ to give
     * @throws Exception 
     */
    public final void GiveGroupMoney(UUID target, int amount) throws Exception
    {
        GiveMoney(target, amount, Helpers.EmptyString, MoneyTransactionType.Gift, TransactionFlags.DestGroup);
    }

    /** 
     * Give L$ to a group
     * 
     * @param target group {@link UUID} to give money to
     * @param amount amount of L$ to give
     * @param description description of transaction
     * @throws Exception 
     */
    public final void GiveGroupMoney(UUID target, int amount, String description) throws Exception
    {
        GiveMoney(target, amount, description, MoneyTransactionType.Gift, TransactionFlags.DestGroup);
    }

    /** 
     * Pay texture/animation upload fee
     * @throws Exception 
     * 
     */
    public final void PayUploadFee() throws Exception
    {
        GiveMoney(UUID.Zero, _Client.Settings.getUPLOAD_COST(), Helpers.EmptyString, MoneyTransactionType.UploadCharge, TransactionFlags.None);
    }

    /** 
     * Pay texture/animation upload fee
     * 
     * @param description description of the transaction
     * @throws Exception 
     */
    public final void PayUploadFee(String description) throws Exception
    {
        GiveMoney(UUID.Zero, _Client.Settings.getUPLOAD_COST(), description, MoneyTransactionType.UploadCharge, TransactionFlags.None);
    }

    /** 
     * Give Money to destination Object or Avatar
     * 
     * @param target UUID of the Target Object/Avatar
     * @param amount Amount in L$
     * @param description Reason (Optional normally)
     * @param type The type of transaction
     * @param flags Transaction flags, mostly for identifying group
     * transactions
     * @throws Exception 
*/
    public final void GiveMoney(UUID target, int amount, String description, MoneyTransactionType type, byte flags) throws Exception
    {
        MoneyTransferRequestPacket money = new MoneyTransferRequestPacket();
        money.AgentData.AgentID = this.agentID;
        money.AgentData.SessionID = this.sessionID;
        money.MoneyData.setDescription(Helpers.StringToBytes(description));
        money.MoneyData.DestID = target;
        money.MoneyData.SourceID = this.agentID;
        money.MoneyData.TransactionType = type.getValue();
        money.MoneyData.AggregatePermInventory = 0; // This is weird, apparently always set to zero though
        money.MoneyData.AggregatePermNextOwner = 0; // This is weird, apparently always set to zero though
        money.MoneyData.Flags = flags;
        money.MoneyData.Amount = amount;

        _Client.Network.SendPacket(money);
    }
	
    // #region Gestures
    /** 
     * Plays a gesture
     * 
     * @param gestureID Asset {@link UUID} of the gesture
     */
    public final void PlayGesture(final UUID gestureID)
    {
    	if (_Client.Assets == null)
    		throw new RuntimeException("Can't play a gesture without the asset manager being instantiated.");

    	// First fetch the guesture
        // TODO: implement waiting for all animations to end that were triggered during playing of this guesture sequence
        Thread thread = new Thread()
        {
        	@Override
			public void run()
        	{
                AssetGesture gesture = null;
                synchronized (gestureCache)
                {
                    if (gestureCache.containsKey(gestureID))
                    {
                        gesture = gestureCache.get(gestureID);
                    }
                }
                
                if (gesture == null)
                {
                    final TimeoutEvent<AssetGesture> gotAsset = new TimeoutEvent<AssetGesture>();

                    class AssetDownloadCallback extends AssetReceivedCallback
                    {
                    	public AssetDownloadCallback()
                    	{
                    		_Client.Assets.super();
                    	}
                    	
						@Override
						public void callback(AssetDownload transfer, AssetItem asset)
						{
                        	if (transfer.Success)
                            {
                                gotAsset.set((AssetGesture)asset);
                            }
                        	else
                        	{
                                gotAsset.set(null);
                        	}
                        }
                    }
                    
                    try
					{
						_Client.Assets.RequestAsset(gestureID, AssetType.Gesture, true, new AssetDownloadCallback());
	                    gesture = gotAsset.waitOne(30 * 1000);
	                    if (gesture != null && gesture.Decode())
	                    {
	                        synchronized (gestureCache)
	                        {
	                            if (!gestureCache.containsKey(gestureID))
	                            {
	                                gestureCache.put(gestureID, gesture);
	                            }
	                        }
	                    }
					}
					catch (Exception ex) {}
                }

                // We got it, now we play it
                if (gesture != null)
                {
                    for (int i = 0; i < gesture.Sequence.size(); i++)
                    {
                        GestureStep step = gesture.Sequence.get(i);
                        try
                        {
                            switch (step.getGestureStepType())
                            {
                                case Chat:
                                    Chat(((GestureStepChat)step).Text, 0, ChatType.Normal);
                                    break;
                                case Animation:
                                    GestureStepAnimation anim = (GestureStepAnimation)step;
                                    if (anim.AnimationStart)
                                    {
                                        if (SignaledAnimations.containsKey(anim.ID))
                                        {
                                            AnimationStop(anim.ID, true);
                                        }
                                        AnimationStart(anim.ID, true);
                                    }
                                    else
                                    {
                                        AnimationStop(anim.ID, true);
                                    }
                                    break;
                                case Sound:
                                	// TODO: Add Sound Manager
                                	// _Client.Sound.PlaySound(((GestureStepSound)step).ID);
                                    break;
                                case Wait:
                                    GestureStepWait wait = (GestureStepWait)step;
                                    if (wait.WaitForTime)
                                    {
                                        Thread.sleep((int)(1000f * wait.WaitTime));
                                    }
                                    if (wait.WaitForAnimation)
                                    {
                                    	
                                    }
                                    break;
                            }
                        }
                        catch (Exception ex)
                        {
                        	
                        }
                    }
                }
        	}
        };

        thread.setDaemon(true);
        thread.setName("Gesture thread: " + gestureID);
        thread.start();
    }

    /* 
     * Mark gesture active
     * 
     * @param invID Inventory {@link UUID} of the gesture
     * @param assetID Asset {@link UUID} of the gesture
*/
    public final void ActivateGesture(UUID invID, UUID assetID) throws Exception
    {
        ActivateGesturesPacket p = new ActivateGesturesPacket();

        p.AgentData.AgentID = this.agentID;
        p.AgentData.SessionID = this.sessionID;
        p.AgentData.Flags = 0x00;

        ActivateGesturesPacket.DataBlock b = p.new DataBlock();
        b.ItemID = invID;
        b.AssetID = assetID;
        b.GestureFlags = 0x00;

        p.Data = new ActivateGesturesPacket.DataBlock[1];
        p.Data[0] = b;

        _Client.Network.SendPacket(p);

    }

    /** 
     * Mark gesture inactive
     * 
     * @param invID Inventory {@link UUID} of the gesture
     */
    public final void DeactivateGesture(UUID invID) throws Exception
    {
        DeactivateGesturesPacket p = new DeactivateGesturesPacket();

        p.AgentData.AgentID = this.agentID;
        p.AgentData.SessionID = this.sessionID;
        p.AgentData.Flags = 0x00;

        DeactivateGesturesPacket.DataBlock b = p.new DataBlock();
        b.ItemID = invID;
        b.GestureFlags = 0x00;

        p.Data = new DeactivateGesturesPacket.DataBlock[1];
        p.Data[0] = b;

        _Client.Network.SendPacket(p);
    }
    // #endregion
    
    // #region Animations

    /** 
     * Send an AgentAnimation packet that toggles a single animation on
     * 
     * @param animation The {@link UUID} of the animation to start playing
     * @param reliable Whether to ensure delivery of this packet or not
     * @throws Exception 
     */
    public final void AnimationStart(UUID animation, boolean reliable) throws Exception
    {
        HashMap<UUID, Boolean> animations = new HashMap<UUID, Boolean>();
        animations.put(animation, true);

        Animate(animations, reliable);
    }

    /** 
     * Send an AgentAnimation packet that toggles a single animation off
     * 
     * @param animation The {@link UUID} of a  currently playing animation to stop playing
     * @param reliable Whether to ensure delivery of this packet or not
     * @throws Exception 
     */
    public final void AnimationStop(UUID animation, boolean reliable) throws Exception
    {
        HashMap<UUID, Boolean> animations = new HashMap<UUID, Boolean>();
        animations.put(animation, false);

        Animate(animations, reliable);
    }

    /** 
     * Send an AgentAnimation packet that will toggle animations on or off
     * 
     * @param animations A list of animation {@link UUID} s, and whether to turn that animation on or off
     * @param reliable Whether to ensure delivery of this packet or not
     * @throws Exception 
     */
    public final void Animate(HashMap<UUID, Boolean> animations, boolean reliable) throws Exception
    {
        AgentAnimationPacket animate = new AgentAnimationPacket();
        animate.getHeader().setReliable(reliable);

        animate.AgentData.AgentID = _Client.Self.getAgentID();
        animate.AgentData.SessionID = _Client.Self.getSessionID();
        animate.AnimationList = new AgentAnimationPacket.AnimationListBlock[animations.size()];
        int i = 0;

        for (Entry<UUID, Boolean> animation : animations.entrySet())
        {
            animate.AnimationList[i] = animate.new AnimationListBlock();
            animate.AnimationList[i].AnimID = animation.getKey();
            animate.AnimationList[i].StartAnim = animation.getValue();
            i++;
        }

        // TODO: Implement support for this
        animate.PhysicalAvatarEventList = new AgentAnimationPacket.PhysicalAvatarEventListBlock[0];
        for (i = 0; i < 0; i++)
        {
        	animate.PhysicalAvatarEventList[i] = animate.new PhysicalAvatarEventListBlock();
        	animate.PhysicalAvatarEventList[i].setTypeData(null);
        }
        _Client.Network.SendPacket(animate);
    }
    // #endregion Animations

    // #region Teleport

    /**
     * Teleports agent to their stored home location
     * 
     * @return true on successful teleport to home location
     * @throws Exception 
     */
    public final boolean GoHome() throws Exception
    {
        return Teleport(UUID.Zero, _Client.Settings.TELEPORT_TIMEOUT);
    }

    /**
     * Teleport agent to a landmark
     * 
     * @param landmark {@link UUID} of the landmark to teleport agent to
     * @return true on success, false on failure
     * @throws Exception 
     */
    public final boolean Teleport(UUID landmark) throws Exception
    {
        return Teleport(landmark, _Client.Settings.TELEPORT_TIMEOUT);
    }
    
    public final boolean Teleport(UUID landmark, long timeout) throws Exception
    {
 		// Start the timeout check
        teleportTimeout.reset();
        RequestTeleport(landmark);
        TeleportStatus teleportStat = teleportTimeout.waitOne(timeout);
		if (teleportStat == null)
        {
            teleportStat = TeleportStatus.Failed;
            OnTeleport.dispatch(new TeleportCallbackArgs("Teleport timed out.", teleportStat, 0));
        }
        return (teleportStat == TeleportStatus.Finished);
    }

    /**
     * Teleport agent to a landmark
     * 
     * @param landmark {@link UUID} of the landmark to teleport agent to
     * @throws Exception 
     */
    public final void RequestTeleport(UUID landmark) throws Exception
    {
        if (_Client.Network.getIsEventQueueRunning())
        {
            TeleportLandmarkRequestPacket p = new TeleportLandmarkRequestPacket();
            p.Info.AgentID = _Client.Self.getAgentID();
            p.Info.SessionID = _Client.Self.getSessionID();
            p.Info.LandmarkID = landmark;

            Logger.Log("Requesting teleport to simulator " + landmark.toString(), LogLevel.Info);

            _Client.Network.SendPacket(p);
        }
        else
        {
        	TeleportStatus teleportStat = TeleportStatus.Failed;
            teleportTimeout.set(teleportStat);
            OnTeleport.dispatch(new TeleportCallbackArgs("CAPS event queue is not running", teleportStat, 0));
        }
    }
    
    /**
     * Start a Teleport request asynchronously. You can either use the callback handler to wait for any message or
     * the returned timeoutEvent to abort the request prematurely if desired.
     * 
     * <example>
     *      // Using a callback handler
     *      final CallbackHandler<TeleportCallbackArgs> handler = new CallbackHandler<TeleportCallbackArgs>()
     *      {
     *			public void callback(TeleportCallbackArgs args)
     *          {
     *              // Do something with the callback args: args.status, args.message, args.flags
     *              switch (args.status)
     *              {
     *                  case Start:
     *                  case Progress:
     *                      break;
     *                  case Canceled:
     *                  case Failed:
     *                  case Finished:
     *                       break;
     *              }
     *          }
     *      }
     *      BeginTeleport(handle, pos, handler);
     *      
     *      // Using the timeout event
     *      TimeoutEvent<TeleportStatus> timo = BeginTeleport(handle, pos, null);
     *      TeleportStatus stat = timo.waitms(timeout);
     *      if (stat == null)
     *      {
     *      	// The timeout occured
     *      }
     * </example>
     * 
     * @param regionHandle The region handle of the region to teleport to
     * @param position The position inside the region to teleport to
     * @param tc The callback handler that will be invoked with progress and final status information
     * @return A timout event that can be used to wait for the 
     * @throws Exception
     */
    public TimeoutEvent<TeleportStatus> BeginTeleport(long regionHandle, Vector3 position, CallbackHandler<TeleportCallbackArgs> tc) throws Exception
	{
		return BeginTeleport(regionHandle, position, new Vector3(position.X + 1.0f, position.Y, position.Z), tc);
	}

    /**
     * Start a Teleport request asynchronously. You can either use the callback handler to wait for any message or
     * the returned timeoutEvent to abort the request prematurely if desired.
     * 
     * @param regionHandle The region handle of the region to teleport to
     * @param position The position inside the region to teleport to
     * @param lookAt The direction in which to look at when arriving
     * @param tc The callback handler that will be invoked with progress and final status information
     * @return A timout event that can be used to wait for the 
     * @throws Exception
     */
	public TimeoutEvent<TeleportStatus> BeginTeleport(long regionHandle, Vector3 position, Vector3 lookAt, CallbackHandler<TeleportCallbackArgs> tc) throws Exception
	{
		if (tc != null)
			OnTeleport.add(tc, true);

		// Start the timeout check
		teleportTimeout.reset();
		RequestTeleport(regionHandle, position, lookAt);
		return teleportTimeout;
	}
	
    /**
     * Request teleport to a another simulator
     * 
     * @param regionHandle handle of region to teleport agent to
     * @param position {@link Vector3} position in destination sim to teleport to
     * @throws Exception 
     */
    public final void RequestTeleport(long regionHandle, Vector3 position) throws Exception
    {
        RequestTeleport(regionHandle, position, new Vector3(0.0f, 1.0f, 0.0f));
    }

    /** Request teleport to a another simulator
     * 
     *  @param regionHandle handle of region to teleport agent to
     *  @param position {@link Vector3} position in destination sim to teleport to
     *  @param lookAt {@link Vector3} direction in destination sim agent will look at
     *  @throws Exception 
     */
    public final void RequestTeleport(long regionHandle, Vector3 position, Vector3 lookAt) throws Exception
    {
        if (_Client.Network.getIsEventQueueRunning())
        {
            TeleportLocationRequestPacket teleport = new TeleportLocationRequestPacket();
            teleport.AgentData.AgentID = _Client.Self.getAgentID();
            teleport.AgentData.SessionID = _Client.Self.getSessionID();
            teleport.Info.LookAt = lookAt;
            teleport.Info.Position = position;
            teleport.Info.RegionHandle = regionHandle;

            Logger.Log("Requesting teleport to region handle " + ((Long)regionHandle).toString(), LogLevel.Info, _Client);

            _Client.Network.SendPacket(teleport);
        }
        else
        {
        	TeleportStatus teleportStat = TeleportStatus.Failed;
            teleportTimeout.set(teleportStat);
            OnTeleport.dispatch(new TeleportCallbackArgs("CAPS event queue is not running", teleportStat, 0));
        }
    }

    /** Teleport agent to another region
     * 
     *  @param regionHandle handle of region to teleport agent to
     *  @param position {@link Vector3} position in destination sim to teleport to
     *  @return true on success, false on failure
     */
	public boolean Teleport(long regionHandle, Vector3 position) throws Exception
	{
		return Teleport(regionHandle, position, new Vector3(0.0f, 1.0f, 0.0f));
	}

    /** Teleport agent to another region
     * 
     *  @param regionHandle handle of region to teleport agent to
     *  @param position {@link Vector3} position in destination sim to teleport to
     *  @param lookAt 
     *  @return true on success, false on failure
     */
	public boolean Teleport(long regionHandle, Vector3 position, Vector3 lookAt) throws Exception {
		// Start the timeout check
		teleportTimeout.reset();
		RequestTeleport(regionHandle, position, lookAt);

		TeleportStatus teleportStat = teleportTimeout.waitOne(_Client.Settings.TELEPORT_TIMEOUT);
		if (teleportStat == null)
		{
			teleportStat = TeleportStatus.Failed;
			OnTeleport.dispatch(new TeleportCallbackArgs("Teleport timed out.", teleportStat, 0));
		}
		return (teleportStat == TeleportStatus.Finished);
	}

    /**
     * Attempt to look up a simulator name and teleport to the discovered destination
     * 
     * @param simName Region name to look up
     * @param position Position to teleport to
     * @return True if the lookup and teleport were successful, otherwise
     */
	public boolean Teleport(String simName, Vector3 position) throws Exception {
		return Teleport(simName, position, new Vector3(0, 1.0F, 0));
	}

    /**
     * Attempt to look up a simulator name and teleport to the discovered destination
     * 
     * @param simName Region name to look up
     * @param position Position to teleport to
     * @param lookAt Target to look at
     * @return True if the lookup and teleport were successful, false otherwise
     * @throws Exception
     */
	public boolean Teleport(String simName, Vector3 position, Vector3 lookAt) throws Exception {
        if (_Client.Network.getCurrentSim() == null)
        {
            return false;
        }

        if (!simName.equals(_Client.Network.getCurrentSim().getName()))
        {
            // Teleporting to a foreign sim
            GridRegion region = _Client.Grid.GetGridRegion(simName, GridLayerType.Objects);
            if (region != null)
            {
                return Teleport(region.RegionHandle, position, lookAt);
            }

            TeleportStatus teleportStat = TeleportStatus.Failed;
            OnTeleport.dispatch(new TeleportCallbackArgs("Unable to resolve name: " + simName, teleportStat, 0));
            return false;
        }

        // Teleporting to the sim we're already in
        return Teleport(_Client.Network.getCurrentSim().getHandle(), position, lookAt);
	}

    /**
     * Send a teleport lure to another avatar with custom invitation message
     * 
     * @param targetID target avatars {@link UUID} to lure
     * @param message custom message to send with invitation
     * @throws Exception 
     */
    public final void SendTeleportLure(UUID targetID, String message) throws Exception
    {
        StartLurePacket p = new StartLurePacket();
        p.AgentData.AgentID = _Client.Self.agentID;
        p.AgentData.SessionID = _Client.Self.sessionID;
        p.Info.LureType = 0;
        p.Info.setMessage(Helpers.StringToBytes(message));
        p.TargetData = new StartLurePacket.TargetDataBlock[1];
        p.TargetData[0].TargetID = targetID;
        _Client.Network.SendPacket(p);
    }

    /** 
     * Acknowledge agent movement complete
     * 
     * @param simulator {@link T:OpenMetaverse.Simulator} Object
     * @throws Exception 
     */
	public void CompleteAgentMovement(Simulator simulator) throws Exception
	{
		CompleteAgentMovementPacket move = new CompleteAgentMovementPacket();

		move.AgentData.AgentID = this.agentID;
		move.AgentData.SessionID = this.sessionID;
		move.AgentData.CircuitCode = _Client.Network.getCircuitCode();

		simulator.SendPacket(move);
	}

	public void SendMovementUpdate(boolean reliable, Simulator simulator) throws Exception
	{
		_Movement.SendUpdate(reliable, simulator);
	}
	
    /** 
     * Reply to script permissions request
     * 
     * @param simulator {@link T:OpenMetaverse.Simulator} Object
     * @param itemID {@link UUID} of the itemID requesting permissions
     * @param taskID {@link UUID} of the taskID requesting permissions
     * @param permissions {@link OpenMetaverse.ScriptPermission} list of permissions to allow
     * @throws Exception 
     */
    public final void ScriptQuestionReply(Simulator simulator, UUID itemID, UUID taskID, int permissions) throws Exception
    {
        ScriptAnswerYesPacket yes = new ScriptAnswerYesPacket();
        yes.AgentData.AgentID = _Client.Self.getAgentID();
        yes.AgentData.SessionID = _Client.Self.getSessionID();
        yes.Data.ItemID = itemID;
        yes.Data.TaskID = taskID;
        yes.Data.Questions = permissions;

        simulator.SendPacket(yes);
    }

    /** 
     * Respond to a group invitation by either accepting or denying it
     * 
     * @param groupID UUID of the group (sent in the AgentID field of the invite message)
     * @param imSessionID IM Session ID from the group invitation message
     * @param accept Accept the group invitation or deny it
     * @throws Exception 
     */
    public final void GroupInviteRespond(UUID groupID, UUID imSessionID, boolean accept) throws Exception
    {
        InstantMessage(getName(), groupID, Helpers.EmptyString, imSessionID, accept ? InstantMessageDialog.GroupInvitationAccept : InstantMessageDialog.GroupInvitationDecline, InstantMessageOnline.Offline, Vector3.Zero, UUID.Zero, Helpers.EmptyBytes);
    }

    /** 
     * Requests script detection of objects and avatars
     * 
     * @param name name of the object/avatar to search for
     * @param searchID UUID of the object or avatar to search for
     * @param type Type of search from ScriptSensorTypeFlags
     * @param range range of scan (96 max?)
     * @param arc the arc in radians to search within
     * @param requestID an user generated ID to correlate replies with
     * @param sim Simulator to perform search in
     * @throws Exception 
     */
    public final void RequestScriptSensor(String name, UUID searchID, byte type, float range, float arc, UUID requestID, Simulator simulator) throws Exception
    {
        ScriptSensorRequestPacket request = new ScriptSensorRequestPacket();
        request.Requester.Arc = arc;
        request.Requester.Range = range;
        request.Requester.RegionHandle = simulator.getHandle();
        request.Requester.RequestID = requestID;
        request.Requester.SearchDir = Quaternion.Identity; // TODO: this needs to be tested
        request.Requester.SearchID = searchID;
        request.Requester.setSearchName(Helpers.StringToBytes(name));
        request.Requester.SearchPos = Vector3.Zero;
        request.Requester.SearchRegions = 0; // TODO: ?
        request.Requester.SourceID = _Client.Self.getAgentID();
        request.Requester.Type = type;

        simulator.SendPacket(request);
    }

    /** 
     * Create or update profile pick
     * 
     * @param pickID UUID of the pick to update, or random UUID to create a new pick
     * @param topPick Is this a top pick? (typically false)
     * @param parcelID UUID of the parcel (UUID.Zero for the current parcel)
     * @param name Name of the pick
     * @param globalPosition Global position of the pick landmark
     * @param textureID UUID of the image displayed with the pick
     * @param description Long description of the pick
     * @throws Exception 
     */
    public final void PickInfoUpdate(UUID pickID, boolean topPick, UUID parcelID, String name, Vector3d globalPosition, UUID textureID, String description) throws Exception
    {
        PickInfoUpdatePacket pick = new PickInfoUpdatePacket();
        pick.AgentData.AgentID = _Client.Self.getAgentID();
        pick.AgentData.SessionID = _Client.Self.getSessionID();
        pick.Data.PickID = pickID;
        pick.Data.setDesc(Helpers.StringToBytes(description));
        pick.Data.CreatorID = _Client.Self.getAgentID();
        pick.Data.TopPick = topPick;
        pick.Data.ParcelID = parcelID;
        pick.Data.setName(Helpers.StringToBytes(name));
        pick.Data.SnapshotID = textureID;
        pick.Data.PosGlobal = globalPosition;
        pick.Data.SortOrder = 0;
        pick.Data.Enabled = false;

        _Client.Network.SendPacket(pick);
    }

    /** 
     * Delete profile pick
     * 
     * @param pickID UUID of the pick to delete
     */
    public final void PickDelete(UUID pickID) throws Exception
    {
        PickDeletePacket delete = new PickDeletePacket();
        delete.AgentData.AgentID = _Client.Self.getAgentID();
        delete.AgentData.SessionID = _Client.Self.getSessionID();
        delete.Data.PickID = pickID;

        _Client.Network.SendPacket(delete);
    }

    /**
     * Create or update profile Classified
     * 
     * @param classifiedID UUID of the classified to update, or random UUID to create a new classified
     * @param category Defines what catagory the classified is in
     * @param snapshotID UUID of the image displayed with the classified
     * @param price Price that the classified will cost to place for a week
     * @param position Global position of the classified landmark
     * @param name Name of the classified
     * @param desc Long description of the classified
     * @param autoRenew if true, auto renew classified after expiration
     * @throws Exception 
     */
    public final void UpdateClassifiedInfo(UUID classifiedID, ClassifiedCategories category, UUID snapshotID, int price, Vector3d position, String name, String desc, boolean autoRenew) throws Exception
    {
        ClassifiedInfoUpdatePacket classified = new ClassifiedInfoUpdatePacket();
        classified.AgentData.AgentID = _Client.Self.getAgentID();
        classified.AgentData.SessionID = _Client.Self.getSessionID();

        classified.Data.ClassifiedID = classifiedID;
        classified.Data.Category = ClassifiedCategories.getValue(category);

        classified.Data.ParcelID = UUID.Zero;
        // TODO: verify/fix ^
        classified.Data.ParentEstate = 0;
        // TODO: verify/fix ^

        classified.Data.SnapshotID = snapshotID;
        classified.Data.PosGlobal = position;

        classified.Data.ClassifiedFlags = autoRenew ? ClassifiedFlags.AutoRenew : ClassifiedFlags.None;
        // TODO: verify/fix ^

        classified.Data.PriceForListing = price;
        classified.Data.setName(Helpers.StringToBytes(name));
        classified.Data.setDesc(Helpers.StringToBytes(desc));
        _Client.Network.SendPacket(classified);
    }

    /** 
     * Create or update profile Classified
     * 
     * @param classifiedID UUID of the classified to update, or random UUID to create a new classified
     * @param category Defines what catagory the classified is in
     * @param snapshotID UUID of the image displayed with the classified
     * @param price Price that the classified will cost to place for a week
     * @param name Name of the classified
     * @param desc Long description of the classified
     * @param autoRenew if true, auto renew classified after expiration
     * @throws Exception 
     */
    public final void UpdateClassifiedInfo(UUID classifiedID, ClassifiedCategories category, UUID snapshotID, int price, String name, String desc, boolean autoRenew) throws Exception
    {
        UpdateClassifiedInfo(classifiedID, category, snapshotID, price, _Client.Self.getGlobalPosition(), name, desc, autoRenew);
    }

    /** 
     * Delete a classified ad
     * 
     * @param classifiedID The classified ads ID
     * @throws Exception 
     */
    public final void DeleteClassfied(UUID classifiedID) throws Exception
    {
        ClassifiedDeletePacket classified = new ClassifiedDeletePacket();
        classified.AgentData.AgentID = _Client.Self.getAgentID();
        classified.AgentData.SessionID = _Client.Self.getSessionID();

        classified.Data.ClassifiedID = classifiedID;
        _Client.Network.SendPacket(classified);
    }

    /** 
     * Fetches resource usage by agents attachmetns
     * 
     * @param callback Called when the requested information is collected
     */
    private class AttachmentResourceReplyHandler implements FutureCallback<OSD>
    {
    	private final CallbackHandler<AttachmentResourcesCallbackArgs> callback;
    	
    	public AttachmentResourceReplyHandler(CallbackHandler<AttachmentResourcesCallbackArgs> callback)
    	{
    		this.callback = callback;
    	}
    	
    	@Override
		public void completed(OSD result)
    	{
            if (result == null)
            {
                callback.dispatch(new AttachmentResourcesCallbackArgs(false, null));
            }
            AttachmentResourcesMessage info = (AttachmentResourcesMessage)_Client.Messages.DecodeEvent(CapsEventType.AttachmentResources, (OSDMap)result);
            callback.dispatch(new AttachmentResourcesCallbackArgs(true, info));
        }

		@Override
		public void failed(Exception ex)
		{
            callback.dispatch(new AttachmentResourcesCallbackArgs(false, null));
		}

		@Override
		public void cancelled()
		{
            callback.dispatch(new AttachmentResourcesCallbackArgs(false, null));
		}
    }
    
    public final void GetAttachmentResources(final CallbackHandler<AttachmentResourcesCallbackArgs> callback) throws IOReactorException
    {
        URI url = _Client.Network.getCapabilityURI("AttachmentResources");
        if (url != null)
        {
            CapsClient request = new CapsClient(url);
            request.BeginGetResponse(_Client.Settings.CAPS_TIMEOUT, new AttachmentResourceReplyHandler(callback));
        }
    }
    // #endregion Misc
	
	public void UpdateCamera(boolean reliable) throws Exception
	{
		AgentUpdatePacket update = new AgentUpdatePacket();
		update.AgentData.AgentID = this.agentID;
		update.AgentData.SessionID = this.sessionID;
		update.AgentData.State = 0;
		update.AgentData.BodyRotation = new Quaternion(0, 0.6519076f, 0, 0);
		update.AgentData.HeadRotation = new Quaternion();
		// Semi-sane default values
		update.AgentData.CameraCenter = new Vector3(9.549901f, 7.033957f,
				11.75f);
		update.AgentData.CameraAtAxis = new Vector3(0.7f, 0.7f, 0);
		update.AgentData.CameraLeftAxis = new Vector3(-0.7f, 0.7f, 0);
		update.AgentData.CameraUpAxis = new Vector3(0.1822026f, 0.9828722f, 0);
		update.AgentData.Far = 384;
		update.AgentData.ControlFlags = 0; // TODO: What is this?
		update.AgentData.Flags = 0;
		update.getHeader().setReliable(reliable);

		_Client.Network.SendPacket(update);

		// Send an AgentFOV packet widening our field of vision
		/*
		 * AgentFOVPacket fovPacket = new AgentFOVPacket();
		 * fovPacket.AgentData.AgentID = this.ID; fovPacket.AgentData.SessionID =
		 * _Client.Network.SessionID; fovPacket.AgentData.CircuitCode =
		 * simulator.CircuitCode; fovPacket.FOVBlock.GenCounter = 0;
		 * fovPacket.FOVBlock.VerticalAngle = 6.28318531f;
		 * fovPacket.Header.Reliable = true;
		 * _Client.Network.SendPacket(fovPacket);
		 */
	}

	private void CoarseLocationHandler(Packet packet, Simulator simulator) throws Exception
	{
		// TODO: This will be useful one day
	}

	private void InstantMessageHandler(Packet packet, Simulator simulator) throws Exception
	{
		if (packet.getType() == PacketType.ImprovedInstantMessage)
		{
			ImprovedInstantMessagePacket im = (ImprovedInstantMessagePacket) packet;

			InstantMessage mess = new InstantMessage();
			
			mess.FromAgentID = im.AgentData.AgentID;
            mess.FromAgentName = Helpers.BytesToString(im.MessageBlock.getFromAgentName());
            mess.ToAgentID = im.MessageBlock.ToAgentID;
            mess.ParentEstateID = im.MessageBlock.ParentEstateID;
            mess.RegionID = im.MessageBlock.RegionID;
            mess.Position = im.MessageBlock.Position;
            mess.GroupIM = im.MessageBlock.FromGroup;
            mess.IMSessionID = im.MessageBlock.ID;
            mess.Timestamp = new Date(im.MessageBlock.Timestamp);
            mess.Message = Helpers.BytesToString(im.MessageBlock.getMessage());
            mess.Offline = InstantMessageOnline.values()[im.MessageBlock.Offline];
            mess.BinaryBucket = im.MessageBlock.getBinaryBucket();

            OnInstantMessage.dispatch(new InstantMessageCallbackArgs(mess, simulator));
		}
	}

	private void ChatHandler(Packet packet, Simulator simulator)
			throws Exception {
		if (packet.getType() == PacketType.ChatFromSimulator) {
			ChatFromSimulatorPacket chat = (ChatFromSimulatorPacket) packet;

			OnChat.dispatch(new ChatCallbackArgs(Helpers.BytesToString(chat.ChatData.getMessage()), chat.ChatData.Audible,
						    chat.ChatData.ChatType, chat.ChatData.SourceType,
						    Helpers.BytesToString(chat.ChatData.getFromName()), chat.ChatData.SourceID));
		}
	}

	private void MovementCompleteHandler(Packet packet, Simulator simulator) throws UnsupportedEncodingException
	{
		AgentMovementCompletePacket movement = (AgentMovementCompletePacket) packet;

        relativePosition = movement.Data.Position;
        _Movement.Camera.LookDirection(movement.Data.LookAt);
        simulator.setHandle(movement.Data.RegionHandle);
        simulator.SimVersion = Helpers.BytesToString(movement.SimData.getChannelVersion());
	}

	private void HealthHandler(Packet packet, Simulator simulator)
	{
		health = ((HealthMessagePacket) packet).HealthData.Health;
	}

	private void BalanceHandler(Packet packet, Simulator simulator) {
		if (packet.getType() == PacketType.MoneyBalanceReply) {
			balance = ((MoneyBalanceReplyPacket) packet).MoneyData.MoneyBalance;
		}

		OnBalanceUpdated.dispatch(new BalanceCallbackArgs(balance));
	}

	private void TeleportHandler(Packet packet, Simulator simulator) throws Exception {
		int flags = 0;
		TeleportStatus teleportStatus =  TeleportStatus.None; 
        String teleportMessage = Helpers.EmptyString;
        boolean finished = false;

        if (packet.getType() == PacketType.TeleportStart) {
            TeleportStartPacket start = (TeleportStartPacket)packet;

            teleportStatus = TeleportStatus.Start;
			teleportMessage = "Teleport started";
            flags = start.Info.TeleportFlags;

            Logger.DebugLog("TeleportStart received, Flags: " + flags, _Client);
		} else if (packet.getType() == PacketType.TeleportProgress) {
            TeleportProgressPacket progress = (TeleportProgressPacket)packet;

			teleportStatus = TeleportStatus.Progress;
            teleportMessage = Helpers.BytesToString(progress.Info.getMessage());
            flags = progress.Info.TeleportFlags;

            Logger.DebugLog("TeleportProgress received, Message: " + teleportMessage + ", Flags: " + flags, _Client);
		} else if (packet.getType() == PacketType.TeleportFailed) { 
            TeleportFailedPacket failed = (TeleportFailedPacket)packet;

            teleportMessage = Helpers.BytesToString(failed.Info.getReason());
            teleportStatus = TeleportStatus.Failed;
            finished = true;

            Logger.DebugLog("TeleportFailed received, Reason: " + teleportMessage, _Client);
		} else if (packet.getType() == PacketType.TeleportCancel) {
            // TeleportCancelPacket cancel = (TeleportCancelPacket)packet;

            teleportMessage = "Cancelled";
            teleportStatus = TeleportStatus.Cancelled;
            finished = true;

            Logger.DebugLog("TeleportCancel received from " + simulator.toString(), _Client);
    	} else if (packet.getType() == PacketType.TeleportFinish) {
			TeleportFinishPacket finish = (TeleportFinishPacket) packet;

			flags = finish.Info.TeleportFlags;
            String seedcaps = Helpers.BytesToString(finish.Info.getSeedCapability());
            finished = true;

            Logger.DebugLog("TeleportFinish received, Flags: " + flags, _Client);
			// Connect to the new sim
			/**
			 * TO BE PORTED Original code created InetAddress from
			 * (ulong)finish.InfoSimIP
			 */
			Simulator newSimulator = _Client.Network.Connect(InetAddress.getByName("" + finish.Info.SimIP),
					finish.Info.SimPort, _Client.Network.getCircuitCode(), true, seedcaps);

			if (newSimulator != null) {
				teleportMessage = "Teleport finished";
				teleportStatus = TeleportStatus.Finished;

				Logger.Log("Moved to new sim " + _Client.Network.getCurrentSim().Name + "("
						   + _Client.Network.getCurrentSim().getIPEndPoint().toString() + ")", LogLevel.Info, _Client);
			} else {
				teleportMessage = "Failed to connect to the new sim after a teleport";
				teleportStatus = TeleportStatus.Failed;

				Logger.Log(teleportMessage, LogLevel.Error, _Client);
			}
    	}
        else if (packet.getType() == PacketType.TeleportLocal)
        {
            TeleportLocalPacket local = (TeleportLocalPacket)packet;

            teleportMessage = "Teleport finished";
            flags = local.Info.TeleportFlags;
            teleportStatus = TeleportStatus.Finished;
            relativePosition = local.Info.Position;
            _Movement.Camera.LookDirection(local.Info.LookAt);
            // This field is apparently not used for anything
            //local.Info.LocationID;
            finished = true;

            Logger.DebugLog("TeleportLocal received, Flags: " + flags, _Client);
        }
        OnTeleport.dispatch(new TeleportCallbackArgs(teleportMessage, teleportStatus, flags));
        if (finished)
        {
            teleportTimeout.set(teleportStatus);
        }
	}

    private static final int CONTROL_AT_POS_INDEX = 0;
    private static final int CONTROL_AT_NEG_INDEX = 1;
    private static final int CONTROL_LEFT_POS_INDEX = 2;
    private static final int CONTROL_LEFT_NEG_INDEX = 3;
    private static final int CONTROL_UP_POS_INDEX = 4;
    private static final int CONTROL_UP_NEG_INDEX = 5;
    private static final int CONTROL_PITCH_POS_INDEX = 6;
    private static final int CONTROL_PITCH_NEG_INDEX = 7;
    private static final int CONTROL_YAW_POS_INDEX = 8;
    private static final int CONTROL_YAW_NEG_INDEX = 9;
    private static final int CONTROL_FAST_AT_INDEX = 10;
    private static final int CONTROL_FAST_LEFT_INDEX = 11;
    private static final int CONTROL_FAST_UP_INDEX = 12;
    private static final int CONTROL_FLY_INDEX = 13;
    private static final int CONTROL_STOP_INDEX = 14;
    private static final int CONTROL_FINISH_ANIM_INDEX = 15;
    private static final int CONTROL_STAND_UP_INDEX = 16;
    private static final int CONTROL_SIT_ON_GROUND_INDEX = 17;
    private static final int CONTROL_MOUSELOOK_INDEX = 18;
    private static final int CONTROL_NUDGE_AT_POS_INDEX = 19;
    private static final int CONTROL_NUDGE_AT_NEG_INDEX = 20;
    private static final int CONTROL_NUDGE_LEFT_POS_INDEX = 21;
    private static final int CONTROL_NUDGE_LEFT_NEG_INDEX = 22;
    private static final int CONTROL_NUDGE_UP_POS_INDEX = 23;
    private static final int CONTROL_NUDGE_UP_NEG_INDEX = 24;
    private static final int CONTROL_TURN_LEFT_INDEX = 25;
    private static final int CONTROL_TURN_RIGHT_INDEX = 26;
    private static final int CONTROL_AWAY_INDEX = 27;
    private static final int CONTROL_LBUTTON_DOWN_INDEX = 28;
    private static final int CONTROL_LBUTTON_UP_INDEX = 29;
    private static final int CONTROL_ML_LBUTTON_DOWN_INDEX = 30;
    private static final int CONTROL_ML_LBUTTON_UP_INDEX = 31;

    /* Used to specify movement actions for your agent */
    public static class ControlFlags
    {
        // Empty flag
        public static final int NONE = 0;
        // Move Forward (SL Keybinding: W/Up Arrow)
        public static final int AGENT_CONTROL_AT_POS = 0x1 << CONTROL_AT_POS_INDEX;
        //t Move Backward (SL Keybinding: S/Down Arrow)
        public static final int AGENT_CONTROL_AT_NEG = 0x1 << CONTROL_AT_NEG_INDEX;
        // Move Left (SL Keybinding: Shift-(A/Left Arrow))
        public static final int AGENT_CONTROL_LEFT_POS = 0x1 << CONTROL_LEFT_POS_INDEX;
        // Move Right (SL Keybinding: Shift-(D/Right Arrow))
        public static final int AGENT_CONTROL_LEFT_NEG = 0x1 << CONTROL_LEFT_NEG_INDEX;
        // Not Flying: Jump/Flying: Move Up (SL Keybinding: E)
        public static final int AGENT_CONTROL_UP_POS = 0x1 << CONTROL_UP_POS_INDEX;
        // Not Flying: Croutch/Flying: Move Down (SL Keybinding: C)
        public static final int AGENT_CONTROL_UP_NEG = 0x1 << CONTROL_UP_NEG_INDEX;
        // Unused
        public static final int AGENT_CONTROL_PITCH_POS = 0x1 << CONTROL_PITCH_POS_INDEX;
        // Unused
        public static final int AGENT_CONTROL_PITCH_NEG = 0x1 << CONTROL_PITCH_NEG_INDEX;
        // Unused
        public static final int AGENT_CONTROL_YAW_POS = 0x1 << CONTROL_YAW_POS_INDEX;
        // Unused
        public static final int AGENT_CONTROL_YAW_NEG = 0x1 << CONTROL_YAW_NEG_INDEX;
        // ORed with AGENT_CONTROL_AT_* if the keyboard is being used
        public static final int AGENT_CONTROL_FAST_AT = 0x1 << CONTROL_FAST_AT_INDEX;
        // ORed with AGENT_CONTROL_LEFT_* if the keyboard is being used
        public static final int AGENT_CONTROL_FAST_LEFT = 0x1 << CONTROL_FAST_LEFT_INDEX;
        // ORed with AGENT_CONTROL_UP_* if the keyboard is being used
        public static final int AGENT_CONTROL_FAST_UP = 0x1 << CONTROL_FAST_UP_INDEX;
        // Fly
        public static final int AGENT_CONTROL_FLY = 0x1 << CONTROL_FLY_INDEX;
        //
        public static final int AGENT_CONTROL_STOP = 0x1 << CONTROL_STOP_INDEX;
        // Finish our current animation
        public static final int AGENT_CONTROL_FINISH_ANIM = 0x1 << CONTROL_FINISH_ANIM_INDEX;
        // Stand up from the ground or a prim seat
        public static final int AGENT_CONTROL_STAND_UP = 0x1 << CONTROL_STAND_UP_INDEX;
        // Sit on the ground at our current location
        public static final int AGENT_CONTROL_SIT_ON_GROUND = 0x1 << CONTROL_SIT_ON_GROUND_INDEX;
        // Whether mouselook is currently enabled
        public static final int AGENT_CONTROL_MOUSELOOK = 0x1 << CONTROL_MOUSELOOK_INDEX;
        // Legacy, used if a key was pressed for less than a certain amount of time
        public static final int AGENT_CONTROL_NUDGE_AT_POS = 0x1 << CONTROL_NUDGE_AT_POS_INDEX;
        // Legacy, used if a key was pressed for less than a certain amount of time
        public static final int AGENT_CONTROL_NUDGE_AT_NEG = 0x1 << CONTROL_NUDGE_AT_NEG_INDEX;
        // Legacy, used if a key was pressed for less than a certain amount of time
        public static final int AGENT_CONTROL_NUDGE_LEFT_POS = 0x1 << CONTROL_NUDGE_LEFT_POS_INDEX;
        // Legacy, used if a key was pressed for less than a certain amount of time
        public static final int AGENT_CONTROL_NUDGE_LEFT_NEG = 0x1 << CONTROL_NUDGE_LEFT_NEG_INDEX;
        // Legacy, used if a key was pressed for less than a certain amount of time
        public static final int AGENT_CONTROL_NUDGE_UP_POS = 0x1 << CONTROL_NUDGE_UP_POS_INDEX;
        // Legacy, used if a key was pressed for less than a certain amount of time
        public static final int AGENT_CONTROL_NUDGE_UP_NEG = 0x1 << CONTROL_NUDGE_UP_NEG_INDEX;
        //
        public static final int AGENT_CONTROL_TURN_LEFT = 0x1 << CONTROL_TURN_LEFT_INDEX;
        //
        public static final int AGENT_CONTROL_TURN_RIGHT = 0x1 << CONTROL_TURN_RIGHT_INDEX;
        // Set when the avatar is idled or set to away. Note that the away animation is 
        // activated separately from setting this flag
        public static final int AGENT_CONTROL_AWAY = 0x1 << CONTROL_AWAY_INDEX;
        //
        public static final int AGENT_CONTROL_LBUTTON_DOWN = 0x1 << CONTROL_LBUTTON_DOWN_INDEX;
        //
        public static final int AGENT_CONTROL_LBUTTON_UP = 0x1 << CONTROL_LBUTTON_UP_INDEX;
        //
        public static final int AGENT_CONTROL_ML_LBUTTON_DOWN = 0x1 << CONTROL_ML_LBUTTON_DOWN_INDEX;
        //
        public static final int AGENT_CONTROL_ML_LBUTTON_UP = 0x1 << CONTROL_ML_LBUTTON_UP_INDEX;
        
        public static int setValue(int value)
        {
        	return value;
        }
        
        public static int getValue(int value)
        {
        	return value;
        }
    }

    /* Agent movement and camera control
     * 
     * Agent movement is controlled by setting specific {@link T:AgentManager.ControlFlags}
     * After the control flags are set, An AgentUpdate is required to update the simulator of the specified flags
     * This is most easily accomplished by setting one or more of the AgentMovement properties
     * 
     * Movement of an avatar is always based on a compass direction, for example AtPos will move the
     * agent from West to East or forward on the X Axis, AtNeg will of course move agent from
     * East to West or backward on the X Axis, LeftPos will be South to North or forward on the Y Axis
     * The Z axis is Up, finer grained control of movements can be done using the Nudge properties
     */
    public class AgentMovement
    {
        /* Move agent positive along the X axis */
        public final boolean getAtPos()
        {
            return GetControlFlag(ControlFlags.AGENT_CONTROL_AT_POS);
        }
        public final void setAtPos(boolean value)
        {
            SetControlFlag(ControlFlags.AGENT_CONTROL_AT_POS, value);
        }
        /* Move agent negative along the X axis */
        public final boolean getAtNeg()
        {
            return GetControlFlag(ControlFlags.AGENT_CONTROL_AT_NEG);
        }
        public final void setAtNeg(boolean value)
        {
            SetControlFlag(ControlFlags.AGENT_CONTROL_AT_NEG, value);
        }
        /* Move agent positive along the Y axis */
        public final boolean getLeftPos()
        {
            return GetControlFlag(ControlFlags.AGENT_CONTROL_LEFT_POS);
        }
        public final void setLeftPos(boolean value)
        {
            SetControlFlag(ControlFlags.AGENT_CONTROL_LEFT_POS, value);
        }
        /* Move agent negative along the Y axis */
        public final boolean getLeftNeg()
        {
            return GetControlFlag(ControlFlags.AGENT_CONTROL_LEFT_NEG);
        }
        public final void setLeftNeg(boolean value)
        {
            SetControlFlag(ControlFlags.AGENT_CONTROL_LEFT_NEG, value);
        }
        /* Move agent positive along the Z axis */
        public final boolean getUpPos()
        {
            return GetControlFlag(ControlFlags.AGENT_CONTROL_UP_POS);
        }
        public final void setUpPos(boolean value)
        {
            SetControlFlag(ControlFlags.AGENT_CONTROL_UP_POS, value);
        }
        /* Move agent negative along the Z axis */
        public final boolean getUpNeg()
        {
            return GetControlFlag(ControlFlags.AGENT_CONTROL_UP_NEG);
        }
        public final void setUpNeg(boolean value)
        {
            SetControlFlag(ControlFlags.AGENT_CONTROL_UP_NEG, value);
        }

        public final boolean getPitchPos()
        {
            return GetControlFlag(ControlFlags.AGENT_CONTROL_PITCH_POS);
        }
        public final void setPitchPos(boolean value)
        {
            SetControlFlag(ControlFlags.AGENT_CONTROL_PITCH_POS, value);
        }

        public final boolean getPitchNeg()
        {
            return GetControlFlag(ControlFlags.AGENT_CONTROL_PITCH_NEG);
        }
        public final void setPitchNeg(boolean value)
        {
            SetControlFlag(ControlFlags.AGENT_CONTROL_PITCH_NEG, value);
        }

        public final boolean getYawPos()
        {
            return GetControlFlag(ControlFlags.AGENT_CONTROL_YAW_POS);
        }
        public final void setYawPos(boolean value)
        {
            SetControlFlag(ControlFlags.AGENT_CONTROL_YAW_POS, value);
        }

        public final boolean getYawNeg()
        {
            return GetControlFlag(ControlFlags.AGENT_CONTROL_YAW_NEG);
        }
        public final void setYawNeg(boolean value)
        {
            SetControlFlag(ControlFlags.AGENT_CONTROL_YAW_NEG, value);
        }

        public final boolean getFastAt()
        {
            return GetControlFlag(ControlFlags.AGENT_CONTROL_FAST_AT);
        }
        public final void setFastAt(boolean value)
        {
            SetControlFlag(ControlFlags.AGENT_CONTROL_FAST_AT, value);
        }

        public final boolean getFastLeft()
        {
            return GetControlFlag(ControlFlags.AGENT_CONTROL_FAST_LEFT);
        }
        public final void setFastLeft(boolean value)
        {
            SetControlFlag(ControlFlags.AGENT_CONTROL_FAST_LEFT, value);
        }

        public final boolean getFastUp()
        {
            return GetControlFlag(ControlFlags.AGENT_CONTROL_FAST_UP);
        }
        public final void setFastUp(boolean value)
        {
            SetControlFlag(ControlFlags.AGENT_CONTROL_FAST_UP, value);
        }
        /* Causes simulator to make agent fly */
        public final boolean getFly()
        {
            return GetControlFlag(ControlFlags.AGENT_CONTROL_FLY);
        }
        public final void setFly(boolean value)
        {
            SetControlFlag(ControlFlags.AGENT_CONTROL_FLY, value);
        }
        /* Stop movement */
        public final boolean getStop()
        {
            return GetControlFlag(ControlFlags.AGENT_CONTROL_STOP);
        }
        public final void setStop(boolean value)
        {
            SetControlFlag(ControlFlags.AGENT_CONTROL_STOP, value);
        }
        /* Finish animation */
        public final boolean getFinishAnim()
        {
            return GetControlFlag(ControlFlags.AGENT_CONTROL_FINISH_ANIM);
        }
        public final void setFinishAnim(boolean value)
        {
            SetControlFlag(ControlFlags.AGENT_CONTROL_FINISH_ANIM, value);
        }
        /* Stand up from a sit */
        public final boolean getStandUp()
        {
            return GetControlFlag(ControlFlags.AGENT_CONTROL_STAND_UP);
        }
        public final void setStandUp(boolean value)
        {
            SetControlFlag(ControlFlags.AGENT_CONTROL_STAND_UP, value);
        }
        /* Tells simulator to sit agent on ground */
        public final boolean getSitOnGround()
        {
            return GetControlFlag(ControlFlags.AGENT_CONTROL_SIT_ON_GROUND);
        }
        public final void setSitOnGround(boolean value)
        {
            SetControlFlag(ControlFlags.AGENT_CONTROL_SIT_ON_GROUND, value);
        }
        /* Place agent into mouselook mode */
        public final boolean getMouselook()
        {
            return GetControlFlag(ControlFlags.AGENT_CONTROL_MOUSELOOK);
        }
        public final void setMouselook(boolean value)
        {
            SetControlFlag(ControlFlags.AGENT_CONTROL_MOUSELOOK, value);
        }
        /* Nudge agent positive along the X axis */
        public final boolean getNudgeAtPos()
        {
            return GetControlFlag(ControlFlags.AGENT_CONTROL_NUDGE_AT_POS);
        }
        public final void setNudgeAtPos(boolean value)
        {
            SetControlFlag(ControlFlags.AGENT_CONTROL_NUDGE_AT_POS, value);
        }
        /* Nudge agent negative along the X axis */
        public final boolean getNudgeAtNeg()
        {
            return GetControlFlag(ControlFlags.AGENT_CONTROL_NUDGE_AT_NEG);
        }
        public final void setNudgeAtNeg(boolean value)
        {
            SetControlFlag(ControlFlags.AGENT_CONTROL_NUDGE_AT_NEG, value);
        }
        /* Nudge agent positive along the Y axis */
        public final boolean getNudgeLeftPos()
        {
            return GetControlFlag(ControlFlags.AGENT_CONTROL_NUDGE_LEFT_POS);
        }
        public final void setNudgeLeftPos(boolean value)
        {
            SetControlFlag(ControlFlags.AGENT_CONTROL_NUDGE_LEFT_POS, value);
        }
        /* Nudge agent negative along the Y axis */
        public final boolean getNudgeLeftNeg()
        {
            return GetControlFlag(ControlFlags.AGENT_CONTROL_NUDGE_LEFT_NEG);
        }
        public final void setNudgeLeftNeg(boolean value)
        {
            SetControlFlag(ControlFlags.AGENT_CONTROL_NUDGE_LEFT_NEG, value);
        }
        /* Nudge agent positive along the Z axis */
        public final boolean getNudgeUpPos()
        {
            return GetControlFlag(ControlFlags.AGENT_CONTROL_NUDGE_UP_POS);
        }
        public final void setNudgeUpPos(boolean value)
        {
            SetControlFlag(ControlFlags.AGENT_CONTROL_NUDGE_UP_POS, value);
        }
        /* Nudge agent negative along the Z axis */
        public final boolean getNudgeUpNeg()
        {
            return GetControlFlag(ControlFlags.AGENT_CONTROL_NUDGE_UP_NEG);
        }
        public final void setNudgeUpNeg(boolean value)
        {
            SetControlFlag(ControlFlags.AGENT_CONTROL_NUDGE_UP_NEG, value);
        }

        public final boolean getTurnLeft()
        {
            return GetControlFlag(ControlFlags.AGENT_CONTROL_TURN_LEFT);
        }
        public final void setTurnLeft(boolean value)
        {
            SetControlFlag(ControlFlags.AGENT_CONTROL_TURN_LEFT, value);
        }

        public final boolean getTurnRight()
        {
            return GetControlFlag(ControlFlags.AGENT_CONTROL_TURN_RIGHT);
        }
        public final void setTurnRight(boolean value)
        {
            SetControlFlag(ControlFlags.AGENT_CONTROL_TURN_RIGHT, value);
        }
        /* Tell simulator to mark agent as away */
        public final boolean getAway()
        {
            return GetControlFlag(ControlFlags.AGENT_CONTROL_AWAY);
        }
        public final void setAway(boolean value)
        {
            SetControlFlag(ControlFlags.AGENT_CONTROL_AWAY, value);
        }

        public final boolean getLButtonDown()
        {
            return GetControlFlag(ControlFlags.AGENT_CONTROL_LBUTTON_DOWN);
        }
        public final void setLButtonDown(boolean value)
        {
            SetControlFlag(ControlFlags.AGENT_CONTROL_LBUTTON_DOWN, value);
        }

        public final boolean getLButtonUp()
        {
            return GetControlFlag(ControlFlags.AGENT_CONTROL_LBUTTON_UP);
        }
        public final void setLButtonUp(boolean value)
        {
            SetControlFlag(ControlFlags.AGENT_CONTROL_LBUTTON_UP, value);
        }

        public final boolean getMLButtonDown()
        {
            return GetControlFlag(ControlFlags.AGENT_CONTROL_ML_LBUTTON_DOWN);
        }
        public final void setMLButtonDown(boolean value)
        {
            SetControlFlag(ControlFlags.AGENT_CONTROL_ML_LBUTTON_DOWN, value);
        }

        public final boolean getMLButtonUp()
        {
            return GetControlFlag(ControlFlags.AGENT_CONTROL_ML_LBUTTON_UP);
        }
        public final void setMLButtonUp(boolean value)
        {
            SetControlFlag(ControlFlags.AGENT_CONTROL_ML_LBUTTON_UP, value);
        }
        /* Returns "always run" value, or changes it by sending a SetAlwaysRunPacket */
        public final boolean getAlwaysRun()
        {
            return alwaysRun;
        }
        public final void setAlwaysRun(boolean value) throws Exception
        {
            alwaysRun = value;
            SetAlwaysRunPacket run = new SetAlwaysRunPacket();
            run.AgentData.AgentID = _Client.Self.getAgentID();
            run.AgentData.SessionID = _Client.Self.getSessionID();
            run.AgentData.AlwaysRun = alwaysRun;
            _Client.Network.SendPacket(run);
        }
        /* The current value of the agent control flags */
        public final int getAgentControls()
        {
            return ControlFlags.getValue(agentControls);
        }

        /* Gets or sets the interval in milliseconds at which AgentUpdate packets are sent to the
           current simulator. Setting this to a non-zero value will also enable the packet sending
           if it was previously off, and setting it to zero will disable */
        public final int getUpdateInterval()
        {
            return updateInterval;
        }
        public final void setUpdateInterval(int value)
        {
            if (value > 0)
            {
                if (updateTimer != null)
                {
                    updateTimer.scheduleAtFixedRate(updateTask, updateInterval, updateInterval);
                }
                updateInterval = value;
            }
            else
            {
                if (updateTimer != null)
                {
                    updateTimer.cancel();
                }
                updateInterval = 0;
            }
        }
        /* Gets or sets whether AgentUpdate packets are sent to the current simulator */
        public final boolean getUpdateEnabled()
        {
            return (updateInterval != 0);
        }

        /* Reset movement controls every time we send an update */
        public final boolean getAutoResetControls()
        {
            return autoResetControls;
        }
        public final void setAutoResetControls(boolean value)
        {
            autoResetControls = value;
        }
        // #endregion Properties

        // Agent camera controls
        public AgentCamera Camera;
        // Currently only used for hiding your group title
        public AgentFlags Flags = AgentFlags.None;
        // Action state of the avatar, which can currently be typing and editing
        public byte State;
        public Quaternion BodyRotation = Quaternion.Identity;
        public Quaternion HeadRotation = Quaternion.Identity;

        ///#region Change tracking
        private Quaternion LastBodyRotation;
        private Quaternion LastHeadRotation;
        private Vector3 LastCameraCenter;
        private Vector3 LastCameraXAxis;
        private Vector3 LastCameraYAxis;
        private Vector3 LastCameraZAxis;
        private float LastFar;

        private boolean alwaysRun;
        private GridClient Client;

        private int agentControls;
        private int duplicateCount;
        private int lastState;
        /* Timer for sending AgentUpdate packets */
        private Timer updateTimer;
        private TimerTask updateTask;
        private int updateInterval;
        private boolean autoResetControls;

        /* Default constructor */
        public AgentMovement(GridClient client)
        {
            Client = client;
            Camera = new AgentCamera();
            _Client.Network.OnDisconnected.add(new Network_OnDisconnected());
            updateInterval = Settings.DEFAULT_AGENT_UPDATE_INTERVAL;
        }

        private void CleanupTimer()
        {
            if (updateTimer != null)
            {
                updateTimer.cancel();
                updateTimer = null;
                updateTask = null;
            }
        }

        private class Network_OnDisconnected extends CallbackHandler<DisconnectedCallbackArgs>
        {
        	@Override
			public void callback(DisconnectedCallbackArgs e)
            {
                 CleanupTimer();
            }
        }

        private class UpdateTimer extends TimerTask
        {
            @Override
			public void run()
            {
                if (_Client.Network.getConnected() && _Client.Settings.SEND_AGENT_UPDATES)
                {
                    //Send an AgentUpdate packet
                    try
                    {
						SendUpdate(false, _Client.Network.getCurrentSim());
					} catch (Exception e) {	}
                }
            }
        }
        
        public void ResetTimer()
        {
            CleanupTimer();
            updateTimer = new Timer();
            updateTask = new UpdateTimer();
            updateTimer.scheduleAtFixedRate(updateTask, updateInterval, updateInterval);
        }

        /**
         * Send an AgentUpdate with the camera set at the current agent
         * 
         * @param heading Camera rotation in radians
         * @param reliable Whether to send the AgentUpdate reliable or not
         * @throws Exception 
         */
        public final void UpdateFromHeading(double heading, boolean reliable) throws Exception
        {
            Camera.setPosition(_Client.Self.getSimPosition());
            Camera.LookDirection(heading);

            BodyRotation.Z = (float)Math.sin(heading / 2.0d);
            BodyRotation.W = (float)Math.cos(heading / 2.0d);
            HeadRotation = BodyRotation;

            SendUpdate(reliable, _Client.Network.getCurrentSim());
        }

        /**
         * Rotates the avatar body and camera toward a target position.
         * This will also anchor the camera position on the avatar
         *  
         * @param target Region coordinates to turn toward
         * @throws Exception 
         */
        public final boolean TurnToward(Vector3 target) throws Exception
        {
            if (_Client.Settings.SEND_AGENT_UPDATES)
            {
                Quaternion parentRot = Quaternion.Identity;

                if (_Client.Self.sittingOn > 0)
                {
                    if (_Client.Network.getCurrentSim().getObjectsPrimitives().containsKey(sittingOn))
                    {
                        parentRot = _Client.Network.getCurrentSim().getObjectsPrimitives().get(sittingOn).Rotation;
                    }
                    else
                    {
                        Logger.Log("Attempted TurnToward but parent prim is not in dictionary", LogLevel.Warning, Client);
                        return false;
                    }
                }

                Quaternion between = Vector3.RotationBetween(Vector3.UnitX, Vector3.Normalize(target.subtract(_Client.Self.getSimPosition())));
                Quaternion rot = Quaternion.multiply(between, Quaternion.divide(Quaternion.Identity, parentRot));

                BodyRotation = rot;
                HeadRotation = rot;
                Camera.LookAt(_Client.Self.getSimPosition(), target);

                SendUpdate(false, _Client.Network.getCurrentSim());

                return true;
            }

            Logger.Log("Attempted TurnToward but agent updates are disabled", LogLevel.Warning, Client);
            return false;
        }

        /**
         * Send new AgentUpdate packet to update our current camera position and rotation
         * 
         * @param reliable Whether to require server acknowledgement of this packet
         * @param simulator Simulator to send the update to
         * @throws Exception 
         */
        public final void SendUpdate(boolean reliable, Simulator simulator) throws Exception
        {
            // Since version 1.40.4 of the Linden simulator, sending this update
            // causes corruption of the agent position in the simulator
            if (simulator != null && (!simulator.handshakeComplete))
            {
                return;
            }

            Vector3 origin = Camera.getPosition();
            Vector3 xAxis = Camera.getLeftAxis();
            Vector3 yAxis = Camera.getAtAxis();
            Vector3 zAxis = Camera.getUpAxis();

            // Attempted to sort these in a rough order of how often they might change
            if (agentControls == 0 && yAxis == LastCameraYAxis && origin == LastCameraCenter && State == lastState &&
            	HeadRotation == LastHeadRotation && BodyRotation == LastBodyRotation && xAxis == LastCameraXAxis &&
            	Camera.Far == LastFar && zAxis == LastCameraZAxis)
            {
                ++duplicateCount;
            }
            else
            {
                duplicateCount = 0;
            }

            if (_Client.Settings.DISABLE_AGENT_UPDATE_DUPLICATE_CHECK || duplicateCount < 10)
            {
                // Store the current state to do duplicate checking
                LastHeadRotation = HeadRotation;
                LastBodyRotation = BodyRotation;
                LastCameraYAxis = yAxis;
                LastCameraCenter = origin;
                LastCameraXAxis = xAxis;
                LastCameraZAxis = zAxis;
                LastFar = Camera.Far;
                lastState = State;

                // Build the AgentUpdate packet and send it
                AgentUpdatePacket update = new AgentUpdatePacket();
                update.getHeader().setReliable(reliable);

                update.AgentData.AgentID = _Client.Self.getAgentID();
                update.AgentData.SessionID = _Client.Self.getSessionID();
                update.AgentData.HeadRotation = HeadRotation;
                update.AgentData.BodyRotation = BodyRotation;
                update.AgentData.CameraAtAxis = yAxis;
                update.AgentData.CameraCenter = origin;
                update.AgentData.CameraLeftAxis = xAxis;
                update.AgentData.CameraUpAxis = zAxis;
                update.AgentData.Far = Camera.Far;
                update.AgentData.State = State;
                update.AgentData.ControlFlags = agentControls;
                update.AgentData.Flags = Flags.getValue();

                simulator.SendPacket(update);

                if (autoResetControls)
                {
                    ResetControlFlags();
                }
            }
        }

        /** Builds an AgentUpdate packet entirely from parameters. This will not touch the state
         *   of Self.Movement or Self.Movement.Camera in any way
         * @param controlFlags
         * @param position
         * @param forwardAxis
         * @param leftAxis
         * @param upAxis
         * @param bodyRotation
         * @param headRotation
         * @param farClip
         * @param flags
         * @param state
         * @param reliable
         * @throws Exception 
         */
        public final void SendManualUpdate(int controlFlags, Vector3 position, Vector3 forwardAxis, Vector3 leftAxis, Vector3 upAxis, Quaternion bodyRotation, Quaternion headRotation, float farClip, AgentFlags flags, byte state, boolean reliable) throws Exception
        {
            // Since version 1.40.4 of the Linden simulator, sending this update
            // causes corruption of the agent position in the simulator
            if (_Client.Network.getCurrentSim() != null && (!_Client.Network.getCurrentSim().handshakeComplete))
            {
                return;
            }

            AgentUpdatePacket update = new AgentUpdatePacket();

            update.AgentData.AgentID = _Client.Self.getAgentID();
            update.AgentData.SessionID = _Client.Self.getSessionID();
            update.AgentData.BodyRotation = bodyRotation;
            update.AgentData.HeadRotation = headRotation;
            update.AgentData.CameraCenter = position;
            update.AgentData.CameraAtAxis = forwardAxis;
            update.AgentData.CameraLeftAxis = leftAxis;
            update.AgentData.CameraUpAxis = upAxis;
            update.AgentData.Far = farClip;
            update.AgentData.ControlFlags = controlFlags;
            update.AgentData.Flags = flags.getValue();
            update.AgentData.State = state;

            update.getHeader().setReliable(reliable);

            _Client.Network.SendPacket(update);
        }

        private boolean GetControlFlag(int flag)
        {
            return ((agentControls & flag) != 0);
        }

        private void SetControlFlag(int flag, boolean value)
        {
            if (value)
            {
                agentControls |= flag;
            }
            else
            {
                agentControls &= ~flag;
            }
        }

        private void ResetControlFlags()
        {
            // Reset all of the flags except for persistent settings like
            // away, fly, mouselook, and crouching
        	agentControls &= (ControlFlags.AGENT_CONTROL_AWAY & ControlFlags.AGENT_CONTROL_FLY & ControlFlags.AGENT_CONTROL_MOUSELOOK & ControlFlags.AGENT_CONTROL_UP_NEG);
        }

     	/* Camera controls for the agent, mostly a thin wrapper around
    	 * CoordinateFrame. This class is only responsible for state
    	 * tracking and math, it does not send any packets
    	 */ 
    	public class AgentCamera
    	{
    		public float Far;

    		// The camera is a local frame of reference inside of
    		// the larger grid space. This is where the math happens
    		private CoordinateFrame Frame;

    		public final Vector3 getPosition()
    		{
    			return Frame.getOrigin();
    		}
    		public final void setPosition(Vector3 value) throws Exception
    		{
    			Frame.setOrigin(value);
    		}

    		public final Vector3 getAtAxis()
    		{
    			return Frame.getYAxis();
    		}
    		public final void setAtAxis(Vector3 value) throws Exception
    		{
    			Frame.setYAxis(value);
    		}

    		public final Vector3 getLeftAxis()
    		{
    			return Frame.getXAxis();
    		}
    		public final void setLeftAxis(Vector3 value) throws Exception
    		{
    			Frame.setXAxis(value);
    		}

    		public final Vector3 getUpAxis()
    		{
    			return Frame.getZAxis();
    		}
    		public final void setUpAxis(Vector3 value) throws Exception
    		{
    			Frame.setZAxis(value);
    		}

    		// Default constructor
    		public AgentCamera()
    		{
    			try {
					Frame = new CoordinateFrame(new Vector3(128f, 128f, 20f));
				} catch (Exception e) {	}
    			Far = 128f;
    		}

    		public final void Roll(float angle) throws Exception
    		{
    			Frame.Roll(angle);
    		}

    		public final void Pitch(float angle) throws Throwable
    		{
    			Frame.Pitch(angle);
    		}

    		public final void Yaw(float angle) throws Throwable
    		{
    			Frame.Yaw(angle);
    		}

    		public final void LookDirection(Vector3 target)
    		{
    			Frame.LookDirection(target);
    		}

    		public final void LookDirection(Vector3 target, Vector3 upDirection)
    		{
    			Frame.LookDirection(target, upDirection);
    		}

    		public final void LookDirection(double heading)
    		{
    			Frame.LookDirection(heading);
    		}

    		public final void LookAt(Vector3 position, Vector3 target)
    		{
    			Frame.LookAt(position, target);
    		}

    		public final void LookAt(Vector3 position, Vector3 target, Vector3 upDirection)
    		{
    			Frame.LookAt(position, target, upDirection);
    		}

    		public final void SetPositionOrientation(Vector3 position, float roll, float pitch, float yaw) throws Throwable
    		{
    			Frame.setOrigin(position);

    			Frame.ResetAxes();

    			Frame.Roll(roll);
    			Frame.Pitch(pitch);
    			Frame.Yaw(yaw);
    		}
    	}
    }
}
