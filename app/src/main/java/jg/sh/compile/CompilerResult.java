package jg.sh.compile;

import java.util.List;

import jg.sh.compile.exceptions.ValidationException;

public class CompilerResult {
    private final ObjectFile objectFile;
    private final List<ValidationException> validationExceptions;

    public CompilerResult(ObjectFile objectFile) {
      this.objectFile = objectFile;
      this.validationExceptions = null;
    }

    public CompilerResult(List<ValidationException> validationExceptions) {
      this.validationExceptions = validationExceptions;
      this.objectFile = null;
    }

    public boolean isSuccessful() {
      return objectFile != null;
    }

    public ObjectFile getObjectFile() {
      return objectFile;
    }

    public List<ValidationException> getValidationExceptions() {
      return validationExceptions;
    }
  }
