package snapshot;

import javax_.vecmath.Point3d;
import javax_.vecmath.Vector3d;

public class Grid {
	Point3d origin;
	int xDivs;
	int yDivs;
	int zDivs;
	
	double h; // Width of grid
	double[][][] masses;
	double[][][][] momenta;
	
	public Grid(Point3d origin, double h, int xDivs, int yDivs, int zDivs) {
		this.origin = origin;
		this.h = h;
		this.xDivs = xDivs;
		this.yDivs = yDivs;
		this.zDivs = zDivs;
		masses = new double[xDivs+1][yDivs+1][zDivs+1];
		momenta = new double[xDivs+1][yDivs+1][zDivs+1][3];
	}

	// Please on't judge me for this code.
	public void gather(Particle p) {
		Point3d cellPos = new Point3d();
		cellPos.sub(p.pos, origin);
		cellPos.scale(1/h);
		Point3d gridPoint = new Point3d();
		double weight;
		
		// Actually do the gathering. This gonna error
		for (gridPoint.x = (Math.max(cellPos.x - 1, 0)); gridPoint.x < Math.min(xDivs, cellPos.x + 2); gridPoint.x++) {
			for (gridPoint.y = (Math.max(cellPos.y - 1, 0)); gridPoint.y < Math.min(yDivs, cellPos.y + 2); gridPoint.y++) {
				for (gridPoint.z = (Math.max(cellPos.z - 1, 0)); gridPoint.z < Math.min(zDivs, cellPos.z + 2); gridPoint.z++) {
					weight = cubicWeight(cellPos, gridPoint);
					masses[(int)gridPoint.x][(int)gridPoint.y][(int)gridPoint.z] += weight*p.mass;
					momenta[(int)gridPoint.x][(int)gridPoint.y][(int)gridPoint.z][0] += weight*p.momentum.x;
					momenta[(int)gridPoint.x][(int)gridPoint.y][(int)gridPoint.z][1] += weight*p.momentum.y;
					momenta[(int)gridPoint.x][(int)gridPoint.y][(int)gridPoint.z][2] += weight*p.momentum.z;
				}
			}
		}
		
		// Divide momentum/mass to get final velocity.
		for (int i = 0; i < xDivs; i++)
			for (int j = 0; j < yDivs; j++)
				for (int k = 0; k < zDivs; k++)
					for (int l = 0; l < 3; l++)
						momenta[i][j][k][l] /= masses[i][j][k]; 
						
					
					
		
		
	}
	
	public void scatter(Particle p) {
		
	}
	
	public double cubicWeight(Point3d p, Point3d q) {
		return cubicWeight(p.x-q.x)*cubicWeight(p.y-q.y)*cubicWeight(p.z-q.z);
	}
	
	public double cubicWeight(double x) {
		double absX = (x<0) ? -x : x;
		if (absX < 1)
			return 0.5*absX*absX*absX - absX*absX + 2.0/3;
		if (absX < 2)
			return (2 - absX)*(2 - absX)*(2 - absX)/6.0;
		return 0;
	}


	public static void main(String args[]) {
		Grid g = new Grid(new Point3d(0,0,0),2,3,3,3);
		Particle p = new Particle(new Point3d(3,3,3), new Vector3d(1,0,0), 5);
		System.out.println("Mass: " + p.mass);
		g.gather(p);
		double m = 0;

		for (int i = 0; i <= 3; i++) 
			for (int j = 0; j <= 3; j++)
				for (int k = 0; k <= 3; k++)
		m += g.masses[i][j][k];
		System.out.println(m);
		
	}
}

