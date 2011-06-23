/*
 * Copyright (c) 2009-2010 jMonkeyEngine
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
 * * Neither the name of 'jMonkeyEngine' nor the names of its contributors
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

package com.jme3.gde.terraineditor;

import com.jme3.asset.AssetManager;
import com.jme3.asset.TextureKey;
import com.jme3.bounding.BoundingBox;
import com.jme3.gde.core.assets.AssetDataObject;
import com.jme3.gde.core.assets.ProjectAssetManager;
import com.jme3.gde.core.scene.SceneApplication;
import com.jme3.gde.core.sceneexplorer.nodes.AbstractSceneExplorerNode;
import com.jme3.gde.core.sceneexplorer.nodes.JmeSpatial;
import com.jme3.gde.core.undoredo.AbstractUndoableSceneEdit;
import com.jme3.gde.core.undoredo.SceneUndoRedoManager;
import com.jme3.material.MatParam;
import com.jme3.math.Vector2f;
import com.jme3.math.Vector3f;
import com.jme3.renderer.Camera;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.terrain.ProgressMonitor;
import com.jme3.terrain.Terrain;
import com.jme3.terrain.geomipmap.TerrainLodControl;
import com.jme3.terrain.geomipmap.TerrainQuad;
import com.jme3.texture.Texture;
import com.jme3.texture.Texture.WrapMode;
import com.jme3.util.SkyFactory;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import javax.imageio.ImageIO;
import javax.swing.undo.CannotRedoException;
import javax.swing.undo.CannotUndoException;
import jme3tools.converters.ImageToAwt;
import org.openide.cookies.SaveCookie;
import org.openide.filesystems.FileObject;
import org.openide.loaders.DataObject;
import org.openide.util.Exceptions;
import org.openide.util.Lookup;

/**
 * Modifies the actual terrain in the scene.
 * 
 * @author normenhansen, bowens
 */
@SuppressWarnings("unchecked")
public class TerrainEditorController {
    private JmeSpatial jmeRootNode;
    private Node terrainNode;
    private Node rootNode;
    private AssetDataObject currentFileObject;

    // texture settings
    protected final String DEFAULT_TERRAIN_TEXTURE = "com/jme3/gde/terraineditor/dirt.jpg";
    protected final float DEFAULT_TEXTURE_SCALE = 16.0625f;
    private final int NUM_ALPHA_TEXTURES = 3;
    private final int BASE_TEXTURE_COUNT = NUM_ALPHA_TEXTURES; // add any others here, like a global specular map
    protected final int MAX_TEXTURE_LAYERS = 7-BASE_TEXTURE_COUNT; // 16 max, minus the ones we are reserving

    

    protected SaveCookie terrainSaveCookie = new SaveCookie() {
      public void save() throws IOException {
            //TODO: On OpenGL thread? -- safest way.. with get()?
            SceneApplication.getApplication().enqueue(new Callable() {

                public Object call() throws Exception {
                    currentFileObject.saveAsset();
                    doSaveAlphaImages((Terrain)getTerrain(null));
                    return null;
                }
            });
        }
    };

    
    public TerrainEditorController(JmeSpatial jmeRootNode, AssetDataObject currentFileObject, TerrainEditorTopComponent topComponent) {
        this.jmeRootNode = jmeRootNode;
        rootNode = this.jmeRootNode.getLookup().lookup(Node.class);
        this.currentFileObject = currentFileObject;
        this.currentFileObject.setSaveCookie(terrainSaveCookie);
    }

    public void setToolController(TerrainToolController toolController) {
        
    }

    public FileObject getCurrentFileObject() {
        return currentFileObject.getPrimaryFile();
    }

    public DataObject getCurrentDataObject() {
        return currentFileObject;
    }

    public void setNeedsSave(boolean state) {
        currentFileObject.setModified(state);
    }

    public Node getTerrain(Spatial root) {
        if (terrainNode != null)
            return terrainNode;

        if (root == null)
            root = rootNode;

        // is this the terrain?
        if (root instanceof Terrain && root instanceof Node) {
            terrainNode = (Node)root;
            return terrainNode;
        }

        if (root instanceof Node) {
            Node n = (Node) root;
            for (Spatial c : n.getChildren()) {
                if (c instanceof Node){
                    Node res = getTerrain(c);
                    if (res != null)
                        return res;
                }
            }
        }

        return terrainNode;
    }

