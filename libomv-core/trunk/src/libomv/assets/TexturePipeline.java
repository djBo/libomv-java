﻿/**
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
package libomv.assets;

import java.util.HashMap;
import java.util.SortedMap;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import libomv.GridClient;
import libomv.LoginManager.LoginProgressCallbackArgs;
import libomv.LoginManager.LoginStatus;
import libomv.NetworkManager.DisconnectedCallbackArgs;
import libomv.LibSettings;
import libomv.Simulator;
import libomv.assets.AssetManager.DelayedTransfer;
import libomv.assets.AssetManager.ImageDownload;
import libomv.assets.AssetManager.ImageType;
import libomv.assets.AssetManager.StatusCode;
import libomv.imaging.ManagedImage.ImageCodec;
import libomv.packets.ImageDataPacket;
import libomv.packets.ImageNotInDatabasePacket;
import libomv.packets.ImagePacketPacket;
import libomv.packets.Packet;
import libomv.packets.PacketType;
import libomv.packets.RequestImagePacket;
import libomv.types.PacketCallback;
import libomv.types.UUID;
import libomv.utils.Callback;
import libomv.utils.Logger;
import libomv.utils.Logger.LogLevel;
import libomv.utils.TimeoutEvent;

public class TexturePipeline implements PacketCallback
{
	// The current status of a texture request as it moves through the pipeline
	// or final result of a texture request.
	public enum TextureRequestState
	{
		// The initial state given to a request. Requests in this state are
		// waiting for an available slot in the pipeline
		Pending,
		// A request that has been added to the pipeline and the request packet
		// has been sent to the simulator
		Started,
		// A request that has received one or more packets back from the
		// simulator
		Progress,
		// A request that has received all packets back from the simulator
		Finished,
		// A request that has taken longer than {@link
		// Settings.PIPELINE_REQUEST_TIMEOUT} to download OR the initial
		// packet containing the packet information was never received
		Timeout,
		// The texture request was aborted by request of the agent
		Aborted,
		// The simulator replied to the request that it was not able to find the
		// requested texture
		NotFound
	}

	/**
	 * Texture request download handler, allows a configurable number of
	 * download slots which manage multiple concurrent texture downloads from
	 * the {@link Simulator}
	 * 
	 * This class makes full use of the internal {@link TextureCache} system for
	 * full texture downloads.
	 */

	// #if DEBUG_TIMING // Timing globals
	// The combined time it has taken for all textures requested sofar. This includes the amount of
	// time the texture spent waiting for a download slot, and the time spent retrieving the actual
	// texture from the Grid
	public static long TotalTime;
	// The amount of time the request spent in the <see cref="TextureRequestState.Progress"/> state
	public static long NetworkTime;
	// The total number of bytes transferred since the TexturePipeline was started
	public static int TotalBytes;

	// #endif

	// A request task containing information and status of a request as it is
	// processed through the <see cref="TexturePipeline"/>
	private class TaskInfo
	{
		// The slot this request is occupying in the threadpoolSlots array
		public int RequestSlot;
		
		public SortedMap<Short, Short> PacketsSeen;
		
		public Simulator Simulator;
		// The timeout event used for this task
		public TimeoutEvent<Boolean> TimeoutEvent;
		// An object that maintains the data of an request thats in-process.
		public ImageDownload Request;
	}

	// A dictionary containing all pending and in-process transfer requests
	// where the Key is both the RequestID
	// and also the Asset Texture ID, and the value is an object containing the
	// current state of the request and also
	// the asset data as it is being re-assembled
	private final HashMap<UUID, TaskInfo> _TexTransfers;
	// Holds the reference to the <see cref="GridClient"/> client object
	private final GridClient _Client;

	private final AssetCache _Cache;

	private final ExecutorService _ThreadPool;
	// An array of worker slots which shows the availablity status of the slot
	private final Future<?>[] _ThreadRequests;

	// The primary thread which manages the requests.
	private Thread downloadMaster;
	// true if the TexturePipeline is currently running
	private boolean _Running;
	// A refresh timer used to increase the priority of stalled requests
	private Timer RefreshDownloadsTimer;

	// Current number of pending and in-process transfers
	public final int getTransferCount()
	{
		return _TexTransfers.size();
	}

	/**
	 * Default constructor, Instantiates a new copy of the TexturePipeline class
	 * 
	 * @param client
	 *            Reference to the instantiated <see cref="GridClient"/> object
	 */
	public TexturePipeline(GridClient client, AssetCache cache)
	{
		_Client = client;
		_Cache = cache;

		int maxDownloads = _Client.Settings.MAX_CONCURRENT_TEXTURE_DOWNLOADS;

		_ThreadPool = Executors.newFixedThreadPool(maxDownloads);
		_ThreadRequests = new Future[maxDownloads];

		_TexTransfers = new HashMap<UUID, TaskInfo>();

		// Handle client connected and disconnected events
		client.Login.OnLoginProgress.add(new Network_LoginProgress());
		client.Network.OnDisconnected.add(new Network_Disconnected());
	}

	@Override
	public void packetCallback(Packet packet, Simulator simulator) throws Exception
	{
		switch (packet.getType())
		{
			case ImageData:
				HandleImageData(packet, simulator);
				break;
			case ImagePacket:
				HandleImagePacket(packet, simulator);
				break;
			case ImageNotInDatabase:
				HandleImageNotInDatabase(packet, simulator);
				break;
			default:
				break;
		}
	}

	private class Network_LoginProgress implements Callback<LoginProgressCallbackArgs>
	{
		@Override
		public boolean callback(LoginProgressCallbackArgs e)
		{
			if (e.getStatus() == LoginStatus.Success)
			{
				startup();
			}
			return false;
		}
	}

	private class Network_Disconnected implements Callback<DisconnectedCallbackArgs>
	{
		@Override
		public boolean callback(DisconnectedCallbackArgs e)
		{
			shutdown();
			return false;
		}
	}

	/**
	 * Initialize callbacks required for the TexturePipeline to operate
	 * 
	 */
	public final void startup()
	{
		if (_Running)
		{
			return;
		}

		if (downloadMaster == null)
		{
			// Instantiate master thread that manages the request pool
			downloadMaster = new Thread(new DownloadThread());
			downloadMaster.setName("TexturePipeline");
			downloadMaster.setDaemon(true);
		}
		_Running = true;

		_Client.Network.RegisterCallback(PacketType.ImageData, this);
		_Client.Network.RegisterCallback(PacketType.ImagePacket, this);
		_Client.Network.RegisterCallback(PacketType.ImageNotInDatabase, this);
		downloadMaster.start();

		if (RefreshDownloadsTimer == null)
		{
			RefreshDownloadsTimer = new Timer();
			RefreshDownloadsTimer.schedule(new RefreshDownloadsTimer_Elapsed(), LibSettings.PIPELINE_REFRESH_INTERVAL,
					LibSettings.PIPELINE_REFRESH_INTERVAL);
		}
	}

	/**
	 * Shutdown the TexturePipeline and cleanup any callbacks or transfers
	 */
	public final void shutdown()
	{
		if (!_Running)
		{
			return;
		}
		// #if DEBUG_TIMING
		Logger.Log(String.format(
				"Combined Execution Time: %d, Network Execution Time %d, Network %d k/sec, Image Size %d", TotalTime,
				NetworkTime, getNetworkThroughput(TotalBytes, NetworkTime), TotalBytes), LogLevel.Debug, _Client);
		// #endif
		if (null != RefreshDownloadsTimer)
		{
			RefreshDownloadsTimer.cancel();
		}
		RefreshDownloadsTimer = null;

		if (downloadMaster != null && downloadMaster.isAlive())
		{
			_Running = false;
		}
		downloadMaster = null;

		_Client.Network.UnregisterCallback(PacketType.ImageNotInDatabase, this);
		_Client.Network.UnregisterCallback(PacketType.ImageData, this);
		_Client.Network.UnregisterCallback(PacketType.ImagePacket, this);

		synchronized (_TexTransfers)
		{
			_TexTransfers.clear();
		}

		synchronized (_ThreadRequests)
		{
			for (int i = 0; i < _ThreadRequests.length; i++)
			{
				if (_ThreadRequests[i] != null)
				{
					_ThreadRequests[i].cancel(true);
					_ThreadRequests[i] = null;
				}
			}
		}
	}

	private class RefreshDownloadsTimer_Elapsed extends TimerTask
	{
		@Override
		public void run()
		{
			synchronized (_TexTransfers)
			{
				for (TaskInfo task : _TexTransfers.values())
				{
					if (task.Request.State.equals(TextureRequestState.Progress))
					{
						// Find the first missing packet in the download
						short packet = 0;
						synchronized (task)
						{
							if (task.PacketsSeen != null && task.PacketsSeen.size() > 0)
							{
								packet = GetFirstMissingPacket(task.PacketsSeen);
							}
						}

						if (task.Request.TimeSinceLastPacket > 5000)
						{
							// We're not receiving data for this texture fast
							// enough, bump up the priority by 5%
							task.Request.Priority *= 1.05f;
							task.Request.TimeSinceLastPacket = 0;
							try
							{
								RequestImage(task.Request.ItemID, task.Request.ImageType, task.Request.Priority, task.Request.DiscardLevel,
										packet);
							}
							catch (Exception e)
							{
							}
						}

						if (task.Request.TimeSinceLastPacket > _Client.Settings.PIPELINE_REQUEST_TIMEOUT)
						{
							task.TimeoutEvent.set(true);
						}
					}
				}
			}
		}
	}

	/**
	 * Request a texture asset from the simulator using the <see
	 * cref="TexturePipeline"/> system to manage the requests and re-assemble
	 * the image from the packets received from the simulator
	 * 
	 * @param textureID
	 *            The <see cref="UUID"/> of the texture asset to download
	 * @param imageType
	 *            The <see cref="ImageType"/> of the texture asset. Use <see
	 *            cref="ImageType.Normal"/> for most textures, or <see
	 *            cref="ImageType.Baked"/> for baked layer texture assets
	 * @param priority
	 *            A float indicating the requested priority for the transfer.
	 *            Higher priority values tell the simulator to prioritize the
	 *            request before lower valued requests. An image already being
	 *            transferred using the <see cref="TexturePipeline"/> can have
	 *            its priority changed by resending the request with the new
	 *            priority value
	 * @param discardLevel
	 *            Number of quality layers to discard. This controls the end
	 *            marker of the data sent
	 * @param packetStart
	 *            The packet number to begin the request at. A value of 0 begins
	 *            the request from the start of the asset texture
	 * @param callback
	 *            The <see cref="TextureDownloadCallback"/> callback to fire
	 *            when the image is retrieved. The callback will contain the
	 *            result of the request and the texture asset data
	 * @param progressive
	 *            If true, the callback will be fired for each chunk of the
	 *            downloaded image. The callback asset parameter will contain
	 *            all previously received chunks of the texture asset starting
	 *            from the beginning of the request
	 */
	public boolean RequestTexture(ImageDownload request)
	{
		TaskInfo task = GetTransferValue(request.ItemID);
		if (task == null)
		{
			request.State = TextureRequestState.Pending;
			task = new TaskInfo();
			task.TimeoutEvent = new TimeoutEvent<Boolean>();
			task.RequestSlot = -1;
			task.Request = request;
		
			synchronized (_TexTransfers)
			{
				_TexTransfers.put(request.ItemID, task);
			}
		}
		return true;
	}

	/**
	 * Sends the actual request packet to the simulator
	 * 
	 * @Note Sending a priority of 0 and a discardlevel of -1 aborts download
	 *
	 * @param imageID
	 *            The image to download
	 * @param type
	 *            Type of the image to download, either a baked avatar texture
	 *            or a normal texture
	 * @param priority
	 *            Priority level of the download. Default is <c>1,013,000.0f</c>
	 * @param discardLevel
	 *            Number of quality layers to discard. This controls the end
	 *            marker of the data sent
	 * @param packetNum
	 *            Packet number to start the download at. This controls the
	 *            start marker of the data sent
	 * @throws Exception
	 */
	private void RequestImage(UUID imageID, ImageType type, float priority, int discardLevel, int packetNum) throws Exception
	{
		// Priority == 0 && DiscardLevel == -1 means cancel the transfer
		if (priority == 0 && discardLevel == -1)
		{
			AbortTextureRequest(imageID);
		}
		else
		{
			TaskInfo task = GetTransferValue(imageID);
			if (task != null)
			{
				if (task.Simulator != null)
				{
					// Already downloading, just updating the priority
					float percentComplete = ((float) task.Request.Transferred / (float) task.Request.Size) * 100f;
					if (Float.isNaN(percentComplete))
					{
						percentComplete = 0f;
					}

					if (percentComplete > 0f)
					{
						Logger.DebugLog(String.format("Updating priority on image transfer %s to %d, %d% complete",
								imageID.toString(), task.Request.Priority, Math.round(percentComplete)));
					}
				}
				else
				{
					task.Simulator = _Client.Network.getCurrentSim();
				}

				// Build and send the request packet
				RequestImagePacket request = new RequestImagePacket();
				request.AgentData.AgentID = _Client.Self.getAgentID();
				request.AgentData.SessionID = _Client.Self.getSessionID();
				request.RequestImage = new RequestImagePacket.RequestImageBlock[1];
				request.RequestImage[0] = request.new RequestImageBlock();
				request.RequestImage[0].DiscardLevel = (byte) discardLevel;
				request.RequestImage[0].DownloadPriority = priority;
				request.RequestImage[0].Packet = packetNum;
				request.RequestImage[0].Image = imageID;
				request.RequestImage[0].Type = type.getValue();

				_Client.Network.sendPacket(request);
			}
			else
			{
				Logger.Log("Received texture download request for a texture that isn't in the download queue: "
						+ imageID, LogLevel.Warning);
			}
		}
	}

	/**
	 * Cancel a pending or in process texture request
	 * 
	 * @param textureID
	 *            The texture assets unique ID
	 * @throws Exception
	 */
	public final void AbortTextureRequest(UUID textureID) throws Exception
	{
		TaskInfo task = GetTransferValue(textureID);
		if (task != null)
		{
			// this means we've actually got the request assigned to the threadpool
			if (task.Request.State == TextureRequestState.Progress)
			{
				RequestImagePacket request = new RequestImagePacket();
				request.AgentData.AgentID = _Client.Self.getAgentID();
				request.AgentData.SessionID = _Client.Self.getSessionID();
				request.RequestImage = new RequestImagePacket.RequestImageBlock[1];
				request.RequestImage[0] = request.new RequestImageBlock();
				request.RequestImage[0].DiscardLevel = -1;
				request.RequestImage[0].DownloadPriority = 0;
				request.RequestImage[0].Packet = 0;
				request.RequestImage[0].Image = textureID;
				request.RequestImage[0].Type = task.Request.ImageType.getValue();
				_Client.Network.sendPacket(request);

				RemoveTransfer(textureID);

				task.TimeoutEvent.set(false);
				task.Request.State = TextureRequestState.Aborted;
				task.Request.callbacks.dispatch(task.Request);
				_Client.Assets.FireImageProgressEvent(task.Request.ItemID, task.Request.Transferred, task.Request.Size);
			}
			else
			{
				RemoveTransfer(textureID);

				task.Request.State = TextureRequestState.Aborted;
				task.Request.callbacks.dispatch(task.Request);
				_Client.Assets.FireImageProgressEvent(task.Request.ItemID, task.Request.Transferred, task.Request.Size);
			}
		}
	}

	/**
	 * Master Download Thread, Queues up downloads in the threadpool
	 */
	private class DownloadThread implements Runnable
	{
		@Override
		public void run()
		{
			while (_Running)
			{
				// find free slots
				int active = 0;
				int slot;

				TaskInfo nextTask = null;

				synchronized (_TexTransfers)
				{
					for (TaskInfo request : _TexTransfers.values())
					{
						if (request.Request.State == TextureRequestState.Pending)
						{
							nextTask = request;
						}
						else if (request.Request.State == TextureRequestState.Started ||
								 request.Request.State == TextureRequestState.Progress)
						{
							++active;
						}
					}
				}

				if (nextTask != null  && active <= _ThreadRequests.length)
				{
					slot = -1;
					// find available slot for reset event
					synchronized (_ThreadRequests)
					{
						for (int i = 0; i < _ThreadRequests.length; i++)
						{
							if (_ThreadRequests[i] == null)
							{
								// found a free slot
								slot = i;
								break;
							}
						}
					}

					// -1 = slot not available
					if (slot != -1)
					{
						nextTask.Request.State = TextureRequestState.Started;
						nextTask.RequestSlot = slot;

						Logger.DebugLog(String.format("Sending Worker thread new download request %d", slot));
						Future<?> request = _ThreadPool.submit(new TextureRequestDoWork(nextTask));
						synchronized (_ThreadRequests)
						{
							_ThreadRequests[slot] = request;
						}
						continue;
					}
				}
				// Queue was empty or all download slots are in use, let's give
				// up some CPU time
				try
				{
					Thread.sleep(500);
				}
				catch (InterruptedException e)
				{
				}
			}
			Logger.Log("Texture pipeline download thread shutting down", LogLevel.Info, _Client);
		}
	}

	/**
	 * The worker thread that sends the request and handles timeouts
	 * 
	 * @param threadContext
	 *            A <see cref="TaskInfo"/> object containing the request details
	 */
	private class TextureRequestDoWork implements Runnable
	{
		private final TaskInfo task;

		public TextureRequestDoWork(TaskInfo task)
		{
			this.task = task;
		}

		@Override
		public void run()
		{

			task.Request.State = TextureRequestState.Progress;
			// Find the first missing packet in the download
			short packet = 0;
			synchronized (task.Request)
			{
				if (task.PacketsSeen != null && task.PacketsSeen.size() > 0)
				{
					packet = GetFirstMissingPacket(task.PacketsSeen);
				}
			}

			// Request the texture
			try
			{
				RequestImage(task.Request.ItemID, task.Request.ImageType, task.Request.Priority, task.Request.DiscardLevel, packet);
			}
			catch (Exception e)
			{
			}

			// Set starting time
			task.Request.TimeSinceLastPacket = 0;

			// Don't release this worker slot until texture is downloaded or
			// timeout occurs
			Boolean timeout;
			try
			{
				timeout = task.TimeoutEvent.waitOne(-1);
				if (timeout == null || !timeout)
				{
					// Timed out
					Logger.Log("Worker " + task.RequestSlot + " timeout waiting for texture " + task.Request.ItemID
							+ " to download got " + task.Request.Transferred + " of " + task.Request.Size,
							LogLevel.Warning);

					RemoveTransfer(task.Request.ItemID);

					task.Request.State = TextureRequestState.Timeout;
					task.Request.callbacks.dispatch(task.Request);
					_Client.Assets
							.FireImageProgressEvent(task.Request.ItemID, task.Request.Transferred, task.Request.Size);
				}
			}
			catch (InterruptedException e)
			{
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

			// Free up this download slot
			synchronized (_ThreadRequests)
			{
				_ThreadRequests[task.RequestSlot] = null;
			}
		}
	}

	private short GetFirstMissingPacket(SortedMap<Short, Short> packetsSeen)
	{
		short packet = 0;

		synchronized (packetsSeen)
		{
			boolean first = true;
			for (short packetSeen : packetsSeen.values())
			{
				if (first)
				{
					// Initially set this to the earliest packet received in the
					// transfer
					packet = packetSeen;
					first = false;
				}
				else
				{
					++packet;

					// If there is a missing packet in the list, break and
					// request the download
					// resume here
					if (packetSeen != packet)
					{
						--packet;
						break;
					}
				}
			}
			++packet;
		}
		return packet;
	}

	// #region Raw Packet Handlers

	/**
	 * Handle responses from the simulator that tell us a texture we have
	 * requested is unable to be located or no longer exists. This will remove
	 * the request from the pipeline and free up a slot if one is in use
	 * 
	 * @param sender
	 *            The sender
	 * @param e
	 *            The EventArgs object containing the packet data
	 */
	private final void HandleImageNotInDatabase(Packet packet, Simulator simulator)
	{
		ImageNotInDatabasePacket imageNotFoundData = (ImageNotInDatabasePacket) packet;
		TaskInfo task = GetTransferValue(imageNotFoundData.ID);
		if (task != null)
		{
			// cancel active request and free up the threadpool slot
			if (task.Request.State.equals(TextureRequestState.Progress))
			{
				task.TimeoutEvent.set(true);
			}

			RemoveTransfer(imageNotFoundData.ID);

			// fire callback to inform the caller
			task.Request.State = TextureRequestState.NotFound;
			task.Request.callbacks.dispatch(task.Request);
			task.TimeoutEvent.set(true);
		}
		else
		{
			Logger.Log("Received an ImageNotFound packet for an image we did not request: "
					+ imageNotFoundData.ID, LogLevel.Warning);
		}
	}

	private boolean processDelayedData(ImageDownload download, DelayedTransfer data)
	{
		while (data != null)
		{
			System.arraycopy(data.Data, 0, download.AssetData, download.Transferred, data.Data.length);
			download.Transferred += data.Data.length;
			if (data.Status == StatusCode.OK && download.Transferred >= download.Size)
			{
				download.State = TextureRequestState.Finished;
			}
			else if (data.Status == StatusCode.Error)
			{
				download.State = TextureRequestState.Aborted;
			}
			
			if (download.State != TextureRequestState.Progress)
			{
				synchronized (_TexTransfers)
				{
					_TexTransfers.remove(download.ItemID);
				}
				download.delayed.clear();

				download.Success = download.State == TextureRequestState.Finished;
				if (download.Success)
				{
					Logger.DebugLog("Transfer for asset " + download.ItemID.toString() + " completed", _Client);

					// Cache successful asset download
					_Cache.put(download.ItemID, download.AssetData, download.suffix);
				}
				else
				{
					Logger.Log("Transfer failed with status code " + download.State, LogLevel.Warning, _Client);
				}
					
				download.callbacks.dispatch(download);
				return true;
			}
			
			download.PacketNum++;
			data = download.delayed.remove(download.PacketNum);
		}
		
		if (download.ReportProgress)
		{
			download.callbacks.dispatch(download);
			_Client.Assets.FireImageProgressEvent(download.ItemID, download.Transferred, download.Size);
		}
		return false;
	}

	/**
	 * Handle the initial ImageDataPacket sent from the simulator
	 * 
	 * @param sender
	 *            The sender
	 * @param e
	 *            The EventArgs object containing the packet data
	 */
	private final void HandleImageData(Packet packet, Simulator simulator)
	{
		ImageDataPacket data = (ImageDataPacket) packet;
		TaskInfo task = GetTransferValue(data.ImageID.ID);
		if (task == null)
		{
			Logger.Log("Received a ImageData packet for a texture we didn't request, Image ID: "
					+ data.ImageID.ID, LogLevel.Warning, _Client);			
			return;
		}

		// reset the timeout interval since we got data
		task.Request.TimeSinceLastPacket = 0;

		if (task.Request.Size == 0)
		{
			task.Request.Codec = ImageCodec.setValue(data.ImageID.Codec);
//			task.Request.PacketCount = data.ImageID.Packets;
			task.Request.Size = data.ImageID.Size;
			task.Request.AssetData = new byte[task.Request.Size];

			processDelayedData(task.Request, _Client.Assets.new DelayedTransfer(StatusCode.OK, data.ImageData.getData()));
		}
	}

	/**
	 * Handles the remaining Image data that did not fit in the initial
	 * ImageData packet
	 * 
	 * @param sender
	 *            The sender
	 * @param e
	 *            The EventArgs object containing the packet data
	 * @throws InterruptedException
	 */
	private final void HandleImagePacket(Packet packet, Simulator simulator) throws InterruptedException
	{
		ImagePacketPacket image = (ImagePacketPacket) packet;
		TaskInfo task = GetTransferValue(image.ImageID.ID);
		if (task != null)
		{
			StatusCode status = StatusCode.OK;
			DelayedTransfer info = _Client.Assets.new DelayedTransfer(status, image.ImageData.getData());
			
			if (!task.Request.gotInfo() || image.ImageID.Packet != task.Request.PacketNum)
			{
				/* We haven't received the header yet, or the packet number is higher than the currently
				 * expected packet. Put it in the out of order hashlist */
				task.Request.delayed.put((int)image.ImageID.Packet, info);
			}
			else
			{
				processDelayedData(task.Request, info);
			}
			task.Request.TimeSinceLastPacket = 0;
		}
	}

	private int getNetworkThroughput(long bytes, long duration)
	{
		return duration != 0 ? Math.round(bytes / duration) : 0;
	}
	
	// #endregion

	private TaskInfo GetTransferValue(UUID textureID)
	{
		synchronized (_TexTransfers)
		{
			return _TexTransfers.get(textureID);
		}
	}

	private boolean RemoveTransfer(UUID textureID)
	{
		synchronized (_TexTransfers)
		{
			return _TexTransfers.remove(textureID) != null;
		}
	}
}
