/**
 * Copyright (c) 2009-2011, Frederick Martian
 * All rights reserved.
 *
 * - Redistribution and use in source and binary forms, with or without 
 *   modification, are permitted provided that the following conditions are met:
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
package libomv;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.text.ParseException;
import java.util.HashMap;
import java.util.Set;
import java.util.prefs.Preferences;

import org.apache.http.HeaderElement;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.HttpResponseException;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.protocol.HTTP;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

import libomv.StructuredData.OSD;
import libomv.StructuredData.OSDArray;
import libomv.StructuredData.OSDMap;
import libomv.StructuredData.OSD.OSDFormat;
import libomv.StructuredData.OSD.OSDType;
import libomv.assets.AssetManager;
import libomv.capabilities.CapsMessage;
import libomv.inventory.InventoryManager;
import libomv.utils.Helpers;

 
/* Main class to expose the functionality of a particular grid to clients. All
 * of the classes needed for sending and receiving data are accessible through
 * this class. */ 
public class GridClient
{
	// #region gridlist definitions
	public class GridInfo
	{
		public String gridnick;    // gridnick
		public String gridname;    // gridname
		public String platform;    // platform
		public String loginuri;    // login, loginuri
		public String loginpage;   // welcome, loginpage
		public String helperuri;   // economy, helperuri
		public String website;     // about, website
		public String support;     // help, support
		public String register;    // register, account
		public String password;    // password
		public int version;
		
		@Override
		public String toString()
		{
			return String.format("Nick: %s, Name: %s, Platform: %s, Ver: %d\n", gridnick, gridname, platform, version);
		}
	}
	
	private static final String listUri = "http://libomv-java.sourceforge.net/grids/default_grids.xml";
	private static HashMap<String, GridInfo> gridlist = new HashMap<String, GridInfo>();
	private static int listversion = 0;

	private String defaultGrid = null;

	private static final String GRIDINFO = "gridinfo";
	private static final String GRID_LIST = "gridlist";
	private static final String DEFAULT_GRID = "defaultGrid";
	private static final String DEFAULT_GRID_NAME1 = "osgrid";
	private static final String DEFAULT_GRID_NAME2 = "secondlife";
	private static final String GRID_INFO_PROTOCOL = "get_grid_info";
	private static final String DEFAULT_GRIDS_VERSION = "default_grids_version";
	private static final String DEFAULT_GRIDS_LIST = "/res/default_grids.xml";
	// #endregion
	
	// Networking Subsystem
	public NetworkManager Network;
	// Login Subsystem of Network handler
    public LoginManager Login;
    // Caps Messages 
    public CapsMessage Messages;
    // AgentThrottle
    public AgentThrottle Throttle;
    /* Settings class including constant values and changeable
       parameters for everything */
    public Settings Settings;
	// 'Client's Avatar' Subsystem
	public AgentManager Self;
	// Other Avatars Subsystem
	public AvatarManager Avatars;
	// Friend Avatars Subsystem
	public FriendsManager Friends;
	// Grid (aka simulator group) Subsystem
	public GridManager Grid;
    /* Asset subsystem */
    public AssetManager Assets;
    /* Inventory subsystem */
    public InventoryManager Inventory;
	// Parcel (subdivided simulator lots) Subsystem
//	public ParcelManager Parcels;
	// Object Subsystem
//	public ObjectManager Objects;
	// Group Subsystem
//	public GroupManager Groups;
    /* Directory searches including classifieds, people, land sales, etc */
//    public DirectoryManager Directory;
    /* Appearance subsystem */
//  public AppearanceManager Appearance;
    /* Handles land, wind, and cloud heightmaps */
//    public TerrainManager Terrain;
    /* Handles sound-related networking */
//    public SoundManager Sound;

	// Debug flag
	public boolean Debug;

	// 
	// Constructor.
	// 
	public GridClient() throws Exception
	{
		this(new Settings());
	}
	
	public GridClient(Settings settings) throws Exception
	{
		initializeGridList();

		Login = new LoginManager(this);
		Network = new NetworkManager(this);
		Settings = settings;
		Settings.Startup(this);
		Self = new AgentManager(this);
		Friends = new FriendsManager(this);
		Messages = new CapsMessage();

		if (Settings.SEND_AGENT_THROTTLE)
		    Throttle = new AgentThrottle(this);

		if (Settings.ENABLE_ASSET_MANAGER)
			Assets = new AssetManager(this);

		if (Settings.ENABLE_INVENTORY_MANAGER)
			Inventory = new InventoryManager(this);
 		Grid = new GridManager(this);

 //		Parcels = new ParcelManager(this);
//		Avatars = new AvatarManager(this);
//		Objects = new ObjectManager(this);
//		Groups = new GroupManager(this);
//		Directory = new DirectoryManager(this);
		Debug = true;
	}

