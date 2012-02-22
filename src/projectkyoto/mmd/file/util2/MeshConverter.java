/*
 * Copyright (c) 2011 Kazuhiko Kobayashi
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 *
 * * Redistributions of source code must retain the above copyright
 *   notice, this list of conditions and the following disclaimer.
 *
 * * Redistributions in binary form must reproduce the above copyright
 *   notice, this list of conditions and the following disclaimer in the
 *   documentation and/or other materials provided with the distribution.
 *
 * * Neither the name of 'MMDLoaderJME' nor the names of its contributors
 *   may be used to endorse or promote products derived from this software
 *   without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED
 * TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR
 * PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package projectkyoto.mmd.file.util2;

import java.io.Serializable;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import projectkyoto.mmd.file.PMDMaterial;
import projectkyoto.mmd.file.PMDModel;
import projectkyoto.mmd.file.PMDSkinData;
import projectkyoto.mmd.file.PMDVertex;

/**
 *
 * @author kobayasi
 */
public class MeshConverter implements Serializable{

    PMDModel model;
    public static int DEFAULT_MAX_BONE_SIZE = 50;
    int maxBoneSize = DEFAULT_MAX_BONE_SIZE;
    List<MeshData> meshDataList = new ArrayList<MeshData>();
    SkinMeshData skinMeshData;
    HashMap<Integer, Integer> meshTmpVertMap = new HashMap<Integer, Integer>();
    HashMap<Integer, Integer> skinTmpVertMap = new HashMap<Integer, Integer>();
    public ByteBuffer interleavedBuffer;
    int currentVertIndex = 0;
    public static int stride = 56;
    public MeshConverter(PMDModel model) {
        this.model = model;
        skinMeshData = new SkinMeshData(this, model);
        initSkinVertSet();
    }
    private final void initSkinVertSet() {
        for(int skinCount = 0;skinCount<model.getSkinCount();skinCount++) {
            PMDSkinData skinData = model.getSkinData()[skinCount];
            if (skinData.getSkinType() == 0) {
                for(int skinVertCount = 0;skinVertCount<skinData.getSkinVertCount();skinVertCount++) {
                    VertIndex vi = new VertIndex(skinData.getSkinVertData()[skinVertCount].getSkinVertIndex());
                    skinVertSet.add(vi);
                }
            }
        }
    }

