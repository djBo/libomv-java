﻿/**
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

import libomv.types.UUID;

// Represents an AssetScriptBinary object containing the LSO compiled bytecode of an LSL script
public class AssetScriptBinary extends AssetItem
{
	// Override the base classes AssetType
	@Override
	public AssetType getAssetType()
	{
		return AssetType.LSLBytecode;
	}

	// Initializes a new instance of an AssetScriptBinary object
	public AssetScriptBinary()
	{
	}

	/**
	 * Initializes a new instance of an AssetScriptBinary object with parameters
	 * 
	 * @param assetID
	 *            A unique <see cref="UUID"/> specific to this asset
	 * @param assetData
	 *            A byte array containing the raw asset data
	 */
	public AssetScriptBinary(UUID assetID, byte[] assetData)
	{
		super(assetID, assetData);
		AssetData = assetData;
	}

	/**
	 * TODO: Encodes a scripts contents into a LSO Bytecode file
	 */
	@Override
	public void Encode()
	{
	}

	/**
	 * TODO: Decode LSO Bytecode into a string
	 * 
	 * @return true
	 */
	@Override
	public boolean Decode()
	{
		return true;
	}
}