	public Set<String> getNames()
	{
		return gridlist.keySet();
	}
	
	public GridInfo getGrid(String grid)
	{
		return gridlist.get(grid);
	}

	public GridInfo getDefaultGrid()
	{
		if (defaultGrid == null | defaultGrid.isEmpty())
			setDefaultGrid(null);
		return gridlist.get(defaultGrid);
	}
	public void setDefaultGrid(String name)
	{
		if (name != null && gridlist.containsKey(name))
		{
			defaultGrid = name;
		}
		else if (gridlist.containsKey(DEFAULT_GRID_NAME1))
		{
			defaultGrid = DEFAULT_GRID_NAME1;
		}
		else if (gridlist.containsKey(DEFAULT_GRID_NAME2))
		{
			defaultGrid = DEFAULT_GRID_NAME2;
		}
		else if (!gridlist.isEmpty())
		{
			defaultGrid = gridlist.keySet().iterator().next();
		}
		else
		{
			defaultGrid = Helpers.EmptyString;
		}
	}

	public GridInfo addGrid(GridInfo info)
	{
		return gridlist.put(info.gridnick, info);
	}
	
	/**
	 * Retrieves the GridInfo settings from the grid user server, when the server supports
	 * the GridInfo protocol.
	 *  
	 * @param loginuri The HTTP address of the user server
	 * @return a filled in GridInfo if the call was successful, null otherwise
	 * @throws Exception 
	 */
	public GridInfo queryGridInfo(String loginuri) throws Exception
	{
		GridInfo info = null;
		HttpClient client = new DefaultHttpClient();
		HttpGet getMethod = new HttpGet(new URI(loginuri + GRID_INFO_PROTOCOL));
		try
		{
		    HttpResponse response = client.execute(getMethod);
			if (response.getStatusLine().getStatusCode() == HttpStatus.SC_OK)
			{
				HttpEntity entity = response.getEntity();
				XmlPullParser parser = XmlPullParserFactory.newInstance().newPullParser();
				parser.setInput(entity.getContent(), entity.getContentEncoding().getValue());
				parser.nextTag();
				parser.require(XmlPullParser.START_TAG, null, GRIDINFO);
				if (!parser.isEmptyElementTag())
				{
				   parser.nextTag();
			       info = parseRecord(parser);
				}
			}
		}
		finally
		{
			getMethod.abort();			
		}
		return info;
	}

	private GridInfo parseRecord(XmlPullParser parser) throws XmlPullParserException, IOException
	{
		GridInfo info = new GridInfo();
		info.version = -1;
	    parser.require(XmlPullParser.START_TAG, null, null);
		do {
		    if (parser.isEmptyElementTag())
		    {
		    	/* forward to end_tag */
		    	parser.nextTag();
		    }
		    else
		    {
		        String name = parser.getName();
		
		        if (name.equals("gridnick"))
	     	    {
				    info.gridnick = parser.nextText().trim();
		        }
		        else if (name.equals("gridname"))
		        {
			        info.gridname = parser.nextText().trim();		    	
		        }
		        else if (name.equals("platform"))
		        {
			        info.platform = parser.nextText().trim();		    	
		        }
		        else if (name.equals("login") || name.equals("loginuri"))
		        {
			        info.loginuri = parser.nextText().trim();		    	
		        }
		        else if (name.equals("welcome") || name.equals("loginpage"))
		        {
			        info.loginpage = parser.nextText().trim();		    	
		        }
		        else if (name.equals("economy") || name.equals("helperuri"))
		        {
			        info.helperuri = parser.nextText().trim();		    	
		        }
		        else if (name.equals("about") || name.equals("website"))
		        {
			        info.website = parser.nextText().trim();		    	
		        }
		        else if (name.equals("help") || name.equals("support"))
		        {
			        info.support = parser.nextText().trim();		    	
		        }
		        else if (name.equals("register") || name.equals("account"))
		        {
			        info.register = parser.nextText().trim();		    	
		        }
		        else if (name.equals("password"))
		        {
			        info.password = parser.nextText().trim();		    	
		        }
		        else
		        {
			    	/* forward to end_tag */
			    	parser.nextTag();
		        }
		    }
		} while (parser.nextTag() == XmlPullParser.START_TAG);
		return info;
	}
	
	public GridInfo deleteGrid(String grid)
	{
		GridInfo info = gridlist.remove(grid);
		if (info != null)
		{
			if (grid.equals(defaultGrid))
			{
				// sets first grid if map is not empty
				setDefaultGrid(Helpers.EmptyString);
			}	
		}
		return info;
	}

