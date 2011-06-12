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
package com.jme3.scene.plugins.blender.helpers.v249;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.logging.Logger;

import com.jme3.animation.AnimControl;
import com.jme3.animation.Bone;
import com.jme3.animation.BoneAnimation;
import com.jme3.animation.BoneTrack;
import com.jme3.animation.Skeleton;
import com.jme3.animation.SkeletonControl;
import com.jme3.bounding.BoundingBox;
import com.jme3.bounding.BoundingSphere;
import com.jme3.bounding.BoundingVolume;
import com.jme3.effect.ParticleEmitter;
import com.jme3.effect.shapes.EmitterMeshVertexShape;
import com.jme3.effect.shapes.EmitterShape;
import com.jme3.material.Material;
import com.jme3.math.Matrix4f;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import com.jme3.scene.Geometry;
import com.jme3.scene.Mesh;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.scene.plugins.blender.data.FileBlockHeader;
import com.jme3.scene.plugins.blender.data.Structure;
import com.jme3.scene.plugins.blender.exception.BlenderFileException;
import com.jme3.scene.plugins.blender.helpers.ParticlesHelper;
import com.jme3.scene.plugins.blender.structures.Constraint;
import com.jme3.scene.plugins.blender.structures.Modifier;
import com.jme3.scene.plugins.blender.utils.AbstractBlenderHelper;
import com.jme3.scene.plugins.blender.utils.DataRepository;
import com.jme3.scene.plugins.blender.utils.DataRepository.LoadedFeatureDataType;
import com.jme3.scene.plugins.blender.utils.DynamicArray;
import com.jme3.scene.plugins.blender.utils.Pointer;
import com.jme3.scene.plugins.ogre.AnimData;

/**
 * A class that is used in modifiers calculations.
 * @author Marcin Roguski
 */
public class ModifierHelper extends AbstractBlenderHelper {

    private static final Logger LOGGER = Logger.getLogger(ModifierHelper.class.getName());

    /**
     * This constructor parses the given blender version and stores the result. Some functionalities may differ in
     * different blender versions.
     * @param blenderVersion
     *        the version read from the blend file
     */
    public ModifierHelper(String blenderVersion) {
        super(blenderVersion);
    }

    /**
     * This method applies modifier to the object.
     * @param node
     *        the loaded object
     * @param modifier
     *        the modifier to apply
     * @param dataRepository
     *        the data repository
     * @return the node to whom the modifier was applied
     */
    public Node applyModifier(Node node, Modifier modifier, DataRepository dataRepository) {
        if (Modifier.ARMATURE_MODIFIER_DATA.equals(modifier.getType())) {
            return this.applyArmatureModifierData(node, modifier, dataRepository);
        } else if (Modifier.ARRAY_MODIFIER_DATA.equals(modifier.getType())) {
            return this.applyArrayModifierData(node, modifier, dataRepository);
        } else if (Modifier.PARTICLE_MODIFIER_DATA.equals(modifier.getType())) {
            return this.applyParticleSystemModifierData(node, modifier, dataRepository);
        } else {
            LOGGER.warning("Modifier: " + modifier.getType() + " not yet implemented!!!");
            return node;
        }
    }

