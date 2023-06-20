/*
 * SeaHorseWholeParser.java
 *
 * THIS FILE HAS BEEN GENERATED AUTOMATICALLY. DO NOT EDIT!
 */

package jg.sh.compile.parsing.parser;

import java.io.Reader;

import net.percederberg.grammatica.parser.ParserCreationException;
import net.percederberg.grammatica.parser.ProductionPattern;
import net.percederberg.grammatica.parser.ProductionPatternAlternative;
import net.percederberg.grammatica.parser.RecursiveDescentParser;
import net.percederberg.grammatica.parser.Tokenizer;

/**
 * A token stream parser.
 *
 *
 */
public class SeaHorseWholeParser extends RecursiveDescentParser {

    /**
     * A generated production node identity constant.
     */
    private static final int SUBPRODUCTION_1 = 3001;

    /**
     * A generated production node identity constant.
     */
    private static final int SUBPRODUCTION_2 = 3002;

    /**
     * A generated production node identity constant.
     */
    private static final int SUBPRODUCTION_3 = 3003;

    /**
     * A generated production node identity constant.
     */
    private static final int SUBPRODUCTION_4 = 3004;

    /**
     * A generated production node identity constant.
     */
    private static final int SUBPRODUCTION_5 = 3005;

    /**
     * A generated production node identity constant.
     */
    private static final int SUBPRODUCTION_6 = 3006;

    /**
     * A generated production node identity constant.
     */
    private static final int SUBPRODUCTION_7 = 3007;

    /**
     * A generated production node identity constant.
     */
    private static final int SUBPRODUCTION_8 = 3008;

    /**
     * A generated production node identity constant.
     */
    private static final int SUBPRODUCTION_9 = 3009;

    /**
     * A generated production node identity constant.
     */
    private static final int SUBPRODUCTION_10 = 3010;

    /**
     * A generated production node identity constant.
     */
    private static final int SUBPRODUCTION_11 = 3011;

    /**
     * A generated production node identity constant.
     */
    private static final int SUBPRODUCTION_12 = 3012;

    /**
     * A generated production node identity constant.
     */
    private static final int SUBPRODUCTION_13 = 3013;

    /**
     * A generated production node identity constant.
     */
    private static final int SUBPRODUCTION_14 = 3014;

    /**
     * A generated production node identity constant.
     */
    private static final int SUBPRODUCTION_15 = 3015;

    /**
     * A generated production node identity constant.
     */
    private static final int SUBPRODUCTION_16 = 3016;

    /**
     * A generated production node identity constant.
     */
    private static final int SUBPRODUCTION_17 = 3017;

    /**
     * A generated production node identity constant.
     */
    private static final int SUBPRODUCTION_18 = 3018;

    /**
     * A generated production node identity constant.
     */
    private static final int SUBPRODUCTION_19 = 3019;

    /**
     * A generated production node identity constant.
     */
    private static final int SUBPRODUCTION_20 = 3020;

    /**
     * A generated production node identity constant.
     */
    private static final int SUBPRODUCTION_21 = 3021;

    /**
     * A generated production node identity constant.
     */
    private static final int SUBPRODUCTION_22 = 3022;

    /**
     * A generated production node identity constant.
     */
    private static final int SUBPRODUCTION_23 = 3023;

    /**
     * A generated production node identity constant.
     */
    private static final int SUBPRODUCTION_24 = 3024;

    /**
     * A generated production node identity constant.
     */
    private static final int SUBPRODUCTION_25 = 3025;

    /**
     * A generated production node identity constant.
     */
    private static final int SUBPRODUCTION_26 = 3026;

    /**
     * A generated production node identity constant.
     */
    private static final int SUBPRODUCTION_27 = 3027;

    /**
     * A generated production node identity constant.
     */
    private static final int SUBPRODUCTION_28 = 3028;

    /**
     * A generated production node identity constant.
     */
    private static final int SUBPRODUCTION_29 = 3029;

    /**
     * A generated production node identity constant.
     */
    private static final int SUBPRODUCTION_30 = 3030;

    /**
     * A generated production node identity constant.
     */
    private static final int SUBPRODUCTION_31 = 3031;

    /**
     * A generated production node identity constant.
     */
    private static final int SUBPRODUCTION_32 = 3032;

    /**
     * A generated production node identity constant.
     */
    private static final int SUBPRODUCTION_33 = 3033;

    /**
     * A generated production node identity constant.
     */
    private static final int SUBPRODUCTION_34 = 3034;

    /**
     * A generated production node identity constant.
     */
    private static final int SUBPRODUCTION_35 = 3035;

    /**
     * A generated production node identity constant.
     */
    private static final int SUBPRODUCTION_36 = 3036;

    /**
     * A generated production node identity constant.
     */
    private static final int SUBPRODUCTION_37 = 3037;

    /**
     * A generated production node identity constant.
     */
    private static final int SUBPRODUCTION_38 = 3038;

    /**
     * A generated production node identity constant.
     */
    private static final int SUBPRODUCTION_39 = 3039;

    /**
     * A generated production node identity constant.
     */
    private static final int SUBPRODUCTION_40 = 3040;

    /**
     * A generated production node identity constant.
     */
    private static final int SUBPRODUCTION_41 = 3041;

    /**
     * A generated production node identity constant.
     */
    private static final int SUBPRODUCTION_42 = 3042;

    /**
     * A generated production node identity constant.
     */
    private static final int SUBPRODUCTION_43 = 3043;

    /**
     * A generated production node identity constant.
     */
    private static final int SUBPRODUCTION_44 = 3044;

    /**
     * A generated production node identity constant.
     */
    private static final int SUBPRODUCTION_45 = 3045;

    /**
     * A generated production node identity constant.
     */
    private static final int SUBPRODUCTION_46 = 3046;

    /**
     * A generated production node identity constant.
     */
    private static final int SUBPRODUCTION_47 = 3047;

    /**
     * A generated production node identity constant.
     */
    private static final int SUBPRODUCTION_48 = 3048;

    /**
     * A generated production node identity constant.
     */
    private static final int SUBPRODUCTION_49 = 3049;

    /**
     * Creates a new parser with a default analyzer.
     *
     * @param in             the input stream to read from
     *
     * @throws ParserCreationException if the parser couldn't be
     *             initialized correctly
     */
    public SeaHorseWholeParser(Reader in)
        throws ParserCreationException {

        super(in);
        createPatterns();
    }

    /**
     * Creates a new parser.
     *
     * @param in             the input stream to read from
     * @param analyzer       the analyzer to use while parsing
     *
     * @throws ParserCreationException if the parser couldn't be
     *             initialized correctly
     */
    public SeaHorseWholeParser(Reader in, SeaHorseWholeAnalyzer analyzer)
        throws ParserCreationException {

        super(in, analyzer);
        createPatterns();
    }

