package jg.sh;

import java.util.Map;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

import jg.sh.InterpreterOptions.IOption;
import net.percederberg.grammatica.parser.ParserCreationException;

public class Main {

  public static void main(String[] args) {
    ParsedCLIOptions options = parseArguments(args);
    if (options != null) {
      try {
        SeaHorseInterpreter interpreter = new SeaHorseInterpreter(options.options);
        if(!interpreter.init()) {
          System.err.println("Couldn't properly initialize interpreter. Exiting...");
          return;
        }
        interpreter.executeModule(options.mainModule, options.programArgs);
      } catch (ParserCreationException e) {
        System.err.println("Error encountered while initializing parser. Exiting...");
      }
    }
  }
   
  static class ParsedCLIOptions {
    private final Map<IOption, Object> options;
    private final String mainModule;
    private final String [] programArgs;
    
    public ParsedCLIOptions(Map<IOption, Object> options, String mainModule, String [] programArgs) {
      this.options = options;
      this.mainModule = mainModule;
      this.programArgs = programArgs;
    }
  }
  
  private static ParsedCLIOptions parseArguments(String [] args) {
    Map<IOption, Object> options = InterpreterOptions.getDefaultOptions();
    
    Options cliOptions = new Options();
    
    Option help = new Option("h", "Prints helpful information for the SeaHorse interpreter");
    help.setLongOpt("help");
    help.setArgs(0);
    help.setRequired(false);
    cliOptions.addOption(help);
    
    Option compToByte = new Option("c", "Writes out the bytecode transformations "+System.lineSeparator()
                                      + "of loaded modules into the current directory.");
    compToByte.setLongOpt("compile");
    compToByte.setArgs(1);
    compToByte.setValueSeparator('=');
    compToByte.setRequired(false);
    compToByte.setType(Boolean.TYPE);
    cliOptions.addOption(compToByte);
    
    Option interpretOnly = new Option("i", "Executes loaded modules "+System.lineSeparator()
                                         + "without any bytecode transformations");
    interpretOnly.setLongOpt("inter");
    interpretOnly.setArgs(1);
    interpretOnly.setValueSeparator('=');
    interpretOnly.setRequired(false);
    interpretOnly.setType(Boolean.TYPE);
    cliOptions.addOption(interpretOnly);

    Option loadFromByte = new Option("b", "Prioritizes the loading of a module's bytecode transformation "+System.lineSeparator()+
                                          "before loading the actual module itself.");
    loadFromByte.setLongOpt("load");
    loadFromByte.setArgs(1);
    loadFromByte.setValueSeparator('=');
    loadFromByte.setRequired(false);
    loadFromByte.setType(Boolean.TYPE);
    cliOptions.addOption(loadFromByte);
    
    Option moduleSearch = new Option("mp", "Sets the paths for which this interpreter will search for modules "+System.lineSeparator()
                                         + "(these paths will be searched for after inspecting the paths for the standard library)");
    moduleSearch.setLongOpt("modpaths");
    moduleSearch.setArgs(Option.UNLIMITED_VALUES);
    moduleSearch.setValueSeparator('=');
    moduleSearch.setRequired(false);
    cliOptions.addOption(moduleSearch);
    
    Option standardPaths = new Option("sp", "Sets the paths for which this interpreter will search for modules "+System.lineSeparator()
                                          + "that are part of its standard library");
    standardPaths.setLongOpt("stpaths");
    standardPaths.setArgs(Option.UNLIMITED_VALUES);
    standardPaths.setValueSeparator('=');
    standardPaths.setRequired(false);
    cliOptions.addOption(standardPaths);
    
    Option validate = new Option("v", "Whether to validate the fails (checking of important errors) prior to execution");
    validate.setLongOpt("validate");
    validate.setArgs(1);
    validate.setValueSeparator('=');
    validate.setRequired(false);
    validate.setType(Boolean.TYPE);
    cliOptions.addOption(validate);
    
    Option measure = new Option("m", "Whether to output the elapsed milliseconds in executing the module");
    measure.setLongOpt("measure");
    measure.setArgs(1);
    measure.setValueSeparator('=');
    measure.setRequired(false);
    measure.setType(Boolean.TYPE);
    cliOptions.addOption(measure);
    
    Option additional = new Option("a", "A set of additonal modules to pre-compile with the main module");
    additional.setLongOpt("add");
    additional.setArgs(Option.UNLIMITED_VALUES);
    additional.setValueSeparator('=');
    additional.setRequired(false);
    cliOptions.addOption(additional);
        
    //Any left over arguments is treated as a program argument 
    HelpFormatter usageFormatter = new HelpFormatter();
    CommandLineParser parser = new DefaultParser();
    final String cliUsage = "seahorse [options] main_module.shr [args...]";
    
    try {
      CommandLine commandLine = parser.parse(cliOptions, args);
      if (commandLine.hasOption(help)) {
        usageFormatter.printHelp(cliUsage, cliOptions);
        return null;
      }
      
      if (commandLine.hasOption(compToByte)) {
        options.put(IOption.COMP_TO_BYTE, commandLine.getOptionValue(compToByte));
      }
      /*
      if (commandLine.hasOption(interpretOnly)) {
        options.put(IOption.INTERPRET_ONLY, commandLine.getOptionValue(interpretOnly));
      }
      if (commandLine.hasOption(loadFromByte)) {
        options.put(IOption.INTERPRET_ONLY, commandLine.getOptionValue(loadFromByte));
      }
      */
      if (commandLine.hasOption(moduleSearch)) {
        options.put(IOption.MODULE_SEARCH, commandLine.getOptionValue(moduleSearch));
      }
      if (commandLine.hasOption(standardPaths)) {
        options.put(IOption.ST_LIB_PATH, commandLine.getOptionValue(standardPaths));
      }
      if (commandLine.hasOption(validate)) {
        options.put(IOption.VALIDATE, commandLine.getOptionValue(validate));
      }
      if (commandLine.hasOption(additional)) {
        options.put(IOption.ADDITIONAL, commandLine.getOptionValue(additional));
      }
      if (commandLine.hasOption(measure)) {
        options.put(IOption.MEASURE, commandLine.getOptionValue(measure));
      }
      
      if (commandLine.getArgList().size() >= 1) {
        String mainModule = commandLine.getArgList().get(0);
        String [] programArgs = new String[commandLine.getArgs().length - 1];
        
        for(int i = 1; i < commandLine.getArgs().length; i++) {
          programArgs[i] = commandLine.getArgs()[i];
        }
        
        return new ParsedCLIOptions(options, mainModule, programArgs);
      }
      else {
        usageFormatter.printHelp(cliUsage, cliOptions);
        return null;
      }   
    } catch (ParseException e) {
      usageFormatter.printHelp(cliUsage, cliOptions);
      return null;
    }

  }

}