    /**
     * This method reads the given object's modifiers.
     * @param objectStructure
     *        the object structure
     * @param dataRepository
     *        the data repository
     * @param converter
     *        the converter object (in some cases we need to read an object first before loading the modifier)
     * @throws BlenderFileException
     *         this exception is thrown when the blender file is somehow corrupted
     */
    @SuppressWarnings("unchecked")
    public void readModifiers(Structure objectStructure, DataRepository dataRepository) throws BlenderFileException {
        Structure modifiersListBase = (Structure) objectStructure.getFieldValue("modifiers");
        List<Structure> modifiers = modifiersListBase.evaluateListBase(dataRepository);
        for (Structure modifier : modifiers) {
            Object loadedModifier = null;
            Object modifierAdditionalData = null;
            if (Modifier.ARRAY_MODIFIER_DATA.equals(modifier.getType())) {//****************ARRAY MODIFIER
                Map<String, Object> params = new HashMap<String, Object>();

                Number fittype = (Number) modifier.getFieldValue("fit_type");
                params.put("fittype", fittype);
                switch (fittype.intValue()) {
                    case 0://FIXED COUNT
                        params.put("count", modifier.getFieldValue("count"));
                        break;
                    case 1://FIXED LENGTH
                        params.put("length", modifier.getFieldValue("length"));
                        break;
                    case 2://FITCURVE
                        //TODO: implement after loading curves is added; warning will be generated during modifier applying
                        break;
                    default:
                        assert false : "Unknown array modifier fit type: " + fittype;
                }

                //offset parameters
                int offsettype = ((Number) modifier.getFieldValue("offset_type")).intValue();
                if ((offsettype & 0x01) != 0) {//Constant offset
                    DynamicArray<Number> offsetArray = (DynamicArray<Number>) modifier.getFieldValue("offset");
                    float[] offset = new float[]{offsetArray.get(0).floatValue(), offsetArray.get(1).floatValue(), offsetArray.get(2).floatValue()};
                    params.put("offset", offset);
                }
                if ((offsettype & 0x02) != 0) {//Relative offset
                    DynamicArray<Number> scaleArray = (DynamicArray<Number>) modifier.getFieldValue("scale");
                    float[] scale = new float[]{scaleArray.get(0).floatValue(), scaleArray.get(1).floatValue(), scaleArray.get(2).floatValue()};
                    params.put("scale", scale);
                }
                if ((offsettype & 0x04) != 0) {//Object offset
                    Pointer pOffsetObject = (Pointer) modifier.getFieldValue("offset_ob");
                    if (!pOffsetObject.isNull()) {
                        params.put("offsetob", pOffsetObject);
                    }
                }

                //start cap and end cap
                Pointer pStartCap = (Pointer) modifier.getFieldValue("start_cap");
                if (!pStartCap.isNull()) {
                    params.put("startcap", pStartCap);
                }
                Pointer pEndCap = (Pointer) modifier.getFieldValue("end_cap");
                if (!pEndCap.isNull()) {
                    params.put("endcap", pEndCap);
                }
                loadedModifier = params;
            } else if (Modifier.ARMATURE_MODIFIER_DATA.equals(modifier.getType())) {//****************ARMATURE MODIFIER
                Pointer pArmatureObject = (Pointer) modifier.getFieldValue("object");
                if (!pArmatureObject.isNull()) {
                    ObjectHelper objectHelper = dataRepository.getHelper(ObjectHelper.class);
                    Structure armatureObject = (Structure) dataRepository.getLoadedFeature(pArmatureObject.getOldMemoryAddress(), LoadedFeatureDataType.LOADED_STRUCTURE);
                    if (armatureObject == null) {//we check this first not to fetch the structure unnecessary
                        armatureObject = pArmatureObject.fetchData(dataRepository.getInputStream()).get(0);
                        objectHelper.toObject(armatureObject, dataRepository);
                    }
                    modifierAdditionalData = armatureObject.getOldMemoryAddress();
                    ArmatureHelper armatureHelper = dataRepository.getHelper(ArmatureHelper.class);

                    //changing bones matrices so that they fit the current object (taht is why we need a copy of a skeleton)
                    Matrix4f armatureObjectMatrix = objectHelper.getTransformationMatrix(armatureObject);
                    Matrix4f inverseMeshObjectMatrix = objectHelper.getTransformationMatrix(objectStructure).invert();
                    Matrix4f additionalRootBoneTransformation = inverseMeshObjectMatrix.multLocal(armatureObjectMatrix);
                    Bone[] bones = armatureHelper.buildBonesStructure(Long.valueOf(0L), additionalRootBoneTransformation);

                    String objectName = objectStructure.getName();
                    Set<String> animationNames = dataRepository.getBlenderKey().getAnimationNames(objectName);
                    if (animationNames != null && animationNames.size() > 0) {
                        ArrayList<BoneAnimation> animations = new ArrayList<BoneAnimation>();
                        List<FileBlockHeader> actionHeaders = dataRepository.getFileBlocks(Integer.valueOf(FileBlockHeader.BLOCK_AC00));
                        for (FileBlockHeader header : actionHeaders) {
                            Structure actionStructure = header.getStructure(dataRepository);
                            String actionName = actionStructure.getName();
                            if (animationNames.contains(actionName)) {
                                int[] animationFrames = dataRepository.getBlenderKey().getAnimationFrames(objectName, actionName);
                                int fps = dataRepository.getBlenderKey().getFps();
                                float start = (float) animationFrames[0] / (float) fps;
                                float stop = (float) animationFrames[1] / (float) fps;
                                BoneAnimation boneAnimation = new BoneAnimation(actionName, stop - start);
                                boneAnimation.setTracks(armatureHelper.getTracks(actionStructure, dataRepository, objectName, actionName));
                                animations.add(boneAnimation);
                            }
                        }
                        loadedModifier = new AnimData(new Skeleton(bones), animations);
                    }
                } else {
                    LOGGER.warning("Unsupported modifier type: " + modifier.getType());
                }
            } else if (Modifier.PARTICLE_MODIFIER_DATA.equals(modifier.getType())) {//****************PARTICLES MODIFIER
                Pointer pParticleSystem = (Pointer) modifier.getFieldValue("psys");
                if (!pParticleSystem.isNull()) {
                    ParticlesHelper particlesHelper = dataRepository.getHelper(ParticlesHelper.class);
                    Structure particleSystem = pParticleSystem.fetchData(dataRepository.getInputStream()).get(0);
                    loadedModifier = particlesHelper.toParticleEmitter(particleSystem, dataRepository);
                }
            }
            //adding modifier to the modifier's lists
            if (loadedModifier != null) {
                dataRepository.addModifier(objectStructure.getOldMemoryAddress(), modifier.getType(), loadedModifier, modifierAdditionalData);
                modifierAdditionalData = null;
            }
        }
    }

