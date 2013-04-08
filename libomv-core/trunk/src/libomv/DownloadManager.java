/**
 * Copyright (c) 2009, openmetaverse.org
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

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.LinkedBlockingQueue;

import org.apache.http.nio.concurrent.FutureCallback;
import org.apache.http.nio.reactor.IOReactorException;

import libomv.capabilities.AsyncHTTPClient;
import libomv.capabilities.AsyncHTTPClient.ProgressCallback;
import libomv.utils.Logger;
import libomv.utils.Logger.LogLevel;

/// Manages async HTTP downloads with a limit on maximum concurrent downloads
public class DownloadManager
{
    // Represents individual HTTP Download request
    public class DownloadRequest
    {
        // URI of the item to fetch 
        public URI address;
        // Timout specified in milliseconds 
        public int millisecondsTimeout;
        // Accept the following content type
        public String contentType;
        // How many times will this request be retried
        public int retries;
        // Current fetch attempt
        public int attempt;
        // Progress callback
        public ProgressCallback progressCallback;
        // Download callback
        public FutureCallback<byte[]> downloadCallback;

        // Default constructor
        public DownloadRequest()
        {
            this.retries = 5;
            this.attempt = 0;
        }

        // Constructor
        public DownloadRequest(URI address, int millisecondsTimeout, String contentType,
        		               ProgressCallback progressCallback, FutureCallback<byte[]> downloadCallback)
        {
        	this();
            this.address = address;
            this.millisecondsTimeout = millisecondsTimeout;
            this.contentType = contentType;
            this.progressCallback = progressCallback;
            this.downloadCallback = downloadCallback;
        }
    }

    private class ActiveDownload extends AsyncHTTPClient<byte[]>
    {
		public List<FutureCallback<byte[]>> downloadHandlers = new ArrayList<FutureCallback<byte[]>>();
		public List<ProgressCallback> progressHandlers = new ArrayList<ProgressCallback>();

        public ActiveDownload() throws IOReactorException
		{
			super("ActiveDownload");
		}
        
		@Override
		protected byte[] convertContent(InputStream in, String encoding) throws IOException {
			// TODO Auto-generated method stub
			return null;
		}
    }

    private ActiveDownload createActiveDownload(FutureCallback<byte[]> callback)
    {
    	try
    	{
    		return new ActiveDownload();
		}
		catch (IOReactorException ex)
		{
			callback.failed(ex);
		}
		return null;
    }
    
    Queue<DownloadRequest> queue = new LinkedBlockingQueue<DownloadRequest>();
    HashMap<String, ActiveDownload> activeDownloads = new HashMap<String, ActiveDownload>();

    int m_ParallelDownloads = 8;
    X509Certificate m_ClientCert;

    // Maximum number of parallel downloads from a single endpoint
    public int getParallelDownloads()
    {
        return m_ParallelDownloads;
    }

    public void setParallelDownloads(int value)
    {
    	m_ParallelDownloads = value;
    }

    //  Client certificate
    public X509Certificate getClientCert()
    {
        return m_ClientCert;
    }

    public void setClientCert(X509Certificate value)
    {
        m_ClientCert = value;
    }

    // Cleanup method
    public void shutdown()
    {
        synchronized (activeDownloads)
        {
            for (ActiveDownload download : activeDownloads.values())
            {
                try
                {
                    download.shutdown(true);
                }
                catch (Exception ex) { }
            }
            activeDownloads.clear();
        }
    }

    // Check the queue for pending work
    private void enquePending()
    {
        synchronized (queue)
        {
            if (queue.size() > 0)
            {
                int nr = 0;
                synchronized (activeDownloads)
                {
                    nr = activeDownloads.size();
                }

                Logger.DebugLog(nr + " active downloads. Queued textures: " + queue.size());
                
                for (int i = nr; i < m_ParallelDownloads && queue.size() > 0; i++)
                {
                    final DownloadRequest item = queue.poll();
                    synchronized (activeDownloads)
                    {
                        final String addr = item.address.toString();
                        if (activeDownloads.containsKey(addr))
                        {
                            if (item.progressCallback != null)
                            {
                            	activeDownloads.get(addr).progressHandlers.add(item.progressCallback);
                            }
                            activeDownloads.get(addr).downloadHandlers.add(item.downloadCallback);
                        }
                        else
                        {
                            final ActiveDownload activeDownload = createActiveDownload(item.downloadCallback);
							if (activeDownload != null)
							{
	                            if (item.progressCallback != null)
	                            {
	                            	activeDownload.progressHandlers.add(item.progressCallback);
	                            }
	                            activeDownload.downloadHandlers.add(item.downloadCallback);

	                            Logger.DebugLog("Requesting " + addr);
	                            
	                            activeDownload.setProgressCallback(new ProgressCallback()
	                            {
	                                @Override
									public void progress(long bytesReceived, long totalBytesToReceive)
	                                {
	                                    for (ProgressCallback handler : activeDownload.progressHandlers)
	                                    {
	                                        handler.progress(bytesReceived, totalBytesToReceive);
	                                    }
	                                }
	                            });
	                            activeDownload.executeHttpGet(item.address, item.contentType, new FutureCallback<byte[]>()
	    	                    {
	                                @Override
									public void completed(byte[] responseData)
	                                {
	                                    synchronized (activeDownloads)
	                                    {
	                                    	activeDownloads.remove(addr);
	                                    }
	                                    for (FutureCallback<byte[]> handler : activeDownload.downloadHandlers)
	                                    {
	                                        handler.completed(responseData);
	                                    }
	                                    enquePending();
	                                }
	                                
	                                @Override
									public void failed(Exception ex)
	                                {
	                                    synchronized (activeDownloads)
	                                    {
	                                    	activeDownloads.remove(addr);
	                                    }
	                                    if (item.attempt >= item.retries)
	                                    {
	                                    	for (FutureCallback<byte[]> handler : activeDownload.downloadHandlers)
	                                    	{
	                                    		handler.failed(ex);
	                                    	}
	                                    }
	                                    else
	                                    {
	                                        item.attempt++;
                                            Logger.Log(String.format("Texture %s HTTP download failed, trying again retry %d/%d",
                                                    item.address, item.attempt, item.retries), LogLevel.Warning);
                                            synchronized(queue)
                                            {
                                            	enque(item);
                                            }
	                                    }
	                                    enquePending();
	                                }

									@Override
									public void cancelled()
									{
	                                    synchronized (activeDownloads)
	                                    {
	                                    	activeDownloads.remove(addr);
	                                    }
	                                    for (FutureCallback<byte[]> handler : activeDownload.downloadHandlers)
	                                    {
	                                        handler.cancelled();
	                                    }
	                                    enquePending();
									}
	                            }, item.millisecondsTimeout); 
	                            activeDownloads.put(addr, activeDownload);
							}
                        }
                    }
                }
            }
        }
    }

    // Enqueue a new HTPP download
    public void enque(DownloadRequest req)
    {
    	synchronized (activeDownloads)
        {
            String addr = req.address.toString();
            if (activeDownloads.containsKey(addr))
            {
                activeDownloads.get(addr).downloadHandlers.add(req.downloadCallback);
                return;
            }
        }

    	synchronized (queue)
        {
            queue.offer(req);
        }
        enquePending();
    }
}
