/*
 *    Copyright 2016 Anand Muralidhar
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

#include <jni.h>
#include "simpleARClass.h"

#ifdef __cplusplus
extern "C" {
#endif

extern SimpleARClass *gSimpleARObject;

JNIEXPORT void JNICALL
Java_com_anandmuralidhar_simplearandroid_SimpleARActivity_GyroscopeUpdated(JNIEnv *env,
                                                                           jobject instance,
                                                                           jfloatArray values_) {
    jfloat *values = env->GetFloatArrayElements(values_, NULL);

    if (gSimpleARObject == NULL) {
        return;
    }
    gSimpleARObject->UpdateUserRotation(glm::vec3(values[0], values[1], values[2]));

    env->ReleaseFloatArrayElements(values_, values, 0);
}

JNIEXPORT void JNICALL
Java_com_anandmuralidhar_simplearandroid_SimpleARActivity_LocationUpdate(JNIEnv *env,
                                                                         jobject instance,
                                                                         jfloat xDifference,
                                                                         jfloat zDifference) {

    if (gSimpleARObject == NULL) {
        return;
    }
    gSimpleARObject->UpdateUserLocation(glm::vec3(xDifference, .0, zDifference));

}

JNIEXPORT void JNICALL
Java_com_anandmuralidhar_simplearandroid_MyGLRenderer_DrawFrameNative(JNIEnv *env,
                                                                      jobject instance) {

    if (gSimpleARObject == NULL) {
        return;
    }
    gSimpleARObject->Render();

}

JNIEXPORT void JNICALL
Java_com_anandmuralidhar_simplearandroid_MyGLRenderer_SurfaceCreatedNative(JNIEnv *env,
                                                                           jobject instance) {

    if (gSimpleARObject == NULL) {
        return;
    }
    gSimpleARObject->PerformGLInits();

}

JNIEXPORT void JNICALL
Java_com_anandmuralidhar_simplearandroid_MyGLRenderer_SurfaceChangedNative(JNIEnv *env,
                                                                           jobject instance,
                                                                           jint width,
                                                                           jint height) {

    if (gSimpleARObject == NULL) {
        return;
    }
    gSimpleARObject->SetViewport(width, height);

}

#ifdef __cplusplus
}
#endif