    /**
     * This method applies particles emitter to the given node.
     * @param node the particles emitter node
     * @param modifier the modifier containing the emitter data
     * @param dataRepository the data repository
     * @return node with particles' emitter applied
     */
    protected Node applyParticleSystemModifierData(Node node, Modifier modifier, DataRepository dataRepository) {
        MaterialHelper materialHelper = dataRepository.getHelper(MaterialHelper.class);
        ParticleEmitter emitter = (ParticleEmitter) modifier.getJmeModifierRepresentation();
        emitter = emitter.clone();

        //veryfying the alpha function for particles' texture
        Integer alphaFunction = MaterialHelper.ALPHA_MASK_HYPERBOLE;
        char nameSuffix = emitter.getName().charAt(emitter.getName().length() - 1);
        if (nameSuffix == 'B' || nameSuffix == 'N') {
            alphaFunction = MaterialHelper.ALPHA_MASK_NONE;
        }
        //removing the type suffix from the name
        emitter.setName(emitter.getName().substring(0, emitter.getName().length() - 1));

        //applying emitter shape
        EmitterShape emitterShape = emitter.getShape();
        List<Mesh> meshes = new ArrayList<Mesh>();
        for (Spatial spatial : node.getChildren()) {
            if (spatial instanceof Geometry) {
                Mesh mesh = ((Geometry) spatial).getMesh();
                if (mesh != null) {
                    meshes.add(mesh);
                    Material material = materialHelper.getParticlesMaterial(((Geometry) spatial).getMaterial(), alphaFunction, dataRepository);
                    emitter.setMaterial(material);//TODO: divide into several pieces
                }
            }
        }
        if (meshes.size() > 0 && emitterShape instanceof EmitterMeshVertexShape) {
            ((EmitterMeshVertexShape) emitterShape).setMeshes(meshes);
        }

        node.attachChild(emitter);
        return node;
    }

