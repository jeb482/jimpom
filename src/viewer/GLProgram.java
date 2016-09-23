package viewer;

import static org.lwjgl.opengl.GL20.glAttachShader;
import static org.lwjgl.opengl.GL20.glCompileShader;
import static org.lwjgl.opengl.GL20.glCreateProgram;
import static org.lwjgl.opengl.GL20.glCreateShader;
import static org.lwjgl.opengl.GL20.glDeleteProgram;
import static org.lwjgl.opengl.GL20.glDeleteShader;
import static org.lwjgl.opengl.GL20.glDetachShader;
import static org.lwjgl.opengl.GL20.glGetAttribLocation;
import static org.lwjgl.opengl.GL20.glGetProgrami;
import static org.lwjgl.opengl.GL20.glGetShaderi;
import static org.lwjgl.opengl.GL20.glGetUniformLocation;
import static org.lwjgl.opengl.GL20.glLinkProgram;
import static org.lwjgl.opengl.GL20.glShaderSource;
import static org.lwjgl.opengl.GL20.glUseProgram;
import static org.lwjgl.opengl.GL20.glValidateProgram;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;

import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL32;

/**
 * Manages and simplifies the OpenGL interface for creating and using GLSL shader programs.
 * @author Cristian Zaloj, Eston Schweickart
 *
 */
public class GLProgram {
    /**
     * Lists the types of supported shaders.
     */
    public enum ShaderType {
    	VertexShader,
    	FragmentShader,
    	GeomShader,
    }
    
    /**
     * An Exception associated with a shader program.
     */
    public static class ShaderException extends Exception {
		private static final long serialVersionUID = 1L;
		public ShaderException() {
			super();
		}
		public ShaderException(String s) {
			super(s);
		}
    }
    
    private static GLProgram programInUse;
    
    private int id, idVS, idFS, idGS;
    private boolean isLinked;

    private final HashMap<String, Integer> uniforms = new HashMap<>();
    private final HashMap<String, Integer> attributes = new HashMap<>();
    
    /**
     * Get the GLProgram that is currently being used.
     * @return The GLProgram currently in use; if no program is in use, returns null.
     */
    public static GLProgram getProgramInUse() {
    	return programInUse;
    }
    
    /**
     * Stop using all shading programs
     */
    public static void unuse() {
        programInUse = null;
        glUseProgram(0);
    }
    
    /**
     * Creates a manager for an OpenGL Shader Program.
     * @param init If true, this will tell OpenGL to create a program; otherwise,
     * you MUST call init() to create the program.
     */
    public GLProgram(boolean init) {
        id = 0;
        idFS = 0;
        idVS = 0;
        idGS = 0;
        
        if(init) init();
        isLinked = false;
    }
    
    /**
     * Creates a manager for an OpenGL Shader Program
     * @note This does not actually create the program; you must call init() to do so
     */
    public GLProgram() {
    	this(false);
    }

    /**
     * Releases GPU resources associated with this program. If the program is
     * in use when this is called, the program is unused
     */
    public void dispose() {
        if(!getIsCreated()) return;

        if(getIsInUse()) unuse();
        if(idVS != 0) {
        	glDetachShader(id, idVS);
            glDeleteShader(idVS);
            idVS = 0;
        }
        if(idFS != 0) {
        	glDetachShader(id, idFS);
            glDeleteShader(idFS);
            idFS = 0;
        }
        if(idGS != 0) {
        	glDetachShader(id, idGS);
        	glDeleteShader(idGS);
        	idGS = 0;
        }
        
        glDeleteProgram(id);

        // Reset Internal State
        id = 0;
        isLinked = false;
    }

    /**
     * Get the OpenGL id (handle) associated with this program.
     * @return The OpenGL id. If the program has not been created, this method returns 0. 
     */
    public int getID() {
    	return id;
    }

    /**
     * Returns true if the program has been created, i.e., init() has been called.
     */
    public boolean getIsCreated() {
        return id != 0;
    }
    /**
     * Returns true if the program has been linked, i.e., link() has been called.
     */
    public boolean getIsLinked() {
    	return isLinked;
    }
    /**
     * Returns true if this program is currently being used.
     */
    public boolean getIsInUse() {
        return programInUse == this;
    }

    /**
     * Tells OpenGL to create the shader program.
     */
    public void init() {
        if(getIsCreated()) return;
        isLinked = false;
        id = glCreateProgram();
    }
    
