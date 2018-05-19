package libomv.model.estate;

import java.util.ArrayList;

import libomv.types.UUID;
import libomv.utils.CallbackArgs;

/**
 * Returned, along with other info, upon a successful .RequestInfo()
 */
public class EstateGroupsReplyCallbackArgs implements CallbackArgs {
	private final int m_estateID;
	private final int m_count;
	private final ArrayList<UUID> m_allowedGroups;

	// The identifier of the estate
	public int getEstateID() {
		return m_estateID;
	}

	// The number of returned items
	public int getCount() {
		return m_count;
	}

	// List of UUIDs of Allowed Groups
	public ArrayList<UUID> getAllowedGroups() {
		return m_allowedGroups;
	}

	/**
	 * Construct a new instance of the EstateGroupsReplyEventArgs class
	 *
	 * @param estateID
	 *            The estate's identifier on the grid
	 * @param count
	 *            The number of returned groups in LandStatReply
	 * @param allowedGroups
	 *            Allowed Groups UUIDs
	 */
	public EstateGroupsReplyCallbackArgs(int estateID, int count, ArrayList<UUID> allowedGroups) {
		this.m_estateID = estateID;
		this.m_count = count;
		this.m_allowedGroups = allowedGroups;
	}
}