    /**
     * This method applies ArmatureModifierData to the loaded object.
     * @param node
     *        the loaded object
     * @param modifier
     *        the modifier to apply
     * @param dataRepository
     *        the data repository
     * @return the node to whom the modifier was applied
     */
    protected Node applyArmatureModifierData(Node node, Modifier modifier, DataRepository dataRepository) {
        AnimData ad = (AnimData) modifier.getJmeModifierRepresentation();
        ArrayList<BoneAnimation> animList = ad.anims;
        Long modifierArmatureObject = (Long) modifier.getAdditionalData();
        if (animList != null && animList.size() > 0) {
            ConstraintHelper constraintHelper = dataRepository.getHelper(ConstraintHelper.class);
            Constraint[] constraints = constraintHelper.getConstraints(modifierArmatureObject);
            HashMap<String, BoneAnimation> anims = new HashMap<String, BoneAnimation>();
            for (int i = 0; i < animList.size(); ++i) {
                BoneAnimation boneAnimation = this.cloneBoneAnimation(animList.get(i));

                //baking constraints into animations
                if (constraints != null && constraints.length > 0) {
                    for (Constraint constraint : constraints) {
                        constraint.affectAnimation(ad.skeleton, boneAnimation);
                    }
                }

                anims.put(boneAnimation.getName(), boneAnimation);
            }

            //getting meshes
            Mesh[] meshes = null;
            List<Mesh> meshesList = new ArrayList<Mesh>();
            List<Spatial> children = node.getChildren();
            for (Spatial child : children) {
                if (child instanceof Geometry) {
                    meshesList.add(((Geometry) child).getMesh());
                }
            }
            if (meshesList.size() > 0) {
                meshes = meshesList.toArray(new Mesh[meshesList.size()]);
            }

            //applying the control to the node
            SkeletonControl skeletonControl = new SkeletonControl(meshes, ad.skeleton);
            AnimControl control = node.getControl(AnimControl.class);

            if (control == null) {
                control = new AnimControl(ad.skeleton);
            } else {
                //merging skeletons
                Skeleton controlSkeleton = control.getSkeleton();
                int boneIndexIncrease = controlSkeleton.getBoneCount();
                Skeleton skeleton = this.merge(controlSkeleton, ad.skeleton);

                //merging animations
                HashMap<String, BoneAnimation> animations = new HashMap<String, BoneAnimation>();
                for (String animationName : control.getAnimationNames()) {
                    animations.put(animationName, control.getAnim(animationName));
                }
                for (Entry<String, BoneAnimation> animEntry : anims.entrySet()) {
                    BoneAnimation ba = animEntry.getValue();
                    for (int i = 0; i < ba.getTracks().length; ++i) {
                        BoneTrack bt = ba.getTracks()[i];
                        int newBoneIndex = bt.getTargetBoneIndex() + boneIndexIncrease;
                        ba.getTracks()[i] = new BoneTrack(newBoneIndex, bt.getTimes(), bt.getTranslations(), bt.getRotations(), bt.getScales());
                    }
                    animations.put(animEntry.getKey(), animEntry.getValue());
                }

                //replacing the control
                node.removeControl(control);
                control = new AnimControl(skeleton);
            }
            control.setAnimations(anims);
            node.addControl(control);
            node.addControl(skeletonControl);
        }
        return node;
    }

