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

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import libomv.ParcelManager.Parcel;
import libomv.packets.AgentPausePacket;
import libomv.packets.AgentResumePacket;
import libomv.packets.CloseCircuitPacket;
import libomv.packets.Packet;
import libomv.packets.PacketAckPacket;
import libomv.packets.PacketType;
import libomv.packets.StartPingCheckPacket;
import libomv.packets.UseCircuitCodePacket;
import libomv.primitives.Avatar;
import libomv.primitives.Primitive;
import libomv.types.UUID;
import libomv.types.TerrainPatch;
import libomv.types.Vector2;
import libomv.types.Vector3;
import libomv.utils.Helpers;
import libomv.utils.Logger;
import libomv.utils.Logger.LogLevel;
import libomv.utils.RefObject;

// Simulator is a wrapper for a network connection to a simulator and the
// Region class representing the block of land in the metaverse.
public class Simulator extends Thread {
    /* Simulator (region) properties */
    public class RegionFlags
    {
        /* No flags set */
        public final int None = 0;
        /* Agents can take damage and be killed */
        public final int AllowDamage = 1 << 0;
        /* Landmarks can be created here */
        public final int AllowLandmark = 1 << 1;
        /* Home position can be set in this sim */
        public final int AllowSetHome = 1 << 2;
        /* Home position is reset when an agent teleports away */
        public final int ResetHomeOnTeleport = 1 << 3;
        /* Sun does not move */
        public final int SunFixed = 1 << 4;
        /* No object, land, etc. taxes */
        public final int TaxFree = 1 << 5;
        /* Disable heightmap alterations (agents can still plant foliage) */
        public final int BlockTerraform = 1 << 6;
        /* Land cannot be released, sold, or purchased */
        public final int BlockLandResell = 1 << 7;
        /* All content is wiped nightly */
        public final int Sandbox = 1 << 8;
        /* Unknown: Related to the availability of an overview world map tile.(Think mainland images when zoomed out.) */
        public final int NullLayer = 1 << 9;
        /* Unknown: Related to region debug flags. Possibly to skip processing of agent interaction with world. */
        public final int SkipAgentAction = 1 << 10;
        /* Region does not update agent prim interest lists. Internal debugging option. */
        public final int SkipUpdateInterestList = 1 << 11;
        /* No collision detection for non-agent objects */
        public final int SkipCollisions = 1 << 12;
        /* No scripts are ran */
        public final int SkipScripts = 1 << 13;
        /* All physics processing is turned off */
        public final int SkipPhysics = 1 << 14;
        /* Region can be seen from other regions on world map. (Legacy world map option?) */
        public final int ExternallyVisible = 1 << 15;
        /* Region can be seen from mainland on world map. (Legacy world map option?) */
        public final int MainlandVisible = 1 << 16;
        /* Agents not explicitly on the access list can visit the region. */
        public final int PublicAllowed = 1 << 17;
        /* Traffic calculations are not run across entire region, overrides parcel settings. */
        public final int BlockDwell = 1 << 18;
        /* Flight is disabled (not currently enforced by the sim) */
        public final int NoFly = 1 << 19;
        /* Allow direct (p2p) teleporting */
        public final int AllowDirectTeleport = 1 << 20;
        /* Estate owner has temporarily disabled scripting */
        public final int EstateSkipScripts = 1 << 21;
        /* Restricts the usage of the LSL llPushObject function, applies to whole region. */
        public final int RestrictPushObject = 1 << 22;
        /* Deny agents with no payment info on file */
        public final int DenyAnonymous = 1 << 23;
        /* Deny agents with payment info on file */
        public final int DenyIdentified = 1 << 24;
        /* Deny agents who have made a monetary transaction */
        public final int DenyTransacted = 1 << 25;
        /* Parcels within the region may be joined or divided by anyone, not just estate owners/managers. */
        public final int AllowParcelChanges = 1 << 26;
        /* Abuse reports sent from within this region are sent to the estate owner defined email. */
        public final int AbuseEmailToEstateOwner = 1 << 27;
        /* Region is Voice Enabled */
        public final int AllowVoice = 1 << 28;
        /* Removes the ability from parcel owners to set their parcels to show in search. */
        public final int BlockParcelSearch = 1 << 29;
        /* Deny agents who have not been age verified from entering the region. */
        public final int DenyAgeUnverified = 1 << 30;
    }

    /* Access level for a simulator */
    public enum SimAccess 
    {
        /* Minimum access level, no additional checks */
        Min(0),
        /* Trial accounts allowed */
        Trial(7),
        /* PG rating */
        PG(13),
        /* Mature rating */
        Mature(21),
        /* Adult rating */
        Adult(42),
        /* Simulator is offline */
        Down(0xFE),
        /* Simulator does not exist */
        NonExistent(0xFF);
        
        public SimAccess setValue(int value)
        {
        	for (SimAccess e : values())
        	{
        		if (e._value == value)
        			return e;
        	}
       		return Min;
       	}
        
