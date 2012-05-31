/**
 * Copyright (c) 2008, openmetaverse.org
 * Copyright (c) 2009-2011, Frederick Martian
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
package libomv.rendering;

import java.util.List;

import libomv.imaging.ManagedImage;
import libomv.primitives.Primitive;
import libomv.primitives.TextureEntry;
import libomv.types.Vector3;

/** Abstract base for rendering plugins */
public interface IRendering
{
    public class RendererNameAttribute
    {
        private String _name;

        public RendererNameAttribute(String name)
         {
        	super();
            _name = name;
        }

        @Override
		public String toString()
        {
            return _name;
        }
    }
    
    /**
     * Generates a basic mesh structure from a primitive
     * 
     * @param prim Primitive to generate the mesh from
     * @param lod Level of detail to generate the mesh at
     * @return The generated mesh
     */
    public SimpleMesh GenerateSimpleMesh(Primitive prim, Mesh.DetailLevel lod);

    /**
     * Generates a basic mesh structure from a sculpted primitive and texture
     * 
     * @param prim Sculpted primitive to generate the mesh from
     * @param sculptTexture Sculpt texture
     * @param lod Level of detail to generate the mesh at
     * @return The generated mesh
     */
    public SimpleMesh GenerateSimpleSculptMesh(Primitive prim, ManagedImage sculptTexture, Mesh.DetailLevel lod);

    /** <summary>
     * Generates a series of faces, each face containing a mesh and metadata
     * 
     * @param prim Primitive to generate the mesh from
     * @param lod Level of detail to generate the mesh at
     * @return The generated mesh
     */
    public FacetedMesh GenerateFacetedMesh(Primitive prim, Mesh.DetailLevel lod);

    /**
     * Generates a series of faces for a sculpted prim, each face 
     * containing a mesh and metadata
     *
     * @param prim Sculpted primitive to generate the mesh from</param>
     * @param sculptTexture Sculpt texture</param>
     * @param lod Level of detail to generate the mesh at</param>
     * @returns The generated mesh</returns>
     */
    public FacetedMesh GenerateFacetedSculptMesh(Primitive prim, ManagedImage sculptTexture, Mesh.DetailLevel lod);

    /**
     * Apply texture coordinate modifications from a
     * <seealso cref="TextureEntryFace"/> to a list of vertices
     *
     * @param vertices Vertex list to modify texture coordinates for</param>
     * @param center">Center-point of the face</param>
     * @param teFace">Face texture parameters</param>
     * @param primScale">Scale of the prim
     */
    public void TransformTexCoords (List<Mesh.Vertex> vertices, Vector3 center, TextureEntry.TextureEntryFace teFace, Vector3 primScale);
}