    /**
     * Perform the actual height modification on the terrain.
     * @param worldLoc the location in the world where the tool was activated
     * @param radius of the tool, terrain in this radius will be affected
     * @param heightFactor the amount to adjust the height by
     */
    public void doModifyTerrainHeight(Vector3f worldLoc, float radius, float heightFactor) {

        Terrain terrain = (Terrain) getTerrain(null);
        if (terrain == null)
            return;

        setNeedsSave(true);

        int radiusStepsX = (int) (radius / ((Node)terrain).getLocalScale().x);
        int radiusStepsZ = (int) (radius / ((Node)terrain).getLocalScale().z);

        float xStepAmount = ((Node)terrain).getLocalScale().x;
        float zStepAmount = ((Node)terrain).getLocalScale().z;

        List<Vector2f> locs = new ArrayList<Vector2f>();
        List<Float> heights = new ArrayList<Float>();

        for (int z=-radiusStepsZ; z<radiusStepsZ; z++) {
            for (int x=-radiusStepsZ; x<radiusStepsX; x++) {

                float locX = worldLoc.x + (x*xStepAmount);
                float locZ = worldLoc.z + (z*zStepAmount);

                // see if it is in the radius of the tool
                if (isInRadius(locX-worldLoc.x,locZ-worldLoc.z,radius)) {
                    // adjust height based on radius of the tool
                    float h = calculateHeight(radius, heightFactor, locX-worldLoc.x, locZ-worldLoc.z);
                    // increase the height
                    locs.add(new Vector2f(locX, locZ));
                    heights.add(h);
                }
            }
        }

        // do the actual height adjustment
        terrain.adjustHeight(locs, heights);

        ((Node)terrain).updateModelBound(); // or else we won't collide with it where we just edited
        
    }

    /**
     * See if the X,Y coordinate is in the radius of the circle. It is assumed
     * that the "grid" being tested is located at 0,0 and its dimensions are 2*radius.
     * @param x
     * @param z
     * @param radius
     * @return
     */
    private boolean isInRadius(float x, float y, float radius) {
        Vector2f point = new Vector2f(x,y);
        // return true if the distance is less than equal to the radius
        return Math.abs(point.length()) <= radius;
    }

    /**
     * Interpolate the height value based on its distance from the center (how far along
     * the radius it is).
     * The farther from the center, the less the height will be.
     * This produces a linear height falloff.
     * @param radius of the tool
     * @param heightFactor potential height value to be adjusted
     * @param x location
     * @param z location
     * @return the adjusted height value
     */
    private float calculateHeight(float radius, float heightFactor, float x, float z) {
        float val = calculateRadiusPercent(radius, x, z);
        return heightFactor * val;
    }

    private float calculateRadiusPercent(float radius, float x, float z) {
         // find percentage for each 'unit' in radius
        Vector2f point = new Vector2f(x,z);
        float val = Math.abs(point.length()) / radius;
        val = 1f - val;
        return val;
    }

    public void cleanup() {
        terrainNode = null;
        rootNode = null;
    }

    /**
     * pre-calculate the terrain's entropy values
     */
    public void generateEntropies(final ProgressMonitor progressMonitor) {
        SceneApplication.getApplication().enqueue(new Callable<Object>() {

            public Object call() throws Exception {
                doGenerateEntropies(progressMonitor);
                return null;
            }
        });
    }

    private void doGenerateEntropies(ProgressMonitor progressMonitor) {
        Terrain terrain = (Terrain) getTerrain(null);
        if (terrain == null)
            return;

        terrain.generateEntropy(progressMonitor);
    }

    // blocks on scale get
    public Float getTextureScale(final int layer) {
        try {
            Float scale =
                SceneApplication.getApplication().enqueue(new Callable<Float>() {
                    public Float call() throws Exception {
                        return doGetTextureScale(layer);
                    }
                }).get();
                return scale;
        } catch (InterruptedException ex) {
            Exceptions.printStackTrace(ex);
        } catch (ExecutionException ex) {
            Exceptions.printStackTrace(ex);
        }
        return null;
    }

