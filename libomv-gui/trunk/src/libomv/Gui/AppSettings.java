/**
 * Copyright (c) 2011-2012, Frederick Martian
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
 * - Neither the name of the libomv-java project nor the names of its
 *   contributors may be used to endorse or promote products derived from
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
package libomv.Gui;

import libomv.GridClient;
import libomv.utils.Logger;
import libomv.utils.Settings;
import libomv.utils.Logger.LogLevel;
import libomv.utils.Settings.DefaultSetting;

public class AppSettings extends Settings
{
	public static String disableSound = "disabledSound";

	/* Initialize Restrained Love Manager */
	public static final String ENABLE_RLV_MANAGER = "rlv_enabled";
	
	DefaultSetting[] defaults = {
			new DefaultSetting(disableSound, true),
			new DefaultSetting(ENABLE_RLV_MANAGER, false),
	};
	
	public AppSettings(GridClient client)
	{
		super("_libomv/settings.app");
		setDefaults(defaults);
		
		try
		{
			load();
		}
		catch (Exception ex)
		{
			Logger.Log("Failed to load settings", LogLevel.Error, ex);
		}
	}
}