        public byte getValue()
        {
        	return _value;
        }
        private byte _value;
        private SimAccess(int value)
        {
        	_value = (byte)value;
        }
    }

    /* Simulator Statistics */
    public final class SimStats
    {
        /* Total number of packets sent by this simulator from this agent */
        public AtomicLong SentPackets;
        /* Total number of packets received by this simulator to this agent */
        public AtomicLong RecvPackets;
        /* Total number of bytes sent by this simulator to this agent */
        public AtomicLong SentBytes;
        /* Total number of bytes received by this simulator to this agent */
        public AtomicLong RecvBytes;
        /* Time in seconds agent has been connected to simulator */
        public long ConnectTime;
        /* Total number of packets that have been resent */
        public AtomicLong ResentPackets;
        /* Total number of resent packets recieved */
        public AtomicLong ReceivedResends;
        /* Total number of pings sent to this simulator by this agent */
        public AtomicInteger SentPings;
        /* Total number of ping replies sent to this agent by this simulator */
        public AtomicLong ReceivedPongs;
        /* Incoming bytes per second
         * 
         * It would be nice to have this calculated on the fly, but this is far, far easier
         */
        public int IncomingBPS;
        /* Outgoing bytes per second
         * 
         * It would be nice to have this claculated on the fly, but this is far, far easier
         */
        public int OutgoingBPS;
        /* Time last ping was sent */
        public long LastPingSent;
        /*  */
        public long LastLag;
        /* ID of last Ping sent */
        public byte LastPingID;
        /*  */
        public AtomicLong MissedPings;
        /* Current time dilation of this simulator */
        public float Dilation;
        /* Current Frames per second of simulator */
        public int FPS;
        /* Current Physics frames per second of simulator */
        public float PhysicsFPS;
        /*  */
        public float AgentUpdates;
        /*  */
        public float FrameTime;
        /*  */
        public float NetTime;
        /*  */
        public float PhysicsTime;
        /*  */
        public float ImageTime;
        /*  */
        public float ScriptTime;
        /*  */
        public float AgentTime;
        /*  */
        public float OtherTime;
        /* Total number of objects Simulator is simulating */
        public int Objects;
        /* Total number of Active (Scripted) objects running */
        public int ScriptedObjects;
        /* Number of agents currently in this simulator */
        public int Agents;
        /* Number of agents in neighbor simulators */
        public int ChildAgents;
        /* Number of Active scripts running in this simulator */
        public int ActiveScripts;
        /*  */
        public int LSLIPS;
        /*  */
        public int INPPS;
        /*  */
        public int OUTPPS;
        /* Number of downloads pending */
        public int PendingDownloads;
        /* Number of uploads pending */
        public int PendingUploads;
        /*  */
        public int VirtualSize;
        /*  */
        public int ResidentSize;
        /* Number of local uploads pending */
        public int PendingLocalUploads;
        /* Unacknowledged bytes in queue */
        public int UnackedBytes;
    }
    
    public final class IncomingPacketIDCollection
    {
        private final int[] Items;
        private HashSet<Integer> hashSet;
        private int first = 0;
        private int next = 0;
        private int capacity;

        public IncomingPacketIDCollection(int capacity)
        {
            this.capacity = capacity;
            Items = new int[capacity];
            hashSet = new HashSet<Integer>();
        }

        public boolean tryEnqueue(int ack)
        {
            synchronized (hashSet)
            {
                if (hashSet.add(ack))
                {
                    Items[next] = ack;
                    next = (next + 1) % capacity;
                    if (next == first)
                    {
                        hashSet.remove(Items[first]);
                        first = (first + 1) % capacity;
                    }
                    return true;
                }
            }
            return false;
        }
    }

    /* The reference to the client that this Simulator object is attached to */
	private GridClient _Client;
    public GridClient getClient() {
		return _Client;
	}

    /* A Unique Cache identifier for this simulator */
    public UUID ID = UUID.Zero;

    /* The capabilities for this simulator */
    private CapsManager _Caps = null;
    
    private long _Handle;
    public long getHandle() {
		return _Handle;
	}
    public void setHandle(long handle) {
		_Handle = handle;
	}

    /* The current sequence number for packets sent to this simulator. Must be interlocked
     * before modifying. Only useful for applications manipulating sequence numbers
     */
	private AtomicInteger Sequence;

    /* Sequence numbers of packets we've received (for duplicate checking) */
    private IncomingPacketIDCollection PacketArchive;
    /* ACKs that are queued up to be sent to the simulator */
	private ConcurrentLinkedQueue<Integer> PendingAcks;
	/* Packets we sent out that need ACKs from the simulator */
	private HashMap<Integer, NetworkManager.OutgoingPacket> NeedAck; // int -> Packet
    /* Sequence number for pause/resume */
    private AtomicInteger pauseSerial;

    /* Indicates if UDP connection to the sim is fully established */
    public boolean handshakeComplete;

    public final TerrainPatch[] Terrain;

