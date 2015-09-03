/**
 * Copyright (c) 2006-2014, openmetaverse.org
 * Copyright (c) 2009-2014, Frederick Martian
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
package libomv.rendering;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.nio.FloatBuffer;
import java.nio.ShortBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeMap;

import org.apache.commons.io.input.SwappedDataInputStream;

import libomv.GridClient;
import libomv.types.Vector2;
import libomv.types.Vector3;
import libomv.utils.Helpers;

public class LindenMesh extends ReferenceMesh
{
    // #region Mesh Structs

    public class Vertex
    {
        public Vector3 Position;
        public Vector3 Normal;
        public Vector3 BiNormal;
        public Vector2 TexCoord;
        public Vector2 DetailTexCoord;
        public float Weight;

        @Override
		public String toString()
        {
            return String.format("Position: %s Norm: %s BiNorm: %s TexCoord: %s DetailTexCoord: %s", Position, Normal, BiNormal, TexCoord, DetailTexCoord, Weight);
        }
    }

    public class MorphVertex
    {
        public int VertexIndex;
        public Vector3 Position;
        public Vector3 Normal;
        public Vector3 BiNormal;
        public Vector2 TexCoord;

        @Override
		public String toString()
        {
            return String.format("Index: %d Position: %s Norm: %s BiNorm: %s TexCoord: %s", VertexIndex, Position, Normal, BiNormal, TexCoord);
        }
    }

    public class Morph
    {
        public String Name;
        public int NumVertices;
        public MorphVertex[] Vertices;

        @Override
		public String toString()
        {
            return Name;
        }
    }

    public class VertexRemap
    {
        public int RemapSource;
        public int RemapDestination;

        @Override
		public String toString()
        {
            return String.format("%d -> %d", RemapSource, RemapDestination);
        }
    }
    // #endregion Mesh Structs

 
    protected String _name;
    public String getName() { return _name; }

    private LindenSkeleton _skeleton;
	public LindenSkeleton getSkeleton() { return _skeleton; }

    protected short _numVertices;


    
    protected FloatBuffer Vertices;
    public Vector3 getVerticeCoord(int index)
    {
    	if (index >= _numVertices)
    		return null;
    	index *= 3;
    	return new Vector3(Vertices.get(index), Vertices.get(index + 1), Vertices.get(index + 2));
    }
    
    protected FloatBuffer Normals;
    protected FloatBuffer BiNormals;
    protected FloatBuffer TexCoords;
    protected FloatBuffer DetailTexCoords;
    protected FloatBuffer Weights;

    public Vertex getVertex(int index)
    {
    	if (index >= _numVertices)
    		return null;

    	Vertex vertex = new Vertex();
    	int offset = index * 3;
    	vertex.Position = new Vector3(Vertices.get(offset), Vertices.get(offset + 1), Vertices.get(offset + 2));
    	vertex.Normal = new Vector3(Normals.get(offset), Normals.get(offset + 1), Normals.get(offset + 2));
    	vertex.BiNormal = new Vector3(BiNormals.get(offset), BiNormals.get(offset + 1), BiNormals.get(offset + 2));
    	offset = index * 2;
    	vertex.TexCoord = new Vector2(TexCoords.get(offset), TexCoords.get(offset + 1));
    	vertex.DetailTexCoord = new Vector2(DetailTexCoords.get(offset), DetailTexCoords.get(offset + 1));

    	vertex.Weight = Weights.get(index);
    	return vertex;
    }
 
    protected short _numSkinJoints;
    public short getNumSkinJoints() { return _numSkinJoints; }
    protected String[] _skinJoints;
    public String[] getSkinJoints() { return _skinJoints; }
    protected Morph[] _morphs;
    public Morph[] getMorphs() { return _morphs; }
    protected int _numRemaps;
    public int getNumRemaps() { return _numRemaps; }
    protected VertexRemap[] _vertexRemaps;
    public VertexRemap[] getVertexRemaps() { return _vertexRemaps; }
    protected TreeMap<Integer, ReferenceMesh> _meshes;
    public TreeMap<Integer, ReferenceMesh> getMeshes() { return _meshes; }

    public LindenMesh(GridClient client, String name) throws IOException, URISyntaxException
    {
        this(client, name, null);
    }

    public LindenMesh(GridClient client, String name, LindenSkeleton skeleton) throws IOException, URISyntaxException
    {
        _name = name;
        _skeleton = skeleton;
        _meshes = new TreeMap<Integer, ReferenceMesh>();
        
        if (_skeleton == null)
        {
        	_skeleton = LindenSkeleton.load(client);
        }
    }
    
    /**
     * Load the mesh from a file
     *
     * @param "filename" The filename and path of the file containing the mesh data
     */
    public void load(String filename) throws IOException
    {
    	load(new FileInputStream(filename));
    }
    
    public void load(InputStream stream) throws IOException
    {
    	SwappedDataInputStream fis = new SwappedDataInputStream(stream);
    	super.load(fis);

        _numVertices = fis.readShort();
        
        // Populate the vertex array
        Vertices = FloatBuffer.allocate(3 * _numVertices);
        for (int i = 0; i < _numVertices; i++)
        {
            Vertices.put(fis.readFloat());
            Vertices.put(fis.readFloat());
            Vertices.put(fis.readFloat());
        }
        
        Normals = FloatBuffer.allocate(3 * _numVertices);
        for (int i = 0; i < _numVertices; i++)
        {
        	Normals.put(fis.readFloat());
        	Normals.put(fis.readFloat());
        	Normals.put(fis.readFloat());
        }
        
        BiNormals = FloatBuffer.allocate(3 * _numVertices);
        for (int i = 0; i < _numVertices; i++)
        {
        	BiNormals.put(fis.readFloat());
        	BiNormals.put(fis.readFloat());
        	BiNormals.put(fis.readFloat());
        }
        
        TexCoords = FloatBuffer.allocate(2 * _numVertices);
        for (int i = 0; i < _numVertices; i++)
        {
            TexCoords.put(fis.readFloat());
            TexCoords.put(fis.readFloat());
        }
        
        if (_hasDetailTexCoords)
        {
            DetailTexCoords = FloatBuffer.allocate(2 * _numVertices);
            for (int i = 0; i < _numVertices; i++)
            {
            	DetailTexCoords.put(fis.readFloat());
            	DetailTexCoords.put(fis.readFloat());
            }
        }

        if (_hasWeights)
        {
        	Weights = FloatBuffer.allocate(_numVertices);
            for (int i = 0; i < _numVertices; i++)
            {
                Weights.put(fis.readFloat());
            }
        }

        _numFaces = fis.readShort();

        _faces = ShortBuffer.allocate(3 * _numFaces);
        for (int i = 0; i < _numFaces; i++)
        {
        	_faces.put(fis.readShort());
        	_faces.put(fis.readShort());
        	_faces.put(fis.readShort());
        }
    
        if (_hasWeights)
        {
            _numSkinJoints = fis.readShort();
            _skinJoints = new String[_numSkinJoints];

            for (int i = 0; i < _numSkinJoints; i++)
            {
                _skinJoints[i] = Helpers.readString(fis, 64);
            }
        }
        else
        {
            _numSkinJoints = 0;
            _skinJoints = new String[0];
        }

        // Grab morphs
        List<Morph> morphs = new ArrayList<Morph>();
        String morphName = Helpers.readString(fis, 64);

        while (!morphName.equals(MORPH_FOOTER))
        {
            Morph morph = new Morph();
            morph.Name = morphName;
            morph.NumVertices = fis.readInt();
            morph.Vertices = new MorphVertex[morph.NumVertices];

            for (int i = 0; i < morph.NumVertices; i++)
            {
                morph.Vertices[i].VertexIndex = fis.readInt();
                morph.Vertices[i].Position = new Vector3(fis);
                morph.Vertices[i].Normal = new Vector3(fis);
                morph.Vertices[i].BiNormal = new Vector3(fis);
                morph.Vertices[i].TexCoord = new Vector2(fis);
            }

            morphs.add(morph);

            // Grab the next name
            morphName = Helpers.readString(fis, 64);
        }

        _morphs = morphs.toArray(_morphs);

        // Check if there are remaps or if we're at the end of the file
        try
        {
            _numRemaps = fis.readInt();
            _vertexRemaps = new VertexRemap[_numRemaps];

            for (int i = 0; i < _numRemaps; i++)
            {
                _vertexRemaps[i].RemapSource = fis.readInt();
                _vertexRemaps[i].RemapDestination = fis.readInt();
            }
        }
        catch (IOException ex)
        {
            _numRemaps = 0;
            _vertexRemaps = new VertexRemap[0];
        }
        
        // uncompress the skin weights
        if (_skeleton != null)
        {
            // some meshes aren't weighted, which doesn't make much sense. We check for
        	// left and right eyeballs, and assign them a 100% to their respective bone.
            List<String> expandedJointList = _skeleton.buildExpandedJointList(_skinJoints);
            if (expandedJointList.size() == 0)
            {
                if (_name.equals("eyeBallLeftMesh"))
                {
                    expandedJointList.add("mEyeLeft");
                    expandedJointList.add("mSkull");
                }
                else if (_name.equals("eyeBallRightMesh"))
                {
                    expandedJointList.add("mEyeRight");
                    expandedJointList.add("mSkull");
                }
            }

            if (expandedJointList.size() > 0)
                expandCompressedSkinWeights(expandedJointList);
        }
    }
    
    //region Skin weight

    /**
     * Layout of one skinweight element
     */
    public class SkinWeightElement
    {
        public String bone1;        // Name of the first bone that influences the vertex
        public String bone2;        // Name of the second bone that influences the vertex
        public float weight1;       // Weight with whitch the first bone influences the vertex
        public float weight2;       // Weight with whitch the second bone influences the vertex
    }

    // List of skinweights, in the same order as the mesh vertices
    public List<SkinWeightElement> SkinWeights = new ArrayList<SkinWeightElement>();

    /**
     * Decompress the skinweights
     *
     * @param expandedJointList The expanded joint list, used to index which bones should influece the vertex
     */
    private void expandCompressedSkinWeights(List<String> expandedJointList)
    {
        for (int i = 0; i < _numVertices; i++)
        {
            int boneIndex = (int)Math.floor(Weights.get(i)); // Whole number part is the index
            float boneWeight = (Weights.get(i) - boneIndex); // fractional part is the weight
            SkinWeightElement elm = new SkinWeightElement();

            if (boneIndex == 0)         // Special case for dealing with eye meshes, which doesn't have any weights
            {
                elm.bone1 = expandedJointList.get(0);
                elm.weight1 = 1;
                elm.bone2 = expandedJointList.get(1);
                elm.weight2 = 0;
            }
            else if (boneIndex < expandedJointList.size())
            {
                elm.bone1 = expandedJointList.get(boneIndex - 1);
                elm.weight1 = 1 - boneWeight;
                elm.bone2 = expandedJointList.get(boneIndex);
                elm.weight2 = boneWeight;
            }
            else
            {   // this should add a weight where the "invalid" Joint has a weight of zero
                elm.bone1 = expandedJointList.get(boneIndex - 1);
                elm.weight1 = 1 - boneWeight;
                elm.bone2 = "mPelvis";
                elm.weight2 = boneWeight;
            }
            SkinWeights.add(elm);
       }
    }
    //#endregion Skin weight

    public ReferenceMesh LoadLodMesh(GridClient client, int level, String filename) throws IOException, URISyntaxException
    {
    	ReferenceMesh refMesh;
        if (filename.equals("avatar_eye_1.llm"))
        {
        	refMesh = new ReferenceMesh();
        }
        else
        {
        	refMesh = new LindenMesh(client, "");
        }
        refMesh.load(filename);
        _meshes.put(level, refMesh);
        return refMesh;
    }
}