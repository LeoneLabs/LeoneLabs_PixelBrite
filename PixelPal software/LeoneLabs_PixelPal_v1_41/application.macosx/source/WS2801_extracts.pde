//pulled these functions out of the WS2801 library from Adafruit
// Constants used for 'order' parameter of zigzag method:
public static final int
	  START_TOP    = 0,
	  START_BOTTOM = 1,
	  START_LEFT   = 0,
	  START_RIGHT  = 2,
	  ROW_MAJOR    = 0,
	  COL_MAJOR    = 4;

int[] zigzag(int width, int height, int order) {
		int i, major, minor, incMajor, incMinor, mulMajor, mulMinor,
		    limitMajor, limitMinor;
		int remap[] = new int[width * height];

		// Determine initial position, incs, muls and limits
		if((order & COL_MAJOR) != 0) {
			mulMajor = 1;
			mulMinor = width;
			if((order & START_RIGHT) != 0) {
				major      = width - 1;
				limitMajor = -1;
			} else {
				major      = 0;
				limitMajor = width;
			}
			minor = ((order & START_BOTTOM) != 0) ? height - 1 : 0;
			limitMinor = height;
		} else { // Row major
			mulMajor = width;
			mulMinor = 1;
			if((order & START_BOTTOM) != 0) {
				major      = height - 1;
				limitMajor = -1;
			} else {
				major      = 0;
				limitMajor = height;
			}
			minor = ((order & START_RIGHT) != 0) ? width - 1 : 0;
			limitMinor = width;
		}
		incMajor = (major > 0) ? -1 : 1;
		incMinor = (minor > 0) ? -1 : 1;

		// Iterate though each position in grid, reversing
		// row/column directions as suited to the given order.
		for(i=0; major != limitMajor; i++) {
			remap[i] = major * mulMajor + minor * mulMinor;
			minor   += incMinor;
			if((minor == -1) || (minor == limitMinor)) {
				incMinor = -incMinor;
				minor   +=  incMinor;
				major   +=  incMajor;
			}
		}

		return remap;
}
// Fancy gamma correction; separate R,G,B ranges and exponents:
public short[][] setGamma(
    int rMin, int rMax, double rGamma,
    int gMin, int gMax, double gGamma,
    int bMin, int bMax, double bGamma) {
 
    short  i;
    double rRange, gRange, bRange, d;
    short[][] outGamma = new short[256][3];
    rRange = (double)(rMax - rMin);
    gRange = (double)(gMax - gMin);
    bRange = (double)(bMax - bMin);

    for(i=0; i<256; i++) {
      d = (double)i / 255.0;
      outGamma[i][0] = (short)(rMin +
        (int)Math.floor(rRange * Math.pow(d,rGamma) + 0.5));
      outGamma[i][1] = (short)(gMin +
        (int)Math.floor(gRange * Math.pow(d,gGamma) + 0.5));
      outGamma[i][2] = (short)(bMin +
        (int)Math.floor(bRange * Math.pow(d,bGamma) + 0.5));
      //println(i + " " + outGamma[i][0]);
    }
    return outGamma;
  }