    public final Vector2[] WindSpeeds;
    
    // Provides access to an internal thread-safe dictionary containing parcel information found in this simulator
    public HashMap<Integer, Parcel> Parcels = new HashMap<Integer, Parcel>();

    // simulator <> parcel LocalID Map
    private int[] _ParcelMap = new int[4096];
    private boolean _DownloadingParcelMap = false;

    // Provides access to an internal thread-safe multidimensional array containing a x,y grid mapped to each 64x64 parcel's LocalID.
    public synchronized final int getParcelMap(int x, int y)
    {
    	return _ParcelMap[x * 64 + y];
    }
    public synchronized final void setParcelMap(int x, int y, int value)
    {
    	_ParcelMap[x * 64 + y] = value;
    }
    public synchronized final void clearParcelMap()
    {
        for (int y = 0; y < 64; y++)
        {
            for (int x = 0; x < 64; x++)
            {
            	_ParcelMap[x * 64 + y] = 0;
            }
        }
    }

    // Provides access to an internal thread-safe multidimensional array containing a x,y grid mapped to each 64x64 parcel's LocalID.
    public synchronized final boolean getDownloadingParcelMap()
    {
    	return _DownloadingParcelMap;
    }
    public synchronized final void setDownloadingParcelMap(boolean value)
    {
    	_DownloadingParcelMap = value;
    }

    /**
     * Checks simulator parcel map to make sure it has downloaded all data successfully
     * 
     * @return true if map is full (contains no 0's)
     */
    public final boolean IsParcelMapFull()
    {
        for (int y = 0; y < 64; y++)
        {
            for (int x = 0; x < 64; x++)
            {
                if (getParcelMap()[y][x] == 0)
                {
                    return false;
                }
            }
        }
        return true;
    }

    /* Statistics information for this simulator and the connection
     * to the simulator, calculated by the simulator itself and the library
     */
    public SimStats Stats;

	/* The current version of software this simulator is running */
    public String SimVersion = "";

    /*  */
    public String Name = "";

    /* A 64x64 grid of parcel coloring values. The values stored 
     * in this array are of the {@link ParcelArrayType} type
     */
    public byte[] ParcelOverlay = new byte[4096];
    /*  */
    public int ParcelOverlaysReceived;
    /*  */
    public float TerrainHeightRange00;
    /*  */
    public float TerrainHeightRange01;
    /*  */
    public float TerrainHeightRange10;
    /*  */
    public float TerrainHeightRange11;
    /*  */
    public float TerrainStartHeight00;
    /*  */
    public float TerrainStartHeight01;
    /*  */
    public float TerrainStartHeight10;
    /*  */
    public float TerrainStartHeight11;
    /*  */
    public float WaterHeight;
    /*  */
    public UUID SimOwner = UUID.Zero;
    /*  */
    public UUID TerrainBase0 = UUID.Zero;
    /*  */
    public UUID TerrainBase1 = UUID.Zero;
    /*  */
    public UUID TerrainBase2 = UUID.Zero;
    /*  */
    public UUID TerrainBase3 = UUID.Zero;
    /*  */
    public UUID TerrainDetail0 = UUID.Zero;
    /*  */
    public UUID TerrainDetail1 = UUID.Zero;
    /*  */
    public UUID TerrainDetail2 = UUID.Zero;
    /*  */
    public UUID TerrainDetail3 = UUID.Zero;
    /* true if your agent has Estate Manager rights on this region */
    public boolean IsEstateManager;
    /*  */
    public int Flags; /* Simulator.RegionFlags */
    /*  */
    public byte Access; /* Simulator.SimAccess */
    /*  */
    public float BillableFactor;
    /* The regions Unique ID */
    public UUID RegionID = UUID.Zero;

    
    
    /* The physical data center the simulator is located Known values are: Dallas, SF */
    public String ColoLocation;
    /* The CPU Class of the simulator
     * Most full mainland/estate sims appear to be 5, Homesteads and Openspace appear to be 501
     */
    public int CPUClass;
    /* The number of regions sharing the same CPU as this one "Full Sims" appear to be 1, Homesteads appear to be 4 */
    public int CPURatio;
    /* The billing product name
     * Known values are: Mainland / Full Region (Sku: 023)
     *                   Estate / Full Region (Sku: 024)
     *                   Estate / Openspace (Sku: 027)
     *                   Estate / Homestead (Sku: 029)
     *                   Mainland / Homestead (Sku: 129) (Linden Owned)
     *                   Mainland / Linden Homes (Sku: 131)
     */
    public String ProductName;
    /* The billing product SKU
     * Known values are: 023 Mainland / Full Region
     *                   024 Estate / Full Region
     *                   027 Estate / Openspace
     *                   029 Estate / Homestead
     *                   129 Mainland / Homestead (Linden Owned)
     *                   131 Linden Homes / Full Region
     */
    public String ProductSku;

	private DatagramSocket Connection;
    // The IP address and port of the server.
	private InetSocketAddress ipEndPoint;