    private Float doGetTextureScale(int layer) {
        Terrain terrain = (Terrain) getTerrain(null);
        if (terrain == null)
            return 1f;
        MatParam matParam = null;
        matParam = terrain.getMaterial().getParam("DiffuseMap_"+layer+"_scale");
        return (Float) matParam.getValue();
    }


    // blocks on scale set
    public void setTextureScale(final int layer, final float scale) {
        try {
            SceneApplication.getApplication().enqueue(new Callable() {
                public Object call() throws Exception {
                    doSetTextureScale(layer, scale);
                    return null;
                }
            }).get();
        } catch (InterruptedException ex) {
            Exceptions.printStackTrace(ex);
        } catch (ExecutionException ex) {
            Exceptions.printStackTrace(ex);
        }
    }

    private void doSetTextureScale(int layer, float scale) {
        Terrain terrain = (Terrain) getTerrain(null);
        if (terrain == null)
            return;
        terrain.getMaterial().setFloat("DiffuseMap_"+layer+"_scale", scale);
        setNeedsSave(true);
    }


    // blocks on texture get
    public Texture getDiffuseTexture(final int layer) {
        try {
            Texture tex =
                SceneApplication.getApplication().enqueue(new Callable<Texture>() {
                    public Texture call() throws Exception {
                        return doGetDiffuseTexture(layer);
                    }
                }).get();
                return tex;
        } catch (InterruptedException ex) {
            Exceptions.printStackTrace(ex);
        } catch (ExecutionException ex) {
            Exceptions.printStackTrace(ex);
        }
        return null;
    }

    /**
     * Get the diffuse texture at the specified layer.
     * Run this on the GL thread!
     */
    private Texture doGetDiffuseTexture(int layer) {
        Terrain terrain = (Terrain) getTerrain(null);
        if (terrain == null)
            return null;
        MatParam matParam = null;
        if (layer == 0)
            matParam = terrain.getMaterial().getParam("DiffuseMap");
        else
            matParam = terrain.getMaterial().getParam("DiffuseMap_"+layer);

        if (matParam == null || matParam.getValue() == null) {
            return null;
        }
        Texture tex = (Texture) matParam.getValue();

        return tex;
    }

    private Texture getAlphaTexture(final int layer) {
        try {
            Texture tex =
                SceneApplication.getApplication().enqueue(new Callable<Texture>() {
                    public Texture call() throws Exception {
                        Terrain terrain = (Terrain) getTerrain(null);
                        return doGetAlphaTexture(terrain, layer);
                    }
                }).get();
                return tex;
        } catch (InterruptedException ex) {
            Exceptions.printStackTrace(ex);
        } catch (ExecutionException ex) {
            Exceptions.printStackTrace(ex);
        }
        return null;
    }

    private Texture doGetAlphaTexture(Terrain terrain, int alphaLayer) {
        if (terrain == null)
            return null;
        MatParam matParam = null;
        if (alphaLayer == 0)
            matParam = terrain.getMaterial().getParam("AlphaMap");
        else if(alphaLayer == 1)
            matParam = terrain.getMaterial().getParam("AlphaMap_1");
        else if(alphaLayer == 2)
            matParam = terrain.getMaterial().getParam("AlphaMap_2");
        
        if (matParam == null || matParam.getValue() == null) {
            return null;
        }
        Texture tex = (Texture) matParam.getValue();
        return tex;
    }

    /**
     * Get the diffuse texture at the specified layer.
     * Run this on the GL thread!
     */
    private Texture doGetAlphaTextureFromDiffuse(Terrain terrain, int diffuseLayer) {
        int alphaIdx = diffuseLayer/4; // 4 = rgba = 4 textures

        return doGetAlphaTexture(terrain, alphaIdx);
       /* Terrain terrain = (Terrain) getTerrain(null);
        MatParam matParam = null;
        //TODO: add when supported
//        if (alphaIdx == 0)
            matParam = terrain.getMaterial().getParam("AlphaMap");
//        else
//            matParam = terrain.getMaterial().getParam("AlphaMap_"+alphaIdx);

        if (matParam == null || matParam.getValue() == null) {
            return null;
        }
        Texture tex = (Texture) matParam.getValue();
        return tex;
        */
    }


