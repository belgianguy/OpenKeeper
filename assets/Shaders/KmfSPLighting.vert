// SPDX-License-Identifier: BSD-3-Clause
// Derived from jMonkeyEngine 3.9 SPLighting.vert.
#import "Common/ShaderLib/GLSLCompat.glsllib"
#import "Common/ShaderLib/Instancing.glsllib"
#import "Common/ShaderLib/Skinning.glsllib"
#import "Common/ShaderLib/Lighting.glsllib"
#import "Common/ShaderLib/MorphAnim.glsllib"

#ifdef VERTEX_LIGHTING
    #import "Common/ShaderLib/BlinnPhongLighting.glsllib"
#endif

#ifdef USE_FOG
varying float fog_distance;
uniform vec3 g_CameraPosition;
#endif

uniform vec4 m_Ambient;
uniform vec4 m_Diffuse;
uniform vec4 m_Specular;
uniform float m_Shininess;

#ifdef VERTEX_LIGHTING
uniform vec4 g_LightData[NB_LIGHTS];
#endif
uniform vec4 g_AmbientLightColor;
varying vec2 texCoord;

#ifdef SEPARATE_TEXCOORD
varying vec2 texCoord2;
attribute vec2 inTexCoord2;
#endif

varying vec3 AmbientSum;
varying vec4 DiffuseSum;
varying vec3 SpecularSum;

attribute vec3 inPosition;
attribute vec2 inTexCoord;
attribute vec3 inNormal;

#ifdef VERTEX_COLOR
attribute vec4 inColor;
#endif

#ifndef VERTEX_LIGHTING
varying vec3 vNormal;
varying vec3 vPos;
    #ifdef NORMALMAP
    attribute vec4 inTangent;
    varying vec4 vTangent;
    #endif
#endif

#ifdef USE_REFLECTION
attribute vec3 inTexCoord3;
varying vec2 dkiiEnvCoord;
#endif

void main() {
    vec4 modelSpacePos = vec4(inPosition, 1.0);
    vec3 modelSpaceNorm = inNormal;

    #if defined(NORMALMAP) && !defined(VERTEX_LIGHTING)
    vec3 modelSpaceTan = inTangent.xyz;
    #endif

    #ifdef NUM_MORPH_TARGETS
        #if defined(NORMALMAP) && !defined(VERTEX_LIGHTING)
        Morph_Compute(modelSpacePos, modelSpaceNorm, modelSpaceTan);
        #else
        Morph_Compute(modelSpacePos, modelSpaceNorm);
        #endif
    #endif

    #ifdef NUM_BONES
        #if defined(NORMALMAP) && !defined(VERTEX_LIGHTING)
        Skinning_Compute(modelSpacePos, modelSpaceNorm, modelSpaceTan);
        #else
        Skinning_Compute(modelSpacePos, modelSpaceNorm);
        #endif
    #endif

    gl_Position = TransformWorldViewProjection(modelSpacePos);
    texCoord = inTexCoord;
    #ifdef SEPARATE_TEXCOORD
    texCoord2 = inTexCoord2;
    #endif

    vec3 wvPosition = TransformWorldView(modelSpacePos).xyz;
    vec3 wvNormal = normalize(TransformNormal(modelSpaceNorm));
    vec3 viewDir = normalize(-wvPosition);

    #if defined(NORMALMAP) && !defined(VERTEX_LIGHTING)
    vTangent = vec4(TransformNormal(modelSpaceTan).xyz, inTangent.w);
    vNormal = wvNormal;
    vPos = wvPosition;
    #elif !defined(VERTEX_LIGHTING)
    vNormal = wvNormal;
    vPos = wvPosition;
    #endif

    #ifdef MATERIAL_COLORS
    AmbientSum = m_Ambient.rgb * g_AmbientLightColor.rgb;
    SpecularSum = m_Specular.rgb;
    DiffuseSum = m_Diffuse;
    #else
    AmbientSum = g_AmbientLightColor.rgb;
    SpecularSum = vec3(0.0);
    DiffuseSum = vec4(1.0);
    #endif

    #ifdef VERTEX_COLOR
    AmbientSum *= inColor.rgb;
    DiffuseSum *= inColor;
    #endif

    #ifdef VERTEX_LIGHTING
    vec3 diffuseAccum = vec3(0.0);
    vec3 specularAccum = vec3(0.0);
    vec4 diffuseColor;
    vec3 specularColor;
    for (int i = 0; i < NB_LIGHTS; i += 3) {
        vec4 lightColor = g_LightData[i];
        vec4 lightData1 = g_LightData[i + 1];
        #ifdef MATERIAL_COLORS
        diffuseColor = m_Diffuse * vec4(lightColor.rgb, 1.0);
        specularColor = m_Specular.rgb * lightColor.rgb;
        #else
        diffuseColor = vec4(lightColor.rgb, 1.0);
        specularColor = vec3(0.0);
        #endif

        vec4 lightDir;
        vec3 lightVec;
        lightComputeDir(wvPosition, lightColor.w, lightData1, lightDir, lightVec);
        float spotFallOff = 1.0;
        #if __VERSION__ >= 110
        if (lightColor.w > 1.0) {
        #endif
            spotFallOff = computeSpotFalloff(g_LightData[i + 2], lightVec);
        #if __VERSION__ >= 110
        }
        #endif

        vec2 light = computeLighting(wvNormal, viewDir, lightDir.xyz,
                lightDir.w * spotFallOff, m_Shininess);
        #ifdef COLORRAMP
        diffuseAccum += texture2D(m_ColorRamp, vec2(light.x, 0.0)).rgb * diffuseColor.rgb;
        specularAccum += texture2D(m_ColorRamp, vec2(light.y, 0.0)).rgb * specularColor;
        #else
        diffuseAccum += light.x * diffuseColor.rgb;
        specularAccum += light.y * specularColor;
        #endif
    }
    DiffuseSum.rgb *= diffuseAccum;
    SpecularSum.rgb *= specularAccum;
    #endif

    #ifdef USE_REFLECTION
    // DKII 0x5826c6..0x58276c: build the two environment coordinates
    // per vertex from the camera-space direction, normal, and the vertex's
    // radial X coordinate relative to the KMF mesh centre.
    vec3 centreView = TransformWorldView(vec4(inTexCoord3, 1.0)).xyz;
    float radialX = wvPosition.x - centreView.x;
    vec3 cameraToVertex = normalize(wvPosition);
    dkiiEnvCoord = vec2(
            radialX * cameraToVertex.x - wvNormal.x * cameraToVertex.z,
            radialX * cameraToVertex.y - wvNormal.y * cameraToVertex.z) * 0.5 + 0.5;
    #endif

    #ifdef USE_FOG
    fog_distance = distance(g_CameraPosition, TransformWorld(modelSpacePos).xyz);
    #endif
}
