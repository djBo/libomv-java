/**
 * Copyright (c) 2010-2011, Frederick Martian
 * All rights reserved.
 *
 * - Redistribution and use in source and binary forms, with or without
 *   modification, are permitted provided that the following conditions are met:
 *
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
package libomv.Gui.components;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JFrame;
import javax.swing.JPanel;

import libomv.GridClient;

public class OnlinePanel extends JPanel
{
	private static final long serialVersionUID = 1L;
	
	private GridClient _Client;
	private JFrame _Parent;
	private ActionListener _Action;
	
	private JPanel jSceneViewer;

	public OnlinePanel(GridClient client, JFrame parent, ActionListener action)
	{
		_Client = client;
		_Parent = parent;
		_Action = action;

		parent.add(getSceneViewer(), BorderLayout.CENTER);

		initializePanel();	
	}
	
	private JPanel getSceneViewer()
	{
		if (jSceneViewer == null)
		{
			jSceneViewer = new JPanel();
		}
		return jSceneViewer;
	}

	private void doReturn(boolean logout)
	{
		_Parent.remove(getSceneViewer());
		_Parent.remove(this);
		_Action.actionPerformed(new ActionEvent(this, ActionEvent.ACTION_PERFORMED, logout ? "logout" : "failed"));
	}

	private void initializePanel()
	{
	}
}