    /**
     * Set the diffuse texture at the specified layer.
     * Blocks on the GL thread
     * @param layer number to set the texture
     * @param texturePath if null, the default texture will be used
     */
    public void setDiffuseTexture(final int layer, final String texturePath) {
        try {
            SceneApplication.getApplication().enqueue(new Callable() {
                public Object call() throws Exception {
                    doSetDiffuseTexture(layer, texturePath);
                    return null;
                }
            }).get();
        } catch (InterruptedException ex) {
            Exceptions.printStackTrace(ex);
        } catch (ExecutionException ex) {
            Exceptions.printStackTrace(ex);
        }
    }

    private void doSetDiffuseTexture(int layer, String texturePath) {
        if (texturePath == null || texturePath.equals(""))
            texturePath = DEFAULT_TERRAIN_TEXTURE;

        Texture tex = SceneApplication.getApplication().getAssetManager().loadTexture(texturePath);
        tex.setWrap(WrapMode.Repeat);
        Terrain terrain = (Terrain) getTerrain(null);
        
        if (layer == 0)
            terrain.getMaterial().setTexture("DiffuseMap", tex);
        else
            terrain.getMaterial().setTexture("DiffuseMap_"+layer, tex);

        doSetTextureScale(layer, DEFAULT_TEXTURE_SCALE);
        
        setNeedsSave(true);
    }
    
    private void doSetDiffuseTexture(int layer, Texture tex) {
        tex.setWrap(WrapMode.Repeat);
        Terrain terrain = (Terrain) getTerrain(null);
        if (terrain == null)
            return;
        if (layer == 0)
            terrain.getMaterial().setTexture("DiffuseMap", tex);
        else
            terrain.getMaterial().setTexture("DiffuseMap_"+layer, tex);

        setNeedsSave(true);
    }

     public void setDiffuseTexture(final int layer, final Texture texture) {
        try {
            SceneApplication.getApplication().enqueue(new Callable() {
                public Object call() throws Exception {
                    doSetDiffuseTexture(layer, texture);
                    return null;
                }
            }).get();
        } catch (InterruptedException ex) {
            Exceptions.printStackTrace(ex);
        } catch (ExecutionException ex) {
            Exceptions.printStackTrace(ex);
        }
    }

    /**
     * Remove a whole texture layer: diffuse and normal map
     * @param layer
     * @param texturePath
     */
    public void removeTextureLayer(final int layer) {
        try {
            SceneApplication.getApplication().enqueue(new Callable() {
                public Object call() throws Exception {
                    doRemoveDiffuseTexture(layer);
                    doRemoveNormalMap(layer);
                    return null;
                }
            }).get();
        } catch (InterruptedException ex) {
            Exceptions.printStackTrace(ex);
        } catch (ExecutionException ex) {
            Exceptions.printStackTrace(ex);
        }
    }

    private void doRemoveDiffuseTexture(int layer) {
        Terrain terrain = (Terrain) getTerrain(null);
        if (terrain == null)
            return;
        if (layer == 0)
            terrain.getMaterial().clearParam("DiffuseMap");
        else
            terrain.getMaterial().clearParam("DiffuseMap_"+layer);

        setNeedsSave(true);
    }

    /**
     * Remove the normal map at the specified layer.
     * @param layer
     */
    public void removeNormalMap(final int layer) {
        try {
            SceneApplication.getApplication().enqueue(new Callable() {
                public Object call() throws Exception {
                    doRemoveNormalMap(layer);
                    return null;
                }
            }).get();
        } catch (InterruptedException ex) {
            Exceptions.printStackTrace(ex);
        } catch (ExecutionException ex) {
            Exceptions.printStackTrace(ex);
        }
    }

