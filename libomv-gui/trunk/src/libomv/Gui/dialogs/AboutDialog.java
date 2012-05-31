/**
 * Copyright (c) 2009-2012, Frederick Martian
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
package libomv.Gui.dialogs;

import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JTextArea;

import libomv.Gui.Resources;
import libomv.Gui.components.ImagePanel;
import javax.swing.GroupLayout;
import javax.swing.GroupLayout.Alignment;
import javax.swing.LayoutStyle.ComponentPlacement;
import java.awt.Font;
import javax.swing.JScrollPane;
import javax.swing.JButton;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;

public class AboutDialog extends JDialog
{

	private static final long serialVersionUID = 1L;
	private JPanel jContentPane = null;

	/**
	 * 
	 */
	public AboutDialog(JFrame owner)
	{
		super(owner);
		setTitle("About Libomv-Java");
		setSize(524, 480);

		// Do not allow resizing
		setResizable(false);

		setContentPane(getJContentPane());
	}

	/**
	 * This method initializes jContentPane
	 * 
	 * @return javax.swing.JPanel
	 */
	private JPanel getJContentPane()
	{
		if (jContentPane == null)
		{
			jContentPane = new JPanel();
						
			ImagePanel jPanImage = new ImagePanel(Resources.IMAGE_LOGO);

			JTextArea jTxtVersion;
			jTxtVersion = new JTextArea();
			jTxtVersion.setText("Version 0.7.1");
			jTxtVersion.setFont(new Font("Tahoma", Font.BOLD, 16));
			jTxtVersion.setOpaque(false);
			jTxtVersion.setEnabled(false);
			jTxtVersion.setEditable(false);
			jTxtVersion.setColumns(10);

			JTextArea jTxtCopyright = new JTextArea();
			jTxtCopyright.setFont(new Font("Tahoma", Font.PLAIN, 11));
			jTxtCopyright.setOpaque(false);
			jTxtCopyright.setEnabled(false);
			jTxtCopyright.setEditable(false);
			jTxtCopyright.setText("Written by Frederick Martian\nwith contributions from\n");
			
			JTextArea jTxtLicenses = new JTextArea();
			jTxtLicenses.setFont(new Font("Tahoma", Font.PLAIN, 11));
			jTxtLicenses.setOpaque(false);
			jTxtLicenses.setEnabled(false);
			jTxtLicenses.setEditable(false);
			jTxtLicenses.setText(Resources.loadTextFile("Licenses.txt"));
			JScrollPane jScrLicense = new JScrollPane();
			jScrLicense.setViewportView(jTxtLicenses);
			
			JTextArea jTxtCredits = new JTextArea();
			jTxtCredits.setFont(new Font("Tahoma", Font.PLAIN, 11));
			jTxtCredits.setOpaque(false);
			jTxtCredits.setEnabled(false);
			jTxtCredits.setEditable(false);
			jTxtCredits.setText(Resources.loadTextFile("Credits.txt"));
			JScrollPane jScrCredits = new JScrollPane();
			jScrCredits.setViewportView(jTxtCredits);
			
			JButton jBtnClose = new JButton("Close");
			jBtnClose.addActionListener(new ActionListener()
			{
				@Override
				public void actionPerformed(ActionEvent e)
				{
					dispose();
				}
			});
			
			GroupLayout gl_jContentPane = new GroupLayout(jContentPane);
			gl_jContentPane.setHorizontalGroup(
				gl_jContentPane.createSequentialGroup()
						.addContainerGap()
						.addGroup(gl_jContentPane.createParallelGroup(Alignment.TRAILING)
							.addComponent(jBtnClose, GroupLayout.PREFERRED_SIZE, 87, GroupLayout.PREFERRED_SIZE)
							.addGroup(Alignment.LEADING, gl_jContentPane.createSequentialGroup()
								.addComponent(jPanImage, GroupLayout.PREFERRED_SIZE, 126, GroupLayout.PREFERRED_SIZE)
								.addPreferredGap(ComponentPlacement.RELATED)
								.addGroup(gl_jContentPane.createParallelGroup(Alignment.LEADING)
									.addComponent(jTxtCopyright, GroupLayout.DEFAULT_SIZE, 366, Short.MAX_VALUE)
									.addComponent(jTxtVersion, GroupLayout.DEFAULT_SIZE, 366, Short.MAX_VALUE)))
							.addComponent(jScrCredits, GroupLayout.DEFAULT_SIZE, 498, Short.MAX_VALUE)
							.addComponent(jScrLicense, GroupLayout.DEFAULT_SIZE, 498, Short.MAX_VALUE))
						.addContainerGap()
			);
			gl_jContentPane.setVerticalGroup(
				gl_jContentPane.createParallelGroup(Alignment.LEADING)
					.addGroup(gl_jContentPane.createSequentialGroup()
						.addContainerGap()
						.addGroup(gl_jContentPane.createParallelGroup(Alignment.LEADING, false)
							.addComponent(jPanImage, GroupLayout.PREFERRED_SIZE, 107, GroupLayout.PREFERRED_SIZE)
							.addGroup(gl_jContentPane.createSequentialGroup()
								.addComponent(jTxtVersion, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
								.addPreferredGap(ComponentPlacement.RELATED, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
								.addComponent(jTxtCopyright, GroupLayout.PREFERRED_SIZE, 77, GroupLayout.PREFERRED_SIZE)))
						.addPreferredGap(ComponentPlacement.RELATED)
						.addComponent(jScrLicense, GroupLayout.PREFERRED_SIZE, 137, GroupLayout.PREFERRED_SIZE)
						.addPreferredGap(ComponentPlacement.RELATED)
						.addComponent(jScrCredits, GroupLayout.PREFERRED_SIZE, 137, GroupLayout.PREFERRED_SIZE)
						.addPreferredGap(ComponentPlacement.RELATED)
						.addComponent(jBtnClose, GroupLayout.DEFAULT_SIZE, 31, Short.MAX_VALUE)
						.addContainerGap())
			);
			
			jContentPane.setLayout(gl_jContentPane);
		}
		return jContentPane;
	}
}