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
import static org.lwjgl.glfw.GLFW.glfwSwapInterval;
import static org.lwjgl.glfw.GLFW.glfwTerminate;
import static org.lwjgl.glfw.GLFW.glfwWindowHint;
import static org.lwjgl.glfw.GLFW.glfwWindowShouldClose;
import static org.lwjgl.opengl.GL11.GL_FLOAT;
import static org.lwjgl.opengl.GL11.glClearColor;
import static org.lwjgl.system.MemoryUtil.NULL;
import javax_.vecmath.Vector2f;

import org.lwjgl.Version;
import org.lwjgl.glfw.GLFWErrorCallback;
import org.lwjgl.glfw.GLFWVidMode;
import org.lwjgl.opengl.GL;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;

import render.PointRenderer;
import sim.VelocitySimulator;
import snapshot.Snapshot;
import camera.Camera;
import camera.PerspectiveCamera;

public class Viewer {

	// The window handle
	private long window;
	
	// The scene camera
	private Camera camera = new PerspectiveCamera(0.01f, 100.0f);
	private PointRenderer renderer;
	private VelocitySimulator simulator;
	private GLProgram program;
	private int VAO;
	private Snapshot currentSnapshot;
	private long t0;
	
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
		
		t0 = System.currentTimeMillis();
		currentSnapshot = new Snapshot(5, 0);
		for (int i = 0; i < 5; i++) {
			currentSnapshot.part[i].pos.set(.5*i, .5*i, .5*i);
			currentSnapshot.part[i].vel.set(-.5*i, -.5*i, -.5*i);
		}
		renderer = new PointRenderer();
		simulator = new VelocitySimulator();
		
	}

	private void loop() {
		// Set the clear color
		glClearColor(1.0f, 0.0f, 0.0f, 0.0f);

		// Run the rendering loop until the user has attempted to close
		// the window or has pressed the ESCAPE key.
		while ( !glfwWindowShouldClose(window) ) {
			renderer.render(currentSnapshot, camera, window);
			currentSnapshot = simulator.simulate(currentSnapshot, (System.currentTimeMillis()-t0)/1000f);
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