    private void doRemoveNormalMap(int layer) {
        Terrain terrain = (Terrain) getTerrain(null);
        if (terrain == null)
            return;
        if (layer == 0)
            terrain.getMaterial().clearParam("NormalMap");
        else
            terrain.getMaterial().clearParam("NormalMap_"+layer);

        setNeedsSave(true);
    }

    // blocks on normal map get
    public Texture getNormalMap(final int layer) {
        try {
            Texture tex =
                SceneApplication.getApplication().enqueue(new Callable<Texture>() {
                    public Texture call() throws Exception {
                        return doGetNormalMap(layer);
                    }
                }).get();
                return tex;
        } catch (InterruptedException ex) {
            Exceptions.printStackTrace(ex);
        } catch (ExecutionException ex) {
            Exceptions.printStackTrace(ex);
        }
        return null;
    }

    /**
     * Get the normal map texture at the specified layer.
     * Run this on the GL thread!
     */
    private Texture doGetNormalMap(int layer) {
        Terrain terrain = (Terrain) getTerrain(null);
        if (terrain == null)
            return null;
        MatParam matParam = null;
        if (layer == 0)
            matParam = terrain.getMaterial().getParam("NormalMap");
        else
            matParam = terrain.getMaterial().getParam("NormalMap_"+layer);

        if (matParam == null || matParam.getValue() == null) {
            return null;
        }
        Texture tex = (Texture) matParam.getValue();
        return tex;
    }

    /**
     * Set the normal map at the specified layer.
     * Blocks on the GL thread
     */
    public void setNormalMap(final int layer, final String texturePath) {
        try {
            SceneApplication.getApplication().enqueue(new Callable() {
                public Object call() throws Exception {
                    doSetNormalMap(layer, texturePath);
                    return null;
                }
            }).get();
        } catch (InterruptedException ex) {
            Exceptions.printStackTrace(ex);
        } catch (ExecutionException ex) {
            Exceptions.printStackTrace(ex);
        }
    }

    private void doSetNormalMap(int layer, String texturePath) {
        Terrain terrain = (Terrain) getTerrain(null);
        if (terrain == null)
            return;

        if (texturePath == null) {
            // remove the texture if it is null
            if (layer == 0)
                terrain.getMaterial().clearParam("NormalMap");
            else
                terrain.getMaterial().clearParam("NormalMap_"+layer);
            return;
        }

        Texture tex = SceneApplication.getApplication().getAssetManager().loadTexture(texturePath);
        tex.setWrap(WrapMode.Repeat);
        
        if (layer == 0)
            terrain.getMaterial().setTexture("NormalMap", tex);
        else
            terrain.getMaterial().setTexture("NormalMap_"+layer, tex);

        setNeedsSave(true);
    }

    public void setNormalMap(final int layer, final Texture texture) {
        try {
            SceneApplication.getApplication().enqueue(new Callable() {
                public Object call() throws Exception {
                    doSetNormalMap(layer, texture);
                    return null;
                }
            }).get();
        } catch (InterruptedException ex) {
            Exceptions.printStackTrace(ex);
        } catch (ExecutionException ex) {
            Exceptions.printStackTrace(ex);
        }
    }

    private void doSetNormalMap(int layer, Texture tex) {
        Terrain terrain = (Terrain) getTerrain(null);
        if (terrain == null)
            return;
        if (tex == null) {
            // remove the texture if it is null
            if (layer == 0)
                terrain.getMaterial().clearParam("NormalMap");
            else
                terrain.getMaterial().clearParam("NormalMap_"+layer);
            return;
        }

        tex.setWrap(WrapMode.Repeat);
        
        if (layer == 0)
            terrain.getMaterial().setTexture("NormalMap", tex);
        else
            terrain.getMaterial().setTexture("NormalMap_"+layer, tex);

        setNeedsSave(true);
    }

