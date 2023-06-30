package jg.sh.compile_old;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;

public class CommentFileReader extends Reader {
  
  private final BufferedReader reader;
    
  public CommentFileReader(File file) throws FileNotFoundException {
    this.reader = new BufferedReader(new FileReader(file));
  }

  @Override
  public int read(char[] cbuf, int off, int len) throws IOException {
    final int readAmnt = reader.read(cbuf, off, len);
    
    int commentStart = -1;
    
    for(int i = off; i < off + len; i++) {
      if (cbuf[i] == '/' && (i + 1) < len && cbuf[i + 1] == '/') {
        commentStart = i;
        break;
      }
    }
    
    if (commentStart >= 0) {
      //We found a comment
      
      for(int i = commentStart; i < off + len; i++) {
        
      }
      return 0;
    }
    else {
      return readAmnt;
    }
  }

  @Override
  public void close() throws IOException {
    reader.close();
  }
}
