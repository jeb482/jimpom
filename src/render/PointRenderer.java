package render;

import javax_.vecmath.Matrix4f;

import org.lwjgl.*;
import org.lwjgl.glfw.*;
import org.lwjgl.opengl.*;

import static org.lwjgl.glfw.Callbacks.*;
import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.system.MemoryUtil.*;
import camera.Camera;
import snapshot.Snapshot;
import viewer.GLProgram;

public class PointRenderer {
	GLProgram program;
	private int VAO;
	
	public PointRenderer() {
		try {
			program = GLProgram.quickCreate("./src/shaders/particles.vert", "./src/shaders/particles.frag");
		} catch (Exception e) {
			System.err.println(e);
			System.exit(1);
		}
		
	}
	
	
	public void render(Snapshot snapshot, Camera camera, long window) {
		// Black where no particles
		glClearColor(0.0f, 0.0f, 0.0f, 1.0f);		
		glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
		
		program.use();
		
		// Get Cam Data
		Matrix4f view = new Matrix4f();
		camera.getViewMatrix(view);
		
		Matrix4f proj = new Matrix4f();
		camera.getProjectionMatrix(proj);
		
		Matrix4f mvp = new Matrix4f();
		mvp.mul(proj, view);
		
		float[] mvpData = new float[16];
		for (int i=0; i<4; i++) {
			for (int j=0; j<4; j++) {
				mvpData[4*i+j] = mvp.getElement(j, i);
			}
		}
		
		// Not sure what you do.
		VAO = GL30.glGenVertexArrays();
		GL30.glBindVertexArray(VAO);
		
		int buffer = GL15.glGenBuffers();
		GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, buffer);
		GL15.glBufferData(GL15.GL_ARRAY_BUFFER, snapshot.getPosArray(), GL15.GL_DYNAMIC_DRAW);
		
		program.use();
		try {
			GL20.glUniformMatrix4fv(program.getUniform("mModelViewProjection"), false, mvpData);
			GL20.glUniform3f(program.getUniform("fColor"), 1.0f, 1.0f, 1.0f);
		} catch (Exception e) {
			System.err.println(e);
			System.exit(1);
		}
		
		GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, 0);
		GL30.glBindVertexArray(0);
		
		glDrawArrays(GL_POINTS, 0, snapshot.part.length);//snapshot.getPosArray());
		
		GLProgram.unuse();
		GL30.glBindVertexArray(0);
		
		glfwSwapBuffers(window);
	}
}