    /* A thread-safe dictionary containing avatars in a simulator */
    private Hashtable<Integer, Avatar> ObjectsAvatars = new Hashtable<Integer, Avatar>();

    public Hashtable<Integer, Avatar> getObjectsAvatars()
    {
    	return ObjectsAvatars;
    }

    /* A thread-safe dictionary containing primitives in a simulator */
    private Hashtable<Integer, Primitive> ObjectsPrimitives = new Hashtable<Integer, Primitive>();

    public Hashtable<Integer, Primitive> getObjectsPrimitives()
    {
    	return ObjectsPrimitives;
    }
    
	/* Coarse locations of avatars in this simulator */
    private Hashtable<UUID, Vector3> avatarPositions = new Hashtable<UUID, Vector3>();
    public void setAvatarPositions(Hashtable<UUID, Vector3> avatarPositions) {
		this.avatarPositions = avatarPositions;
	}

	public Hashtable<UUID, Vector3> getAvatarPositions() {
		return avatarPositions;
	}

    /* AvatarPositions key representing TrackAgent target */
    private UUID preyID = UUID.Zero;
    public final UUID getPreyID()
    {
         return preyID;
    }
    public final void setPreyID(UUID id)
    {
         preyID = id;
    }

	public InetSocketAddress getIPEndPoint() {
		return ipEndPoint;
	}
	// A boolean representing whether there is a working connection to the
	// simulator or not.
	private boolean connected;
	public boolean getConnected() {
		return connected;
	}
	/* Used internally to track sim disconnections, do not modify this variable. */
	private boolean DisconnectCandidate = false;
	public boolean getDisconnectCandidate() {
		return DisconnectCandidate;
	}

	public void setDisconnectCandidate(boolean val) {
		DisconnectCandidate = val;
	}

	private Timer AckTimer;

	private Timer StatsTimer;

	private Timer PingTimer;

    @Override
    public int hashCode()
    {
        return ((Long)getHandle()).hashCode();
    }

	public Simulator(GridClient client, InetAddress ip, short port, long handle) throws Exception
	{
		// Create an endpoint that we will be communicating with
	    this(client, new InetSocketAddress(ip, port), handle);
	}

	public Simulator(GridClient client, InetSocketAddress endPoint, long handle) throws Exception
	{
		_Client = client;

		ipEndPoint = endPoint;
		Connection = new DatagramSocket();
        connected = false;
		DisconnectCandidate = false;

		Sequence.set(0);
        _Handle = handle;
        
		// Initialize the dictionary for reliable packets waiting on ACKs from the server
		NeedAck = new HashMap<Integer, NetworkManager.OutgoingPacket>();

		// Initialize the lists of sequence numbers we've received so far
        PacketArchive = new IncomingPacketIDCollection(Settings.PACKET_ARCHIVE_SIZE);
		PendingAcks = new ConcurrentLinkedQueue<Integer>();

		if (client.Settings.STORE_LAND_PATCHES)
        {
            Terrain = new TerrainPatch[16 * 16];
            WindSpeeds = new Vector2[16 * 16];
        }
        else
        {
            Terrain = null;
            WindSpeeds = null;        
        }

	}

    /** Attempt to connect to this simulator
     * 
     *  @param moveToSim Whether to move our agent in to this sim or not
     *  @return True if the connection succeeded or connection status is
     *          unknown, false if there was a failure
     * @throws Exception 
     */
    public final boolean Connect(boolean moveToSim) throws Exception
    {
        handshakeComplete = false;

        if (connected)
        {
            UseCircuitCode();
            if (moveToSim)
            {
                _Client.Self.CompleteAgentMovement(this);
            }
            return true;
        }

        if (AckTimer == null)
        {
            AckTimer = new Timer();
        }
    	AckTimer.schedule(new TimerTask()
    	{
    		public void run() {
    	    	try {
    				AckTimer_Elapsed();
    			} catch (Exception e) {
    				e.printStackTrace();
    			}
    		}
    	}, Settings.NETWORK_TICK_INTERVAL);

        // Timer for recording simulator connection statistics
        if (StatsTimer == null)
        {
            StatsTimer = new Timer();
    		StatsTimer.scheduleAtFixedRate(new TimerTask()
    		{
    			public void run() {
    				try {
    					StatsTimer_Elapsed();
    				} catch (Exception e) {
    					e.printStackTrace();
    				}
    			}
    		}, 1000, 1000);
        }

        // Timer for periodically pinging the simulator
        if (PingTimer == null && _Client.Settings.SEND_PINGS)
        {
            PingTimer = new Timer();
    		PingTimer.scheduleAtFixedRate(new TimerTask()
    		{
    			public void run() {
    				try {
    					PingTimer_Elapsed();
    				} catch (Exception e) {
    					e.printStackTrace();
    				}
    			}
    		}, Settings.PING_INTERVAL, Settings.PING_INTERVAL);
        }

        Logger.Log("Connecting to " + ipEndPoint.toString(), LogLevel.Info);
		Connection.connect(ipEndPoint);

		try
        {
    		// runs background thread to read from DatagramSocket
            start();

            // Mark ourselves as connected before firing everything else up
            connected = true;

            // Initiate connection
            UseCircuitCode();

            Stats.ConnectTime = System.currentTimeMillis();

            // Move our agent in to the sim to complete the connection
            if (moveToSim)
            {
                _Client.Self.CompleteAgentMovement(this);
            }

            Logger.Log("Waiting for connection", LogLevel.Info);
			while (true) {
				if (connected || System.currentTimeMillis() - Stats.ConnectTime > _Client.Settings.SIMULATOR_TIMEOUT) {
					if (connected) {
						Logger.Log("Connected!", LogLevel.Info);
					} else {
						Logger.Log("Giving up on waiting for RegionHandshake for " + this.toString(), LogLevel.Warning);
		                return false;
					}
					break;
				}
				Thread.sleep(10);
			}

            if (_Client.Settings.SEND_AGENT_THROTTLE)
            {
                _Client.Throttle.Set(this);
            }

            if (_Client.Settings.SEND_AGENT_UPDATES)
            {
                _Client.Self.SendMovementUpdate(true, this);
            }

            return true;
        }
        catch (Throwable e)
        {
        	Logger.Log(e.getMessage(), LogLevel.Error);
        }

        return false;
    }

