/**
 * Copyright (c) 2007-2008, openmetaverse.org
 * Copyright (c) 2009-2012, Frederick Martian
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

import java.net.URI;
import java.util.Hashtable;

import org.apache.http.HttpStatus;
import org.apache.http.client.HttpResponseException;
import org.apache.http.nio.concurrent.FutureCallback;

import libomv.StructuredData.OSD;
import libomv.StructuredData.OSD.OSDType;
import libomv.StructuredData.OSDArray;
import libomv.StructuredData.OSDMap;
import libomv.capabilities.CapsClient;
import libomv.capabilities.CapsEventQueue;
import libomv.utils.Logger;
import libomv.utils.Logger.LogLevel;

/**
 * Capabilities is the name of the bi-directional HTTP REST protocol used to
 * communicate non real-time transactions such as teleporting or group messaging
 */
public class CapsManager
{
	/* Reference to the simulator this system is connected to */
	private Simulator _Simulator;

	private String _SeedCapsURI;
	private CapsClient _Client;
	private Hashtable<String, URI> _Capabilities = new Hashtable<String, URI>();

	private CapsEventQueue _EventQueue = null;

	/* Capabilities URI this system was initialized with */
	public final String getSeedCapsURI()
	{
		return _SeedCapsURI;
	}

	/*
	 * Whether the capabilities event queue is connected and listening for
	 * incoming events
	 */
	public final boolean getIsEventQueueRunning()
	{
		if (_EventQueue != null)
		{
			return _EventQueue.getRunning();
		}
		return false;
	}

	/**
	 * Default constructor
	 * 
	 * @param simulator
	 * @param seedcaps
	 */
	public CapsManager(Simulator simulator, String seedcaps)
	{
		_Simulator = simulator;
		_SeedCapsURI = seedcaps;
		makeSeedRequest();
	}

	public final void disconnect(boolean immediate) throws InterruptedException
	{
		Logger.Log(
				String.format("Caps system for " + _Simulator.getName() + " is "
						+ (immediate ? "aborting" : "disconnecting")), LogLevel.Info, _Simulator.getClient());

		if (_Client != null)
		{
			_Client.shutdown(true);
			_Client = null;
		}

		if (_EventQueue != null)
		{
			_EventQueue.shutdown(immediate);
			_EventQueue = null;
		}
	}

	/**
	 * Request the URI of a named capability
	 * 
	 * @param capability
	 *            Name of the capability to request
	 * @return The URI of the requested capability, or String. Empty if the
	 *         capability does not exist
	 */
	public final URI capabilityURI(String capability)
	{
		return _Capabilities.get(capability);
	}

