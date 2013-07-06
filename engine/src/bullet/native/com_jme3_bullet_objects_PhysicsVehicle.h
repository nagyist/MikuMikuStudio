/* DO NOT EDIT THIS FILE - it is machine generated */
#include <jni.h>
/* Header for class com_jme3_bullet_objects_PhysicsVehicle */

#ifndef _Included_com_jme3_bullet_objects_PhysicsVehicle
#define _Included_com_jme3_bullet_objects_PhysicsVehicle
#ifdef __cplusplus
extern "C" {
#endif
#undef com_jme3_bullet_objects_PhysicsVehicle_COLLISION_GROUP_NONE
#define com_jme3_bullet_objects_PhysicsVehicle_COLLISION_GROUP_NONE 0L
#undef com_jme3_bullet_objects_PhysicsVehicle_COLLISION_GROUP_01
#define com_jme3_bullet_objects_PhysicsVehicle_COLLISION_GROUP_01 1L
#undef com_jme3_bullet_objects_PhysicsVehicle_COLLISION_GROUP_02
#define com_jme3_bullet_objects_PhysicsVehicle_COLLISION_GROUP_02 2L
#undef com_jme3_bullet_objects_PhysicsVehicle_COLLISION_GROUP_03
#define com_jme3_bullet_objects_PhysicsVehicle_COLLISION_GROUP_03 4L
#undef com_jme3_bullet_objects_PhysicsVehicle_COLLISION_GROUP_04
#define com_jme3_bullet_objects_PhysicsVehicle_COLLISION_GROUP_04 8L
#undef com_jme3_bullet_objects_PhysicsVehicle_COLLISION_GROUP_05
#define com_jme3_bullet_objects_PhysicsVehicle_COLLISION_GROUP_05 16L
#undef com_jme3_bullet_objects_PhysicsVehicle_COLLISION_GROUP_06
#define com_jme3_bullet_objects_PhysicsVehicle_COLLISION_GROUP_06 32L
#undef com_jme3_bullet_objects_PhysicsVehicle_COLLISION_GROUP_07
#define com_jme3_bullet_objects_PhysicsVehicle_COLLISION_GROUP_07 64L
#undef com_jme3_bullet_objects_PhysicsVehicle_COLLISION_GROUP_08
#define com_jme3_bullet_objects_PhysicsVehicle_COLLISION_GROUP_08 128L
#undef com_jme3_bullet_objects_PhysicsVehicle_COLLISION_GROUP_09
#define com_jme3_bullet_objects_PhysicsVehicle_COLLISION_GROUP_09 256L
#undef com_jme3_bullet_objects_PhysicsVehicle_COLLISION_GROUP_10
#define com_jme3_bullet_objects_PhysicsVehicle_COLLISION_GROUP_10 512L
#undef com_jme3_bullet_objects_PhysicsVehicle_COLLISION_GROUP_11
#define com_jme3_bullet_objects_PhysicsVehicle_COLLISION_GROUP_11 1024L
#undef com_jme3_bullet_objects_PhysicsVehicle_COLLISION_GROUP_12
#define com_jme3_bullet_objects_PhysicsVehicle_COLLISION_GROUP_12 2048L
#undef com_jme3_bullet_objects_PhysicsVehicle_COLLISION_GROUP_13
#define com_jme3_bullet_objects_PhysicsVehicle_COLLISION_GROUP_13 4096L
#undef com_jme3_bullet_objects_PhysicsVehicle_COLLISION_GROUP_14
#define com_jme3_bullet_objects_PhysicsVehicle_COLLISION_GROUP_14 8192L
#undef com_jme3_bullet_objects_PhysicsVehicle_COLLISION_GROUP_15
#define com_jme3_bullet_objects_PhysicsVehicle_COLLISION_GROUP_15 16384L
#undef com_jme3_bullet_objects_PhysicsVehicle_COLLISION_GROUP_16
#define com_jme3_bullet_objects_PhysicsVehicle_COLLISION_GROUP_16 32768L
/*
 * Class:     com_jme3_bullet_objects_PhysicsVehicle
 * Method:    updateWheelTransform
 * Signature: (JIZ)V
 */
JNIEXPORT void JNICALL Java_com_jme3_bullet_objects_PhysicsVehicle_updateWheelTransform
  (JNIEnv *, jobject, jlong, jint, jboolean);

/*
 * Class:     com_jme3_bullet_objects_PhysicsVehicle
 * Method:    createVehicleRaycaster
 * Signature: (JJ)J
 */
JNIEXPORT jlong JNICALL Java_com_jme3_bullet_objects_PhysicsVehicle_createVehicleRaycaster
  (JNIEnv *, jobject, jlong, jlong);

/*
 * Class:     com_jme3_bullet_objects_PhysicsVehicle
 * Method:    createRaycastVehicle
 * Signature: (JJ)J
 */
JNIEXPORT jlong JNICALL Java_com_jme3_bullet_objects_PhysicsVehicle_createRaycastVehicle
  (JNIEnv *, jobject, jlong, jlong);

/*
 * Class:     com_jme3_bullet_objects_PhysicsVehicle
 * Method:    setCoordinateSystem
 * Signature: (JIII)V
 */
JNIEXPORT void JNICALL Java_com_jme3_bullet_objects_PhysicsVehicle_setCoordinateSystem
  (JNIEnv *, jobject, jlong, jint, jint, jint);

/*
 * Class:     com_jme3_bullet_objects_PhysicsVehicle
 * Method:    addWheel
 * Signature: (JLcom/jme3/math/Vector3f;Lcom/jme3/math/Vector3f;Lcom/jme3/math/Vector3f;FFLcom/jme3/bullet/objects/infos/VehicleTuning;Z)J
 */
JNIEXPORT jlong JNICALL Java_com_jme3_bullet_objects_PhysicsVehicle_addWheel
  (JNIEnv *, jobject, jlong, jobject, jobject, jobject, jfloat, jfloat, jobject, jboolean);

/*
 * Class:     com_jme3_bullet_objects_PhysicsVehicle
 * Method:    getWheel
 * Signature: (JI)J
 */
JNIEXPORT jlong JNICALL Java_com_jme3_bullet_objects_PhysicsVehicle_getWheel
  (JNIEnv *, jobject, jlong, jint);

/*
 * Class:     com_jme3_bullet_objects_PhysicsVehicle
 * Method:    resetSuspension
 * Signature: (J)V
 */
JNIEXPORT void JNICALL Java_com_jme3_bullet_objects_PhysicsVehicle_resetSuspension
  (JNIEnv *, jobject, jlong);

/*
 * Class:     com_jme3_bullet_objects_PhysicsVehicle
 * Method:    applyEngineForce
 * Signature: (JIF)V
 */
JNIEXPORT void JNICALL Java_com_jme3_bullet_objects_PhysicsVehicle_applyEngineForce
  (JNIEnv *, jobject, jlong, jint, jfloat);

/*
 * Class:     com_jme3_bullet_objects_PhysicsVehicle
 * Method:    steer
 * Signature: (JIF)V
 */
JNIEXPORT void JNICALL Java_com_jme3_bullet_objects_PhysicsVehicle_steer
  (JNIEnv *, jobject, jlong, jint, jfloat);

/*
 * Class:     com_jme3_bullet_objects_PhysicsVehicle
 * Method:    brake
 * Signature: (JIF)V
 */
JNIEXPORT void JNICALL Java_com_jme3_bullet_objects_PhysicsVehicle_brake
  (JNIEnv *, jobject, jlong, jint, jfloat);

/*
 * Class:     com_jme3_bullet_objects_PhysicsVehicle
 * Method:    getCurrentVehicleSpeedKmHour
 * Signature: (J)F
 */
JNIEXPORT jfloat JNICALL Java_com_jme3_bullet_objects_PhysicsVehicle_getCurrentVehicleSpeedKmHour
  (JNIEnv *, jobject, jlong);

/*
 * Class:     com_jme3_bullet_objects_PhysicsVehicle
 * Method:    getForwardVector
 * Signature: (JLcom/jme3/math/Vector3f;)V
 */
JNIEXPORT void JNICALL Java_com_jme3_bullet_objects_PhysicsVehicle_getForwardVector
  (JNIEnv *, jobject, jlong, jobject);

/*
 * Class:     com_jme3_bullet_objects_PhysicsVehicle
 * Method:    finalizeNative
 * Signature: (JJ)V
 */
JNIEXPORT void JNICALL Java_com_jme3_bullet_objects_PhysicsVehicle_finalizeNative
  (JNIEnv *, jobject, jlong, jlong);

#ifdef __cplusplus
}
#endif
#endif