	/* Initiates connection to the simulator */
    public final void UseCircuitCode() throws Exception
    {
        // Send the UseCircuitCode packet to initiate the connection
		UseCircuitCodePacket use = new UseCircuitCodePacket();
		use.CircuitCode.Code = _Client.Network.getCircuitCode();
		use.CircuitCode.ID = _Client.Self.getAgentID();
		use.CircuitCode.SessionID = _Client.Self.getSessionID();

		// Send the initial packet out
		SendPacket(use);
    }

    public final void SetSeedCaps(String seedcaps)
    {
        if (_Caps != null)
        {
            if (_Caps.getSeedCapsURI().equals(seedcaps))
            {
                return;
            }

            Logger.Log("Unexpected change of seed capability", LogLevel.Warning);
            _Caps.Disconnect(true);
            _Caps = null;
        }

        if (_Client.Settings.ENABLE_CAPS)
        {
            // Connect to the new CAPS system
            if (seedcaps.isEmpty())
            {
            	Logger.Log("Setting up a sim without a valid capabilities server!", LogLevel.Error);
            }
            else
            {
                _Caps = new CapsManager(this, seedcaps);
            }
        }

    }

	public void Disconnect(boolean sendCloseCircuit) throws Exception
	{
        if (connected)
        {
            connected = false;
            
            // Destroy the timers
            if (AckTimer != null)
            {
                AckTimer.cancel();
            }
            if (StatsTimer != null)
            {
                StatsTimer.cancel();
            }
            if (PingTimer != null)
            {
                PingTimer.cancel();
            }

            AckTimer = null;
            StatsTimer = null;
            PingTimer = null;

            // Kill the current CAPS system
            if (_Caps != null)
            {
                _Caps.Disconnect(true);
                _Caps = null;
            }
            
		    if (sendCloseCircuit)
		    {
			    // Send the CloseCircuit notice
		        CloseCircuitPacket close = new CloseCircuitPacket();

		        try
		        {
			        ByteBuffer data = close.ToBytes();
			        Connection.send(new DatagramPacket(data.array(), data.position()));
		        }
		        catch (IOException e)
		        {
			        // There's a high probability of this failing if the network is
			        // disconnected, so don't even bother logging the error
		        }
		    }

		    try
		    {
			    // Shut the socket communication down
			    Connection.close();
		    }
		    catch (Exception e)
		    {
			    Logger.Log(e.toString(), LogLevel.Error, e);
		    }
        }
	}

    /* Instructs the simulator to stop sending update (and possibly other) packets */
    public final void Pause() throws Exception
    {
        AgentPausePacket pause = new AgentPausePacket();
        pause.AgentData.AgentID = _Client.Self.getAgentID();
        pause.AgentData.SessionID = _Client.Self.getSessionID();
        pause.AgentData.SerialNum = (int)pauseSerial.getAndIncrement();

        SendPacket(pause);
    }

    /* Instructs the simulator to resume sending update packets (unpause) */
    public final void Resume() throws Exception
    {
        AgentResumePacket resume = new AgentResumePacket();
        resume.AgentData.AgentID = _Client.Self.getAgentID();
        resume.AgentData.SessionID = _Client.Self.getSessionID();
        resume.AgentData.SerialNum = pauseSerial.getAndIncrement();

        SendPacket(resume);
    }