	private void makeSeedRequest()
	{
		if (_Simulator == null || !_Simulator.getClient().Network.getConnected())
		{
			return;
		}

		// Create a request list
		OSDArray req = new OSDArray();
		// This list can be updated by using the following command to obtain a
		// current list of capabilities the official linden viewer supports:
		// wget -q -O - https://bitbucket.org/lindenlab/viewer-development/raw/default/indra/newview/llviewerregion.cpp
		// | grep 'capabilityNames.append' | sed 's/^[ \t]*//;s/capabilityNames.append("/req.Add("/'
		req.add(OSD.FromString("AgentState"));
		req.add(OSD.FromString("AttachmentResources"));
		req.add(OSD.FromString("AvatarPickerSearch"));
		req.add(OSD.FromString("CharacterProperties"));
		req.add(OSD.FromString("ChatSessionRequest"));
		req.add(OSD.FromString("CopyInventoryFromNotecard"));
		req.add(OSD.FromString("CreateInventoryCategory"));
		req.add(OSD.FromString("DispatchRegionInfo"));
		req.add(OSD.FromString("EnvironmentSettings"));
		req.add(OSD.FromString("EstateChangeInfo"));
		req.add(OSD.FromString("EventQueueGet"));
		req.add(OSD.FromString("FetchInventory2"));
		req.add(OSD.FromString("FetchInventoryDescendents2"));
		req.add(OSD.FromString("FetchLib2"));
		req.add(OSD.FromString("FetchLibDescendents2"));
		req.add(OSD.FromString("GetDisplayNames"));
		req.add(OSD.FromString("GetMesh"));
		req.add(OSD.FromString("GetObjectCost"));
		req.add(OSD.FromString("GetObjectPhysicsData"));
		req.add(OSD.FromString("GetTexture"));
		req.add(OSD.FromString("GroupMemberData"));
		req.add(OSD.FromString("GroupProposalBallot"));
		req.add(OSD.FromString("HomeLocation"));
		req.add(OSD.FromString("LandResources"));
		req.add(OSD.FromString("MapLayer"));
		req.add(OSD.FromString("MapLayerGod"));
		req.add(OSD.FromString("MeshUploadFlags"));
		req.add(OSD.FromString("NavMeshGenerationStatus"));
		req.add(OSD.FromString("NewFileAgentInventory"));
		req.add(OSD.FromString("ObjectMedia"));
		req.add(OSD.FromString("ObjectMediaNavigate"));
		req.add(OSD.FromString("ObjNavMeshProperties"));
		req.add(OSD.FromString("ParcelPropertiesUpdate"));
		req.add(OSD.FromString("ParcelVoiceInfoRequest"));
		req.add(OSD.FromString("ProductInfoRequest"));
		req.add(OSD.FromString("ProvisionVoiceAccountRequest"));
		req.add(OSD.FromString("RemoteParcelRequest"));
		req.add(OSD.FromString("RequestTextureDownload"));
		req.add(OSD.FromString("ResourceCostSelected"));
		req.add(OSD.FromString("RetrieveNavMeshSrc"));
		req.add(OSD.FromString("SearchStatRequest"));
		req.add(OSD.FromString("SearchStatTracking"));
		req.add(OSD.FromString("SendPostcard"));
		req.add(OSD.FromString("SendUserReport"));
		req.add(OSD.FromString("SendUserReportWithScreenshot"));
		req.add(OSD.FromString("ServerReleaseNotes"));
		req.add(OSD.FromString("SetDisplayName"));
		req.add(OSD.FromString("SimConsoleAsync"));
		req.add(OSD.FromString("SimulatorFeatures"));
		req.add(OSD.FromString("StartGroupProposal"));
		req.add(OSD.FromString("TerrainNavMeshProperties"));
		req.add(OSD.FromString("TextureStats"));
		req.add(OSD.FromString("UntrustedSimulatorMessage"));
		req.add(OSD.FromString("UpdateAgentInformation"));
		req.add(OSD.FromString("UpdateAgentLanguage"));
		req.add(OSD.FromString("UpdateAvatarAppearance"));
		req.add(OSD.FromString("UpdateGestureAgentInventory"));
		req.add(OSD.FromString("UpdateGestureTaskInventory"));
		req.add(OSD.FromString("UpdateNotecardAgentInventory"));
		req.add(OSD.FromString("UpdateNotecardTaskInventory"));
		req.add(OSD.FromString("UpdateScriptAgent"));
		req.add(OSD.FromString("UpdateScriptTask"));
		req.add(OSD.FromString("UploadBakedTexture"));
		req.add(OSD.FromString("ViewerMetrics"));
		req.add(OSD.FromString("ViewerStartAuction"));
		req.add(OSD.FromString("ViewerStats"));
		req.add(OSD.FromString("WebFetchInventoryDescendents"));

		try
		{
			if (_Client == null)
				_Client = new CapsClient("makeSeedRequest");
			_Client.executeHttpPost(new URI(_SeedCapsURI), req, OSD.OSDFormat.Xml,
					new SeedRequestHandler(), _Simulator.getClient().Settings.CAPS_TIMEOUT);
		}
		catch (Exception ex)
		{
			Logger.Log("Couldn't startup capability system", LogLevel.Error, _Simulator.getClient(), ex);
		}
	}

	private class SeedRequestHandler implements FutureCallback<OSD>
	{
		@Override
		public void completed(OSD result)
		{
			if (result != null && result.getType().equals(OSDType.Map))
			{
				OSDMap respTable = (OSDMap) result;
//				OSDMap meta = (OSDMap) respTable.remove("Metadata");
				synchronized (_Capabilities)
				{
					for (String cap : respTable.keySet())
					{
						OSD osd = respTable.get(cap);
						OSD.OSDType type = osd.getType();
						if (type == OSD.OSDType.String || type == OSD.OSDType.URI)
							_Capabilities.put(cap, osd.AsUri());
					}

					if (_Capabilities.containsKey("EventQueueGet"))
					{
						Logger.Log("Starting event queue for " + _Simulator.getName(), LogLevel.Info, _Simulator.getClient());
						try
						{
							_EventQueue = new CapsEventQueue(_Simulator, _Capabilities.get("EventQueueGet"));
							_EventQueue.start();
						}
						catch (Exception ex)
						{
							failed(ex);
						}
					}
				}
			}
			else
			{
				// The initial CAPS connection failed, try again
				makeSeedRequest();
			}
		}

		@Override
		public void failed(Exception ex)
		{
			if (ex instanceof HttpResponseException
					&& ((HttpResponseException) ex).getStatusCode() == HttpStatus.SC_NOT_FOUND)
			{
				Logger.Log("Seed capability returned a 404 status, capability system is aborting", LogLevel.Error, _Simulator.getClient());
			}
			else
			{
				// The initial CAPS connection failed, try again
				makeSeedRequest();
			}
		}

		@Override
		public void cancelled()
		{
			Logger.Log("Seed capability got cancelled, capability system is shutting down", LogLevel.Info, _Simulator.getClient());
		}
	}
}
