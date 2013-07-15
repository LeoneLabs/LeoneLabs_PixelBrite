class FileManager{
  
    //pattern files, names, and listings
    controlP5.ListBox PatternList;
    File[] PatternFiles;
    String[] PatternNames;
    String filePath;
    int x, y, w, h;
  
  FileManager(int x, int y, int w, int h, String path){
    this.x = x;
    this.y = y;
    this.w = w;
    this.h = h;
    //String path;
    PatternList = cp5.addListBox("Patterns", x, y, w, h);
    PatternList.setItemHeight(15);
    PatternList.setBarHeight(15);
  
    PatternList.captionLabel().toUpperCase(true);
    PatternList.captionLabel().set("Patterns");
    PatternList.captionLabel().style().marginTop = 3;
    PatternList.valueLabel().style().marginTop = 3; // the +/- sign
  
    PatternList.setColorBackground(color(64));
    PatternList.setColorActive(color(0, 0, 255, 128));
  
    println(path);
    PatternNames = listFileNames(path);
    PatternFiles = listFiles(path);
    PatternList.clear();
    if (PatternFiles != null && PatternNames != null){
      for (int i = 0; i < PatternFiles.length; i++) {
          PatternList.addItem(PatternNames[i], i);
      }  
    }
    
  }
  
  // This function returns all the files in a directory as an array of File objects
  File[] listFiles(String dir) {
    File file = new File(dir);
    if (file.isDirectory()) {
      File[] files = file.listFiles();
      return files;
    } 
    else {
      // If it's not a directory
      println("listFile=NO");
      return null;
    }
  }
  
  // This function returns all the files in a directory as an array of Strings  
  
  String[] listFileNames(String dir) {
    File file = new File(dir);
    if (file.isDirectory()) {
      String names[] = file.list();
      return names;
    } 
    else {
      // If it's not a directory
      println("listFileNames=NO");
      return null;
    }
  }
  
  void AddToListBox(String path, String[] filenames, File[] files) { //refreshes the list box once a file is saved
  
    filenames = listFileNames(path);    //this isn't actually loading the string[] into pattern_filenames
    files = listFiles(path);
    PatternList.clear();
    for (int i=0;i<files.length;i++) {
      PatternList.addItem(filenames[i], i);
    }
  
  }
  
  byte[] loadPatternFromFile(String filepath){
    // Open a file and read its binary data 
    byte b[] = loadBytes(filepath); 
    //byte a[] = new byte[b.length]; 
    // Print each value, from 0 to 255 
    for (int i = 0; i < b.length; i++) {  
      // bytes are from -128 to 127, this converts to 0 to 255 
      int a = b[i] & 0xff; 
      //print(a + " "); 
    } 
    // Print a blank line at the end 
    println(); 
    return b;
  }
}