    /** 
     * Retrieve the terrain height at a given coordinate
     * 
     * @param x Sim X coordinate, valid range is from 0 to 255
     * @param y Sim Y coordinate, valid range is from 0 to 255
     * @param height The terrain height at the given point if the lookup was successful, otherwise 0.0f
     * @return True if the lookup was successful, otherwise false
     */
    public final boolean TerrainHeightAtPoint(int x, int y, RefObject<Float> height)
    {
        if (Terrain != null && x >= 0 && x < 256 && y >= 0 && y < 256)
        {
            int patchX = x / 16;
            int patchY = y / 16;
            x = x % 16;
            y = y % 16;

            TerrainPatch patch = Terrain[patchY * 16 + patchX];
            if (patch != null)
            {
                height.argvalue = patch.Data[y * 16 + x];
                return true;
            }
        }
        height.argvalue = 0.0f;
        return false;
    }

    public final void SendPing() throws Exception
    {
        int oldestUnacked = 0;

        // Get the oldest NeedAck value, the first entry in the sorted dictionary
        synchronized (NeedAck)
        {
            if (!NeedAck.isEmpty())
            {
                Set<Integer> keys = NeedAck.keySet();
                oldestUnacked = keys.iterator().next();
            }
        }

        //if (oldestUnacked != 0)
        //    Client.DebugLog("Sending ping with oldestUnacked=" + oldestUnacked);

        StartPingCheckPacket ping = new StartPingCheckPacket();
        ping.PingID.PingID = Stats.LastPingID++;
        ping.PingID.OldestUnacked = oldestUnacked;
        ping.getHeader().setReliable(false);
        SendPacket(ping);
        Stats.LastPingSent = System.currentTimeMillis();
    }
    
    public URI getCapabilityURI(String capability)
    {
    	if (_Caps != null)
    	    return _Caps.CapabilityURI(capability);
    	return null;
    }

    public boolean getIsEventQueueRunning()
    {
    	if (_Caps != null)
    	    return _Caps.getIsEventQueueRunning();
    	return false;
    }
    
	public void run()
	{
		byte[] RecvBuffer = new byte[4096];
		DatagramPacket p = new DatagramPacket(RecvBuffer, RecvBuffer.length);
		while (true)
		{
			try
			{
				Connection.receive(p);
				Packet packet = null;
				int numBytes;

				// If we're receiving data the sim connection is open
				connected = true;

				// Update the disconnect flag so this sim doesn't time out
				DisconnectCandidate = false;

				synchronized (RecvBuffer)
				{
					// Retrieve the incoming packet
					try {
						numBytes = p.getLength();

						int packetEnd = numBytes - 1;
						int a_packetEnd[] = { packetEnd }; // gaz
						System.out.println("\n<=============== Received packet length=" + numBytes);
						StringBuffer dump = new StringBuffer(numBytes * 2);
						for (int i = 0; i < numBytes; i++) {
							byte value = RecvBuffer[i];
							dump.append(Integer.toHexString(value & 0xFF));
							dump.append(" ");
						}
						System.out.println(dump);
						ByteBuffer byte_buffer = ByteBuffer.wrap(RecvBuffer, 0, numBytes);
						packet = Packet.BuildPacket(byte_buffer, a_packetEnd);
						System.out.println("Decoded packet " + packet.getClass().getName());
						System.out.println(packet.toString());
					}
					catch (IOException e)
					{
						Logger.Log(ipEndPoint.toString() + " socket is closed, shutting down " + Name, LogLevel.Info, e);

						connected = false;
						_Client.Network.DisconnectSim(this, true);
						return;
					}
				}
	            Stats.RecvBytes.addAndGet(numBytes);
	            Stats.RecvPackets.incrementAndGet();
	            if (packet.getHeader().getResent())
	            {
	                Stats.ReceivedResends.incrementAndGet();
	            }

				// Handle appended ACKs
				if (packet.getHeader().getAppendedAcks() && packet.getHeader().AckList != null)
				{
					synchronized (NeedAck)
					{
						for (int ack : packet.getHeader().AckList)
						{
							if (NeedAck.remove(ack) == null)
							{
								Logger.Log("Appended ACK for a packet we didn't send: "
										  + ack, LogLevel.Warning);
							}
						}
					}
				}
				// Handle PacketAck packets
				if (packet.getType() == PacketType.PacketAck)
				{
					PacketAckPacket ackPacket = (PacketAckPacket) packet;

					synchronized (NeedAck)
					{
						for (PacketAckPacket.PacketsBlock block : ackPacket.Packets)
						{
							if (NeedAck.remove(block.ID) == null)
							{
								Logger.Log("Appended ACK for a packet we didn't send: "
										  + block.ID, LogLevel.Warning);
							}
						}
					}
				}

				// Add this packet to the list of ACKs that need to be sent out
				int sequence = packet.getHeader().getSequence();
				PendingAcks.add(sequence);

				// Send out ACKs if we have a lot of them
	            if (PendingAcks.size() >= _Client.Settings.MAX_PENDING_ACKS)
	            {
	                SendPendingAcks();
	            }

				/* Track the sequence number for this packet if it's marked as reliable */
				if (packet.getHeader().getReliable() && !PacketArchive.tryEnqueue(sequence))
				{
					Logger.Log("Received a duplicate " + packet.getType() + ", " + "sequence=" + sequence + ", "
							 + "resent="+ ((packet.getHeader().getResent()) ? "Yes" : "No"), LogLevel.Info);
					// Avoid firing a callback twice for the same packet
					return;
				}

				// Let the network manager distribute the packets to the callbacks
	            _Client.Network.DistributePacket(this, packet);

	            if (_Client.Settings.TRACK_UTILIZATION)
	            {
	                /* TODO Implement Utilization tracking */
	                //Client.Stats.Update(packet.Type.ToString(), OpenMetaverse.Stats.Type.Packet, 0, packet.Length);
	            }

			}
			catch (Exception e)
			{
				e.printStackTrace();
			}
		}
	}

