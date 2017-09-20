package application;

import static org.lwjgl.opengl.EXTFramebufferObject.GL_COLOR_ATTACHMENT0_EXT;
import static org.lwjgl.opengl.EXTFramebufferObject.GL_FRAMEBUFFER_EXT;
import static org.lwjgl.opengl.EXTFramebufferObject.glBindFramebufferEXT;
import static org.lwjgl.opengl.EXTFramebufferObject.glFramebufferTexture2DEXT;
import static org.lwjgl.opengl.EXTFramebufferObject.glGenFramebuffersEXT;
import static org.lwjgl.opengl.GL11.GL_TEXTURE_2D;
import static org.lwjgl.opengl.GL11.glBegin;
import static org.lwjgl.opengl.GL11.glClear;
import static org.lwjgl.opengl.GL11.glClearColor;
import static org.lwjgl.opengl.GL11.glColor3f;
import static org.lwjgl.opengl.GL11.glEnd;
import static org.lwjgl.opengl.GL11.glFlush;
import static org.lwjgl.opengl.GL11.glLineWidth;
import static org.lwjgl.opengl.GL11.glVertex2f;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import org.eclipse.swt.internal.cocoa.NSOpenGLContext;
import org.eclipse.swt.internal.cocoa.NSOpenGLPixelFormat;
import org.lwjgl.LWJGLException;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GLContext;

import com.sun.javafx.sg.prism.NGNode;
import com.sun.javafx.sg.prism.NGRegion;
import com.sun.prism.Graphics;
import com.sun.prism.GraphicsPipeline;
import com.sun.prism.PixelFormat;
import com.sun.prism.ResourceFactory;
import com.sun.prism.Texture;

import javafx.scene.layout.Pane;

@SuppressWarnings("restriction")
public class NativePane extends Pane {
	private static int WIDTH = 400;
	private static int HEIGHT = 200;

	@Override
	public NGNode impl_createPeer() {
		return new NGNativePane();
	}

	static class NGNativePane extends NGRegion {
		private Texture prismTexture;
		private NSOpenGLContext userContext;

		@Override
		protected void renderContent(Graphics g) {
			// super.renderContent(g);
			Texture texture = getPrismTexture();
			NSOpenGLContext prismContext = NSOpenGLContext.currentContext();
			NSOpenGLContext userContext = getUserContext(prismContext, texture);

			userContext.makeCurrentContext();
			
			drawTriangles();
			
			prismContext.makeCurrentContext();
			g.drawTexture(texture, 0, 0, WIDTH, HEIGHT, 0, 0, WIDTH, HEIGHT);
		}
		
		private void drawTriangles() {
			glClearColor(0.8f, 0.8f, 0.8f, 1.0f);
			glClear(GL11.GL_COLOR_BUFFER_BIT | GL11.GL_DEPTH_BUFFER_BIT);
			
			glBegin(GL11.GL_TRIANGLES);
			glLineWidth(5);
			// Top & Red
			glColor3f(1.0f, 0.0f, 0.0f);
			glVertex2f(0.0f, 1.0f);

			// Right & Green
			glColor3f(0.0f, 1.0f, 0.0f);
			glVertex2f(1.0f, 1.0f);

			// Left & Blue
			glColor3f(0.0f, 0.0f, 1.0f);
			glVertex2f(1.0f, -1.0f);
			glEnd();
			
			glFlush();
		}

		private NSOpenGLContext getUserContext(NSOpenGLContext prismContext, Texture prismTexture) {
			if (userContext == null) {
				userContext = (NSOpenGLContext) new NSOpenGLContext().alloc();
				NSOpenGLPixelFormat pixelFormat = (NSOpenGLPixelFormat) new NSOpenGLPixelFormat().alloc();
				pixelFormat.initWithAttributes(new int[] { 0, });
				userContext = userContext.initWithFormat(pixelFormat, prismContext);
				userContext.makeCurrentContext();
				try {
					GLContext.useContext(userContext, false);
				} catch (LWJGLException e) {
					// TODO: handle exception
					e.printStackTrace();
				}
				int userFBO = glGenFramebuffersEXT();
				glBindFramebufferEXT(GL_FRAMEBUFFER_EXT, userFBO);
				try {
					Method method = prismTexture.getClass().getDeclaredMethod("getNativeSourceHandle");
					method.setAccessible(true);
					int prismTextureID = (int) method.invoke(prismTexture);
					glFramebufferTexture2DEXT(GL_FRAMEBUFFER_EXT, GL_COLOR_ATTACHMENT0_EXT, GL_TEXTURE_2D,
							prismTextureID, 0);
				} catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException
						| NoSuchMethodException | SecurityException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
			return userContext;
		}

		private Texture getPrismTexture() {
			if (prismTexture == null) {
				ResourceFactory f = GraphicsPipeline.getDefaultResourceFactory();
				prismTexture = f.createTexture(PixelFormat.INT_ARGB_PRE, Texture.Usage.DEFAULT,
						Texture.WrapMode.CLAMP_NOT_NEEDED, WIDTH, HEIGHT);
				prismTexture.makePermanent();
			}
			return prismTexture;
		}
	}
}
