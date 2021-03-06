/**
 * Copyright (c) 2006-2014, openmetaverse.org
 * Copyright (c) 2009-2017, Frederick Martian
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
 * - Neither the name of the openmetaverse.org or libomv-java project nor the
 *   names of its contributors may be used to endorse or promote products derived
 *   from this software without specific prior written permission.
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
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import libomv.NetworkManager.DisconnectedCallbackArgs;
import libomv.NetworkManager.EventQueueRunningCallbackArgs;
import libomv.Simulator.RegionProtocols;
import libomv.VisualParams.VisualAlphaParam;
import libomv.VisualParams.VisualColorParam;
import libomv.VisualParams.VisualParam;
import libomv.StructuredData.OSD;
import libomv.StructuredData.OSD.OSDFormat;
import libomv.StructuredData.OSDMap;
import libomv.assets.AssetItem.AssetType;
import libomv.assets.AssetManager;
import libomv.assets.AssetManager.AssetDownload;
import libomv.assets.AssetManager.ImageDownload;
import libomv.assets.AssetTexture;
import libomv.assets.AssetWearable;
import libomv.assets.AssetWearable.AvatarTextureIndex;
import libomv.assets.AssetWearable.WearableType;
import libomv.assets.TexturePipeline.TextureRequestState;
import libomv.capabilities.CapsClient;
import libomv.imaging.Baker;
import libomv.inventory.InventoryAttachment;
import libomv.inventory.InventoryFolder;
import libomv.inventory.InventoryFolder.FolderType;
import libomv.inventory.InventoryItem;
import libomv.inventory.InventoryManager.InventorySortOrder;
import libomv.inventory.InventoryNode;
import libomv.inventory.InventoryNode.InventoryType;
import libomv.inventory.InventoryObject;
import libomv.inventory.InventoryWearable;
import libomv.packets.AgentCachedTexturePacket;
import libomv.packets.AgentCachedTexturePacket.WearableDataBlock;
import libomv.packets.AgentCachedTextureResponsePacket;
import libomv.packets.AgentIsNowWearingPacket;
import libomv.packets.AgentSetAppearancePacket;
import libomv.packets.AgentWearablesRequestPacket;
import libomv.packets.AgentWearablesUpdatePacket;
import libomv.packets.DetachAttachmentIntoInvPacket;
import libomv.packets.Packet;
import libomv.packets.PacketType;
import libomv.packets.RebakeAvatarTexturesPacket;
import libomv.packets.RezMultipleAttachmentsFromInvPacket;
import libomv.packets.RezSingleAttachmentFromInvPacket;
import libomv.primitives.Avatar;
import libomv.primitives.Primitive.AttachmentPoint;
import libomv.primitives.TextureEntry;
import libomv.types.Color4;
import libomv.types.PacketCallback;
import libomv.types.Permissions;
import libomv.types.UUID;
import libomv.types.Vector3;
import libomv.utils.Callback;
import libomv.utils.CallbackArgs;
import libomv.utils.CallbackHandler;
import libomv.utils.Helpers;
import libomv.utils.Logger;
import libomv.utils.Logger.LogLevel;
import libomv.utils.MultiMap;
import libomv.utils.Settings.SettingsUpdateCallbackArgs;
import libomv.utils.TimeoutEvent;

public class AppearanceManager implements PacketCallback
{
    // Bake layers for avatar appearance
    public enum BakeType
    {
    	Unknown, Head, UpperBody, LowerBody, Eyes, Skirt, Hair;
        public static BakeType setValue(int value)
        {
            if (value <= 0 && value < Hair.ordinal())
            	return values()[value + 1];
            return Unknown;
        }

        public static byte getValue(BakeType value)
        {
            return (byte) (value.ordinal() - 1);
        }
        
        public static int getNumValues()
        {
        	return values().length - 1;
        }
 
        public byte getValue()
        {
            return (byte) (ordinal() - 1);
        }
     }

    // Appearance Flags, introdued with server side baking, currently unused
    // [Flags]
    public enum AppearanceFlags
    {
        None;

        public static AppearanceFlags setValue(int value)
        {
            if (value >= 0 && value < values().length)
            	return values()[value];
            Logger.Log("Unknown Appearance flag value" + value, LogLevel.Warning);
            return None;
        }

        public static byte getValue(BakeType value)
        {
            return (byte) (value.ordinal());
        }
    }

    // #region Constants
    // Maximum number of concurrent downloads for wearable assets and textures 
    private static final int MAX_CONCURRENT_DOWNLOADS = 5;
    // Maximum number of concurrent uploads for baked textures 
    private static final int MAX_CONCURRENT_UPLOADS = 6;
    // Timeout for fetching inventory listings
    private static final int INVENTORY_TIMEOUT = 1000 * 30;
    // Timeout for fetching a single wearable, or receiving a single packet response 
    private static final int WEARABLE_TIMEOUT = 1000 * 30;
    // Timeout for fetching a single texture 
    private static final int TEXTURE_TIMEOUT = 1000 * 120;
    // Timeout for uploading a single baked texture 
    private static final int UPLOAD_TIMEOUT = 1000 * 90;
    // Number of times to retry bake upload 
    private static final int UPLOAD_RETRIES = 2;
    // When changing outfit, kick off rebake after 20 seconds has passed since the last change 
    private static final int REBAKE_DELAY = 1000 * 20;

    // Total number of wearables allowed for each avatar
    public static final int WEARABLE_COUNT_MAX = 60;
    // Total number of wearables for each avatar 
    public static final int WEARABLE_COUNT = 16;
    // Total number of baked textures on each avatar
    public static final int BAKED_TEXTURE_COUNT = 6;
    // Total number of wearables per bake layer 
    public static final int WEARABLES_PER_LAYER = 9;
    // Map of what wearables are included in each bake 
    public static final WearableType[][] WEARABLE_BAKE_MAP = new WearableType[][]
    {
        new WearableType[] { WearableType.Shape, WearableType.Skin,    WearableType.Tattoo,  WearableType.Hair,    WearableType.Alpha,   WearableType.Invalid, WearableType.Invalid,    WearableType.Invalid,      WearableType.Invalid },
        new WearableType[] { WearableType.Shape, WearableType.Skin,    WearableType.Tattoo,  WearableType.Shirt,   WearableType.Jacket,  WearableType.Gloves,  WearableType.Undershirt, WearableType.Alpha,        WearableType.Invalid },
        new WearableType[] { WearableType.Shape, WearableType.Skin,    WearableType.Tattoo,  WearableType.Pants,   WearableType.Shoes,   WearableType.Socks,   WearableType.Jacket,     WearableType.Underpants,   WearableType.Alpha   },
        new WearableType[] { WearableType.Eyes,  WearableType.Invalid, WearableType.Invalid, WearableType.Invalid, WearableType.Invalid, WearableType.Invalid, WearableType.Invalid,    WearableType.Invalid,      WearableType.Invalid },
        new WearableType[] { WearableType.Skirt, WearableType.Invalid, WearableType.Invalid, WearableType.Invalid, WearableType.Invalid, WearableType.Invalid, WearableType.Invalid,    WearableType.Invalid,      WearableType.Invalid },
        new WearableType[] { WearableType.Hair,  WearableType.Invalid, WearableType.Invalid, WearableType.Invalid, WearableType.Invalid, WearableType.Invalid, WearableType.Invalid,    WearableType.Invalid,      WearableType.Invalid }
    };

    // Magic values to finalize the cache check hashes for each bake 
    public static final UUID[] BAKED_TEXTURE_HASH = new UUID[]
    {
        new UUID("18ded8d6-bcfc-e415-8539-944c0f5ea7a6"),
        new UUID("338c29e3-3024-4dbb-998d-7c04cf4fa88f"),
        new UUID("91b4a2c7-1b1a-ba16-9a16-1f8f8dcc1c3f"),
        new UUID("b2cf28af-b840-1071-3c6a-78085d8128b5"),
        new UUID("ea800387-ea1a-14e0-56cb-24f2022f969a"),
        new UUID("0af1ef7c-ad24-11dd-8790-001f5bf833e8")
    };
    // Default avatar texture, used to detect when a custom texture is not set for a face
    public static final UUID DEFAULT_AVATAR_TEXTURE = new UUID("c228d1cf-4b5d-4ba8-84f4-899a0796aa97");

    // #endregion Constants

    // #region Structs / Classes

    // Contains information about a wearable inventory item
    public class WearableData
    {
        //Inventory ItemID of the wearable</summary>
        public UUID ItemID;
        //AssetID of the wearable asset</summary>
        public UUID AssetID;
        //WearableType of the wearable</summary>
        public WearableType WearableType;
        //AssetType of the wearable</summary>
        public libomv.assets.AssetItem.AssetType AssetType;
        // Asset data for the wearable</summary>
        public AssetWearable Asset;
        
        @Override
        public String toString()
        {
            return String.format("ItemID: %s, AssetID: %s, WearableType: %s, AssetType: %s, Asset: %s",
                ItemID, AssetID, WearableType, AssetType, Asset != null ? Asset.Name : "(null)");
        }
    }

    // Data collected from visual params for each wearable needed for the calculation of the color
    public class ColorParamInfo
    {
        public VisualParam VisualParam;
        public VisualColorParam VisualColorParam;
        public float Value;
        public WearableType WearableType;
    }

    // Holds a texture assetID and the data needed to bake this layer into an outfit texture.
    // Used to keep track of currently worn textures and baking data
    public class TextureData
    {
        // A texture AssetID
        public UUID TextureID;
        // Asset data for the texture
        public AssetTexture Texture;
        // Collection of alpha masks that needs applying
        public HashMap<libomv.VisualParams.VisualAlphaParam, Float> AlphaMasks;
        // Tint that should be applied to the texture
        public Color4 Color;
        // The avatar texture index this texture is for
        public AvatarTextureIndex TextureIndex;
        // Host address for this texture
        public String Host;

        @Override
        public String toString()
        {
            return String.format("TextureID: %s, Texture: %s",
                TextureID, Texture != null ? Texture.getAssetData().length + " bytes" : "(null)");
        }
    }

    // #endregion Structs / Classes

    // #region Event delegates, Raise Events

    // Triggered when an AgentWearablesUpdate packet is received, telling us what our avatar is currently wearing
    // <see cref="RequestAgentWearables"/> request.
    public class AgentWearablesReplyCallbackArgs implements CallbackArgs
    {
        // Construct a new instance of the AgentWearablesReplyEventArgs class
        public AgentWearablesReplyCallbackArgs()
        {
        }
    }

    public CallbackHandler<AgentWearablesReplyCallbackArgs> OnAgentWearablesReply = new CallbackHandler<AgentWearablesReplyCallbackArgs>();

    // Raised when an AgentCachedTextureResponse packet is received, giving a list of cached bakes that were found
    // on the simulator <see cref="RequestCachedBakes"/> request.
    public class AgentCachedBakesReplyCallbackArgs implements CallbackArgs
    {
    	private final int serialNum;
    	private final int numBakes;
    	
    	public int getSerialNum()
    	{
    		return serialNum;
    	}
    	
    	public int getNumBakes()
    	{
    		return numBakes;
    	}
    	
        // Construct a new instance of the AgentCachedBakesReplyEventArgs class
        public AgentCachedBakesReplyCallbackArgs(int serialNum, int numBakes)
        {
        	this.serialNum = serialNum;
        	this.numBakes = numBakes;
        }
    }

    public CallbackHandler<AgentCachedBakesReplyCallbackArgs> OnAgentCachedBakesReply = new CallbackHandler<AgentCachedBakesReplyCallbackArgs>();

    // Raised when appearance data is sent to the simulator, also indicates the main appearance thread is finished.
    // <see cref="RequestAgentSetAppearance"/> request.
    public class AppearanceSetCallbackArgs implements CallbackArgs
    {
        private final boolean m_success;

        // Indicates whether appearance setting was successful
        public boolean getSuccess() { return m_success; }
        /**
         * Triggered when appearance data is sent to the sim and the main appearance thread is done.
         *
         * @param success Indicates whether appearance setting was successful
         */
        public AppearanceSetCallbackArgs(boolean success)
        {
            this.m_success = success;
        }
    }

    public CallbackHandler<AppearanceSetCallbackArgs> OnAppearanceSet = new CallbackHandler<AppearanceSetCallbackArgs>();

    //  Triggered when the simulator requests the agent rebake its appearance. 
    // <see cref="RebakeAvatarRequest"/>
    public class RebakeAvatarTexturesCallbackArgs implements CallbackArgs
    {
        private final UUID m_textureID;

        // The ID of the Texture Layer to bake
        public UUID getTextureID() { return m_textureID; }

        /**
         * Triggered when the simulator sends a request for this agent to rebake its appearance
         * 
         * @param textureID The ID of the Texture Layer to bake
         */
        public RebakeAvatarTexturesCallbackArgs(UUID textureID)
        {
            this.m_textureID = textureID;
        }
    }

    public CallbackHandler<RebakeAvatarTexturesCallbackArgs> OnRebakeAvatarReply = new CallbackHandler<RebakeAvatarTexturesCallbackArgs>();

    // #endregion

    // #region Properties and public fields

    /**
     * Returns true if AppearanceManager is busy and trying to set or change appearance will fail
     */
    public boolean getManagerBusy()
    {
        return _AppearanceThread.isAlive();
    }

    // Visual parameters last sent to the sim
    public byte[] MyVisualParameters = null;

    // Textures about this client sent to the sim
    public TextureEntry MyTextures = null;

    // #endregion Properties

    // #region Private Members

    // A cache of wearables currently being worn
    private MultiMap<WearableType, WearableData> _Wearables = new MultiMap<WearableType, WearableData>();
    // A cache of textures currently being worn
    private TextureData[] _Textures = new TextureData[AvatarTextureIndex.getNumValues()];
    // Incrementing serial number for AgentCachedTexture packets
    private AtomicInteger CacheCheckSerialNum = new AtomicInteger(-1);
    // Incrementing serial number for AgentSetAppearance packets
    private AtomicInteger SetAppearanceSerialNum = new AtomicInteger();
    // Indicates if WearablesRequest succeeded
    private boolean GotWearables = false;
    // Indicates whether or not the appearance thread is currently running, to prevent multiple
    // appearance threads from running simultaneously