    public void checkDupMaterial() {
        for(int i1 = 0;i1<model.getMaterialCount();i1++) {
            for(int i2=i1+1;i2<model.getMaterialCount();i2++) {
                if (model.getMaterial()[i1].equals(model.getMaterial()[i2])) {
//                    System.out.println("dup material");
                    break;
                }
            }
        }
    }
    void printFaceVertSize() {
        for(int skinCount = 0;skinCount<model.getSkinCount();skinCount++) {
            PMDSkinData skinData = model.getSkinData()[skinCount];
        }
    }
    public void convertMesh() {
        int faceVertNo = 0;
        for(int materialNo = 0; materialNo < model.getMaterialCount(); materialNo++) {
            meshTmpVertMap.clear();
            PMDMaterial material = model.getMaterial()[materialNo];
            // find same material
            MeshData meshData = new MeshData(model, maxBoneSize, material, currentVertIndex);
            for(int meshIndex = meshDataList.size()-1;meshIndex >=0;meshIndex--) {
                PMDMaterial material2 = meshDataList.get(meshIndex).getMaterial();
                if (material.equals(material2)) {
                    meshData = meshDataList.get(meshIndex);
                    for(int i=meshData.getVertIndexList().size()-1;i>=0;i--) {
                        Integer vertIndex = meshData.getVertIndexList().get(i);
                        meshTmpVertMap.put(vertIndex, i);
                    }
                    break;
                }
            }
            if (material.getFaceVertCount() == 0) {
                continue;
            }
            if (!meshDataList.contains(meshData)) {
                meshDataList.add(meshData);
            }
            for (int materialFaceVertNo = 0; materialFaceVertNo < material.getFaceVertCount(); materialFaceVertNo += 3) {
                int i1 = model.getFaceVertIndex()[faceVertNo++];
                int i2 = model.getFaceVertIndex()[faceVertNo++];
                int i3 = model.getFaceVertIndex()[faceVertNo++];
                if (containsSkin(i1, i2, i3)) {
                    addSkinTriangle(material, i1, i2, i3);
                } else {
                    if (!meshData.addTriangle(this, i1, i2, i3)) {
                        meshData = new MeshData(model, maxBoneSize, material, currentVertIndex);
                        meshTmpVertMap.clear();
                        meshDataList.add(meshData);
                        meshData.addTriangle(this, i1, i2, i3);
                    }
                }
            }
            if (meshData.material.getFaceVertCount() == 0) {
                meshDataList.remove(meshDataList.size()-1);
            }
        }
//        meshTmpVertMap = null;
        skinTmpVertMap = null;
//        createInterleavedBuffer();
        Iterator<MeshData> it = meshDataList.iterator();
        while(it.hasNext()) {
            MeshData md = it.next();
            if (md.getIndexList().size() == 0) {
                it.remove();
            }
        }
    }
    void createInterleavedBuffer() {
        int size = 0;
        for(MeshData md : meshDataList) {
            md.offset = size * stride;
            size += md.vertIndexList.size();
        }
        interleavedBuffer = ByteBuffer.allocateDirect(size * stride);
        interleavedBuffer.order(ByteOrder.nativeOrder());
        PMDVertex tmpVert = new PMDVertex();
        for(MeshData md : meshDataList) {
            for(Integer vertIndex : md.getVertIndexList()) {
                PMDVertex v = model.getVertex(vertIndex, tmpVert);
                interleavedBuffer.putFloat(v.getPos().x);
                interleavedBuffer.putFloat(v.getPos().y);
                interleavedBuffer.putFloat(v.getPos().z);
                interleavedBuffer.putFloat(v.getNormal().x);
                interleavedBuffer.putFloat(v.getNormal().y);
                interleavedBuffer.putFloat(v.getNormal().z);
                interleavedBuffer.putFloat(v.getUv().getU()).putFloat(1f - v.getUv().getV());
                float weight = (float) v.getBoneWeight() / 100.0f;
                interleavedBuffer.putFloat(weight).putFloat(1f - weight)
                        .putFloat(0).putFloat(0);
                interleavedBuffer.putShort((short)v.getBoneNum1()).putShort((short)v.getBoneNum2());
            }
        }
    } 
    void printMeshData(MeshData meshData) {
//            System.out.println("vertSize = " + meshData.getVertexList().size()
//                    + " indexSize = " + meshData.getIndexList().size()
//                    + " boneSize = " + meshData.getBoneList().size());
    }
    boolean containsSkin(int i1, int i2, int i3) {
        if (containsSkin(i1)
                || containsSkin(i2)
                || containsSkin(i3)) {
            return true;
        }
        return false;
    }
    VertIndex tmpvi = new VertIndex(0);
    boolean containsSkin(int i) {
        tmpvi.index = i;
        return skinVertSet.contains(tmpvi);
    }
    boolean _containsSkin(int i) {
        for(int skinCount = 0;skinCount<model.getSkinCount();skinCount++) {
            PMDSkinData skinData = model.getSkinData()[skinCount];
            if (skinData.getSkinType() == 0) {
                for(int skinVertCount = 0;skinVertCount<skinData.getSkinVertCount();skinVertCount++) {
                    if (i == skinData.getSkinVertData()[skinVertCount].getSkinVertIndex()) {
                        return true;
                    }
                }
            }
        }
        return false;
    }
    Set<VertIndex> skinVertSet = new java.util.HashSet<VertIndex> ();
    
    void addSkinTriangle(PMDMaterial material, int i1,int i2,int i3) {
        skinMeshData.addTriangle(this, material, i1, i2, i3);
    }

    public int getMaxBoneSize() {
        return maxBoneSize;
    }

    public void setMaxBoneSize(int maxBoneSize) {
        this.maxBoneSize = maxBoneSize;
    }

    public List<MeshData> getMeshDataList() {
        return meshDataList;
    }

    public void setMeshDataList(List<MeshData> meshDataList) {
        this.meshDataList = meshDataList;
    }

    public PMDModel getModel() {
        return model;
    }

    public void setModel(PMDModel model) {
        this.model = model;
    }

    public SkinMeshData getSkinMeshData() {
        return skinMeshData;
    }

    public void setSkinMeshData(SkinMeshData skinMeshData) {
        this.skinMeshData = skinMeshData;
    }
    
}
class VertIndex implements Serializable{
    int index;

    public VertIndex(int index) {
        this.index = index;
    }

    @Override
    public boolean equals(Object obj) {
//        if (obj == null) {
//            return false;
//        }
//        if (getClass() != obj.getClass()) {
//            return false;
//        }
        final VertIndex other = (VertIndex) obj;
        if (this.index != other.index) {
            return false;
        }
        return true;
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 53 * hash + this.index;
        return hash;
    }
}
