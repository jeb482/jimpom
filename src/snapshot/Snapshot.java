package snapshot;

import java.util.Arrays;

public class Snapshot implements java.io.Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	/**
	 * The simulation time in seconds associated with this Snapshot.
	 */
	public final double t;
	/**
	 * The particles that are part of this Snapshot.
	 */
	public final Particle[] part;
	
	/**
	 * Creates a new Snapshot with a given number of Particles.
	 * Particles are default-constructed.
	 * 
	 * @param nParticles The number of Particles in the Snapshot.
	 * @param t The simulation time in seconds associated with the created Snapshot.
	 */
	public Snapshot(int nParticles, double t) {
		this.t = t;
		part = new Particle[nParticles];
		for (int i=0; i<nParticles; i++) {
			part[i] = new Particle();
		}
	}
	
	/**
	 * Creates a deep copy of another Snapshot.
	 * 
	 * @param ss The Snapshot from which to make a deep copy.
	 * @param t The simulation time in seconds associated with the created Snapshot.
	 */
	public Snapshot(Snapshot ss, double t) {
		this.t = t;
		part = Arrays.copyOf(ss.part, ss.part.length);
		for (int i=0; i<part.length; i++) {
			part[i] = part[i].clone();
		}
	}
	
	/**
	 * Gets an array corresponding to the positions of all particles in the scene.
	 * 
	 * @return
	 */
	public float[] getPosArray() {
		float[] array = new float[3*part.length];
		for (int i = 0; i < part.length; i++) {
			array[3*i  ] = (float) part[i].pos.x; 
			array[3*i+1] = (float) part[i].pos.y; 
			array[3*i+2] = (float) part[i].pos.z; 
		}
		return array;
	}

	public static void main(String[] args) {
		Snapshot s1 = new Snapshot(20, 0.0);
		Snapshot s2 = new Snapshot(s1, 1.0);
		
		s1.part[0].pos.x = 1.0;
		
		System.out.println("s1[0].pos: " + s1.part[0].pos);
		System.out.println("s2[0].pos: " + s2.part[0].pos);
	}
}
