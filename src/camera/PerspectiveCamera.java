/*
 * Written for Cornell CS 5625 (Interactive Computer Graphics).
 *  Copyright (c) 2015, Department of Computer Science, Cornell University.
 *
 *  This code repository has been authored collectively by:
 *  Ivaylo Boyadzhiev (iib2), John DeCorato (jd537), Asher Dunn (ad488),
 *  Pramook Khungurn (pk395), and Sean Ryan (ser99)
 */

package camera;

import util.VectorMathUtil;

import javax_.vecmath.Matrix4f;
import javax_.vecmath.Point3f;
import javax_.vecmath.Vector2f;
import javax_.vecmath.Vector3f;

public class PerspectiveCamera extends Camera {
    private static final float DEFAULT_FOVY = 45;
    public float fovy;
    public float dollyFactor = 6;

    public PerspectiveCamera(float newNear, float newFar) {
        this(newNear, newFar, DEFAULT_FOVY);
    }

    public PerspectiveCamera(float newNear, float newFar, float newFovy) {
        super(newNear, newFar);
        fovy = newFovy;
    }

    public PerspectiveCamera(Point3f eyePos, Point3f target, Vector3f up, float newNear, float newFar, float newFovy) {
        super(eyePos, target, up, newNear, newFar);
        fovy = newFovy;
    }

    public PerspectiveCamera(PerspectiveCamera copy) {
        super(new Point3f(copy.getEye()), new Point3f(copy.getTarget()), new Vector3f(copy.getUp()), copy.near, copy.far);
        fovy = copy.fovy;
    }

    /**
     * Perform an orbit move of this camera
     */
    public void orbit(Vector2f mouseDelta) {
        Vector3f negGaze = new Vector3f(eye);
        negGaze.sub(target);
        float dist = negGaze.length();
        negGaze.normalize();

        float azimuth = (float) Math.atan2(negGaze.x, negGaze.z);
        float elevation = (float) Math.atan2(negGaze.y, Math.sqrt(negGaze.x * negGaze.x + negGaze.z * negGaze.z));
        azimuth = (azimuth - mouseDelta.x) % (float) (2 * Math.PI);
        elevation = (float) Math.max(-Math.PI * 0.495, Math.min(Math.PI * 0.495, (elevation - mouseDelta.y)));

        negGaze.set((float) (Math.sin(azimuth) * Math.cos(elevation)),
                (float) Math.sin(elevation),
                (float) (Math.cos(azimuth) * Math.cos(elevation)));
        negGaze.normalize();

        eye.scaleAdd(dist, negGaze, target);

        updateFrame();
    }

    /**
     * Creates the camera from the current camera parameters
     */
    public void updateFrame() {
        negGaze.set(eye);
        negGaze.sub(target);
        negGaze.normalize();

        up.normalize();
        right.cross(VERTICAL, negGaze);
        right.normalize();
        up.cross(negGaze, right);
    }

    /**
     * Returns the height of the viewing frustum, evaluated at the target point.
     */
    public float getHeight() {
        float dist = eye.distance(target);
        return (float) (Math.tan(Math.toRadians(fovy / 2.0)) * dist);
    }

    public void getProjectionMatrix(Matrix4f M) {
        VectorMathUtil.makeProjectionMatrix(M, fovy, aspect, near, far);
    }

    /**
     * Zoom the camera to distance d.
     */
    public void zoom(float d) {
        dolly(-d);
        updateFrame();
    }

    /**
     * Dolly this camera
     */
    public Vector3f dolly(float d) {
        Vector3f gaze = new Vector3f(target);
        gaze.sub(eye);
        double dist = gaze.length();
        gaze.normalize();
        d *= dollyFactor;

        if (dist + d > 0.01) {
            eye.scaleAdd(-d, gaze, eye);
        }

        gaze.scale(-d);

        return gaze;
    }
}
