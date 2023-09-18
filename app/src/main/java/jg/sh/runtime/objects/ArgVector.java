package jg.sh.runtime.objects;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A vector of arguments for a function call.
 */
public final class ArgVector {
  
  private Map<String, RuntimeInstance> keywords;
  private List<RuntimeInstance> positionals;
  
  public ArgVector(RuntimeInstance ... initialPositionals) {
    this.positionals = new ArrayList<>(Arrays.asList(initialPositionals));
    this.keywords = new HashMap<>();
  }

  public ArgVector(Map<String, RuntimeInstance> initialKeywords, List<RuntimeInstance> initialPositionals) {
    this.positionals = new ArrayList<>(initialPositionals);
    this.keywords = new HashMap<>(initialKeywords);
  }

  public ArgVector clone() {
    return new ArgVector(keywords, positionals);
  }

  public void clearOut() {
    keywords.clear();
    positionals.clear();
  }

  public void setKeywordArg(String keyword, RuntimeInstance value) {
    keywords.put(keyword, value);
  }

  public boolean hasKeyword(String keyword) {
    return keywords.containsKey(keyword);
  } 

  public boolean hasKeywords() {
    return !keywords.isEmpty();
  }

  public Map<String, RuntimeInstance> getKeywords() {
    return keywords;
  }

  public void addAtFront(RuntimeInstance instance) {
    positionals.add(0, instance);
  }
  
  public void addPositional(RuntimeInstance instance) {
    positionals.add(instance);
  }
  
  public RuntimeInstance getPositional(int index) {
    return positionals.get(index);
  }
  
  public List<RuntimeInstance> getPositionals() {
    return positionals;
  }

  public String toString() {
    return "ARG_VERCTOR: "+positionals.size();
  }
}