    // blocks on GL thread until terrain is created
    public Terrain createTerrain(final Node parent,
                                final int totalSize,
                                final int patchSize,
                                final int alphaTextureSize,
                                final float[] heightmapData,
                                final String sceneName,
                                final JmeSpatial jmeNodeParent) throws IOException
    {
        try {
            Terrain terrain =
            SceneApplication.getApplication().enqueue(new Callable<Terrain>() {
                public Terrain call() throws Exception {
                    return doCreateTerrain(parent, totalSize, patchSize, alphaTextureSize, heightmapData, sceneName, jmeNodeParent);
                }
            }).get();
            return terrain;
        } catch (InterruptedException ex) {
            Exceptions.printStackTrace(ex);
        } catch (ExecutionException ex) {
            Exceptions.printStackTrace(ex);
        }
        //doCreateTerrain(totalSize, patchSize, alphaTextureSize, heightmapData, sceneName, defaultTextureScale);
        return null; // if failed
    }

    private Terrain doCreateTerrain(Node parent,
                                    int totalSize,
                                    int patchSize,
                                    int alphaTextureSize,
                                    float[] heightmapData,
                                    String sceneName,
                                    JmeSpatial jmeNodeParent) throws IOException
    {
        AssetManager manager = SceneApplication.getApplication().getAssetManager();

        TerrainQuad terrain = new TerrainQuad("terrain-"+sceneName, patchSize, totalSize, heightmapData); //TODO make this pluggable for different Terrain implementations
        com.jme3.material.Material mat = new com.jme3.material.Material(manager, "Common/MatDefs/Terrain/TerrainLighting.j3md");

        String assetFolder = "";
        if (manager != null && manager instanceof ProjectAssetManager)
            assetFolder = ((ProjectAssetManager)manager).getAssetFolderName();

        // write out 3 alpha blend images
        for (int i=0; i<NUM_ALPHA_TEXTURES; i++) {
            BufferedImage alphaBlend = new BufferedImage(alphaTextureSize, alphaTextureSize, BufferedImage.TYPE_INT_ARGB);
            if (i == 0) {
                // the first alpha level should be opaque so we see the first texture over the whole terrain
                for (int h=0; h<alphaTextureSize; h++)
                    for (int w=0; w<alphaTextureSize; w++)
                        alphaBlend.setRGB(w, h, 0x00FF0000);//argb
            }
            File alphaFolder = new File(assetFolder+"/Textures/terrain-alpha/");
            if (!alphaFolder.exists())
                alphaFolder.mkdir();
            String alphaBlendFileName = "/Textures/terrain-alpha/"+sceneName+"-"+terrain.getName()+"-alphablend"+i+".png";
            File alphaImageFile = new File(assetFolder+alphaBlendFileName);
            ImageIO.write(alphaBlend, "png", alphaImageFile);
            Texture tex = manager.loadAsset(new TextureKey(alphaBlendFileName, false));
            if (i == 0)
                mat.setTexture("AlphaMap", tex);
            /*else if (i == 1) // add these in when they are supported
                mat.setTexture("AlphaMap_1", tex);
            else if (i == 2)
                mat.setTexture("AlphaMap_2", tex);*/
        }
        
        // give the first layer default texture
        Texture defaultTexture = manager.loadTexture(DEFAULT_TERRAIN_TEXTURE);
        defaultTexture.setWrap(WrapMode.Repeat);
        mat.setTexture("DiffuseMap", defaultTexture);
        mat.setFloat("DiffuseMap_0_scale", DEFAULT_TEXTURE_SCALE);
        mat.setBoolean("WardIso", true);

        terrain.setMaterial(mat);
        terrain.setModelBound(new BoundingBox());
        terrain.updateModelBound();
        terrain.setLocalTranslation(0, 0, 0);
        terrain.setLocalScale(1f, 1f, 1f);

        // add the lod control
        List<Camera> cameras = new ArrayList<Camera>();
	cameras.add(SceneApplication.getApplication().getCamera());
        TerrainLodControl control = new TerrainLodControl(terrain, cameras);
	//terrain.addControl(control); // removing this until we figure out a way to have it get the cameras when saved/loaded

        parent.attachChild(terrain);

        setNeedsSave(true);

        addSpatialUndo(parent, terrain, jmeNodeParent);
        
        return terrain;
    }

