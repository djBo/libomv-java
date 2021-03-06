/**
 * Copyright (c) 2006-2014, openmetaverse.org
 * Copyright (c) 2009-2017, Frederick Martian
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
package libomv.examples.TestClient.Commands.Movement;

import libomv.examples.TestClient.Command;
import libomv.examples.TestClient.TestClient;
import libomv.primitives.Primitive;
import libomv.types.UUID;
import libomv.types.Vector3;

public class SitCommand extends Command
{
    public SitCommand(TestClient testClient)
	{
		Name = "sit";
		Description = "Attempt to sit on the closest prim. Usage: sit";
        Category = CommandCategory.Movement;
	}
		
    public String execute(String[] args, UUID fromAgentID) throws Exception
	{
        Primitive closest = null;
	    double closestDistance = Double.MAX_VALUE;

        for (Primitive prim : Client.Network.getCurrentSim().getObjectsPrimitives().values())
        {
            float distance = Vector3.distance(Client.Self.getAgentPosition(), prim.Position);

            if (closest == null || distance < closestDistance)
            {
                closest = prim;
                closestDistance = distance;
            }
        }

        if (closest != null)
        {
            Client.Self.RequestSit(closest.ID, Vector3.Zero);
            Client.Self.Sit();

            return "Sat on " + closest.ID + " (" + closest.LocalID + "). Distance: " + closestDistance;
        }
        return "Couldn't find a nearby prim to sit on";
	}
}
