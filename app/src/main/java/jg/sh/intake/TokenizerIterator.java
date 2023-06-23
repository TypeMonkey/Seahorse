package jg.sh.intake;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;

import jg.sh.intake.exceptions.TokenizerException;
import jg.sh.intake.token.Token;
import jg.sh.intake.token.TokenType;

/**
 * An Iterator implementation for Tokens
 * 
 * Note: this Iterator will ignore comment blocks. If a comment block's start is
 * encountered,
 * it will keep reading tokens until the next token after the comment blocke's
 * end
 * is received. The returned Token can be an EOF
 */
public class TokenizerIterator implements Iterator<Token> {

  private final BooleanSupplier hasNextFunction;
  private final Supplier<Token> nextFunction;

  private int index;

  public TokenizerIterator(Tokenizer tokenizer, boolean skipBlockComments) {
    this.hasNextFunction = () -> tokenizer.hasNext();
    if (tokenizer.getGeneratedTokens() == null) {
      // This tokenizer doesn't keep a list of tokens
      this.index = 0;
      final ArrayList<Token> list = new ArrayList<>();
      this.nextFunction = () -> {
        if (index < list.size()) {
          return list.get(index++);
        } else {
          try {
            final Token newToken = tokenizer.nextToken();
            index++;
            list.add(newToken);
            return newToken;
          } catch (IOException | TokenizerException e) {
            throw new IllegalStateException(e);
          }
        }
      };
    } else {
      final List<Token> list = tokenizer.getGeneratedTokens();
      this.index = list.size() == 0 ? 0 : list.size() - 1;
      this.nextFunction = () -> {
        // System.out.println("ITERATOR NEXT(): "+index+" < "+list.size());
        if (index < list.size()) {
          return list.get(index++);
        } else {
          try {
            final Token newToken = tokenizer.nextToken();
            index++;
            return newToken;
          } catch (IOException | TokenizerException e) {
            throw new IllegalStateException(e);
          }
        }
      };
    }
  }

  public TokenizerIterator(List<Token> tokens) {
    this.index = 0;
    hasNextFunction = () -> index < tokens.size();
    nextFunction = () -> {
      final Token temp = tokens.get(index++);
      return temp;
    };
  }

  /**
   * Rewinds this iterator to the previous element's spot.
   * 
   * If the previous element is the first element in this iterator's source,
   * the next call to pushback() will do nothing.
   */
  public void pushback() {
    if (index > 0) {
      index--;
    }
  }

  @Override
  public boolean hasNext() {
    return hasNextFunction.getAsBoolean();
  }

  @Override
  public Token next() throws IllegalStateException {
    Token init = nextFunction.get();

    if (init.getType() == TokenType.COMMENT_BLOCK_START) {
      while (hasNext()) {
        if ((init = next()).getType() == TokenType.COMMENT_BLOCK_END) {
          break;
        }
      }

      return next();
    }

    return init;
  }

}