    private void addSpatialUndo(final Node undoParent, final Spatial undoSpatial, final AbstractSceneExplorerNode parentNode) {
        //add undo
        if (undoParent != null && undoSpatial != null) {
            Lookup.getDefault().lookup(SceneUndoRedoManager.class).addEdit(this, new AbstractUndoableSceneEdit() {

                @Override
                public void sceneUndo() throws CannotUndoException {
                    //undo stuff here
                    undoSpatial.removeFromParent();
                }

                @Override
                public void sceneRedo() throws CannotRedoException {
                    //redo stuff here
                    undoParent.attachChild(undoSpatial);
                }

                @Override
                public void awtRedo() {
                    if (parentNode != null) {
                        parentNode.refresh(true);
                    }
                }

                @Override
                public void awtUndo() {
                    if (parentNode != null) {
                        parentNode.refresh(true);
                    }
                }
            });
        }
    }

    /**
     * Save the terrain's alpha maps to disk, in the Textures/terrain-alpha/ directory
     * @throws IOException
     */
    private synchronized void doSaveAlphaImages(Terrain terrain) {

        AssetManager manager = SceneApplication.getApplication().getAssetManager();
        String assetFolder = null;
        if (manager != null && manager instanceof ProjectAssetManager)
            assetFolder = ((ProjectAssetManager)manager).getAssetFolderName();
        if (assetFolder == null)
            throw new IllegalStateException("AssetManager was not a ProjectAssetManager. Could not locate image save directories.");

        Texture alpha = doGetAlphaTexture(terrain, 0);
        BufferedImage bi = ImageToAwt.convert(alpha.getImage(), false, true, 0);
        File imageFile = new File(assetFolder+alpha.getKey().getName());
        try {
            ImageIO.write(bi, "png", imageFile);
        } catch (IOException ex) {
            Exceptions.printStackTrace(ex);
        }
        
        /*alpha = doGetAlphaTexture(1);
        bi = ImageToAwt.convert(alpha.getImage(), false, true, 0);
        imageFile = new File(alpha.getKey().getName());
        ImageIO.write(bi, "png", imageFile);

        alpha = doGetAlphaTexture(2);
        bi = ImageToAwt.convert(alpha.getImage(), false, true, 0);
        imageFile = new File(alpha.getKey().getName());
        ImageIO.write(bi, "png", imageFile);
        */
    }

    /**
     * Create a skybox with 6 textures.
     * Blocking call.
     */
    protected Spatial createSky(final Node parent,
                                final Texture west,
                                final Texture east,
                                final Texture north,
                                final Texture south,
                                final Texture top,
                                final Texture bottom,
                                final Vector3f normalScale)
    {
        try {
            Spatial sky =
            SceneApplication.getApplication().enqueue(new Callable<Spatial>() {
                public Spatial call() throws Exception {
                    return doCreateSky(parent, west, east, north, south, top, bottom, normalScale);
                }
            }).get();
            return sky;
        } catch (InterruptedException ex) {
            Exceptions.printStackTrace(ex);
        } catch (ExecutionException ex) {
            Exceptions.printStackTrace(ex);
        }
        return null; // if failed
    }

    private Spatial doCreateSky(Node parent,
                                Texture west,
                                Texture east,
                                Texture north,
                                Texture south,
                                Texture top,
                                Texture bottom,
                                Vector3f normalScale)
    {
        AssetManager manager = SceneApplication.getApplication().getAssetManager();
        Spatial sky = SkyFactory.createSky(manager, west, east, north, south, top, bottom, normalScale);
        parent.attachChild(sky);
        return sky;
    }

    /**
     * Create a skybox with a single texture.
     * Blocking call.
     */
    protected Spatial createSky(final Node parent,
                                final Texture texture,
                                final boolean useSpheremap,
                                final Vector3f normalScale)
    {
        try {
            Spatial sky =
            SceneApplication.getApplication().enqueue(new Callable<Spatial>() {
                public Spatial call() throws Exception {
                    return doCreateSky(parent, texture, useSpheremap, normalScale);
                }
            }).get();
            return sky;
        } catch (InterruptedException ex) {
            Exceptions.printStackTrace(ex);
        } catch (ExecutionException ex) {
            Exceptions.printStackTrace(ex);
        }
        return null; // if failed
    }

