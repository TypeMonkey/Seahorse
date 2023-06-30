package jg.sh.compile_old.validation;

import java.util.List;

import jg.sh.compile_old.CompilationException;
import jg.sh.compile_old.parsing.nodes.atoms.constructs.Module;

public class FileValidationReport {

  private final Module targetFile;
  private final List<CompilationException> exceptions;
  
  public FileValidationReport(Module file, 
                              List<CompilationException> validationExceptions) {
    this.targetFile = file;
    this.exceptions = validationExceptions;
  }
  
  public boolean isInvalid() {
    return !exceptions.isEmpty();
  }
  
  public Module getTargetFile() {
    return targetFile;
  }

  public List<CompilationException> getExceptions() {
    return exceptions;
  }
}