	public void SendPacket(Packet packet) throws Exception
	{
        if (packet.hasVariableBlocks)
        {
        	ByteBuffer[] datas;
            try
            {
                datas = packet.ToBytesMultiple();
            }
            catch (NullPointerException ex)
            {
                Logger.Log("Failed to serialize " + packet.getType() + " packet to one or more payloads due to a missing block or field. StackTrace: " + ex.getStackTrace(), LogLevel.Error);
                return;
            }
            int packetCount = datas.length;

            if (packetCount > 1)
            {
                Logger.DebugLog("Split " + packet.getType() + " packet into " + packetCount + " packets");
            }

            for (int i = 0; i < packetCount; i++)
            {
                SendPacketData(datas[i], packet.getType(), packet.getHeader().getZerocoded());
            }
        }
        else
        {
            ByteBuffer data = packet.ToBytes();
            SendPacketData(data, packet.getType(), packet.getHeader().getZerocoded());
        }
	}

    public final void SendPacketData(ByteBuffer data, PacketType type, boolean doZerocode) throws InterruptedException
    {
        // Zerocode if needed
        if (doZerocode)
        {
    		byte[] zeroBuffer = new byte[2000];
    		int bytes = Helpers.ZeroEncode(data, zeroBuffer);
    		if (bytes <= data.capacity())
    		{
    			data = ByteBuffer.wrap(zeroBuffer, 0, bytes);
    		    data.order(ByteOrder.LITTLE_ENDIAN);
    		}
    		else
    		{
    			// Zero encoding actually grew the buffer beyond the original size
    			data.put(0, (byte)(data.get(0) & ~Helpers.MSG_ZEROCODED));
    			data.position(0);
    		}
        }

        // #region Queue or Send
        NetworkManager.OutgoingPacket outgoingPacket = _Client.Network.new OutgoingPacket(this, data);

        // Send ACK and logout packets directly, everything else goes through the queue
        if (_Client.Settings.THROTTLE_OUTGOING_PACKETS == false || type == PacketType.PacketAck || type == PacketType.LogoutRequest)
        {
            SendPacketFinal(outgoingPacket);
        }
        else
        {
            _Client.Network.PacketOutbox.put(outgoingPacket);
        }
        // #endregion Queue or Send

        // #region Stats Tracking
        if (_Client.Settings.TRACK_UTILIZATION)
        {
		   // Stats tracking
		   Stats.SentBytes.addAndGet(data.capacity());
		   Stats.SentPackets.incrementAndGet();
		   
//         _Client.Stats.Update(type.ToString(), OpenMetaverse.Stats.Type.Packet, data.capacity(), 0);
		   _Client.Network.RaisePacketSentCallback(data.array(), data.capacity(), this);
        }
    }	

	/* Sends out pending acknowledgements */
    private void SendPendingAcks()
    {
		synchronized (PendingAcks)
		{
			if (PendingAcks.size() > 0)
			{
				PacketAckPacket acks = new PacketAckPacket();
				acks.Packets = new PacketAckPacket.PacketsBlock[PendingAcks.size()];
				acks.getHeader().setReliable(false);

				int i = 0;
				for (int ack : PendingAcks)
				{
					acks.Packets[i] = acks.createPacketsBlock();
					acks.Packets[i].ID = ack;
					i++;
				}
				try
				{
					SendPacket(acks);
				}
				catch (Exception ex)
				{
				}
				PendingAcks.clear();
			}
		}
    }