    private Spatial doCreateSky(Node parent,
                                Texture texture,
                                boolean useSpheremap,
                                Vector3f normalScale)
    {
        AssetManager manager = SceneApplication.getApplication().getAssetManager();
        Spatial sky = SkyFactory.createSky(manager, texture, normalScale, useSpheremap);
        parent.attachChild(sky);
        return sky;
    }

    public boolean hasTextureAt(final int i) {
        try {
            Boolean result =
                SceneApplication.getApplication().enqueue(new Callable<Boolean>() {
                    public Boolean call() throws Exception {
                        Texture tex = doGetDiffuseTexture(i);
                        return tex != null;
                    }
                }).get();
                return result;
        } catch (InterruptedException ex) {
            Exceptions.printStackTrace(ex);
        } catch (ExecutionException ex) {
            Exceptions.printStackTrace(ex);
        }
        return false;
    }
    

    /**
     * How many textures are currently being used.
     * Blocking call on GL thread
     */
    protected int getNumUsedTextures() {
        try {
            Integer count =
              SceneApplication.getApplication().enqueue(new Callable<Integer>() {
                public Integer call() throws Exception {
                    return doGetNumUsedTextures();
                }
            }).get();
            return count;
        } catch (InterruptedException ex) {
            Exceptions.printStackTrace(ex);
        } catch (ExecutionException ex) {
            Exceptions.printStackTrace(ex);
        }
        return -1;
    }

    private int doGetNumUsedTextures() {
        Terrain terrain = (Terrain) getTerrain(null);
        if (terrain == null)
            return 0;

        int count = 0;

        for (int i=0; i<MAX_TEXTURE_LAYERS; i++) {
            Texture tex = doGetDiffuseTexture(i);
            if (tex != null)
                count++;
            tex = doGetNormalMap(i);
            if (tex != null)
                count++;
        }
        return count;
    }

    public boolean isTriPlanarEnabled() {
        try {
            Boolean isEnabled =
            SceneApplication.getApplication().enqueue(new Callable<Boolean>() {
                public Boolean call() throws Exception {
                    return doIsTriPlanarEnabled();
                }
            }).get();
            return isEnabled;
        } catch (InterruptedException ex) {
            Exceptions.printStackTrace(ex);
        } catch (ExecutionException ex) {
            Exceptions.printStackTrace(ex);
        }
        return false;
    }

    private boolean doIsTriPlanarEnabled() {
        Terrain terrain = (Terrain) getTerrain(null);
        if (terrain == null)
            return false;
        MatParam param = terrain.getMaterial().getParam("useTriPlanarMapping");
        if (param != null)
            return (Boolean)param.getValue();

        return false;
    }

    public void setTriPlanarEnabled(final boolean selected) {
        try {
            SceneApplication.getApplication().enqueue(new Callable() {
                public Object call() throws Exception {
                    doSetTriPlanarEnabled(selected);
                    return null;
                }
            }).get();
        } catch (InterruptedException ex) {
            Exceptions.printStackTrace(ex);
        } catch (ExecutionException ex) {
            Exceptions.printStackTrace(ex);
        }
    }

    /**
     * Also adjusts the scale. Normal texture scale uses texture coordinates,
     * which are each 1/(total size of the terrain). But for tri planar it doesn't
     * use texture coordinates, so we need to re-calculate it to be the same scale.
     * @param enabled
     * @param terrainTotalSize
     */
    private void doSetTriPlanarEnabled(boolean enabled) {
        Terrain terrain = (Terrain) getTerrain(null);
        if (terrain == null)
            return;
        terrain.getMaterial().setBoolean("useTriPlanarMapping", enabled);
        
        float texCoordSize = 1/terrain.getTextureCoordinateScale();

        if (enabled) {
            for (int i=0; i<doGetNumUsedTextures(); i++) {
                float scale = 1f/(float)(texCoordSize/doGetTextureScale(i));
                doSetTextureScale(i, scale);
            }
        } else {
            for (int i=0; i<doGetNumUsedTextures(); i++) {
                float scale = (float)(texCoordSize*doGetTextureScale(i));
                doSetTextureScale(i, scale);
            }
        }

        setNeedsSave(true);
    }

    

}