    /**
     * Adds a shader to the program.
     * @param st The type of shader to add.
     * @param src The shader's source.
     * @throws ShaderException Thrown if the program is already linked, multiple shaders of the same
     * type are added, or the shader does not compile.
     */
    public void addShader(ShaderType st, CharSequence src) throws ShaderException {
        if(getIsLinked()) throw new ShaderException("Program Is Already Linked");

        int idS;
        switch(st) {
            case VertexShader:
                if(idVS != 0)
                    throw new ShaderException("Attempting To Add Another Vertex Shader To Program");
                idS = glCreateShader(GL20.GL_VERTEX_SHADER);
                break;
            case FragmentShader:
                if(idFS != 0)
                    throw new ShaderException("Attempting To Add Another Fragment Shader To Program");
                idS = glCreateShader(GL20.GL_FRAGMENT_SHADER);
                break;
            case GeomShader:
            	if(idGS != 0)
            		throw new ShaderException("Attempting To Add Another Geometry Shader To Program");
            	idS = glCreateShader(GL32.GL_GEOMETRY_SHADER);
            	break;
            default:
                throw new ShaderException("Shader Type Is Not Supported");
        }

        glShaderSource(idS, src);
        glCompileShader(idS);

        // Check Status
        int status = glGetShaderi(idS, GL20.GL_COMPILE_STATUS);
        if(status != 1) {
        	System.err.println("Shader Compilation Error!");
        	System.err.println(GL20.glGetShaderInfoLog(idS));
        	glDeleteShader(idS);
            throw new ShaderException("Shader Had Compilation Errors");
        }

        glAttachShader(id, idS);

        switch(st) {
            case VertexShader: idVS = idS; break;
            case FragmentShader: idFS = idS; break;
            case GeomShader: idGS = idS; break;
        }
    }
    
    /**
     * Adds a shader to the program.
     * @param st The type of shader to add.
     * @param path The path to the shader file.
     * @throws IOException Thrown if the file is not found or cannot be opened.
     * @throws ShaderException Thrown if the program is already linked, multiple shaders of the same
     * type are added, or the shader does not compile.
     */
    public void addShaderFile(ShaderType st, String path) throws IOException, ShaderException {
    	byte[] encoded = Files.readAllBytes(Paths.get(path));
    	addShader(st, new String(encoded, java.nio.charset.StandardCharsets.UTF_8));
    }

    /**
     * Link the program.
     * @throws ShaderException Thrown if the program has not been created, or if linking failed,
     *  e.g., if the appropriate shaders have not been added.
     */
    public void link() throws ShaderException {
    	if(!getIsCreated()) throw new ShaderException("Tried to link a program that has not been created");
        if(isLinked) return;

        glLinkProgram(id);
        glValidateProgram(id);

        int status = glGetProgrami(id, GL20.GL_LINK_STATUS);
        isLinked = status == GL11.GL_TRUE;
        if(!isLinked) {
        	System.err.println("Program link error!");
        	System.err.println(GL20.glGetProgramInfoLog(id));
        	throw new ShaderException("Program Link Error");
        }
    }

    /**
     * Get the handle to an attribute used in this program. Caches the handle after the first query.
     * @param name The name of the attribute.
     * @return The handle of the attribute.
     * @throws ShaderException Thrown if the attribute does not exist or the shader has not been linked.
     */
    public int getAttribute(String name) throws ShaderException {
    	if (!getIsLinked()) throw new ShaderException("Shader program has not been linked");
    	Integer i = attributes.get(name);
    	if (i == null) {
    		int l = glGetAttribLocation(id, name);
    		if (l == -1)
    			throw new ShaderException("Shader attribute \"" + name + "\" does not exist");
    		attributes.put(name, l);
    		return l;
    	} else {
    		return i;
    	}
    }
    
    /**
     * Get the handle to a uniform used in this program. Caches the handle after the first query.
     * @param name The name of the uniform.
     * @return The handle of the uniform.
     * @throws ShaderException Thrown if the uniform does not exist or the shader has not been linked.
     */
    public int getUniform(String name) throws ShaderException {
    	if (!getIsLinked()) throw new ShaderException("Shader program has not been linked");
    	Integer i = uniforms.get(name);
    	if (i == null) {
    		int l = glGetUniformLocation(id, name);
    		if (l == -1)
    			throw new ShaderException("Shader uniform \"" + name + "\" does not exist");
    		uniforms.put(name, l);
    	}
    	return uniforms.get(name);
    }
    
