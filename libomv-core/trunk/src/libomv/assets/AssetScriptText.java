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

import java.io.UnsupportedEncodingException;

import libomv.types.UUID;
import libomv.utils.Helpers;

// Represents an LSL Text object containing a string of UTF encoded characters
public class AssetScriptText extends AssetItem
{
	// Override the base classes AssetType
	@Override
	public AssetType getAssetType()
	{
		return AssetType.LSLText;
	}

	// A string of characters representing the script contents
	private String Source;

	public String getSource()
	{
		return Source;
	}
	
	public void setParent(String source)
	{
		invalidateAssetData();
		Source = source;
	}

	/**
	 * Initializes a new AssetScriptText object with parameters
	 * 
	 * @param assetID
	 *            A unique <see cref="UUID"/> specific to this asset
	 * @param assetData
	 *            A byte array containing the raw asset data
	 */
	public AssetScriptText(UUID assetID, byte[] assetData)
	{
		super(assetID, assetData);
	}

	/**
	 * Initializes a new AssetScriptText object with parameters
	 * 
	 * @param source
	 *            A string containing the scripts contents
	 */
	public AssetScriptText(String source)
	{
		super(null, null);
		Source = source;
	}

	/**
	 * Encode a string containing the scripts contents into byte encoded
	 * AssetData
	 * 
	 */
	@Override
	protected void encode()
	{
		AssetData = Helpers.StringToBytes(Source);
	}

	/**
	 * Decode a byte array containing the scripts contents into a string
	 * 
	 * @return true if decoding is successful
	 */
	@Override
	protected boolean decode()
	{
		try
		{
			Source = Helpers.BytesToString(AssetData);
			return true;
		}
		catch (UnsupportedEncodingException e)
		{
		}
		return false;
	}
}