	private void initializeGridList() throws IOException, ParseException, IllegalStateException, URISyntaxException, IllegalArgumentException, IllegalAccessException
	{
		boolean modified = setList(loadSettings(), false);
		modified |= setList(loadDefaults(), true);
		modified |= setList(downloadList(), true);
		if (modified)
			saveList();
	}
		
	private boolean setList(OSD list, boolean merge) throws IllegalArgumentException, IllegalAccessException
	{
		if (list == null || list.getType() != OSDType.Array)
			return false;
		
		if (!merge)
		{
			gridlist.clear();
			listversion = 0;
		}
		
		boolean modified = false;
		int version = 0;
		OSDArray array = (OSDArray)list;
		for (int i = 0; i < array.size(); i++)
		{
			OSDMap map = (OSDMap)array.get(i);
			if (map.containsKey(DEFAULT_GRIDS_VERSION))
			{
				version = map.get(DEFAULT_GRIDS_VERSION).AsInteger();
				if (version <= listversion)
				{
					return false;
				}
			}
			else
			{
		    	GridInfo newinfo = new GridInfo();
		    	map.deserializeMembers(newinfo);
				GridInfo oldinfo = gridlist.get(newinfo.gridname);
				if (!merge || oldinfo == null || oldinfo.version < newinfo.version)
				{
					gridlist.put(newinfo.gridnick, newinfo);
					modified = true;
				}
			}
			if (modified)
				listversion = version;
		}
		return modified;
	}
	
	private void saveList() throws IllegalArgumentException, IllegalAccessException, IOException
	{
		OSDArray array = new OSDArray();
		OSDMap map = new OSDMap();
		map.put(DEFAULT_GRIDS_VERSION, OSD.FromInteger(listversion));
		array.add(map);
		
	    for (GridInfo info : gridlist.values())
	    {
		    array.add(OSD.serializeMembers(info));
	    }
	    Preferences prefs = Preferences.userNodeForPackage(this.getClass());
		prefs.put(GRID_LIST, array.serializeToString(OSDFormat.Xml));
		prefs.put(DEFAULT_GRID, defaultGrid);
	}
	
	private OSD loadDefaults() throws IOException, ParseException
	{
		OSD osd = null;
		System.out.println(System.getProperty("java.class.path"));
		InputStream stream = getClass().getResourceAsStream(DEFAULT_GRIDS_LIST);
		if (stream != null) 
		{
			try
			{
				osd = OSD.parse(stream, Helpers.UTF8_ENCODING);
			}
			finally
			{
				stream.close();
			}
		}
		return osd;
	}

	private OSD loadSettings() throws IOException, ParseException
	{
		Preferences prefs = Preferences.userNodeForPackage(this.getClass());
		defaultGrid = prefs.get(DEFAULT_GRID, Helpers.EmptyString);
		return OSD.parse(prefs.get(GRID_LIST, Helpers.EmptyString));
	}

	private OSD downloadList() throws IOException, IllegalStateException, ParseException, URISyntaxException
	{
		OSD osd = null;
		HttpClient client = new DefaultHttpClient();
		HttpGet getMethod = new HttpGet(new URI(listUri));
		try
		{
		    HttpResponse response = client.execute(getMethod);
            if (response.getStatusLine().getStatusCode() != HttpStatus.SC_OK)
            {
            	throw new HttpResponseException(response.getStatusLine().getStatusCode(), response.getStatusLine().getReasonPhrase());
            }

            HttpEntity entity = response.getEntity();
            if (entity != null)
            {
				InputStream stream = entity.getContent();
                String charset = null;
                if (entity.getContentType() != null) { 
                    HeaderElement values[] = entity.getContentType().getElements();
                    if (values.length  > 0) {
                        NameValuePair param = values[0].getParameterByName("charset");
                        if (param != null) {
                            charset = param.getValue();
                        }
                    }
                }
                if (charset == null)
                {
                    charset = HTTP.DEFAULT_CONTENT_CHARSET;
                }
				osd = OSD.parse(stream, charset);
            }
		}
		finally
		{
			getMethod.abort();
		}
		return osd;
	}
	
	public String dumpGridlist()
	{
		return String.format("Version: %d, Default: %s\n%s", listversion, defaultGrid, gridlist);
	}

	// <returns>Client Avatar's Full Name</returns>
	@Override
	public String toString()
	{
		return Self.getName();
	}

	// A simple sleep function that will allow pending threads to run
	public void Tick(long millis) throws Exception
	{
		Thread.sleep(millis);
	}

	// A simple sleep function that will allow pending threads to run
	public void Tick() throws Exception
	{
		Thread.sleep(0);
	}
}
