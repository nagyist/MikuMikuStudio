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

package projectkyoto.mmd.file;

import java.io.IOException;
import java.io.Serializable;
import java.nio.ByteBuffer;
import javax.vecmath.Point3f;
import javax.vecmath.Quat4f;
import projectkyoto.mmd.file.util2.BufferUtil;

/**
 *
 * @author kobayasi
 */
public class VMDMotion implements Serializable{

    private String boneName; // char[15]
    private int frameNo;
    private Point3f location;
    private Quat4f rotation;
    private byte[] interpolation = new byte[64];
    public VMDMotion() {
        location = new Point3f();
        rotation = new Quat4f();
    }
    public VMDMotion(DataInputStreamLittleEndian is) throws IOException {
        boneName = is.readString(15);
//        System.out.println("boneName = "+boneName);
        frameNo = is.readInt();
        location = new Point3f();
        location.x = is.readFloat();
        location.y = is.readFloat();
        location.z = -is.readFloat();
        rotation = new Quat4f(is.readFloat(), is.readFloat(), -is.readFloat(), -is.readFloat());
        is.read(interpolation);
    }
//    public VMDMotion readFromBuffer(ByteBuffer bb) {
//        boneName = BufferUtil.readString(bb, 15);
//        frameNo = bb.getInt();
//        BufferUtil.readPoint3f(bb, location);
//        BufferUtil.readQuat4f(bb, rotation);
//        bb.get(interpolation);
//        return this;
//    }
//    public VMDMotion writeToBuffer(ByteBuffer bb) {
//        BufferUtil.writeString(bb, boneName, 15);
//        bb.putInt(frameNo);
//        
//        return this;
//    }

    @Override
    public String toString() {
        StringBuffer sb = new StringBuffer();
        sb.append("{boneName = " + boneName
                + " frameNo = " + frameNo
                + " location = " + location
                + " rotation = " + rotation
                + " interpolation = {");
        for (int i = 0; i < 64; i++) {
            sb.append(interpolation[i]).append(',');
        }
        sb.append("}}\n");

        return sb.toString();
    }

    public String getBoneName() {
        return boneName;
    }

    public void setBoneName(String boneName) {
        this.boneName = boneName;
    }

    public int getFrameNo() {
        return frameNo;
    }

    public void setFlameNo(int frameNo) {
        this.frameNo = frameNo;
    }

    public byte[] getInterpolation() {
        return interpolation;
    }

    public void setInterpolation(byte[] interpolation) {
        this.interpolation = interpolation;
    }

    public Point3f getLocation() {
        return location;
    }

    public void setLocation(Point3f location) {
        this.location = location;
    }

    public Quat4f getRotation() {
        return rotation;
    }

    public void setRotation(Quat4f rotation) {
        this.rotation = rotation;
    }
    
}
