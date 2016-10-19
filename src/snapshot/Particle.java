package snapshot;

import javax_.vecmath.Point3d;
import javax_.vecmath.Vector3d;

public class Particle implements java.io.Serializable, Cloneable {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	public final Point3d pos = new Point3d();
	public final Vector3d vel = new Vector3d();
	public final Vector3d momentum = new Vector3d();
	public double mass;

	
	public Particle() {
		// TODO
	}
	
	public Particle(Point3d p, Vector3d v, double m) {
		pos.set(p);
		vel.set(v);
		mass = m;
		momentum.scale(m, v);	
	}
	
	public Particle(Particle p) {
		pos.set(p.pos);
		vel.set(p.vel);
	}
	
	public Particle clone() {
		return new Particle(this);
	}
}
