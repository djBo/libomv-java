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
package libomv.assets;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

import libomv.imaging.ManagedImage;
import libomv.imaging.J2KImage;
import libomv.imaging.J2KImage.J2KLayerInfo;
import libomv.types.UUID;
import libomv.utils.Logger;
import libomv.utils.Logger.LogLevel;

public class AssetTexture extends AssetItem
{
	// Override the base classes AssetType
	@Override
	public AssetType getAssetType()
	{
		return AssetType.Texture;
	}

	// A {@link Image} object containing image data
	public ManagedImage Image;

	public J2KLayerInfo[] LayerInfo;

	public int Components;

	// Initializes a new instance of an AssetTexture object
	public AssetTexture()
	{
	}

	/**
	 * Initializes a new instance of an AssetTexture object
	 * 
	 * @param assetID
	 *            A unique <see cref="UUID"/> specific to this asset
	 * @param assetData
	 *            A byte array containing the raw asset data
	 */
	public AssetTexture(UUID assetID, byte[] assetData)
	{
		super(assetID, assetData);
	}

	/**
	 * Initializes a new instance of an AssetTexture object
	 * 
	 * @param image
	 *            A {@link ManagedImage} object containing texture data
	 */
	public AssetTexture(ManagedImage image)
	{
		Image = image;
		Components = 0;
		if ((Image.Channels & ManagedImage.ImageChannels.Color) != 0)
			Components += 3;
		else if ((Image.Channels & ManagedImage.ImageChannels.Gray) != 0)
			++Components;
		if ((Image.Channels & ManagedImage.ImageChannels.Bump) != 0)
			++Components;
		if ((Image.Channels & ManagedImage.ImageChannels.Alpha) != 0)
			++Components;
	}

	/**
	 * Populates the {@link AssetData} byte array with a JPEG2000 encoded image
	 * created from the data in {@link Image}
	 */
	@Override
	public void encode()
	{
		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		try
		{
			J2KImage.encode(bos, Image, false);
			AssetData = bos.toByteArray();
		}
		catch (Exception ex)
		{
			Logger.Log("Failed to encode JPEG2000 image", LogLevel.Error, ex);
		}
	}

	/**
	 * Decodes the JPEG2000 data in <code>AssetData</code> to the
	 * {@link ManagedImage} object {@link Image}
	 * 
	 * @return True if the decoding was successful, otherwise false
	 * @throws IOException 
	 */
	@Override
	public boolean decode()
	{
		Components = 0;

		InputStream is = new ByteArrayInputStream(AssetData);
		try
		{
			Image = new J2KImage(is);
		}
		catch (Exception ex)
		{
			Logger.Log("Error decoding asset texture data", LogLevel.Error, ex);
		}
		finally
		{
			try
			{
				is.close();
			}
			catch (IOException e)
			{
			}
		}

		if (Image != null)
		{
			if ((Image.Channels & ManagedImage.ImageChannels.Color) != 0)
				Components += 3;
			else if ((Image.Channels & ManagedImage.ImageChannels.Gray) != 0)
				++Components;
			if ((Image.Channels & ManagedImage.ImageChannels.Bump) != 0)
				++Components;
			if ((Image.Channels & ManagedImage.ImageChannels.Alpha) != 0)
				++Components;

			return true;
		}
		return false;
	}

	/**
	 * Decodes the begin and end byte positions for each quality layer in the image
	 * 
	 * @return
	 */
	public boolean decodeLayerBoundaries()
	{
		LayerInfo = J2KImage.decodeLayerBoundaries(AssetData);
		Components = LayerInfo.length;
		return (Components > 0);
	}

}
