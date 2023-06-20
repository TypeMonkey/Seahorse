package jg.sh.common.presenters;

import java.util.HashSet;
import java.util.Set;
import java.util.Arrays;

import jg.sh.compile.parsing.nodes.ReservedWords;

public class VariablePresenter {

  private final String name;
  private final Set<ReservedWords> keywords;
  
  public VariablePresenter(String name) {
    this(name, new HashSet<>());
  }
  
  public VariablePresenter(String name, ReservedWords ... keywords) {
    this(name, new HashSet<>(Arrays.asList(keywords)));
  }
  
  public VariablePresenter(String name, Set<ReservedWords> modifiers) {
    this.name = name;
    this.keywords = modifiers;
  }
  
  @Override
  public int hashCode() {
    return name.hashCode();
  }

  @Override
  public boolean equals(Object obj) {
    if (obj instanceof VariablePresenter) {
      VariablePresenter other = (VariablePresenter) obj;
      return other.name.equals(name);
    }
    return false;
  }
  
  public String getName() {
    return name;
  }

  public Set<ReservedWords> getKeywords() {
    return keywords;
  }
  
  @Override
  public String toString() {
    return name+" :=> "+keywords;
  }
}