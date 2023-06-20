package jg.sh.compile.parsing.nodes.atoms;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

import jg.sh.compile.parsing.nodes.ASTNode;
import jg.sh.compile.parsing.nodes.NodeVisitor;
import jg.sh.compile.parsing.nodes.ReservedWords;

/**
 * A placeholder Atom for parsing purposes
 * @author Jose
 *
 */
public class Keyword extends ASTNode{

  private final ReservedWords keyWord;

  public Keyword(int line, int column, ReservedWords word) {
    super(line, column);
    this.keyWord = word;
  }

  public ReservedWords getKeyWord() {
    return keyWord;
  }

  @Override
  public boolean equals(Object obj) {
    if (obj instanceof Keyword) {
      return ((Keyword) obj).keyWord == keyWord;
    }
    return false;
  }

  @Override
  public int hashCode() {
    return keyWord.actualWord.hashCode();
  }
  
  @Override
  public void acceptVisitor(NodeVisitor visitor) {
    visitor.visit(this);
  }
  
  @Override
  public boolean isLValue() {
    return false;
  }

  @Override
  public String toString() {
    return "KW ~ "+keyWord.actualWord;
  }

  public static ReservedWords getEquivalentReservedWord(String keyword) {
    return ReservedWords.stringToReservedWord(keyword);
  }
  
  public static Set<ReservedWords> toReservedWords(Set<Keyword> keywords) {
    return keywords.stream().map(x -> x.getKeyWord()).collect(Collectors.toSet());
  }
  
  public static Set<ReservedWords> toReservedWords(Keyword ... keywords) {
    return Arrays.stream(keywords).map(x -> x.getKeyWord()).collect(Collectors.toSet());
  }
}