    /**
     * Get the handle to an attribute used in this program. Caches the handle after the first query.
     * @warning This method fails (nearly) silently. Use the getAttribute() method instead unless
     * you have a very good reason not to.
     * @param name The name of the attribute.
     * @return The handle of the attribute, or -1 if the attribute doesn't exist or if the shader
     * has not been linked.
     */
    public int getAttributeNoExcept(String name) {
    	if (!getIsCreated() || !getIsLinked()) return -1;
    	Integer i = attributes.get(name);
    	if (i == null) {
    		return glGetAttribLocation(id, name);
    	}
    	return i;
    }
    
    /**
     * Get the handle to a uniform used in this program. Caches the handle after the first query.
     * @warning This method fails (nearly) silently. Use the getUniform() method instead unless
     * you have a very good reason not to.
     * @param name The name of the uniform.
     * @return The handle of the uniform, or -1 if the attribute doesn't exist or if the shader
     * has not been linked.
     */
    public int getUniformNoExcept(String name) {
    	if (!getIsCreated() || !getIsLinked()) return -1;
    	Integer i = uniforms.get(name);
    	if (i == null) {
    		return glGetUniformLocation(id, name);
    	}
    	return i;
    }
    
    /**
     * Tell OpenGL to use this program.
     * @return True if the program is in use after this call; false otherwise (e.g., the program
     * has not been created or linked).
     */
    public boolean use() {
        if(!getIsCreated() || !getIsLinked()) return false;
        programInUse = this;
        glUseProgram(id);
        return true;
    }

    /**
     * Create a GLProgram from shader source files.
     * @param vsFile The path to the vertex shader.
     * @param fsFile The path to the fragment shader.
     * @return The created GLProgram.
     * @throws IOException Thrown if the files are not found or cannot be opened.
     * @throws ShaderException Thrown if adding shaders or linking fails.
     */
    public static GLProgram quickCreate(String vsFile, String fsFile) throws IOException, ShaderException {
    	GLProgram p = new GLProgram(true);
        p.addShaderFile(ShaderType.VertexShader, vsFile);
        p.addShaderFile(ShaderType.FragmentShader, fsFile);
        p.link();
        return p;
    }
    
    /**
     * Create a GLProgram from shader source files.
     * @param vsFile The path to the vertex shader.
     * @param fsFile The path to the fragment shader.
     * @param gsFile The path to the geometry shader.
     * @return The created GLProgram.
     * @throws IOException Thrown if the files are not found or cannot be opened.
     * @throws ShaderException Thrown if adding shaders or linking fails.
     */
    public static GLProgram quickCreate(String vsFile, String fsFile, String gsFile) throws IOException, ShaderException {
    	GLProgram p = new GLProgram(true);
    	p.addShaderFile(ShaderType.VertexShader, vsFile);
    	p.addShaderFile(ShaderType.FragmentShader, fsFile);
    	p.addShaderFile(ShaderType.GeomShader, gsFile);
    	p.link();
    	return p;
    }
    
    /**
     * Create a GLProgram from source strings.
     * @param vsFile The vertex shader program.
     * @param fsFile The fragment shader program.
     * @return The created GLProgram.
     * @throws ShaderException Thrown if adding shaders or linking fails.
     */
    public static GLProgram quickCreateSource(String vsSrc, String fsSrc) throws ShaderException {
        GLProgram p = new GLProgram(true);
        p.addShader(ShaderType.VertexShader, vsSrc);
        p.addShader(ShaderType.FragmentShader, fsSrc);
        p.link();
        return p;
    }

    /**
     * Create a GLProgram from source strings.
     * @param vsFile The vertex shader program.
     * @param fsFile The fragment shader program.
     * @param gsFile The geometry shader program.
     * @return The created GLProgram.
     * @throws ShaderException Thrown if adding shaders or linking fails.
     */
    public static GLProgram quickCreateSource(String vsSrc, String fsSrc, String gsSrc) throws ShaderException {
        GLProgram p = new GLProgram(true);
        p.addShader(ShaderType.VertexShader, vsSrc);
        p.addShader(ShaderType.FragmentShader, fsSrc);
        p.addShader(ShaderType.GeomShader, gsSrc);
        p.link();
        return p;
    }
}