    /**
     * This method applies the array modifier to the node.
     * @param node
     *            the object the modifier will be applied to
     * @param modifier
     *            the modifier to be applied
     * @param dataRepository
     *            the data repository
     * @return object node with arry modifier applied
     */
    @SuppressWarnings("unchecked")
    protected Node applyArrayModifierData(Node node, Modifier modifier, DataRepository dataRepository) {
        Map<String, Object> modifierData = (Map<String, Object>) modifier.getJmeModifierRepresentation();
        int fittype = ((Number) modifierData.get("fittype")).intValue();
        float[] offset = (float[]) modifierData.get("offset");
        if (offset == null) {//the node will be repeated several times in the same place
            offset = new float[]{0.0f, 0.0f, 0.0f};
        }
        float[] scale = (float[]) modifierData.get("scale");
        if (scale == null) {//the node will be repeated several times in the same place
            scale = new float[]{0.0f, 0.0f, 0.0f};
        } else {
            //getting bounding box
            node.updateModelBound();
            BoundingVolume boundingVolume = node.getWorldBound();
            if (boundingVolume instanceof BoundingBox) {
                scale[0] *= ((BoundingBox) boundingVolume).getXExtent() * 2.0f;
                scale[1] *= ((BoundingBox) boundingVolume).getYExtent() * 2.0f;
                scale[2] *= ((BoundingBox) boundingVolume).getZExtent() * 2.0f;
            } else if (boundingVolume instanceof BoundingSphere) {
                float radius = ((BoundingSphere) boundingVolume).getRadius();
                scale[0] *= radius * 2.0f;
                scale[1] *= radius * 2.0f;
                scale[2] *= radius * 2.0f;
            } else {
                throw new IllegalStateException("Unknown bounding volume type: " + boundingVolume.getClass().getName());
            }
        }

        //adding object's offset
        float[] objectOffset = new float[]{0.0f, 0.0f, 0.0f};
        Pointer pOffsetObject = (Pointer) modifierData.get("offsetob");
        if (pOffsetObject != null) {
            FileBlockHeader offsetObjectBlock = dataRepository.getFileBlock(pOffsetObject.getOldMemoryAddress());
            ObjectHelper objectHelper = dataRepository.getHelper(ObjectHelper.class);
            try {//we take the structure in case the object was not yet loaded
                Structure offsetStructure = offsetObjectBlock.getStructure(dataRepository);
                Vector3f translation = objectHelper.getTransformation(offsetStructure).getTranslation();
                objectOffset[0] = translation.x;
                objectOffset[1] = translation.y;
                objectOffset[2] = translation.z;
            } catch (BlenderFileException e) {
                LOGGER.warning("Problems in blender file structure! Object offset cannot be applied! The problem: " + e.getMessage());
            }
        }

        //getting start and end caps
        Node[] caps = new Node[]{null, null};
        Pointer[] pCaps = new Pointer[]{(Pointer) modifierData.get("startcap"), (Pointer) modifierData.get("endcap")};
        for (int i = 0; i < pCaps.length; ++i) {
            if (pCaps[i] != null) {
                caps[i] = (Node) dataRepository.getLoadedFeature(pCaps[i].getOldMemoryAddress(), LoadedFeatureDataType.LOADED_FEATURE);
                if (caps[i] != null) {
                    caps[i] = (Node) caps[i].clone();
                } else {
                    FileBlockHeader capBlock = dataRepository.getFileBlock(pOffsetObject.getOldMemoryAddress());
                    try {//we take the structure in case the object was not yet loaded
                        Structure capStructure = capBlock.getStructure(dataRepository);
                        ObjectHelper objectHelper = dataRepository.getHelper(ObjectHelper.class);
                        caps[i] = (Node) objectHelper.toObject(capStructure, dataRepository);
                        if (caps[i] == null) {
                            LOGGER.warning("Cap object '" + capStructure.getName() + "' couldn't be loaded!");
                        }
                    } catch (BlenderFileException e) {
                        LOGGER.warning("Problems in blender file structure! Cap object cannot be applied! The problem: " + e.getMessage());
                    }
                }
            }
        }

        Vector3f translationVector = new Vector3f(offset[0] + scale[0] + objectOffset[0],
                offset[1] + scale[1] + objectOffset[1],
                offset[2] + scale[2] + objectOffset[2]);

        //getting/calculating repeats amount
        int count = 0;
        if (fittype == 0) {//Fixed count
            count = ((Number) modifierData.get("count")).intValue() - 1;
        } else if (fittype == 1) {//Fixed length
            float length = ((Number) modifierData.get("length")).floatValue();
            if (translationVector.length() > 0.0f) {
                count = (int) (length / translationVector.length()) - 1;
            }
        } else if (fittype == 2) {//Fit curve
            LOGGER.warning("Fit curve mode in array modifier not yet implemented!");//TODO: implement fit curve
        } else {
            throw new IllegalStateException("Unknown fit type: " + fittype);
        }

        //adding translated nodes and caps
        if (count > 0) {
            Node[] arrayNodes = new Node[count];
            Vector3f newTranslation = node.getLocalTranslation().clone();
            for (int i = 0; i < count; ++i) {
                newTranslation.addLocal(translationVector);
                Node nodeClone = (Node) node.clone();
                nodeClone.setLocalTranslation(newTranslation);
                arrayNodes[i] = nodeClone;
            }
            for (Node nodeClone : arrayNodes) {
                node.attachChild(nodeClone);
            }
            if (caps[0] != null) {
                caps[0].getLocalTranslation().set(node.getLocalTranslation()).subtractLocal(translationVector);
                node.attachChild(caps[0]);
            }
            if (caps[1] != null) {
                caps[1].getLocalTranslation().set(newTranslation).addLocal(translationVector);
                node.attachChild(caps[1]);
            }
        }
        return node;
    }

