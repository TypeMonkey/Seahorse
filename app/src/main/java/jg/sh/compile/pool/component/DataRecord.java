package jg.sh.compile.pool.component;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Set;
import java.util.stream.Collectors;

import jg.sh.common.FunctionSignature;
import jg.sh.parsing.nodes.Keyword;
import jg.sh.parsing.token.TokenType;

public class DataRecord implements PoolComponent {

  private final String name;
  private final FunctionSignature constructorSignature;
  private final LinkedHashMap<String, Set<Integer>> attributes;

  /**
   * Cosntructs a DataRecord
   * @param name - name of this DataRecord
   * @param attributes - the attributes set by this DataRecord, and the 
   *                     descriptors of each attribute.
   */
  public DataRecord(String name, 
                    FunctionSignature constructorSignature, 
                    LinkedHashMap<String, Set<Integer>> attributes) {
    this.name = name;
    this.constructorSignature = constructorSignature;
    this.attributes = attributes;
  }

  @Override
  public boolean equals(Object o) {
    return o instanceof DataRecord && ((DataRecord) o).name.equals(name);
  }

  @Override
  public int hashCode() {
    return name.hashCode();
  }

  public boolean hasAttr(String name) {
    return attributes.containsKey(name);
  }

  public Set<Integer> getAttrDescriptors(String attrName) {
    return attributes.get(attrName);
  }

  public String getName() {
    return name;
  }

  public FunctionSignature getConstrSignature() {
    return constructorSignature;
  }

  public LinkedHashMap<String, Set<Integer>> getAttributes() {
    return attributes;
  }

  @Override
  public ComponentType getType() {
    return ComponentType.DATA_RECORD;
  }

  public static Set<Integer> keywordToInt(Set<Keyword> keywords) {
    return keywords.stream().map(x -> x.getKeyword().ordinal()).collect(Collectors.toSet());
  }

  public static Set<Integer> keywordToInt(Keyword ... keywords) {
    return Arrays.asList(keywords).stream()
                                  .map(x -> x.getKeyword().ordinal())
                                  .collect(Collectors.toSet());
  }

  public static Set<Integer> keywordToInt(TokenType ... keywords) {
    return Arrays.asList(keywords).stream()
                                  .map(x -> x.ordinal())
                                  .collect(Collectors.toSet());
  }
}