    /**
     * Resend unacknowledged packets
     */
    private void ResendUnacked()
    {
        if (NeedAck.size() > 0)
        {
            ArrayList<NetworkManager.OutgoingPacket> array;

            synchronized (NeedAck)
            {
                // Create a temporary copy of the outgoing packets array to iterate over
                array = new ArrayList<NetworkManager.OutgoingPacket>(NeedAck.size());
                array.addAll(NeedAck.values());
            }

            long now = System.currentTimeMillis();

            // Resend packets
            for (int i = 0; i < array.size(); i++)
            {
                NetworkManager.OutgoingPacket outgoing = array.get(i);

                if (outgoing.TickCount != 0 && now - outgoing.TickCount > _Client.Settings.RESEND_TIMEOUT)
                {
                    if (outgoing.ResendCount.get() < _Client.Settings.MAX_RESEND_COUNT)
                    {
                        if (_Client.Settings.LOG_RESENDS)
                        {
                            Logger.DebugLog(String.format("Resending packet #%d, %d ms have passed", outgoing.SequenceNumber, now - outgoing.TickCount), _Client);
                        }

                        // The TickCount will be set to the current time when the packet is actually sent out again
                        outgoing.TickCount = 0;

                        // Set the resent flag
                        outgoing.Buffer.array()[0] |= Helpers.MSG_RESENT;

                        // Stats tracking
                        outgoing.ResendCount.incrementAndGet();
                        Stats.ResentPackets.incrementAndGet();

                        SendPacketFinal(outgoing);
                    }
                    else
                    {
                        Logger.DebugLog(String.format("Dropping packet #%d after %d failed attempts", outgoing.SequenceNumber, outgoing.ResendCount));

                        synchronized (NeedAck)
                        {
                            NeedAck.remove(outgoing.SequenceNumber);
                        }
                    }
                }
            }
        }
    }

    public final void SendPacketFinal(NetworkManager.OutgoingPacket outgoingPacket)
    {
        ByteBuffer buffer = outgoingPacket.Buffer;
        byte[] bytes = buffer.array();
        byte flags = buffer.get(0);
        boolean isResend = (flags & Helpers.MSG_RESENT) != 0;
        boolean isReliable = (flags & Helpers.MSG_RELIABLE) != 0;

        // Keep track of when this packet was sent out (right now)
        outgoingPacket.TickCount = System.currentTimeMillis();

        // #region ACK Appending
        int dataLength = buffer.capacity();

        // Keep appending ACKs until there is no room left in the packet or there are
        // no more ACKs to append
        int ackCount = 0; 
        while (dataLength + 5 < buffer.limit() && !PendingAcks.isEmpty())
        {
        	dataLength += Helpers.UInt32ToBytesB(PendingAcks.poll(), bytes, dataLength);
            ++ackCount;
        }

        if (ackCount > 0)
        {
            // Set the last byte of the packet equal to the number of appended ACKs
            buffer.put(dataLength++, (byte)ackCount);
            // Set the appended ACKs flag on this packet
            buffer.put(0, (byte)(flags | Helpers.MSG_APPENDED_ACKS));
        }
        // #endregion ACK Appending

        if (!isResend)
        {
            // Not a resend, assign a new sequence number
            outgoingPacket.SequenceNumber = Sequence.incrementAndGet();
            Helpers.UInt32ToBytesB(outgoingPacket.SequenceNumber, bytes, 1);

            if (isReliable)
            {
                // Add this packet to the list of ACK responses we are waiting on from the server
                synchronized (NeedAck)
                {
                    NeedAck.put(outgoingPacket.SequenceNumber, outgoingPacket);
                }
            }
        }

		try
		{
			Connection.send(new DatagramPacket(buffer.array(), dataLength));
		}
		catch (IOException ex)
		{
			Logger.Log(ex.toString(), LogLevel.Error, ex);
		}
    }

	/* #region timer callbacks */
	private void AckTimer_Elapsed()
	{
		if (!connected) {
			return;
		}

		SendPendingAcks();
		ResendUnacked();

		try
        {
    		AckTimer.schedule(new TimerTask()
    		{
    			public void run() {
    				try {
    					AckTimer_Elapsed();
    				} catch (Exception e) {
    					e.printStackTrace();
    				}
    			}
    		}, Settings.NETWORK_TICK_INTERVAL);
        }
        catch (Throwable t)
        {
        }
	}

    private void StatsTimer_Elapsed()
    {
        long old_in = 0, old_out = 0;
        long recv = Stats.RecvBytes.longValue();
        long sent = Stats.SentBytes.longValue();

/*
 *         if (InBytes.size() >= Client.Settings.STATS_QUEUE_SIZE)

        {
            old_in = InBytes.poll();
        }
        if (OutBytes.size() >= Client.Settings.STATS_QUEUE_SIZE)
        {
            old_out = OutBytes.poll();
        }

        InBytes.offer(recv);
        OutBytes.offer(sent);
*/
        if (old_in > 0 && old_out > 0)
        {
            Stats.IncomingBPS = (int)(recv - old_in) / _Client.Settings.STATS_QUEUE_SIZE;
            Stats.OutgoingBPS = (int)(sent - old_out) / _Client.Settings.STATS_QUEUE_SIZE;
            //Client.Log("Incoming: " + Stats.IncomingBPS + " Out: " + Stats.OutgoingBPS +
            //    " Lag: " + Stats.LastLag + " Pings: " + Stats.ReceivedPongs +
            //    "/" + Stats.SentPings, Helpers.LogLevel.Debug); 
        }
    }

    private void PingTimer_Elapsed() throws Exception
    {
        SendPing();
        Stats.SentPings.incrementAndGet();
    }
}

