package jg.sh.parsing.nodes.statements.blocks;

import java.util.List;

import jg.sh.common.Location;
import jg.sh.parsing.nodes.Identifier;
import jg.sh.parsing.nodes.statements.Statement;

/**
 * Used to handle any runtime exceptions that arises
 * from a region of code.
 * 
 * Format:
 * 
 * try {
 *    statements....
 * } 
 * catch e {
 *    statements....
 * }
 * 
 * where 'e' is an variable that holds the thrown exception
 */
public class TryCatch extends Block{

  private final Block catchBlock;
  private final Identifier exceptionHandler;

  public TryCatch(List<Statement> testStatements, 
                  Block catchBlock, 
                  Identifier exceptionHandler, 
                  Location start, 
                  Location end) {
    super(testStatements, start, end);
    this.exceptionHandler = exceptionHandler;
    this.catchBlock = catchBlock;
  }

  public Block getCatchBlock() {
    return catchBlock;
  }

  public Identifier getExceptionHandler() {
    return exceptionHandler;
  }
  
}