    /**
     * This class clones the bone animation data.
     * @param source
     *        the source that is to be cloned
     * @return the copy of the given bone animation
     */
    protected BoneAnimation cloneBoneAnimation(BoneAnimation source) {
        BoneAnimation result = new BoneAnimation(source.getName(), source.getLength());

        //copying tracks and applying constraints
        BoneTrack[] sourceTracks = source.getTracks();
        BoneTrack[] boneTracks = new BoneTrack[sourceTracks.length];
        for (int i = 0; i < sourceTracks.length; ++i) {
            int tablesLength = sourceTracks[i].getTimes().length;

            Vector3f[] sourceTranslations = sourceTracks[i].getTranslations();
            Quaternion[] sourceRotations = sourceTracks[i].getRotations();
            Vector3f[] sourceScales = sourceTracks[i].getScales();

            Vector3f[] translations = new Vector3f[tablesLength];
            Quaternion[] rotations = new Quaternion[tablesLength];
            Vector3f[] scales = sourceScales == null ? null : new Vector3f[tablesLength];
            for (int j = 0; j < tablesLength; ++j) {
                translations[j] = sourceTranslations[j].clone();
                rotations[j] = sourceRotations[j].clone();
                if (sourceScales != null) {//only scales may not be applied
                    scales[j] = sourceScales[j].clone();
                }
            }
            boneTracks[i] = new BoneTrack(sourceTracks[i].getTargetBoneIndex(), sourceTracks[i].getTimes(),//times do not change, no need to clone them,
                    translations, rotations, scales);
        }
        result.setTracks(boneTracks);
        return result;
    }

    /**
     * This method merges two skeletons into one. I assume that each skeleton's 0-indexed bone is objectAnimationBone so
     * only one such bone should be placed in the result
     * @param s1
     *        first skeleton
     * @param s2
     *        second skeleton
     * @return merged skeleton
     */
    protected Skeleton merge(Skeleton s1, Skeleton s2) {
        List<Bone> bones = new ArrayList<Bone>(s1.getBoneCount() + s2.getBoneCount());
        for (int i = 0; i < s1.getBoneCount(); ++i) {
            bones.add(s1.getBone(i));
        }
        for (int i = 1; i < s2.getBoneCount(); ++i) {//ommit objectAnimationBone
            bones.add(s2.getBone(i));
        }
        return new Skeleton(bones.toArray(new Bone[bones.size()]));
    }
}
