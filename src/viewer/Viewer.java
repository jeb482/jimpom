package viewer;

import static org.lwjgl.glfw.Callbacks.glfwFreeCallbacks;
import static org.lwjgl.glfw.GLFW.GLFW_FALSE;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_ESCAPE;
import static org.lwjgl.glfw.GLFW.GLFW_RELEASE;
import static org.lwjgl.glfw.GLFW.GLFW_RESIZABLE;
import static org.lwjgl.glfw.GLFW.GLFW_TRUE;
import static org.lwjgl.glfw.GLFW.GLFW_VISIBLE;
import static org.lwjgl.glfw.GLFW.glfwCreateWindow;
import static org.lwjgl.glfw.GLFW.glfwDefaultWindowHints;
import static org.lwjgl.glfw.GLFW.glfwDestroyWindow;
import static org.lwjgl.glfw.GLFW.glfwGetPrimaryMonitor;
import static org.lwjgl.glfw.GLFW.glfwGetVideoMode;
import static org.lwjgl.glfw.GLFW.glfwInit;
import static org.lwjgl.glfw.GLFW.glfwMakeContextCurrent;
import static org.lwjgl.glfw.GLFW.glfwPollEvents;
import static org.lwjgl.glfw.GLFW.glfwSetErrorCallback;
import static org.lwjgl.glfw.GLFW.glfwSetKeyCallback;
import static org.lwjgl.glfw.GLFW.glfwSetScrollCallback;
import static org.lwjgl.glfw.GLFW.glfwSetWindowPos;
import static org.lwjgl.glfw.GLFW.glfwSetWindowShouldClose;
import static org.lwjgl.glfw.GLFW.glfwShowWindow;
import static org.lwjgl.glfw.GLFW.glfwSwapBuffers;
import static org.lwjgl.glfw.GLFW.glfwSwapInterval;
import static org.lwjgl.glfw.GLFW.glfwTerminate;
import static org.lwjgl.glfw.GLFW.glfwWindowHint;
import static org.lwjgl.glfw.GLFW.glfwWindowShouldClose;
import static org.lwjgl.opengl.GL11.GL_COLOR_BUFFER_BIT;
import static org.lwjgl.opengl.GL11.GL_DEPTH_BUFFER_BIT;
import static org.lwjgl.opengl.GL11.GL_FLOAT;
import static org.lwjgl.opengl.GL11.GL_TRIANGLES;
import static org.lwjgl.opengl.GL11.glClear;
import static org.lwjgl.opengl.GL11.glClearColor;
import static org.lwjgl.opengl.GL11.glDrawArrays;
import static org.lwjgl.system.MemoryUtil.NULL;

import javax_.vecmath.Matrix4f;
import javax_.vecmath.Vector2f;

import org.lwjgl.Version;
import org.lwjgl.glfw.GLFWErrorCallback;
import org.lwjgl.glfw.GLFWVidMode;
import org.lwjgl.opengl.GL;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;

import render.PointRenderer;
import snapshot.Snapshot;
import camera.Camera;
import camera.PerspectiveCamera;

public class Viewer {

	// The window handle
	private long window;
	
	// The scene camera
	private Camera camera = new PerspectiveCamera(0.01f, 100.0f);
	private PointRenderer renderer;
	private GLProgram program;
	private int VAO;
	private Snapshot lazySnapshot;
	
	
	public void run() {
		System.out.println("Hello LWJGL " + Version.getVersion() + "!");

		try {
			init();
			loop();
			
			// Free the window callbacks and destroy the window
			glfwFreeCallbacks(window);
			glfwDestroyWindow(window);
		} finally {
			// Terminate GLFW and free the error callback
			glfwTerminate();
			glfwSetErrorCallback(null).free();
		}
	}

