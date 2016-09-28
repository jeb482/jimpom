package sim;

import javax_.vecmath.Vector3d;

import snapshot.Particle;
import snapshot.Snapshot;

public class VelocitySimulator {
	public VelocitySimulator(){}
	
	public Snapshot simulate(Snapshot snapshot1, double t) {
		System.out.println(t);
		Snapshot snapshot2 = new Snapshot(snapshot1, t);
		Vector3d displacement = new Vector3d(); 
		for (Particle p : snapshot2.part) {
			displacement.scale(t-snapshot1.t, p.vel);
			p.pos.add(displacement);
		}
		return snapshot2;
	}
}
