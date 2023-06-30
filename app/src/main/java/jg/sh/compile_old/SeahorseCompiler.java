package jg.sh.compile_old;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.StringReader;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;

import jg.sh.compile_old.parsing.exceptions.BadIdentifierException;
import jg.sh.compile_old.parsing.nodes.atoms.constructs.Module;
import jg.sh.compile_old.parsing.parser.ExpressionBuilder;
import jg.sh.compile_old.parsing.parser.SeaHorseWholeParser;
import jg.sh.compile_old.parsing.parser.SeaHorseWholeTokenizer;
import jg.sh.util.StringUtils;
import net.percederberg.grammatica.parser.ParseException;
import net.percederberg.grammatica.parser.ParserCreationException;
import net.percederberg.grammatica.parser.ParserLogException;
import net.percederberg.grammatica.parser.Token;

/**
 * Front-end for the SeaHorse first phase of SeaHorse module parsing. 
 *   
 * @author Jose
 *
 */
public class SeahorseCompiler {
  
  private final ExpressionBuilder expressionBuilder;
  private final SeaHorseWholeTokenizer tokenizer;
  private final SeaHorseWholeParser parser;
  
  public SeahorseCompiler() throws ParserCreationException {
    this.expressionBuilder = new ExpressionBuilder(null);
    this.tokenizer = new SeaHorseWholeTokenizer(null);
    this.parser = new SeaHorseWholeParser(null, expressionBuilder);
  }
    
  /**
   * Given an array of SeaHorse module source file paths, this 
   * method parses the source files into raw Modules.
   * 
   * @param sourceFiles - an array of String paths to module source files
   * @return an array of raw Modules, corresponding to their path counterparts in sourceFiles
   * @throws IOException - if an IOException occurred while reading the source files
   * @throws ParserCreationException - if an exception was encountered in creating the parser
   * @throws ParseException - if a syntax mistake was encountered while parsing a source file
   * @throws ParserLogException - contains a list of errors while parsing
   * @throws IllegalArgumentException - if the module's name is reserved and cannot be used.
   */
  public Module [] formSourceFiles(String ... sourceFiles) throws IOException, 
                                                   ParserCreationException, 
                                                   ParseException, 
                                                   ParserLogException,
                                                   IllegalArgumentException {
    
    Module [] sourceConstructs = new Module[sourceFiles.length];
        
    //go through each source and tokenize each file
    for (int i = 0; i < sourceFiles.length; i++) {
      parser.reset(null);
      
      final Path sourceFilePath = Paths.get(sourceFiles[i]);
      
      if (!sourceFilePath.toString().endsWith(".shr")) {
        throw new IllegalArgumentException("Not a SeaHorse (.shr) file: "+sourceFilePath);
      }
      
      final String moduleName = StringUtils.getBareFileName(sourceFilePath.getFileName().toString());
      
      expressionBuilder.setFileName(moduleName);
      
      //throw out files that have bad names
      if (ExpressionBuilder.INVALID_IDENTIFIERS.contains(moduleName)) {
        throw new IllegalArgumentException("The path module name '"+moduleName+"' is reserved and cannot be used!");
      }
      
      /*
       * TODO: This way of in-taking files maybe a memory heavy strategy.
       * 
       * We're essentially intaking the whole source file - albeit, parsing each line
       * Immediately - and passing a massive Token list to the parser.
       * 
       * An efficient way of doing this is to allow the parser and tokenizer
       * to work together on when to read from the source file.
       * 
       * Until there's a nice way of filtering out comments, this is the current way.
       */
      
      //tokenize file
      ArrayList<Token> tokens = new ArrayList<>();
      BufferedReader reader = new BufferedReader(new FileReader(sourceFilePath.toFile()));
      
      //THIS IS TEST CODE - START
      tokenizer.reset(reader);

      Token current = null;
      while ((current = tokenizer.next()) != null) {
        tokens.add(current);
      }
      //THIS IS TEST CODE - END
      
      /*
       * Need to filter out line comments.
       */
      
      /*
      String currentLine = null;
            
      while ((currentLine = reader.readLine()) != null) {        
        currentLine = currentLine.startsWith("//") ? String.valueOf('\n') : currentLine+String.valueOf('\n');
        StringReader lineReader = new StringReader(currentLine);
        tokenizer.reset(lineReader);

        Token current = null;
        while ((current = tokenizer.next()) != null) {
          tokens.add(current);
        }
      }
      */
      
      reader.close();
      
      //tokens.forEach(x -> {System.out.println(x);});
      
      //System.out.println("=======================================");
      
      //from token list, form the components of the file
      parser.parseFromTokenList(tokens);

      Module parsedConstruct = expressionBuilder.getModule();
      //System.out.println(" -------- PARSED: "+parsedConstruct.getName());
      sourceConstructs[i] = parsedConstruct;
    }
    
    return sourceConstructs;
  }
  
  
}