	private void init() {
		// Setup an error callback. The default implementation
		// will print the error message in System.err.
		GLFWErrorCallback.createPrint(System.err).set();

		// Initialize GLFW. Most GLFW functions will not work before doing this.
		if (!glfwInit())
			throw new IllegalStateException("Unable to initialize GLFW");

		// Configure our window
		glfwDefaultWindowHints(); // optional, the current window hints are already the default
		glfwWindowHint(GLFW_VISIBLE, GLFW_FALSE); // the window will stay hidden after creation
		glfwWindowHint(GLFW_RESIZABLE, GLFW_TRUE); // the window will be resizable

		int WIDTH = 1280;
		int HEIGHT = 720;

		// Create the window
		window = glfwCreateWindow(WIDTH, HEIGHT, "Hello World!", NULL, NULL);
		if (window == NULL)
			throw new RuntimeException("Failed to create the GLFW window");

		// Setup a key callback. It will be called every time a key is pressed, repeated or released.
		glfwSetKeyCallback(window, (window, key, scancode, action, mods) -> {
			if (key == GLFW_KEY_ESCAPE && action == GLFW_RELEASE)
				glfwSetWindowShouldClose(window, true); // We will detect this in our rendering loop
		});
	
		glfwSetScrollCallback(window, (window, xoffset, yoffset) -> {
			camera.orbit(new Vector2f((float)xoffset, (float)yoffset));
		});
		
		// Get the resolution of the primary monitor
		GLFWVidMode vidmode = glfwGetVideoMode(glfwGetPrimaryMonitor());
		// Center our window
		glfwSetWindowPos(
				window,
				(vidmode.width() - WIDTH) / 2,
				(vidmode.height() - HEIGHT) / 2
				);

		// Make the OpenGL context current
		glfwMakeContextCurrent(window);
		// Enable v-sync
		glfwSwapInterval(1);
		
		// This line is critical for LWJGL's interoperation with GLFW's
		// OpenGL context, or any context that is managed externally.
		// LWJGL detects the context that is current in the current thread,
		// creates the GLCapabilities instance and makes the OpenGL
		// bindings available for use.
		GL.createCapabilities();

		try {
			program = GLProgram.quickCreate("./src/shaders/particles.vert", "./src/shaders/particles.frag");
		} catch (Exception e) {
			System.err.println(e);
			System.exit(1);
		}
		
		VAO = GL30.glGenVertexArrays();
		GL30.glBindVertexArray(VAO);
		
		int buffer = GL15.glGenBuffers();
		float[] tri = {
				0.0f, 0.0f, 0.0f,
				0.5f, 0.0f, 0.0f,
				0.0f, 1.0f, 0.0f
				};
		
		GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, buffer);
		GL15.glBufferData(GL15.GL_ARRAY_BUFFER, tri, GL15.GL_STATIC_DRAW);
		try {
			int vVertex = program.getAttribute("vVertex");
			GL20.glEnableVertexAttribArray(vVertex);
			GL20.glVertexAttribPointer(vVertex, 3, GL_FLOAT, false, 0, 0);
		} catch (Exception e) {
			System.err.println(e);
			System.exit(1);
		}
		
		GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, 0);
		GL30.glBindVertexArray(0);
		
		// Make the window visible
		glfwShowWindow(window);
		
		lazySnapshot = new Snapshot(5,1);
		for (int i = 0; i < 5; i++) {
			lazySnapshot.part[i].pos.set(.5*i, .5*i, .5*i);
		}
		renderer = new PointRenderer();
		
	}

	private void loop() {
		// Set the clear color
		glClearColor(1.0f, 0.0f, 0.0f, 0.0f);

		// Run the rendering loop until the user has attempted to close
		// the window or has pressed the ESCAPE key.
		while ( !glfwWindowShouldClose(window) ) {
			renderer.render(lazySnapshot, camera, window);
				
			//			glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT); // clear the framebuffer
//			
//			Matrix4f view = new Matrix4f();
//			camera.getViewMatrix(view);
//			
//			Matrix4f proj = new Matrix4f();
//			camera.getProjectionMatrix(proj);
//			
//			Matrix4f mvp = new Matrix4f();
//			mvp.mul(proj, view);
//			
//			float[] mvpData = new float[16];
//			for (int i=0; i<4; i++) {
//				for (int j=0; j<4; j++) {
//					mvpData[4*i+j] = mvp.getElement(j, i);
//				}
//			}
//			
//			GL30.glBindVertexArray(VAO);
//			program.use();
//			try {
//				GL20.glUniformMatrix4fv(program.getUniform("mModelViewProjection"), false, mvpData);
//				GL20.glUniform3f(program.getUniform("fColor"), 0.0f, 1.0f, 0.0f);
//			} catch (Exception e) {
//				System.err.println(e);
//				System.exit(1);
//			}
//			
//			glDrawArrays(GL_TRIANGLES, 0, 3);
//			
//			GLProgram.unuse();
//			GL30.glBindVertexArray(0);
//
//			glfwSwapBuffers(window); // swap the color buffers
//
//			// Poll for window events. The key callback above will only be
//			// invoked during this call.
			glfwPollEvents();
		}
	}
	
	public Viewer() {
		// TODO Auto-generated constructor stub
	}

	public static void main(String[] args) {
		new Viewer().run();
	}
}