// 	private AtomicBoolean AppearanceThreadRunning = new AtomicBoolean(false);
    // Reference to our agent
    private GridClient _Client;
    // Timer used for delaying rebake on changing outfit
    private Timer _RebakeScheduleTimer;
    // Main appearance thread
    private Thread _AppearanceThread;
    // Is server baking complete. It needs doing only once
    private boolean ServerBakingDone = false;
    // #endregion Private Members

    private boolean sendAppearanceUpdates;

    private class SettingsUpdate implements Callback<SettingsUpdateCallbackArgs>
    {
        @Override
        public boolean callback(SettingsUpdateCallbackArgs params)
        {
            String key = params.getName();
            if (key == null)
            {
                sendAppearanceUpdates = _Client.Settings.getBool(LibSettings.SEND_AGENT_APPEARANCE);
            }
            else if (key.equals(LibSettings.SEND_AGENT_APPEARANCE))
            {
                sendAppearanceUpdates = params.getValue().AsBoolean();
            }
            return false;
        }
    }

    /**
     * Default constructor
     *
     * @param client A reference to our agent
     */
    public AppearanceManager(GridClient client)
    {
        _Client = client;

        _Client.Settings.OnSettingsUpdate.add(new SettingsUpdate());
        sendAppearanceUpdates = _Client.Settings.getBool(LibSettings.SEND_AGENT_APPEARANCE);
        
        for (int i = 0; i < _Textures.length; i++)
        {
        	_Textures[i] = new TextureData();
        	_Textures[i].TextureIndex = AvatarTextureIndex.setValue(i);
        }

        if (_Client.Assets == null)
        	Logger.Log("AppearanceManager requires a working AssetManager!", LogLevel.Error, _Client);
        
        _Client.Network.RegisterCallback(PacketType.AgentWearablesUpdate, this);
        _Client.Network.RegisterCallback(PacketType.AgentCachedTextureResponse, this);
        _Client.Network.RegisterCallback(PacketType.RebakeAvatarTextures, this);

        _Client.Network.OnEventQueueRunning.add(new Network_OnEventQueueRunning());
        _Client.Network.OnDisconnected.add(new Network_OnDisconnected(), true);
    }

    @Override
    public void packetCallback(Packet packet, Simulator simulator) throws Exception
    {
        switch (packet.getType())
        {
            case AgentWearablesUpdate:
                HandleAgentWearablesUpdate(packet, simulator);
                break;
            case AgentCachedTextureResponse:
                HandleAgentCachedTextureResponse(packet, simulator);
                break;
           case RebakeAvatarTextures:
                HandleRebakeAvatarTextures(packet, simulator);
                break;
			default:
				break;
       }
   }

    // #region Publics Methods

    /**
	 * Check if current region supports server side baking
     *
     * @returns True if server side baking support is detected
     */
    public boolean isServerBakingRegion()
    {
         return _Client.Network.getCurrentSim() != null && ((_Client.Network.getCurrentSim().Protocols & RegionProtocols.AgentAppearanceService) != 0);
    }

    /**
     * Starts the appearance setting thread
     */ 
    public void RequestSetAppearance()
    {
        RequestSetAppearance(false);
    }

    /** 
     * Starts the appearance setting thread
     * 
     * @param forceRebake True to force rebaking, otherwise false
     */
    public void RequestSetAppearance(final boolean forceRebake)
    {
        if (_AppearanceThread != null && _AppearanceThread.isAlive())
        {
            Logger.Log("Appearance thread is already running, skipping", LogLevel.Warning, _Client);
            return;
        }

        // If we have an active delayed scheduled appearance bake, we dispose of it
        if (_RebakeScheduleTimer != null)
        {
            _RebakeScheduleTimer.cancel();
            _RebakeScheduleTimer = null;
        }

        // This is the first time setting appearance, run through the entire sequence
        _AppearanceThread = new Thread("AppearenceThread")
        {
            @Override
            public void run()
            {
                boolean success = true;
                try
                {
                    if (forceRebake)
                    {
                        // Set all of the baked textures to UUID.Zero to force rebaking
                        for (BakeType type : BakeType.values())
                        {
                            if (type != BakeType.Unknown)
                            	_Textures[BakeTypeToAgentTextureIndex(type).getValue()].TextureID = UUID.Zero;
                        }
                    }

                    // Is this server side baking enabled sim
                    if (isServerBakingRegion())
                    {
                    	if (!GotWearables)
                    	{
                    		// Fetch a list of the current agent wearables
                    		if (GetAgentWearables())
                    		{
                    			GotWearables = true;
                    		}
                    	}

                        if (!ServerBakingDone || forceRebake)
                        {
                            success = UpdateAvatarAppearance();
                            if (success)
                            {
                                ServerBakingDone = true;
                            }
                        }
                    }
                    else // Classic client side baking
                    {
                    	if (!GotWearables)
                    	{
                    		// Fetch a list of the current agent wearables
                    		if (!GetAgentWearables())
                    		{
                    			Logger.Log("Failed to retrieve a list of current agent wearables, appearance cannot be set", LogLevel.Error, _Client);
                    			throw new Exception("Failed to retrieve a list of current agent wearables, appearance cannot be set");
                    		}
                    		GotWearables = true;
                    	}
                    	
                        // If we get back to server side baking region re-request server bake
                        ServerBakingDone = false;

                        // Download and parse all of the agent wearables
                        success = DownloadWearables();
                        if (!success)
                        {
                            Logger.Log("One or more agent wearables failed to download, appearance will be incomplete",
                                LogLevel.Warning, _Client);
                        }

                        // If this is the first time setting appearance and we're not forcing rebakes, check the server
                        // for cached bakes
                        if (SetAppearanceSerialNum.get() == 0 && !forceRebake)
                        {
                            // Compute hashes for each bake layer and compare against what the simulator currently has
                            if (!GetCachedBakes())
                            {
                                Logger.Log("Failed to get a list of cached bakes from the simulator, appearance will be rebaked", LogLevel.Warning, _Client);
                            }
                        }

                        // Download textures, compute bakes, and upload for any cache misses
                        if (!CreateBakes())
                        {
                            success = false;
                            Logger.Log("Failed to create or upload one or more bakes, appearance will be incomplete", LogLevel.Warning, _Client);
                        }

                        // Send the appearance packet
                        RequestAgentSetAppearance();
                    }
                }
                catch (Exception ex)
                {
                    success = false;
                    Logger.Log("Failed to get cached bakes from the simulator, appearance will be rebaked", LogLevel.Warning, _Client, ex);
                }
                finally
                {
                    OnAppearanceSet.dispatch(new AppearanceSetCallbackArgs(success));
                }
            }
        };
        _AppearanceThread.setDaemon(true);
        _AppearanceThread.start();
    }

    /**
     * Check if current region supports server side baking
     *
     * @return True if server side baking support is detected
     */
    public boolean ServerBakingRegion()
    {
        return _Client.Network.getCurrentSim() != null &&
            ((_Client.Network.getCurrentSim().Protocols & RegionProtocols.AgentAppearanceService) != 0);
    }

    /**
     * Ask the server what textures our agent is currently wearing
     *
     * @throws Exception 
     */
    public void RequestAgentWearables() throws Exception
    {
        AgentWearablesRequestPacket request = new AgentWearablesRequestPacket();
        request.AgentData.AgentID = _Client.Self.getAgentID();
        request.AgentData.SessionID = _Client.Self.getSessionID();

        _Client.Network.sendPacket(request);
    }

    /**
     * Build hashes out of the texture assetIDs for each baking layer to
     * ask the simulator whether it has cached copies of each baked texture
     *
     * @throws Exception 
     */
    public void RequestCachedBakes() throws Exception
    {
        ArrayList<AgentCachedTexturePacket.WearableDataBlock> hashes = new ArrayList<AgentCachedTexturePacket.WearableDataBlock>();
        AgentCachedTexturePacket cache = new AgentCachedTexturePacket();

        // Build hashes for each of the bake layers from the individual components
        synchronized (_Wearables)
        {
            for (BakeType bakeType : BakeType.values())
            {
                // Don't do a cache request for a skirt bake if we're not wearing a skirt
                if (bakeType == BakeType.Unknown || (bakeType == BakeType.Skirt && !_Wearables.containsKey(WearableType.Skirt)))
                    continue;

                // Build a hash of all the texture asset IDs in this baking layer
                UUID hash = UUID.Zero;
                for (int wearableIndex = 0; wearableIndex < WEARABLES_PER_LAYER; wearableIndex++)
                {
                    WearableType type = WEARABLE_BAKE_MAP[bakeType.getValue()][wearableIndex];

                    if (type != WearableType.Invalid)
                    {
                        if (_Wearables.containsKey(type))
                        {
                    	    for (WearableData data : _Wearables.get(type))
                    	    {
                    		    hash = UUID.XOr(hash, data.AssetID);
                    	    }
                        }
                    }
                }

                if (!hash.equals(UUID.Zero))
                {
                    // Hash with our secret value for this baked layer
                    hash = UUID.XOr(hash, BAKED_TEXTURE_HASH[bakeType.getValue()]);

                    // Add this to the list of hashes to send out
                    AgentCachedTexturePacket.WearableDataBlock block = cache.new WearableDataBlock();
                    block.ID = hash;
                    block.TextureIndex = BakeTypeToAgentTextureIndex(bakeType).getValue();
                    hashes.add(block);

                    Logger.DebugLog("Checking cache for " + bakeType + ", hash = " + block.ID, _Client);
                }
            }
        }

        // Only send the packet out if there's something to check
        if (hashes.size() > 0)
        {
            cache.AgentData.AgentID = _Client.Self.getAgentID();
            cache.AgentData.SessionID = _Client.Self.getSessionID();
            cache.AgentData.SerialNum = CacheCheckSerialNum.incrementAndGet();

            cache.WearableData = new WearableDataBlock[hashes.size()];
            for (int i = 0; i < hashes.size(); i++)
            {
            	cache.WearableData[i] = hashes.get(i);
            }
            _Client.Network.sendPacket(cache);
        }
    }

    /// <summary>
    /// OBSOLETE! Returns the AssetID of the first asset that is currently 
    /// being worn in a given WearableType slot
    /// </summary>
    /// <param name="type">WearableType slot to get the AssetID for</param>
    /// <returns>The UUID of the asset being worn in the given slot, or
    /// UUID.Zero if no wearable is attached to the given slot or wearables
    /// have not been downloaded yet
    public UUID GetWearableAsset(WearableType type)
    {
    	synchronized (_Wearables)
    	{
    		if (_Wearables.containsKey(type) && _Wearables.get(type).get(0) != null)
    		{
     			return _Wearables.get(type).get(0).AssetID;
    		}
    	}
        return UUID.Zero;
    }

    
    
    /**
     * Returns the AssetID of the asset that is currently being worn in a 
     * given WearableType slot
     *
     * @param type WearableType slot to get the AssetID for 
     * @returns A list of UUIDs of the assets being worn in the given slot, or an empty list if no wearable is attached
     *          to the given slot or wearables have not been downloaded yet
     */
    public List<UUID> GetWearableAssets(WearableType type)
    {
	    ArrayList<UUID> list = new ArrayList<UUID>();
     	synchronized (_Wearables)
    	{
    		if (_Wearables.containsKey(type))
    		{
     			for (WearableData data : _Wearables.get(type))
    				list.add(data.AssetID);
    		}
    	}
        return list;
    }

    /**
     * Add a wearable to the current outfit and set appearance
     *
     * @param wearableItem Wearable to be added to the outfit
     * @param replace Should existing item on the same point or of the same type be replaced
     * @throws Exception 
     */
    public void AddToOutfit(InventoryItem wearableItem, boolean replace) throws Exception
    {
        List<InventoryItem> wearableItems = new ArrayList<InventoryItem>();
        wearableItems.add(wearableItem);
        AddToOutfit(wearableItems, replace);
    }

    /**
     * Add a list of wearables to the current outfit and set appearance
     *
     * @param wearableItems List of wearable inventory items to be added to the outfit
     * @throws Exception 
     */
    public void AddToOutfit(List<InventoryItem> wearableItems) throws Exception
    {
    	AddToOutfit(wearableItems, true);
    }
    
    /**
     * Add a list of wearables to the current outfit and set appearance
     *
     * @param wearableItems List of wearable inventory items to be added to the outfit
     * @param replace Should existing item on the same point or of the same type be replaced
     * @throws Exception 
     */
    public void AddToOutfit(List<InventoryItem> wearableItems, boolean replace) throws Exception
    {
        List<InventoryWearable> wearables = new ArrayList<InventoryWearable>();
        List<InventoryItem> attachments = new ArrayList<InventoryItem>();

        for (InventoryItem item : wearableItems)
        {
            if (item instanceof InventoryWearable)
                wearables.add((InventoryWearable)item);
            else if (item instanceof InventoryAttachment || item instanceof InventoryObject)
                attachments.add(item);
        }

        synchronized (_Wearables)
        {
            // Add the given wearables to the wearables collection
            for (InventoryWearable wearableItem : wearables)
            {
                WearableData wd = new WearableData();
                wd.AssetID = wearableItem.assetID;
                wd.AssetType = wearableItem.assetType;
                wd.ItemID = wearableItem.itemID;
                wd.WearableType = wearableItem.getWearableType();

                if (replace) // Dump everything from the key
                    _Wearables.remove(wearableItem.getWearableType());
                _Wearables.put(wearableItem.getWearableType(), wd);
            }
        }

        if (attachments.size() > 0)
        {
            AddAttachments(attachments, false, replace);
        }

        if (wearables.size() > 0)
        {
            SendAgentIsNowWearing();
            DelayedRequestSetAppearance();
        }
    }

    /**
     * Remove a wearable from the current outfit and set appearance
     *
     * @param wearableItem Wearable to be removed from the outfit
     * @throws Exception 
     */
    public void RemoveFromOutfit(InventoryItem wearableItem) throws Exception
    {
        List<InventoryItem> wearableItems = new ArrayList<InventoryItem>();
        wearableItems.add(wearableItem);
        RemoveFromOutfit(wearableItems);
    }

    /**
     * Removes a list of wearables from the current outfit and set appearance
     *
     * @param wearableItems List of wearable inventory items to be removed from the outfit
     * @throws Exception 
     */
    public void RemoveFromOutfit(List<InventoryItem> wearableItems) throws Exception
    {
        List<InventoryWearable> wearables = new ArrayList<InventoryWearable>();
        List<InventoryItem> attachments = new ArrayList<InventoryItem>();

        for (InventoryItem item : wearableItems)
        {
            if (item instanceof InventoryWearable)
                wearables.add((InventoryWearable)item);
            else if (item instanceof InventoryAttachment || item instanceof InventoryObject)
                attachments.add(item);
        }

        boolean needSetAppearance = false;
        synchronized (_Wearables)
        {
            // Remove the given wearables from the wearables collection
            for (InventoryWearable wearableItem : wearables)
            {
                if (wearableItem.assetType != AssetType.Bodypart        // Remove if it's not a body part
                    && _Wearables.containsKey(wearableItem.getWearableType())) // And we have that wearabe type
                {
                    Collection<WearableData> worn = _Wearables.get(wearableItem.getWearableType());
                    if (worn != null)
                    {
                    	for (WearableData wearable : worn)
                    	{
                    		if (wearable.ItemID.equals(wearableItem.itemID))
                    		{
                                _Wearables.remove(wearableItem.getWearableType(), wearable);
                                 needSetAppearance = true;
                    		}
                    	}
                    }
                }
            }
        }

        for (int i = 0; i < attachments.size(); i++)
        {
            Detach(attachments.get(i).itemID);
        }

        if (needSetAppearance)
        {
            SendAgentIsNowWearing();
            DelayedRequestSetAppearance();
        }
    }

    /**
     * Replace the current outfit with a list of wearables and set appearance
     *
     * @param wearableItems List of wearable inventory items that define a new outfit
     * @throws Exception 
     */
    public void ReplaceOutfit(List<InventoryItem> wearableItems) throws Exception
    {
        ReplaceOutfit(wearableItems, true);
    }

    /**
     * Replace the current outfit with a list of wearables and set appearance
     *
     * @param wearableItems List of wearable inventory items that define a new outfit
     * @param safe Check if we have all body parts, set this to false only if you know what you're doing
     * @throws Exception 
     */
    public void ReplaceOutfit(List<InventoryItem> wearableItems, boolean safe) throws Exception
    {
        List<InventoryWearable> wearables = new ArrayList<InventoryWearable>();
        List<InventoryItem> attachments = new ArrayList<InventoryItem>();

        for (int i = 0; i < wearableItems.size(); i++)
        {
            InventoryItem item = wearableItems.get(i);

            if (item instanceof InventoryWearable)
                wearables.add((InventoryWearable)item);
            else if (item instanceof InventoryAttachment || item instanceof InventoryObject)
                attachments.add(item);
        }

        if (safe)
        {
            // If we don't already have a the current agent wearables downloaded, updating to a
            // new set of wearables that doesn't have all of the bodyparts can leave the avatar
            // in an inconsistent state. If any bodypart entries are empty, we need to fetch the
            // current wearables first
            boolean needsCurrentWearables = false;
            synchronized (_Wearables)
            {
                for (WearableType wearableType : WearableType.values())
                {
                	if (wearableType != WearableType.Invalid && WearableTypeToAssetType(wearableType) == AssetType.Bodypart && !_Wearables.containsKey(wearableType))
                    {
                        needsCurrentWearables = true;
                        break;
                    }
                }
            }

            if (needsCurrentWearables && !GetAgentWearables())
            {
                Logger.Log("Failed to fetch the current agent wearables, cannot safely replace outfit", LogLevel.Error, _Client);
                return;
            }
        }

        // Replace our local Wearables collection, send the packet(s) to update our
        // attachments, tell sim what we are wearing now, and start the baking process
        if (!safe)
        {
            SetAppearanceSerialNum.incrementAndGet();
        }
        ReplaceWearables(wearables);
        AddAttachments(attachments, true, false);
        SendAgentIsNowWearing();
        DelayedRequestSetAppearance();
    }

    /** 
     * Checks if an inventory item is currently being worn
     *
     * @param item The inventory item to check against the agent wearables
     * @returnsThe WearableType slot that the item is being worn in, or WearableType.Invalid if it is not currently being worn
     */
    public WearableType IsItemWorn(InventoryItem item)
    {
        synchronized (_Wearables)
        {
            for (Entry<WearableType, List<WearableData>> entry : _Wearables.entrySet())
            {
                for (WearableData data : entry.getValue())
                {
                	if (data.ItemID.equals(item.itemID))
                        return entry.getKey();
                }
            }
        }
        return WearableType.Invalid;
    }

    /**
     * Returns a copy of the agents currently worn wearables
     * Avoid calling this function multiple times as it will make a copy of all of the wearable data each time 
     *
     * @returnsA copy of the agents currently worn wearables
     */
    public Collection<WearableData> GetWearables()
    {
        synchronized (_Wearables)
        {
            return new ArrayList<WearableData>(_Wearables.values());
        }
    }

    public MultiMap<WearableType, WearableData> GetWearablesByType()
    {
        synchronized (_Wearables)
        {
            return new MultiMap<WearableType, WearableData>(_Wearables);
        }
    }

    /**
     * Calls either <see cref="ReplaceOutfit"/> orb<see cref="AddToOutfit"/> depending on the value of replaceItems
     *
     * @param wearables List of wearable inventory items to add to the outfit or become a new outfit
     * @param replaceItems True to replace existing items with the new list of items, false to add these items to the existing outfit
     * @throws Exception 
     */
    public void WearOutfit(List<InventoryItem> wearables, boolean replaceItems) throws Exception
    {
        List<InventoryItem> wearableItems = new ArrayList<InventoryItem>(wearables.size());
        Iterator<InventoryItem> iter = wearables.iterator();
        while (iter.hasNext())
        {
            wearableItems.add(iter .next());
        }

        if (replaceItems)
            ReplaceOutfit(wearableItems);
        else
            AddToOutfit(wearableItems);
    }
    // #endregion Publics Methods

    // #region Attachments

    /**
     * Adds a list of attachments to our agent
     *
     * @param attachments A List containing the attachments to add
     * @param removeExistingFirst If true, tells simulator to remove existing attachment first
     * @throws Exception 
     */
    public void AddAttachments(List<InventoryItem> attachments, boolean removeExistingFirst) throws Exception
    {
        AddAttachments(attachments, removeExistingFirst, true);
    }

    /**
     * Adds a list of attachments to our agent
     *
     * @param attachments A List containing the attachments to add
     * @param removeExistingFirst If true, tells simulator to remove existing attachment first
     * @param replace If true replace existing attachment on this attachment point, otherwise add to it (multi-attachments)
     * @throws Exception 
     */
    public void AddAttachments(List<InventoryItem> attachments, boolean removeExistingFirst, boolean replace) throws Exception
    {
        // Use RezMultipleAttachmentsFromInv to clear out current attachments, and attach new ones
        RezMultipleAttachmentsFromInvPacket attachmentsPacket = new RezMultipleAttachmentsFromInvPacket();
        attachmentsPacket.AgentData.AgentID = _Client.Self.getAgentID();
        attachmentsPacket.AgentData.SessionID = _Client.Self.getSessionID();

        attachmentsPacket.HeaderData.CompoundMsgID = new UUID();
        attachmentsPacket.HeaderData.FirstDetachAll = removeExistingFirst;
        attachmentsPacket.HeaderData.TotalObjects = (byte)attachments.size();

        attachmentsPacket.ObjectData = new RezMultipleAttachmentsFromInvPacket.ObjectDataBlock[attachments.size()];
        for (int i = 0; i < attachments.size(); i++)
        {
            InventoryAttachment attachment = (InventoryAttachment)attachments.get(i);
            if (attachments.get(i) instanceof InventoryAttachment)
            {
                attachmentsPacket.ObjectData[i] = attachmentsPacket.new ObjectDataBlock();
                attachmentsPacket.ObjectData[i].AttachmentPt = attachment.getAttachmentPoint().getValue(replace);
                attachmentsPacket.ObjectData[i].EveryoneMask = attachment.Permissions.EveryoneMask;
                attachmentsPacket.ObjectData[i].GroupMask = attachment.Permissions.GroupMask;
                attachmentsPacket.ObjectData[i].ItemFlags = attachment.ItemFlags;
                attachmentsPacket.ObjectData[i].ItemID = attachment.itemID;
                attachmentsPacket.ObjectData[i].setName(Helpers.StringToBytes(attachment.name));
                attachmentsPacket.ObjectData[i].setDescription(Helpers.StringToBytes(attachment.Description));
                attachmentsPacket.ObjectData[i].NextOwnerMask = attachment.Permissions.NextOwnerMask;
                attachmentsPacket.ObjectData[i].OwnerID = attachment.getOwnerID();
            }
            else if (attachments.get(i) instanceof InventoryObject)
            {
                attachmentsPacket.ObjectData[i] = attachmentsPacket.new ObjectDataBlock();
                attachmentsPacket.ObjectData[i].AttachmentPt = AttachmentPoint.Default.getValue(replace);
                attachmentsPacket.ObjectData[i].EveryoneMask = attachment.Permissions.EveryoneMask;
                attachmentsPacket.ObjectData[i].GroupMask = attachment.Permissions.GroupMask;
                attachmentsPacket.ObjectData[i].ItemFlags = attachment.ItemFlags;
                attachmentsPacket.ObjectData[i].ItemID = attachment.itemID;
                attachmentsPacket.ObjectData[i].setName(Helpers.StringToBytes(attachment.name));
                attachmentsPacket.ObjectData[i].setDescription(Helpers.StringToBytes(attachment.Description));
                attachmentsPacket.ObjectData[i].NextOwnerMask = attachment.Permissions.NextOwnerMask;
                attachmentsPacket.ObjectData[i].OwnerID = attachment.getOwnerID();
            }
            else
            {
                Logger.Log("Cannot attach inventory item " + attachment.name, LogLevel.Warning, _Client);
            }
        }
        _Client.Network.sendPacket(attachmentsPacket);
    }

    /**
     * Attach an item to our agent at a specific attach point
     *
     * @param item A <seealso cref="OpenMetaverse.InventoryItem"/> to attach
     * @param attachPoint the <seealso cref="OpenMetaverse.AttachmentPoint"/> on the avatar to attach the item to
     * @throws Exception 
     */
    public void Attach(InventoryItem item, AttachmentPoint attachPoint) throws Exception
    {
        Attach(item.itemID, item.getOwnerID(), item.name, item.Description, item.Permissions, item.ItemFlags, attachPoint, true);
    }

    /**
     * Attach an item to our agent at a specific attach point
     *
     * @param item A <seealso cref="OpenMetaverse.InventoryItem"/> to attach
     * @param attachPoint the <seealso cref="OpenMetaverse.AttachmentPoint"/> on the avatar to attach the item to
     * @param replace If true replace existing attachment on this attachment point, otherwise add to it (multi-attachments)
     * @throws Exception
     */
    public void Attach(InventoryItem item, AttachmentPoint attachPoint, boolean replace) throws Exception
    {
        Attach(item.itemID, item.getOwnerID(), item.name, item.Description, item.Permissions, item.ItemFlags, attachPoint, replace);
    }

    /**
     * Attach an item to our agent specifying attachment details
     *
     * @param itemID The <seealso cref="OpenMetaverse.UUID"/> of the item to attach
     * @param ownerID The <seealso cref="OpenMetaverse.UUID"/> attachments owner
     * @param name The name of the attachment
     * @param description The description of the attahment
     * @param perms The <seealso cref="OpenMetaverse.Permissions"/> to apply when attached
     * @param itemFlags The <seealso cref="OpenMetaverse.InventoryItemFlags"/> of the attachment
     * @param attachPoint The <seealso cref="OpenMetaverse.AttachmentPoint"/> on the agent to attach the item to
     * @throws Exception
     */
    public void Attach(UUID itemID, UUID ownerID, String name, String description,
        Permissions perms, int itemFlags, AttachmentPoint attachPoint) throws Exception
    {
        Attach(itemID, ownerID, name, description, perms, itemFlags, attachPoint, true);
    }

    /**
     * Attach an item to our agent specifying attachment details
     *
     * @param itemID The <seealso cref="OpenMetaverse.UUID"/> of the item to attach
     * @param ownerID The <seealso cref="OpenMetaverse.UUID"/> attachments owner
     * @param name The name of the attachment
     * @param description The description of the attahment
     * @param perms The <seealso cref="OpenMetaverse.Permissions"/> to apply when attached
     * @param itemFlags The <seealso cref="OpenMetaverse.InventoryItemFlags"/> of the attachment
     * @param attachPoint The <seealso cref="OpenMetaverse.AttachmentPoint"/> on the agent to attach the item to
     * @param replace If true replace existing attachment on this attachment point, otherwise add to it (multi-attachments)
     * @throws Exception
     */
    public void Attach(UUID itemID, UUID ownerID, String name, String description,
        Permissions perms, int itemFlags, AttachmentPoint attachPoint, boolean replace) throws Exception
    {
        // TODO: At some point it might be beneficial to have AppearanceManager track what we
        // are currently wearing for attachments to make enumeration and detachment easier
        RezSingleAttachmentFromInvPacket attach = new RezSingleAttachmentFromInvPacket();

        attach.AgentData.AgentID = _Client.Self.getAgentID();
        attach.AgentData.SessionID = _Client.Self.getSessionID();

        attach.ObjectData.AttachmentPt = attachPoint.getValue(replace);
        attach.ObjectData.setDescription(Helpers.StringToBytes(description));
        attach.ObjectData.EveryoneMask = perms.EveryoneMask;
        attach.ObjectData.GroupMask = perms.GroupMask;
        attach.ObjectData.ItemFlags = itemFlags;
        attach.ObjectData.ItemID = itemID;
        attach.ObjectData.setName(Helpers.StringToBytes(name));
        attach.ObjectData.NextOwnerMask = perms.NextOwnerMask;
        attach.ObjectData.OwnerID = ownerID;

        _Client.Network.sendPacket(attach);
    }

    /**
     * Detach an item from our agent using an <seealso cref="OpenMetaverse.InventoryItem"/> object
     *
     * @param item An <see cref="OpenMetaverse.InventoryItem"/> object
     * @throws Exception
     */
    public void Detach(InventoryItem item) throws Exception
    {
        Detach(item.itemID);
    }

    /**
     * Detach an item from our agent
     * 
     * @param itemID The inventory itemID of the item to detach
     * @throws Exception
     */
    public void Detach(UUID itemID) throws Exception
    {
        DetachAttachmentIntoInvPacket detach = new DetachAttachmentIntoInvPacket();
        detach.ObjectData.AgentID = _Client.Self.getAgentID();
        detach.ObjectData.ItemID = itemID;

        _Client.Network.sendPacket(detach);
    }
    // #endregion Attachments

    // #region Appearance Helpers

    /**
     * Inform the sim which wearables are part of our current outfit
     *
     * @throws Exception
     */
    private void SendAgentIsNowWearing() throws Exception
    {
        AgentIsNowWearingPacket wearing = new AgentIsNowWearingPacket();
        wearing.AgentData.AgentID = _Client.Self.getAgentID();
        wearing.AgentData.SessionID = _Client.Self.getSessionID();
        wearing.WearableData = new AgentIsNowWearingPacket.WearableDataBlock[WearableType.getNumValues()];

        synchronized (_Wearables)
        {
            for (WearableType type : WearableType.values())
            {
            	if (type != WearableType.Invalid)
            	{
            		AgentIsNowWearingPacket.WearableDataBlock block = wearing.new WearableDataBlock();
            		block.WearableType = type.getValue();

                    // This appears to be hacked on SL server side to support multi-layers
            		if (_Wearables.containsKey(type) && _Wearables.get(type).get(0) != null)
            			block.ItemID = _Wearables.get(type).get(0).ItemID;
            		else
            			block.ItemID = UUID.Zero;
                	wearing.WearableData[type.getValue()] = block;
            	}
            }
        }
        _Client.Network.sendPacket(wearing);
    }

    /**
     * Replaces the Wearables collection with a list of new wearable items
     *
     * @param wearableItems Wearable items to replace the Wearables collection with
     */
    private void ReplaceWearables(List<InventoryWearable> wearableItems)
    {
        MultiMap<WearableType, WearableData> newWearables = new MultiMap<WearableType, WearableData>();

        synchronized (_Wearables)
        {
            // Preserve body parts from the previous set of wearables. They may be overwritten,
            // but cannot be missing in the new set
            for (Entry<WearableType, List<WearableData>> entry : _Wearables.entrySet())
            {
            	for (WearableData data : entry.getValue())
            	{
            		if (data.AssetType == AssetType.Bodypart)
            			newWearables.put(entry.getKey(), data);
            	}
            }

            // Add the given wearables to the new wearables collection
            for (InventoryWearable wearableItem : wearableItems)
            {
                WearableData data = new WearableData();
                data.AssetID = wearableItem.assetID;
                data.AssetType = wearableItem.assetType;
                data.ItemID = wearableItem.itemID;
                data.WearableType = wearableItem.getWearableType();

                newWearables.put(data.WearableType, data);
            }

            // Replace the Wearables collection
            _Wearables = newWearables;
        }
    }

    /**
     * Calculates base color/tint for a specific wearable based on its params
     *
     * @param param All the color info gathered from wearable's VisualParams passed as list of ColorParamInfo tuples
     * @returns Base color/tint for the wearable
     */
    public Color4 GetColorFromParams(List<ColorParamInfo> param)
    {
        // Start off with a blank slate, black, fully transparent
        Color4 res = new Color4(0, 0, 0, 0);

        // Apply color modification from each color parameter
        Iterator<ColorParamInfo> iter = param.iterator();
        while (iter.hasNext())
        {
        	ColorParamInfo p = iter.next();
            int n = p.VisualColorParam.Colors.length;

            Color4 paramColor = new Color4(0, 0, 0, 0);

            if (n == 1)
            {
                // We got only one color in this param, use it for application
                // to the final color
                paramColor = p.VisualColorParam.Colors[0];
            }
            else if (n > 1)
            {
                // We have an array of colors in this parameter
                // First, we need to find out, based on param value
                // between which two elements of the array our value lands

                // Size of the step using which we iterate from Min to Max
                float step = (p.VisualParam.MaxValue - p.VisualParam.MinValue) / ((float)n - 1);

                // Our color should land inbetween colors in the array with index a and b
                int indexa = 0;
                int indexb = 0;

                int i = 0;

                for (float a = p.VisualParam.MinValue; a <= p.VisualParam.MaxValue; a += step)
                {
                    if (a <= p.Value)
                    {
                        indexa = i;
                    }
                    else
                    {
                        break;
                    }

                    i++;
                }

                // Sanity check that we don't go outside bounds of the array
                if (indexa > n - 1)
                    indexa = n - 1;

                indexb = (indexa == n - 1) ? indexa : indexa + 1;

                // How far is our value from Index A on the 
                // line from Index A to Index B
                float distance = p.Value - indexa * step;

                // We are at Index A (allowing for some floating point math fuzz),
                // use the color on that index
                if (distance < 0.00001f || indexa == indexb)
                {
                    paramColor = p.VisualColorParam.Colors[indexa];
                }
                else
                {
                    // Not so simple as being precisely on the index eh? No problem.
                    // We take the two colors that our param value places us between
                    // and then find the value for each ARGB element that is
                    // somewhere on the line between color1 and color2 at some
                    // distance from the first color
                    Color4 c1 = paramColor = p.VisualColorParam.Colors[indexa];
                    Color4 c2 = paramColor = p.VisualColorParam.Colors[indexb];

                    // Distance is some fraction of the step, use that fraction
                    // to find the value in the range from color1 to color2
                    paramColor = Color4.lerp(c1, c2, distance / step);
                }

                // Please leave this fragment even if its commented out
                // might prove useful should ($deity forbid) there be bugs in this code
                //string carray = "";
                //foreach (Color c in p.VisualColorParam.Colors)
                //{
                //    carray += c.ToString() + " - ";
                //}
                //Logger.DebugLog("Calculating color for " + p.WearableType + " from " + p.VisualParam.Name + ", value is " + p.Value + " in range " + p.VisualParam.MinValue + " - " + p.VisualParam.MaxValue + " step " + step + " with " + n + " elements " + carray + " A: " + indexa + " B: " + indexb + " at distance " + distance);
            }

            // Now that we have calculated color from the scale of colors
            // that visual params provided, lets apply it to the result
            switch (p.VisualColorParam.Operation)
            {
                case Add:
                    res = Color4.add(res, paramColor);
                    break;
                case Multiply:
                    res = Color4.multiply(res, paramColor);
                    break;
                case Blend:
                    res = Color4.lerp(res, paramColor, p.Value);
                    break;
                default:
                	break;
            }
        }
        return res;
    }

    /**
     * Blocking method to populate the Wearables dictionary
     *
     * @returns True on success, otherwise false
     * @throws Exception
     */
    boolean GetAgentWearables() throws Exception
    {
        final TimeoutEvent<Boolean> wearablesEvent = new TimeoutEvent<Boolean>();
        Callback<AgentWearablesReplyCallbackArgs> wearablesCallback = new Callback<AgentWearablesReplyCallbackArgs>()
        {
            @Override
            public boolean callback(AgentWearablesReplyCallbackArgs e)
            {
                wearablesEvent.set(true);
                return false;
            }
        };

        OnAgentWearablesReply.add(wearablesCallback);

        RequestAgentWearables();

        boolean success = wearablesEvent.waitOne(WEARABLE_TIMEOUT);

        OnAgentWearablesReply.remove(wearablesCallback);

        return success;
    }

    /**
     * Blocking method to populate the Textures array with cached bakes
     *
     * @returns True on success, otherwise false
     * @throws Exception
     */
    boolean GetCachedBakes() throws Exception
    {
        final TimeoutEvent<Boolean> cacheCheckEvent = new TimeoutEvent<Boolean>();
        Callback<AgentCachedBakesReplyCallbackArgs> cacheCallback = new Callback<AgentCachedBakesReplyCallbackArgs>()
        {
            @Override
            public boolean callback(AgentCachedBakesReplyCallbackArgs e)
            {
                cacheCheckEvent.set(true);
                return false;
            }
        };

        OnAgentCachedBakesReply.add(cacheCallback);

        RequestCachedBakes();

        Boolean success = cacheCheckEvent.waitOne(WEARABLE_TIMEOUT);

        OnAgentCachedBakesReply.remove(cacheCallback);

        return success != null ? success : false;
    }

    /**
     * Populates textures and visual params from a decoded asset
     *
     * @param wearable Wearable to decode
     * @param textures Texture data
     */
    public void DecodeWearableParams(WearableData wearable, TextureData[] textures)
    {
        HashMap<VisualAlphaParam, Float> alphaMasks = new HashMap<VisualAlphaParam, Float>();
        List<ColorParamInfo> colorParams = new ArrayList<ColorParamInfo>();

        // Populate collection of alpha masks from visual params
        // also add color tinting information
        for (Entry<Integer, Float> kvp : wearable.Asset.Params.entrySet())
        {
            if (!VisualParams.Params.containsKey(kvp.getKey())) continue;

            VisualParam p = VisualParams.Params.get(kvp.getKey());

            ColorParamInfo colorInfo = new ColorParamInfo();
            colorInfo.VisualParam = p;
            colorInfo.Value = kvp.getValue();

            // Color params
            if (p.ColorParams != null)
            {
                colorInfo.VisualColorParam = p.ColorParams;
                int key = kvp.getKey();
                		
                if (wearable.WearableType == WearableType.Tattoo)
                {
                    if (key == 1062 || key == 1063 || key == 1064)
                    {
                        colorParams.add(colorInfo);
                    }
                }
                else if (wearable.WearableType == WearableType.Jacket)
                {
                    if (key == 809 || key == 810 || key == 811)
                    {
                        colorParams.add(colorInfo);
                    }
                }
                else if (wearable.WearableType == WearableType.Hair)
                {
                    // Param 112 - Rainbow
                    // Param 113 - Red
                    // Param 114 - Blonde
                    // Param 115 - White
                    if (key == 112 || key == 113 || key == 114 || key == 115)
                    {
                        colorParams.add(colorInfo);
                    }
                }
                else if (wearable.WearableType == WearableType.Skin)
                {
                    // For skin we skip makeup params for now and use only the 3
                    // that are used to determine base skin tone
                    // Param 108 - Rainbow Color
                    // Param 110 - Red Skin (Ruddiness)
                    // Param 111 - Pigment
                    if (kvp.getKey() == 108 || kvp.getKey() == 110 || kvp.getKey() == 111)
                    {
                        colorParams.add(colorInfo);
                    }
                }
                else
                {
                    colorParams.add(colorInfo);
                }
            }

            // Add alpha mask
            if (p.AlphaParams != null && !p.AlphaParams.TGAFile.isEmpty() && !p.IsBumpAttribute && !alphaMasks.containsKey(p.AlphaParams))
            {
                alphaMasks.put(p.AlphaParams, kvp.getValue() == 0 ? 0.01f : kvp.getValue());
            }

            // Alhpa masks can also be specified in sub "driver" params
            if (p.Drivers != null)
            {
                for (int i = 0; i < p.Drivers.length; i++)
                {
                    if (VisualParams.Params.containsKey(p.Drivers[i]))
                    {
                        VisualParam driver = VisualParams.Params.get(p.Drivers[i]);
                        if (driver.AlphaParams != null && !driver.AlphaParams.TGAFile.isEmpty() && !driver.IsBumpAttribute && !alphaMasks.containsKey(driver.AlphaParams))
                        {
                            alphaMasks.put(driver.AlphaParams, kvp.getValue() == 0 ? 0.01f : kvp.getValue());
                        }
                    }
                }
            }
        }

        Color4 wearableColor = Color4.White; // Never actually used
        if (colorParams.size() > 0)
        {
            wearableColor = GetColorFromParams(colorParams);
            Logger.DebugLog("Setting tint " + wearableColor + " for " + wearable.WearableType);
        }

        // Loop through all of the texture IDs in this decoded asset and put them in our cache of worn textures
        for (Entry<AvatarTextureIndex, UUID> entry : wearable.Asset.Textures.entrySet())
        {
            int i = AvatarTextureIndex.getValue(entry.getKey());

            // Update information about color and alpha masks for this texture
            textures[i].AlphaMasks = alphaMasks;
            textures[i].Color = wearableColor;

            // If this texture changed, update the TextureID and clear out the old cached texture asset
            if (textures[i].TextureID == null || !textures[i].TextureID.equals(entry.getValue()))
            {
                // Treat DEFAULT_AVATAR_TEXTURE as null
                if (entry.getValue().equals(DEFAULT_AVATAR_TEXTURE))
                    textures[i].TextureID = UUID.Zero;
                else
                    textures[i].TextureID = entry.getValue();
                Logger.DebugLog("Set " + entry.getKey() + " to " + textures[i].TextureID, _Client);

                textures[i].Texture = null;
            }
        }
    }

    private class WearablesReceived implements Callback<AssetDownload>
    {
        private final WearableData wearable;
        private final CountDownLatch latch;
        
        public WearablesReceived(CountDownLatch latch, WearableData wearable)
        {
            this.latch = latch;
            this.wearable = wearable;
        }

        public boolean callback(AssetDownload transfer)
        {
            if (transfer.Success)
            {
            	wearable.Asset = (AssetWearable)AssetManager.CreateAssetItem(transfer.AssetType, transfer.ItemID, transfer.AssetData);
                if (wearable.Asset != null)
                {
                    DecodeWearableParams(wearable, _Textures);
                    Logger.DebugLog("Downloaded wearable asset " + wearable.WearableType + " with " + wearable.Asset.Params.size() +
                        " visual params and " + wearable.Asset.Textures.size() + " textures", _Client);

                }
                else
                {
                    Logger.Log("Failed to decode wearable asset: " + transfer.ItemID, LogLevel.Error, _Client);
                }
            }
            else
            {
                Logger.Log("Wearable " + wearable.WearableType + " {" + wearable.AssetID + "} failed to download, status:  " + transfer.Status,
                		   LogLevel.Warning, _Client);
            }
            latch.countDown();
            return true;
        }
    }
    /**
     * Blocking method to download and parse currently worn wearable assets
     *
     * @returns True on success, otherwise false
     */
    private boolean DownloadWearables()
    {
        boolean success = true;

        // Make a copy of the wearables dictionary to enumerate over
        MultiMap<WearableType, WearableData> wearables;
        synchronized (_Wearables)
        {
            wearables = new MultiMap<WearableType, WearableData>(_Wearables);
        }

        // We will refresh the textures (zero out all non bake textures)
        for (int i = 0; i < _Textures.length; i++)
        {
            boolean isBake = false;
            for (BakeType type : BakeType.values())
            {
                if (BakeTypeToAgentTextureIndex(type).getValue() == i)
                {
                    isBake = true;
                    break;
                }
            }
            if (!isBake)
            {
                _Textures[i].Texture = null;
                _Textures[i].TextureID = null;
                _Textures[i].Color = null;
            }
        }

        final CountDownLatch latch = new CountDownLatch(wearables.size());
        for (WearableData wearable : wearables.values())
        {
            if (wearable.Asset != null)
            {
                DecodeWearableParams(wearable, _Textures);
                latch.countDown();
            }
        }

        int pendingWearables = (int)latch.getCount();
        if (pendingWearables == 0)
            return true;

        Logger.DebugLog("Downloading " + pendingWearables + " wearable assets");

        ExecutorService executor = Executors.newFixedThreadPool(Math.min(pendingWearables, MAX_CONCURRENT_DOWNLOADS));
        for (final WearableData wearable : wearables.values())
        {
            if (wearable.Asset == null)
            {
                executor.submit(new Runnable()
                {
                    @Override
                    public void run()
                    {
                        // Fetch this wearable asset
                        try
                        {
                            _Client.Assets.RequestAsset(wearable.AssetID, wearable.AssetType, true, new WearablesReceived(latch, wearable));
                        }
                        catch (Exception ex) { }
                    }
                });
            }
        }

        try
        {
            success = latch.await(TEXTURE_TIMEOUT, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {}
        executor.shutdown();
        return success;
    }

    /**
     * Get a list of all of the textures that need to be downloaded for a single bake layer
     *
     * @param bakeType Bake layer to get texture AssetIDs for
     * @returns A list of texture AssetIDs to download
     */
    private List<UUID> GetTextureDownloadList(BakeType bakeType)
    {
        List<AvatarTextureIndex> indices = BakeTypeToTextures(bakeType);
        List<UUID> textures = new ArrayList<UUID>();

        for (AvatarTextureIndex index : indices)
        {
            // If this is not the skirt layer or we're wearing a skirt then add it
            if (index != AvatarTextureIndex.Skirt || _Wearables.containsKey(WearableType.Skirt))
                AddTextureDownload(index, textures);
        }
        return textures;
    }

    /**
     * Helper method to lookup the TextureID for a single layer and add it to the texture list if it
     * is not already present
     *
     * @param index 
     * @param textures 
     */
    private void AddTextureDownload(AvatarTextureIndex index, List<UUID> textures)
    {
        TextureData textureData = _Textures[index.getValue()];
        // Add the textureID to the list if this layer has a valid textureID set, it has not already
        // been downloaded, and it is not already in the download list
        if (!UUID.isZeroOrNull(textureData.TextureID) && textureData.Texture == null && !textures.contains(textureData.TextureID))
            textures.add(textureData.TextureID);
    }

    /**
     * Blocking method to download all of the textures needed for baking the given bake layers
     * No return value is given because the baking will happen whether or not all textures are successfully downloaded
     *
     * @param bakeLayers A list of layers that need baking
     */
    private void DownloadTextures(List<BakeType> bakeLayers)
    {
        List<UUID> textureIDs = new ArrayList<UUID>();

        for (int i = 0; i < bakeLayers.size(); i++)
        {
            List<UUID> layerTextureIDs = GetTextureDownloadList(bakeLayers.get(i));

            for (int j = 0; j < layerTextureIDs.size(); j++)
            {
                UUID uuid = layerTextureIDs.get(j);
                if (!textureIDs.contains(uuid))
                    textureIDs.add(uuid);
            }
        }

        Logger.DebugLog("Downloading " + textureIDs.size() + " textures for baking");

        final CountDownLatch latch = new CountDownLatch(textureIDs.size());
        for (UUID textureID : textureIDs)
        {
            _Client.Assets.RequestImage(textureID, new Callback<ImageDownload>()
            {
                @Override
                public boolean callback(ImageDownload download)
                {
                	if (download.State == TextureRequestState.Finished && download.AssetData != null)
                	{
                        AssetTexture texture = (AssetTexture)AssetManager.CreateAssetItem(AssetType.Texture, download.ItemID, download.AssetData);
                        if (texture == null)
                        {
                            Logger.Log("Failed to decode texture: " + textureID, LogLevel.Error, _Client);
                        }
 
                        for (int i = 0; i < _Textures.length; i++)
                        {
                            if (_Textures[i].TextureID != null && _Textures[i].TextureID.equals(download.ItemID))
                                _Textures[i].Texture = texture;
                        }
                    }
                    else
                    {
                        Logger.Log("Texture " + download.ItemID + " failed to download, one or more bakes will be incomplete", LogLevel.Warning, _Client);
                    }
                    latch.countDown();
                    return true;
                }
            });
        }

        try
        {
            latch.await(TEXTURE_TIMEOUT, TimeUnit.MILLISECONDS);
        }
        catch (InterruptedException e) {}
    }

    /**
     * Blocking method to create and upload baked textures for all of the missing bakes
     *
     * @returns True on success, otherwise false
     */
    private boolean CreateBakes()
    {
        List<BakeType> pendingBakes = new ArrayList<BakeType>();

        // Check each bake layer in the Textures array for missing bakes
        for (BakeType type : BakeType.values())
        {
            if (type != BakeType.Unknown)
            {
            	UUID uuid = _Textures[BakeTypeToAgentTextureIndex(type).getValue()].TextureID;
                if (UUID.isZeroOrNull(uuid))
                {
                    // If this is the skirt layer and we're not wearing a skirt then skip it
                    if (type == BakeType.Skirt && !_Wearables.containsKey(WearableType.Skirt))
                        continue;

                    pendingBakes.add(type);
                }
            }
        }

        final AtomicBoolean success = new AtomicBoolean(true);
        if (pendingBakes.size() > 0)
        {
            DownloadTextures(pendingBakes);

            ExecutorService executor = Executors.newFixedThreadPool(Math.min(pendingBakes.size(), MAX_CONCURRENT_UPLOADS));
            for (final BakeType bakeType : pendingBakes)
            {
                executor.submit(new Runnable()
                {
                    @Override
                    public void run()
                    {
                        try
                        {
                            if (!CreateBake(bakeType))
                                success.set(false);
                        }
                        catch (Exception e)
                        {
                            success.set(false);
                        }
                    }
                });
            }
        }

        // Free up all the textures we're holding on to
        for (int i = 0; i < _Textures.length; i++)
        {
            _Textures[i].Texture = null;
        }

        // We just allocated and freed a ridiculous amount of memory while baking. Signal to the GC to clean up
        Runtime.getRuntime().gc();

        return success.get();
    }

    /**
     * Blocking method to create and upload a baked texture for a single bake layer
     *
     * @param bakeType Layer to bake
     * @throws URISyntaxException 
     * @throws CloneNotSupportedException 
     * @returns True on success, otherwise false
     */
    private boolean CreateBake(BakeType bakeType) throws URISyntaxException, CloneNotSupportedException
    {
        List<AvatarTextureIndex> textureIndices = BakeTypeToTextures(bakeType);
        Baker oven = new Baker(_Client, bakeType);

        for (int i = 0; i < textureIndices.size(); i++)
        {
            AvatarTextureIndex textureIndex = textureIndices.get(i);
            TextureData texture = _Textures[AvatarTextureIndex.getValue(textureIndex)];

            oven.AddTexture(texture);
        }

        long start = System.currentTimeMillis();;
        oven.Bake();
        Logger.DebugLog("Baking " + bakeType + " took " + (System.currentTimeMillis() - start) + "ms");

        UUID newAssetID = UUID.Zero;
        int retries = UPLOAD_RETRIES;

        do
        {
            try
            {
                newAssetID = UploadBake(new AssetTexture(oven.getBakedTexture()).getAssetData());
            }
            catch (IOException e)
            {
                return false;
            }
            catch (InterruptedException e)
            {
                return false;
            }
            --retries;
        }
        while (UUID.isZeroOrNull(newAssetID) && retries > 0);

        _Textures[BakeTypeToAgentTextureIndex(bakeType).getValue()].TextureID = newAssetID;

        if (UUID.isZeroOrNull(newAssetID))
        {
            Logger.Log("Failed uploading bake " + bakeType, LogLevel.Warning, _Client);
            return false;
        }
        return true;
    }

    /**
     * Blocking method to upload a baked texture
     *
     * @param textureData Five channel JPEG2000 texture data to upload
     * @returns UUID of the newly created asset on success, otherwise UUID.Zero
     * @throws IOException
     * @throws InterruptedException
     */
    private UUID UploadBake(byte[] textureData) throws IOException, InterruptedException
    {
        final TimeoutEvent<UUID> uploadEvent = new TimeoutEvent<UUID>();

        _Client.Assets.RequestUploadBakedTexture(textureData, _Client.Assets.new BakedTextureUploadedCallback()
        {
            @Override
            public void callback(UUID newAssetID)
            {
                uploadEvent.set(newAssetID);
            }
        });

        // FIXME: evaluate the need for timeout here, RequestUploadBakedTexture() will
        // timeout either on Client.Settings.TRANSFER_TIMEOUT or Client.Settings.CAPS_TIMEOUT
        // depending on which upload method is used.
        UUID bakeID = uploadEvent.waitOne(UPLOAD_TIMEOUT);
        return bakeID != null ? bakeID : UUID.Zero;
    }

    /**
     * Initate server baking process
     * @throws Exception 
     *
     * @returns True if the server baking was successful
     */
    private boolean UpdateAvatarAppearance() throws Exception
    {
        URI url = _Client.Network.getCapabilityURI("UpdateAvatarAppearance");
        if (url == null)
        {
            return false;
        }

        InventoryFolder COF = _Client.Inventory.FindFolderForType(FolderType.CurrentOutfit);
        if (COF == null)
        {
            _Client.Inventory.FolderContents(_Client.Inventory.getRootNode(false).itemID, _Client.Self.getAgentID(), true, true, InventorySortOrder.ByDate, true, _Client.Settings.CAPS_TIMEOUT);
            COF = _Client.Inventory.FindFolderForType(FolderType.CurrentOutfit);
        }

        if (COF == null)
        {
            // TODO: create Current Outfit Folder
            return false;
        }
            
        CapsClient capsRequest = new CapsClient(_Client, "UpdateAvatarAppearance");
        OSDMap request = new OSDMap(1);
        request.put("cof_version", OSD.FromInteger(COF.version));

        String msg = "Setting server side baking failed";
        OSD res = capsRequest.getResponse(url, request, OSDFormat.Xml, _Client.Settings.CAPS_TIMEOUT * 2);
        if (res != null && res instanceof OSDMap)
        {
            OSDMap result = (OSDMap)res;
            if (result.get("success").AsBoolean())
            {
                Logger.Log("Successfully set appearance", LogLevel.Info, _Client);
                // TODO: Set local visual params and baked textures based on the result here
                return true;
            }
            if (result.containsKey("error"))
            {
                msg += ": " + result.get("error").AsString();
            }
        }
        capsRequest.shutdown(true);
        Logger.Log(msg, LogLevel.Error, _Client);

        return false;
    }

    /**
     * Create an AgentSetAppearance packet from Wearables data and the Textures array and send it
     *
     * @throws Exception
     */
    private void RequestAgentSetAppearance() throws Exception
    {
        AgentSetAppearancePacket set = new AgentSetAppearancePacket();
        set.AgentData.AgentID = _Client.Self.getAgentID();
        set.AgentData.SessionID = _Client.Self.getSessionID();
        set.AgentData.SerialNum = SetAppearanceSerialNum.incrementAndGet();

        // Visual params used in the agent height calculation
        float agentSizeVPHeight = 0.0f;
        float agentSizeVPHeelHeight = 0.0f;
        float agentSizeVPPlatformHeight = 0.0f;
        float agentSizeVPHeadSize = 0.5f;
        float agentSizeVPLegLength = 0.0f;
        float agentSizeVPNeckLength = 0.0f;
        float agentSizeVPHipLength = 0.0f;

        synchronized (_Wearables)
        {
            // #region VisualParam

            int vpIndex = 0;
            int nrParams;
            boolean wearingPhysics = _Wearables.containsKey(WearableType.Physics);

            if (wearingPhysics)
            {
                nrParams = 251;
            }
            else
            {
                nrParams = 218;
            }

            set.ParamValue = new byte[nrParams];

            for (Entry<Integer, VisualParam> kvp : VisualParams.Params.entrySet())
            {
                VisualParam vp = kvp.getValue();
                float paramValue = 0f;
                boolean found = false;

                // Try and find this value in our collection of downloaded wearables
                for (Entry<WearableType, List<WearableData>> entry : _Wearables.entrySet())
                {
                	for (WearableData data : entry.getValue())
                	{
                		if (data.Asset != null && data.Asset.Params.containsKey(vp.ParamID))
                		{
                			paramValue = data.Asset.Params.get(vp.ParamID);
                			found = true;
                			break;
                		}
                	}
                	if (found)
                		break;
                }

                // Use a default value if we don't have one set for it
                if (!found)
                    paramValue = vp.DefaultValue;

                // Only Group-0 parameters are sent in AgentSetAppearance packets
                if (kvp.getValue().Group == 0)
                {
                    set.ParamValue[vpIndex] = Helpers.FloatToByte(paramValue, vp.MinValue, vp.MaxValue);
                    ++vpIndex;
                }

                // Check if this is one of the visual params used in the agent height calculation
                switch (vp.ParamID)
                {
                    case 33:
                        agentSizeVPHeight = paramValue;
                        break;
                    case 198:
                        agentSizeVPHeelHeight = paramValue;
                        break;
                    case 503:
                        agentSizeVPPlatformHeight = paramValue;
                        break;
                    case 682:
                        agentSizeVPHeadSize = paramValue;
                        break;
                    case 692:
                        agentSizeVPLegLength = paramValue;
                        break;
                    case 756:
                        agentSizeVPNeckLength = paramValue;
                        break;
                    case 842:
                        agentSizeVPHipLength = paramValue;
                        break;
                    default:
                    	break;
                }

                if (vpIndex == nrParams) break;
            }

            MyVisualParameters = new byte[set.ParamValue.length];
            System.arraycopy(MyVisualParameters, 0, set.ParamValue, 0, set.ParamValue.length);
            // #endregion VisualParam

            // #region TextureEntry

            TextureEntry te = new TextureEntry(DEFAULT_AVATAR_TEXTURE);

            for (int i = 0; i < _Textures.length; i++)
            {
                TextureEntry.TextureEntryFace face = te.createFace(i);
                if ((i == 0 || i == 5 || i == 6) && !_Client.Settings.CLIENT_IDENTIFICATION_TAG.equals(UUID.Zero))
                {
                    face.setTextureID(_Client.Settings.CLIENT_IDENTIFICATION_TAG);
                    Logger.DebugLog("Sending client identification tag: " + _Client.Settings.CLIENT_IDENTIFICATION_TAG, _Client);
                }
                else if (_Textures[i].TextureID != UUID.Zero)
                {
                    face.setTextureID(_Textures[i].TextureID);
                    Logger.DebugLog("Sending texture entry for " + i + " to " + _Textures[i].TextureID, _Client);
                }
            }

            set.ObjectData.setTextureEntry(te.getBytes());
            MyTextures = te;

            // #endregion TextureEntry

            // #region WearableData

            set.WearableData = new AgentSetAppearancePacket.WearableDataBlock[BakeType.getNumValues()];

            // Build hashes for each of the bake layers from the individual components
            for (BakeType bakeType : BakeType.values())
            {
                if (bakeType == BakeType.Unknown)
                    continue;

                UUID hash = UUID.Zero;

                for (int wearableIndex = 0; wearableIndex < WEARABLES_PER_LAYER; wearableIndex++)
                {
                    WearableType type = WEARABLE_BAKE_MAP[bakeType.getValue()][wearableIndex];

                    if (type != WearableType.Invalid && _Wearables.containsKey(type))
                    {
                        for (WearableData wearable : _Wearables.get(type))
                        {
                             hash = UUID.XOr(hash, wearable.AssetID);    
                        }
                    }
                }

                if (!hash.equals(UUID.Zero))
                {
                    // Hash with our magic value for this baked layer
                    hash = UUID.XOr(hash, BAKED_TEXTURE_HASH[bakeType.getValue()]);
                }

                // Tell the server what cached texture assetID to use for each bake layer
                AgentSetAppearancePacket.WearableDataBlock block = set.new WearableDataBlock();
                block.TextureIndex = BakeTypeToAgentTextureIndex(bakeType).getValue();
                block.CacheID = hash;
                set.WearableData[bakeType.getValue()] = block;
                Logger.DebugLog("Sending TextureIndex " + bakeType + " with CacheID " + hash, _Client);
            }

            // #endregion WearableData

            // #region Agent Size

            // Takes into account the Shoe Heel/Platform offsets but not the HeadSize offset. Seems to work.
            double agentSizeBase = 1.706;

            // The calculation for the HeadSize scalar may be incorrect, but it seems to work
            double agentHeight = agentSizeBase + (agentSizeVPLegLength * .1918) + (agentSizeVPHipLength * .0375) +
                (agentSizeVPHeight * .12022) + (agentSizeVPHeadSize * .01117) + (agentSizeVPNeckLength * .038) +
                (agentSizeVPHeelHeight * .08) + (agentSizeVPPlatformHeight * .07);

            set.AgentData.Size = new Vector3(0.45f, 0.6f, (float)agentHeight);

            // #endregion Agent Size

            if (_Client.Settings.getBool(LibSettings.AVATAR_TRACKING))
            {
                Avatar me = _Client.Network.getCurrentSim().getObjectsAvatars().get(_Client.Self.getLocalID());
                if (me != null)
                {
                    me.Textures = MyTextures;
                    me.VisualParameters = MyVisualParameters;
                }
            }
        }
        _Client.Network.sendPacket(set);
        Logger.DebugLog("Sent AgentSetAppearance packet", _Client);        
    }

    private void DelayedRequestSetAppearance()
    {
        if (_RebakeScheduleTimer == null)
        {
            _RebakeScheduleTimer = new Timer("DelayedRequestSetAppearance");
        }
        _RebakeScheduleTimer.schedule(new TimerTask()
        {
            @Override
            public void run()
            {
                RequestSetAppearance(true);
            }
        }, REBAKE_DELAY);
    }
    // #endregion Appearance Helpers

    // #region Inventory Helpers

    public boolean GetFolderWearables(String[] folderPath, List<InventoryWearable> wearables, List<InventoryItem> attachments) throws Exception
    {
        UUID folder = _Client.Inventory.FindObjectByPath(
            _Client.Inventory.getRootNode(false).itemID, _Client.Self.getAgentID(), String.join("/", folderPath), INVENTORY_TIMEOUT);

        if (folder != UUID.Zero)
        {
            return GetFolderWearables(folder, wearables, attachments);
        }
        Logger.Log("Failed to resolve outfit folder path " + folderPath, LogLevel.Error, _Client);
        wearables = null;
        attachments = null;
        return false;
    }

    private boolean GetFolderWearables(UUID folder, List<InventoryWearable> wearables, List<InventoryItem> attachments) throws Exception
    {
        List<InventoryNode> objects = _Client.Inventory.FolderContents(folder, _Client.Self.getAgentID(), false, true, InventorySortOrder.ByName, INVENTORY_TIMEOUT);

        if (objects != null)
        {
            for (InventoryNode ib : objects)
            {
                if (ib.getType() == InventoryType.Wearable)
                {
                    Logger.DebugLog("Adding wearable " + ib.name, _Client);
                    wearables.add((InventoryWearable)ib);
                }
                else if (ib.getType() == InventoryType.Attachment)
                {
                    Logger.DebugLog("Adding attachment (attachment) " + ib.name, _Client);
                    attachments.add((InventoryItem)ib);
                }
                else if (ib.getType() == InventoryType.Object)
                {
                    Logger.DebugLog("Adding attachment (object) " + ib.name, _Client);
                    attachments.add((InventoryItem)ib);
                }
                else
                {
                    Logger.DebugLog("Ignoring inventory item " + ib.name, _Client);
                }
            }
        }
        else
        {
            Logger.Log("Failed to download folder contents of + " + folder, LogLevel.Error, _Client);
            return false;
        }

        return true;
    }

    // #endregion Inventory Helpers

    // #region Callbacks

    private void HandleAgentWearablesUpdate(Packet packet, Simulator simulator)
    {
        boolean changed = false;
        AgentWearablesUpdatePacket update = (AgentWearablesUpdatePacket)packet;

        synchronized (_Wearables)
        {
            // #region Test if anything changed in this update
            for (AgentWearablesUpdatePacket.WearableDataBlock block : update.WearableData)
            {
                WearableType type = WearableType.setValue(block.WearableType);

                if (!block.AssetID.equals(UUID.Zero))
                {
                	if (_Wearables.containsKey(type))
                	{
                		boolean match = false;
                		for (WearableData wearable : _Wearables.get(type))
                		{
                			if (wearable != null && wearable.AssetID.equals(block.AssetID) && wearable.ItemID.equals(block.ItemID))
                			{
                				// Same wearable as before
                				match = true;
                				break;
                			}
                		}
                        changed = !match;
                        if (changed)
                    	     break;
                	}
                	else
                	{
                        // A wearable is now set for this index
                        changed = true;
                        break;
                	}
                }
                else if (_Wearables.containsKey(type))
                {
                    // This index is now empty
                    changed = true;
                    break;
                }
            }
        }
        // #endregion Test if anything changed in this update

        if (changed)
        {
            Logger.DebugLog("New wearables received in AgentWearablesUpdate", _Client);
            synchronized (_Wearables)
            {
                _Wearables.clear();

                for (int i = 0; i < update.WearableData.length; i++)
                {
                    AgentWearablesUpdatePacket.WearableDataBlock block = update.WearableData[i];

                    if (!block.AssetID.equals(UUID.Zero))
                    {
                        WearableType type = WearableType.setValue(block.WearableType);

                        WearableData data = new WearableData();
                        data.Asset = null;
                        data.AssetID = block.AssetID;
                        data.AssetType = WearableTypeToAssetType(type);
                        data.ItemID = block.ItemID;
                        data.WearableType = type;

                        // Add this wearable to our collection
                        _Wearables.put(type, data);
                    }
                }
            }
            // Fire the callback
            OnAgentWearablesReply.dispatch(new AgentWearablesReplyCallbackArgs());
        }
        else
        {
            Logger.DebugLog("Duplicate AgentWearablesUpdate received, discarding", _Client);
        }
    }

    private void HandleRebakeAvatarTextures(Packet packet, Simulator simulator)
    {
        RebakeAvatarTexturesPacket rebake = (RebakeAvatarTexturesPacket)packet;

        // allow the library to do the rebake
        if (sendAppearanceUpdates)
        {
            RequestSetAppearance(true);
        }
        OnRebakeAvatarReply.dispatch(new RebakeAvatarTexturesCallbackArgs(rebake.TextureID));
    }

    private void HandleAgentCachedTextureResponse(Packet packet, Simulator simulator) throws UnsupportedEncodingException
    {
        AgentCachedTextureResponsePacket response = (AgentCachedTextureResponsePacket)packet;

        for (AgentCachedTextureResponsePacket.WearableDataBlock block : response.WearableData)
        {
            AvatarTextureIndex index = AvatarTextureIndex.setValue(block.TextureIndex);
          
            Logger.DebugLog("Cache response for " + index + ", TextureID = " + block.TextureID, _Client);

            TextureData tex = _Textures[index.getValue()];
            if (!block.TextureID.equals(UUID.Zero))
            {
                // A simulator has a cache of this bake layer
                tex.TextureID = block.TextureID;
                tex.Host = Helpers.BytesToString(block.getHostName());
            }
            else
            {
                // The server does not have a cache of this bake layer, request upload
                // FIXME:
            }
        }
        if (OnAgentCachedBakesReply.count() > 0)
            OnAgentCachedBakesReply.dispatch(new AgentCachedBakesReplyCallbackArgs(response.AgentData.SerialNum, response.WearableData.length));
    }

    private class Network_OnEventQueueRunning implements Callback<EventQueueRunningCallbackArgs>
    {
        @Override
        public boolean callback(EventQueueRunningCallbackArgs e)
        {
            if (sendAppearanceUpdates && e.getSimulator().equals(_Client.Network.getCurrentSim()))
            {
                // Update appearance each time we enter a new sim and capabilities have been retrieved
                Logger.Log("Starting AppearanceRequest from server " + e.getSimulator().getSimName(), LogLevel.Warning, _Client);
                RequestSetAppearance(false);
            }
            return false;
        }
    }

    private class Network_OnDisconnected implements Callback<DisconnectedCallbackArgs>
    {
        @SuppressWarnings("deprecation")
        @Override
        public boolean callback(DisconnectedCallbackArgs e)
        {
            if (_RebakeScheduleTimer != null)
            {
                _RebakeScheduleTimer.cancel();
                _RebakeScheduleTimer = null;
            }

            if (_AppearanceThread != null)
            {
                if (_AppearanceThread.isAlive())
                {
                    _AppearanceThread.stop();
                }
                _AppearanceThread = null;
            }
            return true;
        }
    }
    // #endregion Callbacks

    // #region Static Helpers

    /**
     * Converts a WearableType to a bodypart or clothing WearableType
     *
     * @param type A WearableType
     * @returns AssetType.Bodypart or AssetType.Clothing or AssetType.Unknown
     */
    public static AssetType WearableTypeToAssetType(WearableType type)
    {
        switch (type)
        {
            case Shape:
            case Skin:
            case Hair:
            case Eyes:
                 return AssetType.Bodypart;
            case Shirt:
            case Pants:
            case Shoes:
            case Socks:
            case Jacket:
            case Gloves:
            case Undershirt:
            case Underpants:
            case Skirt:
            case Tattoo:
            case Alpha:
            case Physics:
                return AssetType.Clothing;
            default:
                return AssetType.Unknown;
        }
    }

    /**
     * Converts a BakeType to the corresponding baked texture slot in AvatarTextureIndex
     *
     * @param index A BakeType
     * @returns The AvatarTextureIndex slot that holds the given BakeType
     */
    public static AvatarTextureIndex BakeTypeToAgentTextureIndex(BakeType index)
    {
        switch (index)
        {
            case Head:
                return AvatarTextureIndex.HeadBaked;
            case UpperBody:
                return AvatarTextureIndex.UpperBaked;
            case LowerBody:
                return AvatarTextureIndex.LowerBaked;
            case Eyes:
                return AvatarTextureIndex.EyesBaked;
            case Skirt:
                return AvatarTextureIndex.SkirtBaked;
            case Hair:
                return AvatarTextureIndex.HairBaked;
            default:
                return AvatarTextureIndex.Unknown;
        }
    }

    /**
     * Gives the layer number that is used for morph mask
     *
     * @param bakeType A BakeType
     * @returns Which layer number as defined in BakeTypeToTextures is used for morph mask
     */
    public static AvatarTextureIndex MorphLayerForBakeType(BakeType bakeType)
    {
        // Indexes return here correspond to those returned
        // in BakeTypeToTextures(), those two need to be in sync.
        // Which wearable layer is used for morph is defined in avatar_lad.xml
        // by looking for <layer> that has <morph_mask> defined in it, and
        // looking up which wearable is defined in that layer. Morph mask
        // is never combined, it's always a straight copy of one single clothing
        // item's alpha channel per bake.
        switch (bakeType)
        {
            case Head:
                return AvatarTextureIndex.Hair; // hair
            case UpperBody:
                return AvatarTextureIndex.UpperShirt; // shirt
            case LowerBody:
                return AvatarTextureIndex.LowerPants; // lower pants
            case Skirt:
                return AvatarTextureIndex.Skirt; // skirt
            case Hair:
                return AvatarTextureIndex.Hair; // hair
            default:
                return AvatarTextureIndex.Unknown;
        }
    }

    /**
     * Converts a BakeType to a list of the texture slots that make up that bake
     *
     * @param bakeType A BakeType
     * @returns A list of texture slots that are inputs for the given bake
     */
    public static List<AvatarTextureIndex> BakeTypeToTextures(BakeType bakeType)
    {
        List<AvatarTextureIndex> textures = new ArrayList<AvatarTextureIndex>();

        switch (bakeType)
        {
            case Head:
                textures.add(AvatarTextureIndex.HeadBodypaint);
                textures.add(AvatarTextureIndex.HeadTattoo);
                // textures.add(AvatarTextureIndex.Hair);
                textures.add(AvatarTextureIndex.HeadAlpha);
                break;
            case UpperBody:
                textures.add(AvatarTextureIndex.UpperBodypaint);
                textures.add(AvatarTextureIndex.UpperTattoo);
                textures.add(AvatarTextureIndex.UpperGloves);
                textures.add(AvatarTextureIndex.UpperUndershirt);
                textures.add(AvatarTextureIndex.UpperShirt);
                textures.add(AvatarTextureIndex.UpperJacket);
                textures.add(AvatarTextureIndex.UpperAlpha);
                break;
            case LowerBody:
                textures.add(AvatarTextureIndex.LowerBodypaint);
                textures.add(AvatarTextureIndex.LowerTattoo);
                textures.add(AvatarTextureIndex.LowerUnderpants);
                textures.add(AvatarTextureIndex.LowerSocks);
                textures.add(AvatarTextureIndex.LowerShoes);
                textures.add(AvatarTextureIndex.LowerPants);
                textures.add(AvatarTextureIndex.LowerJacket);
                textures.add(AvatarTextureIndex.LowerAlpha);
                break;
            case Eyes:
                textures.add(AvatarTextureIndex.EyesIris);
                textures.add(AvatarTextureIndex.EyesAlpha);
                break;
            case Skirt:
                textures.add(AvatarTextureIndex.Skirt);
                break;
            case Hair:
                textures.add(AvatarTextureIndex.Hair);
                textures.add(AvatarTextureIndex.HairAlpha);
                break;
			default:
				break;
        }
        return textures;
    }
    // #endregion Static Helpers
}