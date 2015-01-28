# ARTangoOpenGL
Another attempt at getting an AR Magic Lens with Tango

Built on this example: http://maninara.blogspot.com/2012/09/render-camera-preview-using-opengl-es.html

Status 1-27-2015:  The code creates an OpenGL ES scene using a GLSurfaceType and places the camera image in the back buffer.  The code will also start the TANGO server and display position and orienation information on top of the OpenGL GLSurfaceView.

TODO:

- Use the Tango Surface callbacks to get Tango color bytes (instead of starting google native camera code.  Otherwise we will run into issues when w try to get point clouds)

- Add some 3D shapes and billboards into the 3D scene.

