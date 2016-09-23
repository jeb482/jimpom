/*
 *
 *  * Written for Cornell CS 5625 (Interactive Computer Graphics).
 *  * Copyright (c) 2015, Department of Computer Science, Cornell University.
 *  *
 *  * This code repository has been authored collectively by:
 *  * Ivaylo Boyadzhiev (iib2), John DeCorato (jd537), Asher Dunn (ad488),
 *  * Pramook Khungurn (pk395), Steve Marschner (srm2), and Sean Ryan (ser99)
 *
 */

package util;

import javax_.vecmath.*;

public class VectorMathUtil {
    public static final Matrix4f IDENTITY_MATRIX = new Matrix4f();

    static {
        IDENTITY_MATRIX.setIdentity();
    }

    /**
     * Create a projection matrix as used by OpenGL
     * @param fovy field of view in the Y direction in degrees
     * @param aspect aspect ratio
     * @param near near z cutoff
     * @param far far z cutoff
     * @return
     */
    public static Matrix4f createProjectionMatrix(float fovy, float aspect, float near, float far) {
        Matrix4f M = new Matrix4f();
        makeProjectionMatrix(M, fovy, aspect, near, far);
        return M;
    }

    public static void makeProjectionMatrix(Matrix4f M, float fovy, float aspect, float near, float far) {
        float f = (float)(1.0 / Math.tan(Math.toRadians(fovy / 2.0)));
        M.setZero();
        M.m00 = f / aspect;
        M.m11 = f;
        M.m22 = (far + near)/(near - far);
        M.m23 = (2*far*near) /(near - far);
        M.m32 = -1;
    }

    public static void makeOrthographicMatrix(Matrix4f M, float height, float aspect, float near, float far) {
        M.setZero();
        M.m00 = 2 / height / aspect;
        M.m11 = 2 / height;
        M.m22 = 2 / (near - far);
        M.m23 = (near + far) / (near - far);
        M.m33 = 1;
    }

    /*
     *  Create a viewport matrix that matches the OpenGL viewport transformation after the calls
     *    glViewport(left, bottom, width, height);
     *    glDepthRange(minDepth, maxDepth);  (normally 0, 1)
     */
    public static void makeViewportMatrix(Matrix4f M, int left, int bottom, int width, int height, 
    		float minDepth, float maxDepth) {
    	M.setIdentity();
    	M.m00 = width / 2.0f;
    	M.m03 = left + width / 2.0f;
    	M.m11 = height / 2.0f;
    	M.m13 = bottom + height / 2.0f;
    	M.m22 = (maxDepth - minDepth) / 2;
    	M.m23 = (maxDepth + minDepth) / 2;
    }

    public static Matrix4f createLookAtMatrix(float eyeX, float eyeY, float eyeZ,
                                              float atX, float atY, float atZ,
                                              float upX, float upY, float upZ) {
        Matrix4f M = new Matrix4f();
        makeLookAtMatrix(M, eyeX, eyeY, eyeZ, atX, atY, atZ, upX, upY, upZ);
        return M;
    }

    public static void makeLookAtMatrix(Matrix4f M, float eyeX, float eyeY, float eyeZ,
                                        float atX, float atY, float atZ,
                                        float upX, float upY, float upZ) {
        Vector3f z = new Vector3f(eyeX - atX, eyeY - atY, eyeZ - atZ);
        z.normalize();
        Vector3f y = new Vector3f(upX, upY, upZ);
        y.normalize();
        Vector3f x = new Vector3f();
        x.cross(y, z);
        x.normalize();
        y.cross(z, x);
        y.normalize();

        M.m00 = x.x;
        M.m10 = x.y;
        M.m20 = x.z;
        M.m30 = 0;

        M.m01 = y.x;
        M.m11 = y.y;
        M.m21 = y.z;
        M.m31 = 0;

        M.m02 = z.x;
        M.m12 = z.y;
        M.m22 = z.z;
        M.m32 = 0;

        M.m03 = eyeX;
        M.m13 = eyeY;
        M.m23 = eyeZ;
        M.m33 = 1;

        M.invert();
    }

    public static void coordinateSystem(Vector3f normal, Vector3f tangent, Vector3f binormal) {
        normal.normalize();

        float nx = Math.abs(normal.x);
        float ny = Math.abs(normal.y);
        float nz = Math.abs(normal.z);

        if (nx > ny && nx > nz) {
            tangent.set(-normal.y, normal.x, 0);
        } else if (ny > nx && ny > nz) {
            tangent.set(0, -normal.z, normal.y);
        } else {
            tangent.set(normal.z, 0, -normal.x);
        }
        tangent.normalize();

        binormal.cross(normal, tangent);
        binormal.normalize();
    }

    /**
     * Rotates the tuple (vector or point) by a quaternion.
     *
     * Just does `quat * tuple * inverse(quat)`.
     *
     * @param quat The quaternion to rotate by.
     * @param tuple The tuple to rotate. The rotation is done in-place; on
     *        output, `tuple` has been rotated by `quat`.
     */
    public static void rotateTuple(Quat4f quat, Tuple3f tuple)
    {
        if (tuple.x == 0.0f && tuple.y == 0.0f && tuple.z == 0.0f)
        {
            return;
        }

		/* Quat4f.mul() implicitly normalizes the result, so remember the length. */
        float length = (float)Math.sqrt(tuple.x * tuple.x + tuple.y * tuple.y + tuple.z * tuple.z);
        tuple.scale(1.0f / length);

		/* quat * tuple * inverse(quat) */
        Quat4f temp = new Quat4f(quat);
        temp.mul(new Quat4f(tuple.x, tuple.y, tuple.z, 0.0f));
        temp.mulInverse(quat);

        tuple.x = temp.x * length;
        tuple.y = temp.y * length;
        tuple.z = temp.z * length;
    }

    public static void set3x3Part(Matrix3f A, Matrix4f B) {
        A.m00 = B.m00;
        A.m01 = B.m01;
        A.m02 = B.m02;
        A.m10 = B.m10;
        A.m11 = B.m11;
        A.m12 = B.m12;
        A.m20 = B.m20;
        A.m21 = B.m21;
        A.m22 = B.m22;
    }
}