    /**
     * Creates a new tokenizer for this parser. Can be overridden by a
     * subclass to provide a custom implementation.
     *
     * @param in             the input stream to read from
     *
     * @return the tokenizer created
     *
     * @throws ParserCreationException if the tokenizer couldn't be
     *             initialized correctly
     */
    protected Tokenizer newTokenizer(Reader in)
        throws ParserCreationException {

        return new SeaHorseWholeTokenizer(in);
    }

    /**
     * Initializes the parser by creating all the production patterns.
     *
     * @throws ParserCreationException if the parser couldn't be
     *             initialized correctly
     */
    private void createPatterns() throws ParserCreationException {
        ProductionPattern             pattern;
        ProductionPatternAlternative  alt;

        pattern = new ProductionPattern(SeaHorseWholeConstants.SOURCE_FILE,
                                        "sourceFile");
        alt = new ProductionPatternAlternative();
        alt.addProduction(SeaHorseWholeConstants.USE_STATEMENT, 0, -1);
        alt.addProduction(SUBPRODUCTION_1, 1, -1);
        pattern.addAlternative(alt);
        addPattern(pattern);

        pattern = new ProductionPattern(SeaHorseWholeConstants.USE_STATEMENT,
                                        "useStatement");
        alt = new ProductionPatternAlternative();
        alt.addProduction(SUBPRODUCTION_5, 1, 1);
        alt.addToken(SeaHorseWholeConstants.SEMICOLON, 1, 1);
        pattern.addAlternative(alt);
        addPattern(pattern);

        pattern = new ProductionPattern(SeaHorseWholeConstants.DATA_DEFINITION,
                                        "dataDefinition");
        alt = new ProductionPatternAlternative();
        alt.addToken(SeaHorseWholeConstants.EXPORT, 0, 1);
        alt.addToken(SeaHorseWholeConstants.CONST, 0, 1);
        alt.addToken(SeaHorseWholeConstants.DATA, 1, 1);
        alt.addToken(SeaHorseWholeConstants.NAME, 1, 1);
        alt.addToken(SeaHorseWholeConstants.OP_CU_BRACK, 1, 1);
        alt.addProduction(SUBPRODUCTION_6, 0, -1);
        alt.addToken(SeaHorseWholeConstants.CL_CU_BRACK, 1, 1);
        pattern.addAlternative(alt);
        addPattern(pattern);

        pattern = new ProductionPattern(SeaHorseWholeConstants.CONSTRUCTOR_BODY,
                                        "constructorBody");
        alt = new ProductionPatternAlternative();
        alt.addToken(SeaHorseWholeConstants.CONSTR, 1, 1);
        alt.addToken(SeaHorseWholeConstants.OP_PAREN, 1, 1);
        alt.addProduction(SeaHorseWholeConstants.PARAMETER, 0, 1);
        alt.addProduction(SUBPRODUCTION_7, 0, -1);
        alt.addToken(SeaHorseWholeConstants.CL_PAREN, 1, 1);
        alt.addToken(SeaHorseWholeConstants.OP_CU_BRACK, 1, 1);
        alt.addProduction(SUBPRODUCTION_8, 0, -1);
        alt.addToken(SeaHorseWholeConstants.CL_CU_BRACK, 1, 1);
        pattern.addAlternative(alt);
        addPattern(pattern);

        pattern = new ProductionPattern(SeaHorseWholeConstants.FUNCTION_BODY,
                                        "functionBody");
        alt = new ProductionPatternAlternative();
        alt.addToken(SeaHorseWholeConstants.EXPORT, 0, 1);
        alt.addToken(SeaHorseWholeConstants.CONST, 0, 1);
        alt.addToken(SeaHorseWholeConstants.FUNC, 1, 1);
        alt.addToken(SeaHorseWholeConstants.NAME, 1, 1);
        alt.addToken(SeaHorseWholeConstants.OP_PAREN, 1, 1);
        alt.addProduction(SeaHorseWholeConstants.PARAMETER, 0, 1);
        alt.addProduction(SUBPRODUCTION_9, 0, -1);
        alt.addToken(SeaHorseWholeConstants.CL_PAREN, 1, 1);
        alt.addToken(SeaHorseWholeConstants.OP_CU_BRACK, 1, 1);
        alt.addProduction(SUBPRODUCTION_10, 0, -1);
        alt.addToken(SeaHorseWholeConstants.CL_CU_BRACK, 1, 1);
        pattern.addAlternative(alt);
        addPattern(pattern);

        pattern = new ProductionPattern(SeaHorseWholeConstants.PARAMETER,
                                        "parameter");
        alt = new ProductionPatternAlternative();
        alt.addToken(SeaHorseWholeConstants.CONST, 0, 1);
        alt.addToken(SeaHorseWholeConstants.GREAT, 0, 1);
        alt.addToken(SeaHorseWholeConstants.NAME, 1, 1);
        alt.addProduction(SUBPRODUCTION_11, 0, 1);
        pattern.addAlternative(alt);
        addPattern(pattern);

        pattern = new ProductionPattern(SeaHorseWholeConstants.VARIABLE,
                                        "variable");
        alt = new ProductionPatternAlternative();
        alt.addToken(SeaHorseWholeConstants.EXPORT, 0, 1);
        alt.addProduction(SUBPRODUCTION_12, 1, 1);
        alt.addToken(SeaHorseWholeConstants.NAME, 1, 1);
        alt.addToken(SeaHorseWholeConstants.COLON, 1, 1);
        alt.addToken(SeaHorseWholeConstants.EQUAL, 1, 1);
        alt.addProduction(SeaHorseWholeConstants.EXPR, 1, 1);
        alt.addToken(SeaHorseWholeConstants.SEMICOLON, 1, 1);
        pattern.addAlternative(alt);
        addPattern(pattern);

        pattern = new ProductionPattern(SeaHorseWholeConstants.BLOCK,
                                        "block");
        alt = new ProductionPatternAlternative();
        alt.addProduction(SeaHorseWholeConstants.ERROR_HANDLING, 1, 1);
        pattern.addAlternative(alt);
        alt = new ProductionPatternAlternative();
        alt.addProduction(SeaHorseWholeConstants.IF_ELSE, 1, 1);
        pattern.addAlternative(alt);
        alt = new ProductionPatternAlternative();
        alt.addProduction(SeaHorseWholeConstants.WHILE_LOOP, 1, 1);
        pattern.addAlternative(alt);
        alt = new ProductionPatternAlternative();
        alt.addProduction(SeaHorseWholeConstants.SCOPE_BLOCK, 1, 1);
        pattern.addAlternative(alt);
        addPattern(pattern);

        pattern = new ProductionPattern(SeaHorseWholeConstants.SCOPE_BLOCK,
                                        "scopeBlock");
        alt = new ProductionPatternAlternative();
        alt.addProduction(SUBPRODUCTION_14, 1, 1);
        pattern.addAlternative(alt);
        addPattern(pattern);

        pattern = new ProductionPattern(SeaHorseWholeConstants.ERROR_HANDLING,
                                        "errorHandling");
        alt = new ProductionPatternAlternative();
        alt.addToken(SeaHorseWholeConstants.TRY, 1, 1);
        alt.addToken(SeaHorseWholeConstants.OP_CU_BRACK, 1, 1);
        alt.addProduction(SUBPRODUCTION_15, 0, -1);
        alt.addToken(SeaHorseWholeConstants.CL_CU_BRACK, 1, 1);
        alt.addToken(SeaHorseWholeConstants.CATCH, 1, 1);
        alt.addToken(SeaHorseWholeConstants.OP_PAREN, 1, 1);
        alt.addToken(SeaHorseWholeConstants.NAME, 1, 1);
        alt.addToken(SeaHorseWholeConstants.CL_PAREN, 1, 1);
        alt.addToken(SeaHorseWholeConstants.OP_CU_BRACK, 1, 1);
        alt.addProduction(SUBPRODUCTION_16, 0, -1);
        alt.addToken(SeaHorseWholeConstants.CL_CU_BRACK, 1, 1);
        pattern.addAlternative(alt);
        addPattern(pattern);

        pattern = new ProductionPattern(SeaHorseWholeConstants.IF_ELSE,
                                        "ifElse");
        alt = new ProductionPatternAlternative();
        alt.addToken(SeaHorseWholeConstants.IF, 1, 1);
        alt.addToken(SeaHorseWholeConstants.OP_PAREN, 1, 1);
        alt.addProduction(SeaHorseWholeConstants.EXPR, 1, 1);
        alt.addToken(SeaHorseWholeConstants.CL_PAREN, 1, 1);
        alt.addToken(SeaHorseWholeConstants.OP_CU_BRACK, 1, 1);
        alt.addProduction(SUBPRODUCTION_17, 0, -1);
        alt.addToken(SeaHorseWholeConstants.CL_CU_BRACK, 1, 1);
        alt.addProduction(SUBPRODUCTION_19, 0, -1);
        alt.addProduction(SUBPRODUCTION_21, 0, 1);
        pattern.addAlternative(alt);
        addPattern(pattern);

        pattern = new ProductionPattern(SeaHorseWholeConstants.WHILE_LOOP,
                                        "whileLoop");
        alt = new ProductionPatternAlternative();
        alt.addToken(SeaHorseWholeConstants.WHILE, 1, 1);
        alt.addToken(SeaHorseWholeConstants.OP_PAREN, 1, 1);
        alt.addProduction(SeaHorseWholeConstants.EXPR, 1, 1);
        alt.addToken(SeaHorseWholeConstants.CL_PAREN, 1, 1);
        alt.addToken(SeaHorseWholeConstants.OP_CU_BRACK, 1, 1);
        alt.addProduction(SUBPRODUCTION_22, 0, -1);
        alt.addToken(SeaHorseWholeConstants.CL_CU_BRACK, 1, 1);
        pattern.addAlternative(alt);
        addPattern(pattern);

        pattern = new ProductionPattern(SeaHorseWholeConstants.STATEMENT,
                                        "statement");
        alt = new ProductionPatternAlternative();
        alt.addProduction(SUBPRODUCTION_26, 1, 1);
        alt.addToken(SeaHorseWholeConstants.SEMICOLON, 1, 1);
        pattern.addAlternative(alt);
        addPattern(pattern);

        pattern = new ProductionPattern(SeaHorseWholeConstants.CAPTURE_STATEMENT,
                                        "captureStatement");
        alt = new ProductionPatternAlternative();
        alt.addToken(SeaHorseWholeConstants.CAPTURE, 1, 1);
        alt.addProduction(SUBPRODUCTION_27, 0, 1);
        alt.addToken(SeaHorseWholeConstants.NAME, 1, 1);
        alt.addProduction(SUBPRODUCTION_29, 0, -1);
        alt.addToken(SeaHorseWholeConstants.SEMICOLON, 1, 1);
        pattern.addAlternative(alt);
        addPattern(pattern);

        pattern = new ProductionPattern(SeaHorseWholeConstants.EXPR,
                                        "expr");
        alt = new ProductionPatternAlternative();
        alt.addProduction(SeaHorseWholeConstants.UNARY, 1, 1);
        alt.addProduction(SUBPRODUCTION_31, 0, -1);
        pattern.addAlternative(alt);
        addPattern(pattern);

        pattern = new ProductionPattern(SeaHorseWholeConstants.UNARY,
                                        "unary");
        alt = new ProductionPatternAlternative();
        alt.addProduction(SUBPRODUCTION_32, 0, 1);
        alt.addProduction(SeaHorseWholeConstants.UNIT, 1, 1);
        pattern.addAlternative(alt);
        addPattern(pattern);

        pattern = new ProductionPattern(SeaHorseWholeConstants.FUNC_CALL,
                                        "func_call");
        alt = new ProductionPatternAlternative();
        alt.addToken(SeaHorseWholeConstants.OP_PAREN, 1, 1);
        alt.addProduction(SUBPRODUCTION_34, 0, 1);
        alt.addToken(SeaHorseWholeConstants.CL_PAREN, 1, 1);
        pattern.addAlternative(alt);
        addPattern(pattern);

        pattern = new ProductionPattern(SeaHorseWholeConstants.CALL_ARG,
                                        "callArg");
        alt = new ProductionPatternAlternative();
        alt.addProduction(SUBPRODUCTION_35, 0, 1);
        alt.addProduction(SeaHorseWholeConstants.EXPR, 1, 1);
        pattern.addAlternative(alt);
        addPattern(pattern);

        pattern = new ProductionPattern(SeaHorseWholeConstants.PAREN_EXPR,
                                        "paren_expr");
        alt = new ProductionPatternAlternative();
        alt.addToken(SeaHorseWholeConstants.OP_PAREN, 1, 1);
        alt.addProduction(SeaHorseWholeConstants.EXPR, 1, 1);
        alt.addToken(SeaHorseWholeConstants.CL_PAREN, 1, 1);
        pattern.addAlternative(alt);
        addPattern(pattern);

        pattern = new ProductionPattern(SeaHorseWholeConstants.ATTR_LOOKUP,
                                        "attrLookup");
        alt = new ProductionPatternAlternative();
        alt.addToken(SeaHorseWholeConstants.DOT, 1, 1);
        alt.addProduction(SUBPRODUCTION_36, 1, 1);
        pattern.addAlternative(alt);
        addPattern(pattern);

        pattern = new ProductionPattern(SeaHorseWholeConstants.ARRAY_ACC,
                                        "array_acc");
        alt = new ProductionPatternAlternative();
        alt.addProduction(SUBPRODUCTION_37, 1, -1);
        pattern.addAlternative(alt);
        addPattern(pattern);

        pattern = new ProductionPattern(SeaHorseWholeConstants.ARRAY_LITERAL,
                                        "array_literal");
        alt = new ProductionPatternAlternative();
        alt.addToken(SeaHorseWholeConstants.OP_SQ_BRACK, 1, 1);
        alt.addProduction(SUBPRODUCTION_39, 0, 1);
        alt.addToken(SeaHorseWholeConstants.CL_SQ_BRACK, 1, 1);
        pattern.addAlternative(alt);
        addPattern(pattern);

        pattern = new ProductionPattern(SeaHorseWholeConstants.DICTIONARY,
                                        "dictionary");
        alt = new ProductionPatternAlternative();
        alt.addToken(SeaHorseWholeConstants.OP_CU_BRACK, 1, 1);
        alt.addProduction(SUBPRODUCTION_44, 0, 1);
        alt.addToken(SeaHorseWholeConstants.CL_CU_BRACK, 1, 1);
        pattern.addAlternative(alt);
        addPattern(pattern);

        pattern = new ProductionPattern(SeaHorseWholeConstants.ANON_FUNC,
                                        "anonFunc");
        alt = new ProductionPatternAlternative();
        alt.addToken(SeaHorseWholeConstants.FUNC, 1, 1);
        alt.addProduction(SUBPRODUCTION_45, 0, 1);
        alt.addToken(SeaHorseWholeConstants.OP_PAREN, 1, 1);
        alt.addProduction(SeaHorseWholeConstants.PARAMETER, 0, 1);
        alt.addProduction(SUBPRODUCTION_46, 0, -1);
        alt.addToken(SeaHorseWholeConstants.CL_PAREN, 1, 1);
        alt.addToken(SeaHorseWholeConstants.OP_CU_BRACK, 1, 1);
        alt.addProduction(SeaHorseWholeConstants.CAPTURE_STATEMENT, 0, -1);
        alt.addProduction(SUBPRODUCTION_47, 0, -1);
        alt.addToken(SeaHorseWholeConstants.CL_CU_BRACK, 1, 1);
        pattern.addAlternative(alt);
        addPattern(pattern);

        pattern = new ProductionPattern(SeaHorseWholeConstants.UNIT,
                                        "unit");
        alt = new ProductionPatternAlternative();
        alt.addProduction(SUBPRODUCTION_48, 1, 1);
        alt.addProduction(SUBPRODUCTION_49, 0, -1);
        pattern.addAlternative(alt);
        addPattern(pattern);

        pattern = new ProductionPattern(SUBPRODUCTION_1,
                                        "Subproduction1");
        pattern.setSynthetic(true);
        alt = new ProductionPatternAlternative();
        alt.addProduction(SeaHorseWholeConstants.FUNCTION_BODY, 1, 1);
        pattern.addAlternative(alt);
        alt = new ProductionPatternAlternative();
        alt.addProduction(SeaHorseWholeConstants.VARIABLE, 1, 1);
        pattern.addAlternative(alt);
        alt = new ProductionPatternAlternative();
        alt.addProduction(SeaHorseWholeConstants.DATA_DEFINITION, 1, 1);
        pattern.addAlternative(alt);
        alt = new ProductionPatternAlternative();
        alt.addProduction(SeaHorseWholeConstants.STATEMENT, 1, 1);
        pattern.addAlternative(alt);
        alt = new ProductionPatternAlternative();
        alt.addProduction(SeaHorseWholeConstants.BLOCK, 1, 1);
        pattern.addAlternative(alt);
        addPattern(pattern);

        pattern = new ProductionPattern(SUBPRODUCTION_2,
                                        "Subproduction2");
        pattern.setSynthetic(true);
        alt = new ProductionPatternAlternative();
        alt.addToken(SeaHorseWholeConstants.USE, 1, 1);
        alt.addToken(SeaHorseWholeConstants.NAME, 1, 1);
        pattern.addAlternative(alt);
        addPattern(pattern);

        pattern = new ProductionPattern(SUBPRODUCTION_3,
                                        "Subproduction3");
        pattern.setSynthetic(true);
        alt = new ProductionPatternAlternative();
        alt.addToken(SeaHorseWholeConstants.COMMA, 1, 1);
        alt.addToken(SeaHorseWholeConstants.NAME, 1, 1);
        pattern.addAlternative(alt);
        addPattern(pattern);

        pattern = new ProductionPattern(SUBPRODUCTION_4,
                                        "Subproduction4");
        pattern.setSynthetic(true);
        alt = new ProductionPatternAlternative();
        alt.addToken(SeaHorseWholeConstants.FROM, 1, 1);
        alt.addToken(SeaHorseWholeConstants.NAME, 1, 1);
        alt.addToken(SeaHorseWholeConstants.USE, 1, 1);
        alt.addToken(SeaHorseWholeConstants.NAME, 1, 1);
        alt.addProduction(SUBPRODUCTION_3, 0, -1);
        pattern.addAlternative(alt);
        addPattern(pattern);

        pattern = new ProductionPattern(SUBPRODUCTION_5,
                                        "Subproduction5");
        pattern.setSynthetic(true);
        alt = new ProductionPatternAlternative();
        alt.addProduction(SUBPRODUCTION_2, 1, 1);
        pattern.addAlternative(alt);
        alt = new ProductionPatternAlternative();
        alt.addProduction(SUBPRODUCTION_4, 1, 1);
        pattern.addAlternative(alt);
        addPattern(pattern);

        pattern = new ProductionPattern(SUBPRODUCTION_6,
                                        "Subproduction6");
        pattern.setSynthetic(true);
        alt = new ProductionPatternAlternative();
        alt.addProduction(SeaHorseWholeConstants.FUNCTION_BODY, 1, 1);
        pattern.addAlternative(alt);
        alt = new ProductionPatternAlternative();
        alt.addProduction(SeaHorseWholeConstants.CONSTRUCTOR_BODY, 1, 1);
        pattern.addAlternative(alt);
        addPattern(pattern);

        pattern = new ProductionPattern(SUBPRODUCTION_7,
                                        "Subproduction7");
        pattern.setSynthetic(true);
        alt = new ProductionPatternAlternative();
        alt.addToken(SeaHorseWholeConstants.COMMA, 1, 1);
        alt.addProduction(SeaHorseWholeConstants.PARAMETER, 1, 1);
        pattern.addAlternative(alt);
        addPattern(pattern);

        pattern = new ProductionPattern(SUBPRODUCTION_8,
                                        "Subproduction8");
        pattern.setSynthetic(true);
        alt = new ProductionPatternAlternative();
        alt.addProduction(SeaHorseWholeConstants.STATEMENT, 1, 1);
        pattern.addAlternative(alt);
        alt = new ProductionPatternAlternative();
        alt.addProduction(SeaHorseWholeConstants.VARIABLE, 1, 1);
        pattern.addAlternative(alt);
        alt = new ProductionPatternAlternative();
        alt.addProduction(SeaHorseWholeConstants.BLOCK, 1, 1);
        pattern.addAlternative(alt);
        addPattern(pattern);

        pattern = new ProductionPattern(SUBPRODUCTION_9,
                                        "Subproduction9");
        pattern.setSynthetic(true);
        alt = new ProductionPatternAlternative();
        alt.addToken(SeaHorseWholeConstants.COMMA, 1, 1);
        alt.addProduction(SeaHorseWholeConstants.PARAMETER, 1, 1);
        pattern.addAlternative(alt);
        addPattern(pattern);

        pattern = new ProductionPattern(SUBPRODUCTION_10,
                                        "Subproduction10");
        pattern.setSynthetic(true);
        alt = new ProductionPatternAlternative();
        alt.addProduction(SeaHorseWholeConstants.STATEMENT, 1, 1);
        pattern.addAlternative(alt);
        alt = new ProductionPatternAlternative();
        alt.addProduction(SeaHorseWholeConstants.VARIABLE, 1, 1);
        pattern.addAlternative(alt);
        alt = new ProductionPatternAlternative();
        alt.addProduction(SeaHorseWholeConstants.BLOCK, 1, 1);
        pattern.addAlternative(alt);
        addPattern(pattern);

        pattern = new ProductionPattern(SUBPRODUCTION_11,
                                        "Subproduction11");
        pattern.setSynthetic(true);
        alt = new ProductionPatternAlternative();
        alt.addToken(SeaHorseWholeConstants.COLON, 1, 1);
        alt.addToken(SeaHorseWholeConstants.EQUAL, 1, 1);
        alt.addProduction(SeaHorseWholeConstants.EXPR, 1, 1);
        pattern.addAlternative(alt);
        addPattern(pattern);

        pattern = new ProductionPattern(SUBPRODUCTION_12,
                                        "Subproduction12");
        pattern.setSynthetic(true);
        alt = new ProductionPatternAlternative();
        alt.addToken(SeaHorseWholeConstants.VAR, 1, 1);
        pattern.addAlternative(alt);
        alt = new ProductionPatternAlternative();
        alt.addToken(SeaHorseWholeConstants.CONST, 1, 1);
        pattern.addAlternative(alt);
        addPattern(pattern);

        pattern = new ProductionPattern(SUBPRODUCTION_13,
                                        "Subproduction13");
        pattern.setSynthetic(true);
        alt = new ProductionPatternAlternative();
        alt.addProduction(SeaHorseWholeConstants.STATEMENT, 1, 1);
        pattern.addAlternative(alt);
        alt = new ProductionPatternAlternative();
        alt.addProduction(SeaHorseWholeConstants.VARIABLE, 1, 1);
        pattern.addAlternative(alt);
        alt = new ProductionPatternAlternative();
        alt.addProduction(SeaHorseWholeConstants.BLOCK, 1, 1);
        pattern.addAlternative(alt);
        addPattern(pattern);

        pattern = new ProductionPattern(SUBPRODUCTION_14,
                                        "Subproduction14");
        pattern.setSynthetic(true);
        alt = new ProductionPatternAlternative();
        alt.addToken(SeaHorseWholeConstants.OP_CU_BRACK, 1, 1);
        alt.addProduction(SUBPRODUCTION_13, 0, -1);
        alt.addToken(SeaHorseWholeConstants.CL_CU_BRACK, 1, 1);
        pattern.addAlternative(alt);
        addPattern(pattern);

        pattern = new ProductionPattern(SUBPRODUCTION_15,
                                        "Subproduction15");
        pattern.setSynthetic(true);
        alt = new ProductionPatternAlternative();
        alt.addProduction(SeaHorseWholeConstants.STATEMENT, 1, 1);
        pattern.addAlternative(alt);
        alt = new ProductionPatternAlternative();
        alt.addProduction(SeaHorseWholeConstants.VARIABLE, 1, 1);
        pattern.addAlternative(alt);
        alt = new ProductionPatternAlternative();
        alt.addProduction(SeaHorseWholeConstants.BLOCK, 1, 1);
        pattern.addAlternative(alt);
        addPattern(pattern);

        pattern = new ProductionPattern(SUBPRODUCTION_16,
                                        "Subproduction16");
        pattern.setSynthetic(true);
        alt = new ProductionPatternAlternative();
        alt.addProduction(SeaHorseWholeConstants.STATEMENT, 1, 1);
        pattern.addAlternative(alt);
        alt = new ProductionPatternAlternative();
        alt.addProduction(SeaHorseWholeConstants.VARIABLE, 1, 1);
        pattern.addAlternative(alt);
        alt = new ProductionPatternAlternative();
        alt.addProduction(SeaHorseWholeConstants.BLOCK, 1, 1);
        pattern.addAlternative(alt);
        addPattern(pattern);

        pattern = new ProductionPattern(SUBPRODUCTION_17,
                                        "Subproduction17");
        pattern.setSynthetic(true);
        alt = new ProductionPatternAlternative();
        alt.addProduction(SeaHorseWholeConstants.STATEMENT, 1, 1);
        pattern.addAlternative(alt);
        alt = new ProductionPatternAlternative();
        alt.addProduction(SeaHorseWholeConstants.VARIABLE, 1, 1);
        pattern.addAlternative(alt);
        alt = new ProductionPatternAlternative();
        alt.addProduction(SeaHorseWholeConstants.BLOCK, 1, 1);
        pattern.addAlternative(alt);
        addPattern(pattern);

        pattern = new ProductionPattern(SUBPRODUCTION_18,
                                        "Subproduction18");
        pattern.setSynthetic(true);
        alt = new ProductionPatternAlternative();
        alt.addProduction(SeaHorseWholeConstants.STATEMENT, 1, 1);
        pattern.addAlternative(alt);
        alt = new ProductionPatternAlternative();
        alt.addProduction(SeaHorseWholeConstants.VARIABLE, 1, 1);
        pattern.addAlternative(alt);
        alt = new ProductionPatternAlternative();
        alt.addProduction(SeaHorseWholeConstants.BLOCK, 1, 1);
        pattern.addAlternative(alt);
        addPattern(pattern);

        pattern = new ProductionPattern(SUBPRODUCTION_19,
                                        "Subproduction19");
        pattern.setSynthetic(true);
        alt = new ProductionPatternAlternative();
        alt.addToken(SeaHorseWholeConstants.ELIF, 1, 1);
        alt.addToken(SeaHorseWholeConstants.OP_PAREN, 1, 1);
        alt.addProduction(SeaHorseWholeConstants.EXPR, 1, 1);
        alt.addToken(SeaHorseWholeConstants.CL_PAREN, 1, 1);
        alt.addToken(SeaHorseWholeConstants.OP_CU_BRACK, 1, 1);
        alt.addProduction(SUBPRODUCTION_18, 0, -1);
        alt.addToken(SeaHorseWholeConstants.CL_CU_BRACK, 1, 1);
        pattern.addAlternative(alt);
        addPattern(pattern);

        pattern = new ProductionPattern(SUBPRODUCTION_20,
                                        "Subproduction20");
        pattern.setSynthetic(true);
        alt = new ProductionPatternAlternative();
        alt.addProduction(SeaHorseWholeConstants.STATEMENT, 1, 1);
        pattern.addAlternative(alt);
        alt = new ProductionPatternAlternative();
        alt.addProduction(SeaHorseWholeConstants.VARIABLE, 1, 1);
        pattern.addAlternative(alt);
        alt = new ProductionPatternAlternative();
        alt.addProduction(SeaHorseWholeConstants.BLOCK, 1, 1);
        pattern.addAlternative(alt);
        addPattern(pattern);

        pattern = new ProductionPattern(SUBPRODUCTION_21,
                                        "Subproduction21");
        pattern.setSynthetic(true);
        alt = new ProductionPatternAlternative();
        alt.addToken(SeaHorseWholeConstants.ELSE, 1, 1);
        alt.addToken(SeaHorseWholeConstants.OP_CU_BRACK, 1, 1);
        alt.addProduction(SUBPRODUCTION_20, 0, -1);
        alt.addToken(SeaHorseWholeConstants.CL_CU_BRACK, 1, 1);
        pattern.addAlternative(alt);
        addPattern(pattern);

        pattern = new ProductionPattern(SUBPRODUCTION_22,
                                        "Subproduction22");
        pattern.setSynthetic(true);
        alt = new ProductionPatternAlternative();
        alt.addProduction(SeaHorseWholeConstants.STATEMENT, 1, 1);
        pattern.addAlternative(alt);
        alt = new ProductionPatternAlternative();
        alt.addProduction(SeaHorseWholeConstants.VARIABLE, 1, 1);
        pattern.addAlternative(alt);
        alt = new ProductionPatternAlternative();
        alt.addProduction(SeaHorseWholeConstants.BLOCK, 1, 1);
        pattern.addAlternative(alt);
        addPattern(pattern);

        pattern = new ProductionPattern(SUBPRODUCTION_23,
                                        "Subproduction23");
        pattern.setSynthetic(true);
        alt = new ProductionPatternAlternative();
        alt.addToken(SeaHorseWholeConstants.CONT, 1, 1);
        pattern.addAlternative(alt);
        alt = new ProductionPatternAlternative();
        alt.addToken(SeaHorseWholeConstants.BREAK, 1, 1);
        pattern.addAlternative(alt);
        alt = new ProductionPatternAlternative();
        alt.addToken(SeaHorseWholeConstants.RETURN, 1, 1);
        pattern.addAlternative(alt);
        addPattern(pattern);

        pattern = new ProductionPattern(SUBPRODUCTION_24,
                                        "Subproduction24");
        pattern.setSynthetic(true);
        alt = new ProductionPatternAlternative();
        alt.addToken(SeaHorseWholeConstants.RETURN, 1, 1);
        pattern.addAlternative(alt);
        alt = new ProductionPatternAlternative();
        alt.addToken(SeaHorseWholeConstants.THROW, 1, 1);
        pattern.addAlternative(alt);
        addPattern(pattern);

        pattern = new ProductionPattern(SUBPRODUCTION_25,
                                        "Subproduction25");
        pattern.setSynthetic(true);
        alt = new ProductionPatternAlternative();
        alt.addProduction(SUBPRODUCTION_24, 0, 1);
        alt.addProduction(SeaHorseWholeConstants.EXPR, 1, 1);
        pattern.addAlternative(alt);
        addPattern(pattern);

        pattern = new ProductionPattern(SUBPRODUCTION_26,
                                        "Subproduction26");
        pattern.setSynthetic(true);
        alt = new ProductionPatternAlternative();
        alt.addProduction(SUBPRODUCTION_23, 1, 1);
        pattern.addAlternative(alt);
        alt = new ProductionPatternAlternative();
        alt.addProduction(SUBPRODUCTION_25, 1, 1);
        pattern.addAlternative(alt);
        addPattern(pattern);

        pattern = new ProductionPattern(SUBPRODUCTION_27,
                                        "Subproduction27");
        pattern.setSynthetic(true);
        alt = new ProductionPatternAlternative();
        alt.addToken(SeaHorseWholeConstants.VAR, 1, 1);
        pattern.addAlternative(alt);
        alt = new ProductionPatternAlternative();
        alt.addToken(SeaHorseWholeConstants.CONST, 1, 1);
        pattern.addAlternative(alt);
        addPattern(pattern);

        pattern = new ProductionPattern(SUBPRODUCTION_28,
                                        "Subproduction28");
        pattern.setSynthetic(true);
        alt = new ProductionPatternAlternative();
        alt.addToken(SeaHorseWholeConstants.VAR, 1, 1);
        pattern.addAlternative(alt);
        alt = new ProductionPatternAlternative();
        alt.addToken(SeaHorseWholeConstants.CONST, 1, 1);
        pattern.addAlternative(alt);
        addPattern(pattern);

        pattern = new ProductionPattern(SUBPRODUCTION_29,
                                        "Subproduction29");
        pattern.setSynthetic(true);
        alt = new ProductionPatternAlternative();
        alt.addProduction(SUBPRODUCTION_28, 0, 1);
        alt.addToken(SeaHorseWholeConstants.COMMA, 1, 1);
        alt.addToken(SeaHorseWholeConstants.NAME, 1, 1);
        pattern.addAlternative(alt);
        addPattern(pattern);

        pattern = new ProductionPattern(SUBPRODUCTION_30,
                                        "Subproduction30");
        pattern.setSynthetic(true);
        alt = new ProductionPatternAlternative();
        alt.addToken(SeaHorseWholeConstants.EQUAL, 1, 1);
        pattern.addAlternative(alt);
        alt = new ProductionPatternAlternative();
        alt.addToken(SeaHorseWholeConstants.EQ_MULT, 1, 1);
        pattern.addAlternative(alt);
        alt = new ProductionPatternAlternative();
        alt.addToken(SeaHorseWholeConstants.EQ_ADD, 1, 1);
        pattern.addAlternative(alt);
        alt = new ProductionPatternAlternative();
        alt.addToken(SeaHorseWholeConstants.EQ_DIV, 1, 1);
        pattern.addAlternative(alt);
        alt = new ProductionPatternAlternative();
        alt.addToken(SeaHorseWholeConstants.EQ_MIN, 1, 1);
        pattern.addAlternative(alt);
        alt = new ProductionPatternAlternative();
        alt.addToken(SeaHorseWholeConstants.EQ_MOD, 1, 1);
        pattern.addAlternative(alt);
        alt = new ProductionPatternAlternative();
        alt.addToken(SeaHorseWholeConstants.BOOL_OR, 1, 1);
        pattern.addAlternative(alt);
        alt = new ProductionPatternAlternative();
        alt.addToken(SeaHorseWholeConstants.AND, 1, 1);
        pattern.addAlternative(alt);
        alt = new ProductionPatternAlternative();
        alt.addToken(SeaHorseWholeConstants.OR, 1, 1);
        pattern.addAlternative(alt);
        alt = new ProductionPatternAlternative();
        alt.addToken(SeaHorseWholeConstants.IS, 1, 1);
        pattern.addAlternative(alt);
        alt = new ProductionPatternAlternative();
        alt.addToken(SeaHorseWholeConstants.BOOL_AND, 1, 1);
        pattern.addAlternative(alt);
        alt = new ProductionPatternAlternative();
        alt.addToken(SeaHorseWholeConstants.NOT_EQ, 1, 1);
        pattern.addAlternative(alt);
        alt = new ProductionPatternAlternative();
        alt.addToken(SeaHorseWholeConstants.EQ_EQ, 1, 1);
        pattern.addAlternative(alt);
        alt = new ProductionPatternAlternative();
        alt.addToken(SeaHorseWholeConstants.GREAT, 1, 1);
        pattern.addAlternative(alt);
        alt = new ProductionPatternAlternative();
        alt.addToken(SeaHorseWholeConstants.GR_EQ, 1, 1);
        pattern.addAlternative(alt);
        alt = new ProductionPatternAlternative();
        alt.addToken(SeaHorseWholeConstants.LESS, 1, 1);
        pattern.addAlternative(alt);
        alt = new ProductionPatternAlternative();
        alt.addToken(SeaHorseWholeConstants.LS_EQ, 1, 1);
        pattern.addAlternative(alt);
        alt = new ProductionPatternAlternative();
        alt.addToken(SeaHorseWholeConstants.MINUS, 1, 1);
        pattern.addAlternative(alt);
        alt = new ProductionPatternAlternative();
        alt.addToken(SeaHorseWholeConstants.PLUS, 1, 1);
        pattern.addAlternative(alt);
        alt = new ProductionPatternAlternative();
        alt.addToken(SeaHorseWholeConstants.DIV, 1, 1);
        pattern.addAlternative(alt);
        alt = new ProductionPatternAlternative();
        alt.addToken(SeaHorseWholeConstants.MULT, 1, 1);
        pattern.addAlternative(alt);
        alt = new ProductionPatternAlternative();
        alt.addToken(SeaHorseWholeConstants.ARROW, 1, 1);
        pattern.addAlternative(alt);
        alt = new ProductionPatternAlternative();
        alt.addToken(SeaHorseWholeConstants.MOD, 1, 1);
        pattern.addAlternative(alt);
        addPattern(pattern);

        pattern = new ProductionPattern(SUBPRODUCTION_31,
                                        "Subproduction31");
        pattern.setSynthetic(true);
        alt = new ProductionPatternAlternative();
        alt.addProduction(SUBPRODUCTION_30, 1, 1);
        alt.addProduction(SeaHorseWholeConstants.UNARY, 1, 1);
        pattern.addAlternative(alt);
        addPattern(pattern);

        pattern = new ProductionPattern(SUBPRODUCTION_32,
                                        "Subproduction32");
        pattern.setSynthetic(true);
        alt = new ProductionPatternAlternative();
        alt.addToken(SeaHorseWholeConstants.BANG, 1, 1);
        pattern.addAlternative(alt);
        alt = new ProductionPatternAlternative();
        alt.addToken(SeaHorseWholeConstants.MINUS, 1, 1);
        pattern.addAlternative(alt);
        addPattern(pattern);

        pattern = new ProductionPattern(SUBPRODUCTION_33,
                                        "Subproduction33");
        pattern.setSynthetic(true);
        alt = new ProductionPatternAlternative();
        alt.addToken(SeaHorseWholeConstants.COMMA, 1, 1);
        alt.addProduction(SeaHorseWholeConstants.CALL_ARG, 1, 1);
        pattern.addAlternative(alt);
        addPattern(pattern);

        pattern = new ProductionPattern(SUBPRODUCTION_34,
                                        "Subproduction34");
        pattern.setSynthetic(true);
        alt = new ProductionPatternAlternative();
        alt.addProduction(SeaHorseWholeConstants.CALL_ARG, 1, 1);
        alt.addProduction(SUBPRODUCTION_33, 0, -1);
        pattern.addAlternative(alt);
        addPattern(pattern);

        pattern = new ProductionPattern(SUBPRODUCTION_35,
                                        "Subproduction35");
        pattern.setSynthetic(true);
        alt = new ProductionPatternAlternative();
        alt.addToken(SeaHorseWholeConstants.NAME, 1, 1);
        alt.addToken(SeaHorseWholeConstants.COLON, 1, 1);
        alt.addToken(SeaHorseWholeConstants.EQUAL, 1, 1);
        pattern.addAlternative(alt);
        addPattern(pattern);

        pattern = new ProductionPattern(SUBPRODUCTION_36,
                                        "Subproduction36");
        pattern.setSynthetic(true);
        alt = new ProductionPatternAlternative();
        alt.addToken(SeaHorseWholeConstants.NAME, 1, 1);
        pattern.addAlternative(alt);
        alt = new ProductionPatternAlternative();
        alt.addToken(SeaHorseWholeConstants.STRING, 1, 1);
        pattern.addAlternative(alt);
        addPattern(pattern);

        pattern = new ProductionPattern(SUBPRODUCTION_37,
                                        "Subproduction37");
        pattern.setSynthetic(true);
        alt = new ProductionPatternAlternative();
        alt.addToken(SeaHorseWholeConstants.OP_SQ_BRACK, 1, 1);
        alt.addProduction(SeaHorseWholeConstants.EXPR, 1, 1);
        alt.addToken(SeaHorseWholeConstants.CL_SQ_BRACK, 1, 1);
        pattern.addAlternative(alt);
        addPattern(pattern);

        pattern = new ProductionPattern(SUBPRODUCTION_38,
                                        "Subproduction38");
        pattern.setSynthetic(true);
        alt = new ProductionPatternAlternative();
        alt.addToken(SeaHorseWholeConstants.COMMA, 1, 1);
        alt.addProduction(SeaHorseWholeConstants.EXPR, 1, 1);
        pattern.addAlternative(alt);
        addPattern(pattern);

        pattern = new ProductionPattern(SUBPRODUCTION_39,
                                        "Subproduction39");
        pattern.setSynthetic(true);
        alt = new ProductionPatternAlternative();
        alt.addProduction(SeaHorseWholeConstants.EXPR, 1, 1);
        alt.addProduction(SUBPRODUCTION_38, 0, -1);
        pattern.addAlternative(alt);
        addPattern(pattern);

        pattern = new ProductionPattern(SUBPRODUCTION_40,
                                        "Subproduction40");
        pattern.setSynthetic(true);
        alt = new ProductionPatternAlternative();
        alt.addToken(SeaHorseWholeConstants.STRING, 1, 1);
        pattern.addAlternative(alt);
        alt = new ProductionPatternAlternative();
        alt.addToken(SeaHorseWholeConstants.NAME, 1, 1);
        pattern.addAlternative(alt);
        addPattern(pattern);

        pattern = new ProductionPattern(SUBPRODUCTION_41,
                                        "Subproduction41");
        pattern.setSynthetic(true);
        alt = new ProductionPatternAlternative();
        alt.addProduction(SUBPRODUCTION_40, 1, 1);
        alt.addToken(SeaHorseWholeConstants.COLON, 1, 1);
        alt.addProduction(SeaHorseWholeConstants.EXPR, 1, 1);
        pattern.addAlternative(alt);
        addPattern(pattern);

        pattern = new ProductionPattern(SUBPRODUCTION_42,
                                        "Subproduction42");
        pattern.setSynthetic(true);
        alt = new ProductionPatternAlternative();
        alt.addToken(SeaHorseWholeConstants.STRING, 1, 1);
        pattern.addAlternative(alt);
        alt = new ProductionPatternAlternative();
        alt.addToken(SeaHorseWholeConstants.NAME, 1, 1);
        pattern.addAlternative(alt);
        addPattern(pattern);

        pattern = new ProductionPattern(SUBPRODUCTION_43,
                                        "Subproduction43");
        pattern.setSynthetic(true);
        alt = new ProductionPatternAlternative();
        alt.addToken(SeaHorseWholeConstants.COMMA, 1, 1);
        alt.addProduction(SUBPRODUCTION_42, 1, 1);
        alt.addToken(SeaHorseWholeConstants.COLON, 1, 1);
        alt.addProduction(SeaHorseWholeConstants.EXPR, 1, 1);
        pattern.addAlternative(alt);
        addPattern(pattern);

        pattern = new ProductionPattern(SUBPRODUCTION_44,
                                        "Subproduction44");
        pattern.setSynthetic(true);
        alt = new ProductionPatternAlternative();
        alt.addProduction(SUBPRODUCTION_41, 1, 1);
        alt.addProduction(SUBPRODUCTION_43, 0, -1);
        pattern.addAlternative(alt);
        addPattern(pattern);

        pattern = new ProductionPattern(SUBPRODUCTION_45,
                                        "Subproduction45");
        pattern.setSynthetic(true);
        alt = new ProductionPatternAlternative();
        alt.addToken(SeaHorseWholeConstants.COLON, 1, 1);
        alt.addToken(SeaHorseWholeConstants.NAME, 1, 1);
        pattern.addAlternative(alt);
        addPattern(pattern);

        pattern = new ProductionPattern(SUBPRODUCTION_46,
                                        "Subproduction46");
        pattern.setSynthetic(true);
        alt = new ProductionPatternAlternative();
        alt.addToken(SeaHorseWholeConstants.COMMA, 1, 1);
        alt.addProduction(SeaHorseWholeConstants.PARAMETER, 1, 1);
        pattern.addAlternative(alt);
        addPattern(pattern);

        pattern = new ProductionPattern(SUBPRODUCTION_47,
                                        "Subproduction47");
        pattern.setSynthetic(true);
        alt = new ProductionPatternAlternative();
        alt.addProduction(SeaHorseWholeConstants.STATEMENT, 1, 1);
        pattern.addAlternative(alt);
        alt = new ProductionPatternAlternative();
        alt.addProduction(SeaHorseWholeConstants.VARIABLE, 1, 1);
        pattern.addAlternative(alt);
        alt = new ProductionPatternAlternative();
        alt.addProduction(SeaHorseWholeConstants.BLOCK, 1, 1);
        pattern.addAlternative(alt);
        addPattern(pattern);

        pattern = new ProductionPattern(SUBPRODUCTION_48,
                                        "Subproduction48");
        pattern.setSynthetic(true);
        alt = new ProductionPatternAlternative();
        alt.addToken(SeaHorseWholeConstants.INTEGER, 1, 1);
        pattern.addAlternative(alt);
        alt = new ProductionPatternAlternative();
        alt.addToken(SeaHorseWholeConstants.DOUBLE, 1, 1);
        pattern.addAlternative(alt);
        alt = new ProductionPatternAlternative();
        alt.addToken(SeaHorseWholeConstants.STRING, 1, 1);
        pattern.addAlternative(alt);
        alt = new ProductionPatternAlternative();
        alt.addToken(SeaHorseWholeConstants.TRUE, 1, 1);
        pattern.addAlternative(alt);
        alt = new ProductionPatternAlternative();
        alt.addToken(SeaHorseWholeConstants.FALSE, 1, 1);
        pattern.addAlternative(alt);
        alt = new ProductionPatternAlternative();
        alt.addToken(SeaHorseWholeConstants.NULL, 1, 1);
        pattern.addAlternative(alt);
        alt = new ProductionPatternAlternative();
        alt.addToken(SeaHorseWholeConstants.NAME, 1, 1);
        pattern.addAlternative(alt);
        alt = new ProductionPatternAlternative();
        alt.addToken(SeaHorseWholeConstants.MODULE, 1, 1);
        pattern.addAlternative(alt);
        alt = new ProductionPatternAlternative();
        alt.addToken(SeaHorseWholeConstants.SELF, 1, 1);
        pattern.addAlternative(alt);
        alt = new ProductionPatternAlternative();
        alt.addProduction(SeaHorseWholeConstants.PAREN_EXPR, 1, 1);
        pattern.addAlternative(alt);
        alt = new ProductionPatternAlternative();
        alt.addProduction(SeaHorseWholeConstants.ARRAY_LITERAL, 1, 1);
        pattern.addAlternative(alt);
        alt = new ProductionPatternAlternative();
        alt.addProduction(SeaHorseWholeConstants.ANON_FUNC, 1, 1);
        pattern.addAlternative(alt);
        alt = new ProductionPatternAlternative();
        alt.addProduction(SeaHorseWholeConstants.DICTIONARY, 1, 1);
        pattern.addAlternative(alt);
        addPattern(pattern);

        pattern = new ProductionPattern(SUBPRODUCTION_49,
                                        "Subproduction49");
        pattern.setSynthetic(true);
        alt = new ProductionPatternAlternative();
        alt.addProduction(SeaHorseWholeConstants.ATTR_LOOKUP, 1, 1);
        pattern.addAlternative(alt);
        alt = new ProductionPatternAlternative();
        alt.addProduction(SeaHorseWholeConstants.ARRAY_ACC, 1, 1);
        pattern.addAlternative(alt);
        alt = new ProductionPatternAlternative();
        alt.addProduction(SeaHorseWholeConstants.FUNC_CALL, 1, 1);
        pattern.addAlternative(alt);
        addPattern(pattern);
    }
}
